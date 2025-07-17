package com.winrisk.game.view;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.util.ArrayList;

public class FieldDetectorTest {
    private static class Dummy implements FieldListener {
        FieldList list = new FieldList();
        @Override public FieldList getFieldList() { return list; }
        @Override public boolean onFieldDrag(Field from, Field to) { return true; }
        @Override public boolean onFromField(Field field) { return true; }
        @Override public boolean onLongClick(Field field) { return true; }
        @Override public boolean onShortClick(Field field) { return true; }
        @Override public boolean onToField(Field field) { return true; }
        @Override public boolean onVoidClick(PointF point) { return true; }
        @Override public boolean onVoidDrag(PointF from, PointF to) { return true; }
    }

    @Test
    public void testFeed() {
        Dummy d = new Dummy();
        d.list.add(new Field(new PointF(0,0)));
        FieldDetector detector = new FieldDetector(d);
        long t = System.currentTimeMillis();
        MotionEvent down = new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0);
        MotionEvent up = new MotionEvent(MotionEvent.ACTION_UP, t+600, t+600, 0,0);
        detector.feed(down);
        detector.feed(up);
    }
}
