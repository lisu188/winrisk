package com.winrisk.web.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "winrisk.ai-step-millis=1",
        "winrisk.saves-dir=build/test-saves",
        "winrisk.maps-dir=build/test-maps"
})
class GameControllerTests {

    @Autowired
    private MockMvc mvc;

    private String createGame() throws Exception {
        String body = mvc.perform(post("/api/games")
                        .contentType("application/json")
                        .content("{\"mode\":\"classic\",\"aiPlayers\":3," +
                                "\"map\":{\"builtin\":\"world\"},\"seed\":42}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andExpect(jsonPath("$.view.fields", hasSize(42)))
                .andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void mapsEndpointListsBuiltinBoards() throws Exception {
        mvc.perform(get("/api/maps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.builtin", hasItems(
                        "world", "pangaea", "laurasia", "gondwana", "rodinia")));
    }

    @Test
    void gameLifecycleThroughHttp() throws Exception {
        String id = createGame();

        mvc.perform(get("/api/games/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(4)))
                .andExpect(jsonPath("$.humanPlayerIndex", is(0)));

        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(get("/api/games/" + id + "/background"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        mvc.perform(delete("/api/games/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/games/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString(id)));
    }

    @Test
    void backgroundIs404ForImagelessBoards() throws Exception {
        String body = mvc.perform(post("/api/games")
                        .contentType("application/json")
                        .content("{\"mode\":\"classic\",\"aiPlayers\":3," +
                                "\"map\":{\"builtin\":\"pangaea\"},\"seed\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/games/" + id + "/background"))
                .andExpect(status().isNotFound());
    }

    @Test
    void badRequestsAndConflictsAreMapped() throws Exception {
        String id = createGame();

        mvc.perform(post("/api/games/" + id + "/place")
                        .contentType("application/json")
                        .content("{\"fieldIndex\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("fieldIndex")));

        mvc.perform(post("/api/games/" + id + "/attack")
                        .contentType("application/json")
                        .content("{\"from\":0,\"to\":1}"))
                .andExpect(status().is(anyOf(is(409), is(400))));

        mvc.perform(post("/api/games")
                        .contentType("application/json")
                        .content("{\"mode\":\"bogus\",\"aiPlayers\":3}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/games/" + id + "/save")
                        .contentType("application/json")
                        .content("{\"name\":\"../evil\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownGameIs404Everywhere() throws Exception {
        mvc.perform(get("/api/games/nope")).andExpect(status().isNotFound());
        mvc.perform(post("/api/games/nope/end-phase")).andExpect(status().isNotFound());
        mvc.perform(get("/api/games/nope/trade-sets")).andExpect(status().isNotFound());
    }
}
