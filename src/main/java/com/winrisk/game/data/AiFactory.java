package com.winrisk.game.data;

import com.winrisk.game.ai.PlayerInterface;

public interface AiFactory {
    PlayerInterface getAi(int i);
}
