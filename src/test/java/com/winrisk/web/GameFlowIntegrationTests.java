package com.winrisk.web;

import com.winrisk.web.api.dto.GameView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end proof of the live-AI-turn animation: create a game over REST,
 * subscribe to its topic over STOMP, end the human's phases and observe a
 * stream of views with increasing versions until control returns to the human.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "winrisk.ai-step-millis=1",
        "winrisk.saves-dir=build/it-saves",
        "winrisk.maps-dir=build/it-maps"
})
class GameFlowIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void websocketStreamsAiTurnsUntilHumanControl() throws Exception {
        ResponseEntity<Map> created = rest.postForEntity("/api/games",
                Map.of("mode", "classic", "aiPlayers", 3,
                        "map", Map.of("builtin", "world"), "seed", 42),
                Map.class);
        assertEquals(201, created.getStatusCode().value());
        String id = (String) created.getBody().get("id");

        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<GameView> frames = new LinkedBlockingQueue<>();
        StompSession session = stomp.connectAsync("ws://localhost:" + port + "/ws",
                new StompSessionHandlerAdapter() {
                }).get(10, TimeUnit.SECONDS);
        session.subscribe("/topic/games/" + id, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return GameView.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                frames.add((GameView) payload);
            }
        });

        // Wait for the human's turn, then run the whole human turn via REST.
        GameView view = awaitHumanTurn(id);
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/games/" + id + "/end-phase", Map.of(), Map.class);
        }

        // First drain the human's own end-phase echoes until an AI player
        // holds control; then the stepper's frames stream in with strictly
        // increasing versions until control genuinely returns to the human.
        long lastVersion = -1;
        boolean aiSeen = false;
        boolean humanAgain = false;
        int aiFrames = 0;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            GameView frame = frames.poll(2, TimeUnit.SECONDS);
            if (frame == null) {
                break;
            }
            assertTrue(frame.version > lastVersion,
                    "frame versions must increase (got " + frame.version
                            + " after " + lastVersion + ")");
            lastVersion = frame.version;
            if (frame.winnerIndex >= 0) {
                humanAgain = true;
                break;
            }
            if (frame.currentPlayerIndex != frame.humanPlayerIndex) {
                aiSeen = true;
                aiFrames++;
            } else if (aiSeen) {
                humanAgain = true;
                break;
            }
        }
        assertTrue(aiFrames >= 2, "expected a stream of AI-turn frames, got " + aiFrames);
        assertTrue(humanAgain, "control should return to the human (or the game end)");
        session.disconnect();
    }

    private GameView awaitHumanTurn(String id) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            GameView view = rest.getForObject("/api/games/" + id, GameView.class);
            if (view.winnerIndex >= 0
                    || view.currentPlayerIndex == view.humanPlayerIndex) {
                return view;
            }
            Thread.sleep(5);
        }
        throw new AssertionError("human never got control");
    }

    @Test
    void mapsEndpointServesBuiltinBoards() {
        ResponseEntity<Map> maps = rest.getForEntity("/api/maps", Map.class);
        assertEquals(200, maps.getStatusCode().value());
        assertTrue(((List<?>) maps.getBody().get("builtin")).contains("pangaea"));
    }
}
