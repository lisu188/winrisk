package com.winrisk.web.game;

import com.winrisk.game.data.GameMode;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.CombatResult;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.view.Game;
import com.winrisk.web.api.dto.AttackResponse;
import com.winrisk.web.api.dto.CreateGameRequest;
import com.winrisk.web.api.dto.GameSummary;
import com.winrisk.web.api.dto.GameView;
import com.winrisk.web.api.dto.TradeSetView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hosts the running games. Every mutation validates and executes under the
 * session's lock, then broadcasts the fresh view to the game's topic.
 */
@Service
public class GameService {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final AiStepper stepper;
    private final SaveStore saveStore;
    private final SimpMessagingTemplate messaging;
    private final long idleEvictionHours;

    public GameService(AiStepper stepper,
                       SaveStore saveStore,
                       SimpMessagingTemplate messaging,
                       @Value("${winrisk.session-idle-eviction-hours:24}") long idleEvictionHours) {
        this.stepper = stepper;
        this.saveStore = saveStore;
        this.messaging = messaging;
        this.idleEvictionHours = idleEvictionHours;
    }

    public GameView create(CreateGameRequest request) {
        Params params = toParams(request);
        Game game = new Game(params);
        GameSession session = register(game, params.getHumanPlayers() > 0 ? 0 : -1);
        startAiIfNeeded(session);
        return session.view();
    }

