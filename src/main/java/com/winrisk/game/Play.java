package com.winrisk.game;

import com.winrisk.game.data.Params;
import com.winrisk.game.object.Player;
import com.winrisk.game.view.Game;

public class Play {

    private final Params params;
    private final int maxTurns;

    public Play(Params params) {
        this(params, 5000);
    }

    public Play(Params params, int maxTurns) {
        this.params = params;
        this.maxTurns = maxTurns;
    }

    public Player play() {
        return playResult().getWinner();
    }

    public Result playResult() {
        if (maxTurns <= 0) {
            throw new IllegalStateException(
                    "Game did not finish within " + maxTurns + " turns");
        }
        Game game = new Game(params);
        int completedTurns = 0;
        int phaseSteps = 0;
        int maxPhaseSteps = maxTurns * Math.max(1, game.getPlayers().size()) * 3;
        while (!game.end()) {
            if (phaseSteps++ >= maxPhaseSteps) {
                throw new IllegalStateException(
                        "Game did not finish within " + maxTurns + " turns");
            }
            if (game.getPhase() == com.winrisk.game.data.GamePhase.MOVE) {
                completedTurns++;
            }
            game.next();
        }
        return new Result(params, completedTurns, getWinner(game), game.getWinReason());
    }

    public static Player playHeadless(Params params) {
        return new Play(params).play();
    }

    public static Player playWithDefaults() {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        return playHeadless(params);
    }

    private Player getWinner(Game game) {
        return game.getWinner();
    }

    public static void main(String[] args) {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(3);
        Result result = new Play(params).playResult();
        System.out.println(result.toReport());
    }

    public static class Result {
        private final Params params;
        private final int turns;
        private final Player winner;
        private final String winReason;

        Result(Params params, int turns, Player winner, String winReason) {
            this.params = params;
            this.turns = turns;
            this.winner = winner;
            this.winReason = winReason;
        }

        public Params getParams() {
            return params;
        }

        public int getTurns() {
            return turns;
        }

        public Player getWinner() {
            return winner;
        }

        public String getWinReason() {
            return winReason;
        }

        public String toReport() {
            return "mode=" + params.getGameMode().toCliValue()
                    + ", seed=" + (params.getRandomSeed() == null ? "random" : params.getRandomSeed())
                    + ", turns=" + turns
                    + ", winner=" + (winner == null ? "none" : winner.getColor())
                    + ", reason=" + winReason;
        }
    }
}
