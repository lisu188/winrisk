package com.winrisk.gui;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

public class BoardViewportTests {

    private FieldList fieldsAt(int... xy) {
        FieldList fields = new FieldList();
        for (int i = 0; i < xy.length; i += 2) {
            fields.add(new Field(new PointF(xy[i], xy[i + 1])));
        }
        return fields;
    }

    @Test
    public void defaultBoardCoversAuthoringSpace() {
        BoardViewport viewport = BoardViewport.defaultBoard();
        assertEquals(0, viewport.getBoardX());
        assertEquals(0, viewport.getBoardY());
        assertEquals(900, viewport.getBoardWidth());
        assertEquals(600, viewport.getBoardHeight());
    }

    @Test
    public void forFieldsKeepsDefaultRectWhenFieldsFitInside() {
        BoardViewport viewport = BoardViewport.forFields(fieldsAt(100, 100, 700, 500));
        assertEquals(0, viewport.getBoardX());
        assertEquals(900, viewport.getBoardWidth());
        assertEquals(600, viewport.getBoardHeight());
    }

    @Test
    public void forFieldsGrowsRectAroundOutlyingFields() {
        BoardViewport viewport = BoardViewport.forFields(fieldsAt(-20, 100, 1000, 700));
        // 50-unit margin on each side of the outliers.
        assertEquals(-70, viewport.getBoardX());
        assertEquals(1050 - -70, viewport.getBoardWidth());
        assertEquals(750, viewport.getBoardY() + viewport.getBoardHeight());
    }

    @Test
    public void forFieldsFallsBackToDefaultWhenEmptyOrNull() {
        assertEquals(900, BoardViewport.forFields(new FieldList()).getBoardWidth());
        assertEquals(900, BoardViewport.forFields(null).getBoardWidth());
    }

    @Test
    public void wideScreenLetterboxesHorizontally() {
        BoardViewport viewport = BoardViewport.defaultBoard();
        viewport.setScreenSize(1800, 600);
        assertEquals(1f, viewport.getScale(), 0.0001f);
        // Board centred: 450px bars on either side.
        assertEquals(450, viewport.boardToScreenX(0));
        assertEquals(1350, viewport.boardToScreenX(900));
        assertEquals(0, viewport.boardToScreenY(0));
    }

    @Test
    public void tallScreenLetterboxesVertically() {
        BoardViewport viewport = BoardViewport.defaultBoard();
        viewport.setScreenSize(900, 1200);
        assertEquals(1f, viewport.getScale(), 0.0001f);
        assertEquals(300, viewport.boardToScreenY(0));
        assertEquals(0, viewport.boardToScreenX(0));
    }

    @Test
    public void scalingIsUniformAndRoundTrips() {
        BoardViewport viewport = BoardViewport.defaultBoard();
        viewport.setScreenSize(450, 300);
        assertEquals(0.5f, viewport.getScale(), 0.0001f);
        for (int x : new int[]{0, 123, 456, 900}) {
            int roundTrip = viewport.screenToBoardX(viewport.boardToScreenX(x));
            assertTrue("round trip within a pixel", Math.abs(roundTrip - x) <= 1);
        }
        for (int y : new int[]{0, 77, 599}) {
            int roundTrip = viewport.screenToBoardY(viewport.boardToScreenY(y));
            assertTrue("round trip within a pixel", Math.abs(roundTrip - y) <= 1);
        }
    }

    @Test
    public void zeroScreenSizeClampsInsteadOfDividingByZero() {
        BoardViewport viewport = BoardViewport.defaultBoard();
        viewport.setScreenSize(0, 0);
        assertEquals(1f, viewport.getScale(), 0.0001f);
        assertEquals(42, viewport.boardToScreenX(42));
        assertEquals(42, viewport.screenToBoardX(42));
    }
}