    public GameView load(String saveName) {
        Game game = Game.loadGame(saveStore.existingSaveFile(saveName).getAbsolutePath());
        int humanIndex = -1;
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getPlayerInterface().isInteractive()) {
                humanIndex = i;
                break;
            }
        }
        GameSession session = register(game, humanIndex);
        startAiIfNeeded(session);
        return session.view();
    }

    public void save(String id, String saveName) {
        GameSession session = require(id);
        saveStore.ensureSavesDir();
        String path = saveStore.saveFile(saveName).getAbsolutePath();
        session.withLock(game -> {
            game.save(path);
            return null;
        });
        session.touch();
    }

    public GameView view(String id) {
        return require(id).view();
    }

    public byte[] backgroundImage(String id) {
        return require(id).withLock(game -> game.getMap().getImage());
    }

    public List<GameSummary> list() {
        List<GameSummary> summaries = new ArrayList<>();
        for (GameSession session : sessions.values()) {
            GameView view = session.view();
            GameSummary summary = new GameSummary();
            summary.id = view.id;
            summary.mode = view.mode;
            summary.mapName = view.fields.size() + " territories";
            summary.players = view.players.size();
            summary.phase = view.phase;
            summary.winnerIndex = view.winnerIndex;
            summary.createdAt = session.getCreatedAt().toString();
            summaries.add(summary);
        }
        summaries.sort(Comparator.comparing(summary -> summary.createdAt));
        return summaries;
    }

    public void delete(String id) {
        GameSession session = require(id);
        stepper.cancel(session);
        sessions.remove(id);
    }

    public GameView place(String id, int fieldIndex, boolean all) {
        GameSession session = require(id);
        GameView view = session.mutate(game -> {
            requireHumanTurn(game, session);
            requirePhase(game, GamePhase.REINFORCE, "place reinforcements");
            Field field = requireField(game, fieldIndex);
            boolean ok = all ? game.onLongClick(field) : game.onShortClick(field);
            if (!ok) {
                throw new IllegalActionException("You can only reinforce your own territory");
            }
            return null;
        });
        broadcast(session, view);
        return view;
    }

    public AttackResponse attack(String id, int from, int to) {
        GameSession session = require(id);
        CombatResult[] outcome = new CombatResult[1];
        GameView view = session.mutate(game -> {
            requireHumanTurn(game, session);
            requirePhase(game, GamePhase.ATTACK, "attack");
            Field source = requireField(game, from);
            Field target = requireField(game, to);
            if (source.getPlayer() != game.getPlayer()) {
                throw new IllegalActionException("Attack must start from your own territory");
            }
            if (target.getPlayer() == game.getPlayer()) {
                throw new IllegalActionException("You cannot attack your own territory");
            }
            if (!source.getNext().contains(target)) {
                throw new IllegalActionException("Territories are not adjacent");
            }
            if (source.getArmy() < 2) {
                throw new IllegalActionException("Attacking needs at least 2 armies");
            }
            CombatResult result = game.attack(source, target);
            if (!result.isLegal()) {
                throw new IllegalActionException("Illegal attack");
            }
            outcome[0] = result;
            return null;
        });
        broadcast(session, view);
        session.touch();
        AttackResponse response = new AttackResponse();
        response.view = view;
        response.combat = new AttackResponse.CombatView();
        response.combat.captured = outcome[0].isCaptured();
        response.combat.attackerDice = outcome[0].getAttackerDice();
        response.combat.defenderDice = outcome[0].getDefenderDice();
        response.combat.attackerLosses = outcome[0].getAttackerLosses();
        response.combat.defenderLosses = outcome[0].getDefenderLosses();
        return response;
    }

    public GameView maneuver(String id, int from, int to, int troops) {
        GameSession session = require(id);
        GameView view = session.mutate(game -> {
            requireHumanTurn(game, session);
            requirePhase(game, GamePhase.MOVE, "maneuver");
            Field source = requireField(game, from);
            Field target = requireField(game, to);
            if (troops < 1) {
                throw new IllegalActionException("Troop count must be at least 1");
            }
            if (!game.maneuver(source, target, troops)) {
                throw new IllegalActionException(
                        "Illegal maneuver: territories must both be yours, connected, "
                                + "and only one fortification route is allowed per turn");
            }
            return null;
        });
        broadcast(session, view);
        session.touch();
        return view;
    }

    public GameView endPhase(String id) {
        GameSession session = require(id);
        GameView view = session.mutate(game -> {
            requireHumanTurn(game, session);
            game.next();
            return null;
        });
        broadcast(session, view);
        session.touch();
        startAiIfNeeded(session);
        return view;
    }

    public List<TradeSetView> tradeSets(String id) {
        GameSession session = require(id);
        return session.withLock(game -> {
            Player human = humanPlayer(game, session);
            List<TradeSetView> sets = new ArrayList<>();
            for (List<RiskCard> set : game.getCardService().findTradeSets(human.getRiskCards())) {
                TradeSetView view = new TradeSetView();
                view.cardIndexes = set.stream()
                        .map(card -> human.getRiskCards().indexOf(card))
                        .mapToInt(Integer::intValue)
                        .toArray();
                view.symbols = set.stream().map(card -> card.getSymbol().name())
                        .toArray(String[]::new);
                view.territoryNames = set.stream().map(RiskCard::getTerritoryName)
                        .toArray(String[]::new);
                view.value = game.getCardService().nextTradeValue();
                sets.add(view);
            }
            return sets;
        });
    }

    public GameView trade(String id, int[] cardIndexes) {
        GameSession session = require(id);
        GameView view = session.mutate(game -> {
            requireHumanTurn(game, session);
            requirePhase(game, GamePhase.REINFORCE, "trade cards");
            Player human = humanPlayer(game, session);
            if (cardIndexes == null || cardIndexes.length != 3) {
                throw new IllegalArgumentException("A trade needs exactly 3 card indexes");
            }
            List<RiskCard> cards = new ArrayList<>();
            for (int index : cardIndexes) {
                if (index < 0 || index >= human.getRiskCards().size()) {
                    throw new IllegalArgumentException("Card index out of range: " + index);
                }
                cards.add(human.getRiskCards().get(index));
            }
            game.getCardService().trade(game, human, cards);
            return null;
        });
        broadcast(session, view);
        session.touch();
        return view;
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void evictIdleSessions() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(idleEvictionHours));
        for (GameSession session : new ArrayList<>(sessions.values())) {
            if (session.getLastActivity().isBefore(cutoff)) {
                stepper.cancel(session);
                sessions.remove(session.getId());
            }
        }
    }

    int sessionCount() {
        return sessions.size();
    }

    private GameSession register(Game game, int humanIndex) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(id, game, humanIndex);
        sessions.put(id, session);
        return session;
    }

    private void startAiIfNeeded(GameSession session) {
        boolean aiTurn = session.withLock(game ->
                !game.end() && !game.getPlayer().getPlayerInterface().isInteractive());
        if (aiTurn) {
            stepper.ensureStepping(session, view -> broadcast(session, view));
        }
    }

    private void broadcast(GameSession session, GameView view) {
        if (messaging != null) {
            messaging.convertAndSend("/topic/games/" + session.getId(), view);
        }
    }

    private GameSession require(String id) {
        GameSession session = sessions.get(id);
        if (session == null) {
            throw new GameNotFoundException(id);
        }
        return session;
    }

    private void requireHumanTurn(Game game, GameSession session) {
        if (game.end()) {
            throw new IllegalActionException("The game is over");
        }
        if (game.getCurPlayer() != session.getHumanPlayerIndex()
                || !game.getPlayer().getPlayerInterface().isInteractive()) {
            throw new IllegalActionException("It is not your turn");
        }
    }

    private void requirePhase(Game game, GamePhase phase, String action) {
        if (game.getPhase() != phase) {
            throw new IllegalActionException(
                    "You can only " + action + " during " + phase + " (current: "
                            + game.getPhase() + ")");
        }
    }

    private Field requireField(Game game, int index) {
        if (index < 0 || index >= game.getFields().size()) {
            throw new IllegalArgumentException("Field index out of range: " + index);
        }
        return game.getFields().get(index);
    }

    private Player humanPlayer(Game game, GameSession session) {
        int index = session.getHumanPlayerIndex();
        if (index < 0 || index >= game.getPlayers().size()) {
            throw new IllegalActionException("This game has no human seat");
        }
        return game.getPlayers().get(index);
    }

    private Params toParams(CreateGameRequest request) {
        Params params = new Params();
        params.setHumanPlayers(1);
        int aiPlayers = request.aiPlayers <= 0 ? 3 : request.aiPlayers;
        params.setAiPlayers(aiPlayers);
        if (request.mode != null) {
            params.setGameMode(GameMode.fromCli(request.mode));
        }
        if (request.seed != null) {
            params.setRandomSeed(request.seed);
        }
        if (request.rules != null) {
            params.setAttackWithAll(request.rules.attackWithAll);
            params.setFogOfWar(request.rules.fogOfWar);
            params.setSkynetMode(request.rules.skynet);
            params.getRulesOptions()
                    .setIncrementalCardSetValues(request.rules.incrementalCardSetValues);
            params.getRulesOptions().setExpandedManeuver(request.rules.expandedManeuver);
            params.getRulesOptions().setAttackCardReroll(request.rules.attackCardReroll);
            params.getRulesOptions().setCommanderDie(request.rules.commanderDie);
        }
        if (request.map != null) {
            if (request.map.builtin != null) {
                params.setBuiltinMap(request.map.builtin);
            } else if (request.map.file != null) {
                params.setMap(saveStore.mapFile(request.map.file).getAbsolutePath());
            } else if (request.map.random != null) {
                params.setRandomMap(true);
                if (request.map.random.fields > 0) {
                    params.setRandomFields(request.map.random.fields);
                }
                if (request.map.random.continents > 0) {
                    params.setRandomContinents(request.map.random.continents);
                }
            }
        }
        return params;
    }
}
