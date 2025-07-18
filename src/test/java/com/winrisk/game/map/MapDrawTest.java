package com.winrisk.game.map;

import com.winrisk.game.view.GameSurface;
import com.winrisk.game.TestUtil;
import org.junit.Test;

public class MapDrawTest {
    @Test
    public void testDraw() throws Exception {
        Map map = TestUtil.createNewGame().getMap();
        GameSurface surface = new GameSurface() {
            @Override public void drawBackground(byte[] b) {}
            @Override public void drawLine(int x,int y,int x2,int y2) {}
            @Override public void drawOval(int x,int y,int w,int h) {}
            @Override public void drawString(String s,int x,int y) {}
            @Override public void setColor(java.awt.Color c) {}
        };
        map.draw(surface, null);
    }
}
