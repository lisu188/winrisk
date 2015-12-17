package com.winrisk.game.data;

public class MotionEvent {

    public static final int ACTION_DOWN = 0;

    public static final int ACTION_UP = 1;

    private final int action;

    private final long downTime;

    private final long eventTime;

    private final int x;

    private final int y;

    public MotionEvent(int action, long l, long m, int x, int y) {
        super();
        this.action = action;
        this.downTime = l;
        this.eventTime = m;
        this.x = x;
        this.y = y;
    }

    public int getAction() {
        return action;
    }

    public long getDownTime() {
        return downTime;
    }

    public long getEventTime() {
        return eventTime;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
