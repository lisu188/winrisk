package com.winrisk.game.serialization;

public interface Saveable {
    void fromSketch(Sketch sketch);

    Class<? extends Sketch> getSketchClass();

    Sketch toSketch();
}
