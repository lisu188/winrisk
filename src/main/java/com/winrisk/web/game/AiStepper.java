package com.winrisk.web.game;

import com.winrisk.web.api.dto.GameView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Advances AI turns one phase at a time on a schedule, so connected clients
 * see the opponents play out live over the WebSocket instead of the whole AI
 * round resolving in a single frame.
 */
@Component
public class AiStepper {

    private final ScheduledExecutorService scheduler;
    private final long stepMillis;

    public AiStepper(ScheduledExecutorService aiScheduler,
                     @Value("${winrisk.ai-step-millis:400}") long stepMillis) {
        this.scheduler = aiScheduler;
        this.stepMillis = stepMillis;
    }

    /**
     * Starts stepping the session's AI turns if not already running. Each tick
     * advances one phase and hands the fresh view to {@code broadcast}; the
     * task cancels itself once the game ends or control returns to a human.
     */
    public void ensureStepping(GameSession session, Consumer<GameView> broadcast) {
        if (session.getStepTask().get() != null) {
            return;
        }
        Runnable tick = () -> {
            GameView view = null;
            try {
                view = session.stepAi();
            } catch (RuntimeException e) {
                cancel(session);
                return;
            }
            if (view == null) {
                cancel(session);
                return;
            }
            broadcast.accept(view);
        };
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                tick, 0, stepMillis, TimeUnit.MILLISECONDS);
        if (!session.getStepTask().compareAndSet(null, future)) {
            future.cancel(false);
        }
    }

    public void cancel(GameSession session) {
        ScheduledFuture<?> future = session.getStepTask().getAndSet(null);
        if (future != null) {
            future.cancel(false);
        }
    }
}
