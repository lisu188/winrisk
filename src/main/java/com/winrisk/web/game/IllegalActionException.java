package com.winrisk.web.game;

/**
 * A request that is well-formed but not legal right now (wrong phase, not the
 * human's turn, illegal move). Mapped to HTTP 409.
 */
public class IllegalActionException extends RuntimeException {
    public IllegalActionException(String message) {
        super(message);
    }
}
