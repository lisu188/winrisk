package com.winrisk.game.view;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import static org.junit.Assert.*;

public class FieldDetectorExtendedTest {
    private static class Dummy implements FieldListener {
        FieldList list = new FieldList();
        String last = "";
        @Override public FieldList getFieldList() { return list; }
        @Override public boolean onFieldDrag(Field from, Field to) { last = "drag"; return true; }
        @Override public boolean onFromField(Field field) { last = "from"; return true; }
        @Override public boolean onLongClick(Field field) { last = "long"; return true; }
        @Override public boolean onShortClick(Field field) { last = "short"; return true; }
        @Override public boolean onToField(Field field) { last = "to"; return true; }
        @Override public boolean onVoidClick(PointF point) { last = "voidClick"; return true; }
        @Override public boolean onVoidDrag(PointF from, PointF to) { last = "voidDrag"; return true; }
    }

    @Test
    public void testVariousInteractions() {
        Dummy d = new Dummy();
        Field a = new Field(new PointF(0,0));
        Field b = new Field(new PointF(30,0));
        d.list.add(a);
        d.list.add(b);
        FieldDetector detector = new FieldDetector(d);
        long t = System.currentTimeMillis();
        // short click
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 0,0));
        assertEquals("short", d.last);
        // long click
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+600, 0,0));
        assertEquals("long", d.last);
        // drag to another field
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 30,0));
        assertEquals("drag", d.last);
        // void drag
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 100,100));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 150,150));
        assertEquals("voidDrag", d.last);
    }
}
