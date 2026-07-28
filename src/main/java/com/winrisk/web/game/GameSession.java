package com.winrisk.web.game;

import com.winrisk.game.view.Game;
import com.winrisk.web.api.dto.GameView;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * One hosted game. The engine is not thread-safe, so every read or mutation of
 * the {@link Game} goes through {@link #withLock}; the AI stepper and REST
 * handlers therefore never interleave within a session.
 */
public class GameSession {

    private final String id;
    private final Game game;
    private final int humanPlayerIndex;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicReference<ScheduledFuture<?>> stepTask = new AtomicReference<>();
    private long version;
    private volatile Instant lastActivity = Instant.now();
    private final Instant createdAt = Instant.now();

    public GameSession(String id, Game game, int humanPlayerIndex) {
        this.id = id;
        this.game = game;
        this.humanPlayerIndex = humanPlayerIndex;
    }

    public String getId() {
        return id;
    }

    public int getHumanPlayerIndex() {
        return humanPlayerIndex;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void touch() {
        lastActivity = Instant.now();
    }

    public AtomicReference<ScheduledFuture<?>> getStepTask() {
        return stepTask;
    }

    public <T> T withLock(Function<Game, T> action) {
        lock.lock();
        try {
            return action.apply(game);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the action under the lock and returns a fresh view with a bumped
     * version. Use for every mutation.
     */
    public GameView mutate(Function<Game, ?> action) {
        lock.lock();
        try {
            action.apply(game);
            version++;
            return GameViewMapper.map(id, version, humanPlayerIndex, game);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Maps the current state under the lock without bumping the version.
     */
    public GameView view() {
        lock.lock();
        try {
            return GameViewMapper.map(id, version, humanPlayerIndex, game);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Advances one AI phase. Returns the fresh view, or {@code null} when the
     * game is over or control is back with an interactive player (the caller
     * should stop stepping).
     */
    public GameView stepAi() {
        lock.lock();
        try {
            if (game.end() || game.getPlayer().getPlayerInterface().isInteractive()) {
                return null;
            }
            game.next();
            version++;
            return GameViewMapper.map(id, version, humanPlayerIndex, game);
        } finally {
            lock.unlock();
        }
    }
}
