package com.winrisk.web.game;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(String id) {
        super("No such game: " + id);
    }
}
