package com.winrisk.game.view;

import com.winrisk.game.data.MotionEvent;

public interface Viewable {
    void onAction();

    void onClose();

    void onDraw(GameSurface surface);

    void onEvent(MotionEvent event);

    void onSave(String path);
}
