package com.winrisk.game.view;

import com.winrisk.game.TestUtil;
import com.winrisk.game.ai.ContinentAI;
import com.winrisk.game.ai.EasyAI;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.cluster.PlayerList;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.map.Map;
import com.winrisk.game.object.Field;
import com.winrisk.game.data.Params;
import com.winrisk.game.util.PointF;
import org.junit.Test;

import java.awt.HeadlessException;
import java.io.File;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class ViewTests {
    @Test
    public void editorOnEvent() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        Editor editor = new Editor(map);
        MotionEvent evDown = new MotionEvent(MotionEvent.ACTION_DOWN,0,0,0,0);
        MotionEvent evUp = new MotionEvent(MotionEvent.ACTION_UP,0,0,0,0);
        editor.onEvent(evDown);
        editor.onEvent(evUp);
    }

    @Test
    public void editorSave() throws Exception {
        Map map = new Map();
        map.addField(new PointF(0,0));
        Editor editor = new Editor(map);
        File tmp = File.createTempFile("map",".dat");
        editor.onSave(tmp.getAbsolutePath());
        tmp.delete();
    }

    @Test
    public void editorMethods() {
        Map map = new Map();
        map.addField(new PointF(0,0));
        map.addContinent(0);
        Field field = map.getFields().get(0);
        map.getContinents().get(0).addField(field);

        Editor editor = new Editor(map);
        editor.onAction();
        editor.onVoidClick(new PointF(1,1));
        Field second = map.getFields().get(1);
        editor.onFieldDrag(field, second);
        editor.onFieldDrag(field, second);
        editor.onShortClick(field);
        editor.onToField(field);
        editor.onFromField(field);
        editor.onLongClick(second);
    }

    @Test
    public void editorVoidDrag() {
        Map map = new Map();
        Editor editor = new Editor(map);
        assertTrue(editor.onVoidDrag(new PointF(0,0), new PointF(1,1)));
    }

    private static class DummyListener implements FieldListener {
        FieldList list = new FieldList();
        String last = "";
        @Override public FieldList getFieldList() { return list; }
        @Override public boolean onFieldDrag(Field from, Field to) { last="drag"; return true; }
        @Override public boolean onFromField(Field field) { last="from"; return true; }
        @Override public boolean onLongClick(Field field) { last="long"; return true; }
        @Override public boolean onShortClick(Field field) { last="short"; return true; }
        @Override public boolean onToField(Field field) { last="to"; return true; }
        @Override public boolean onVoidClick(PointF point) { last="voidClick"; return true; }
        @Override public boolean onVoidDrag(PointF from, PointF to) { last="voidDrag"; return true; }
    }

    @Test
    public void fieldDetectorVarious() {
        DummyListener d = new DummyListener();
        Field a = new Field(new PointF(0,0));
        Field b = new Field(new PointF(30,0));
        d.list.add(a);
        d.list.add(b);
        FieldDetector detector = new FieldDetector(d);
        long t = System.currentTimeMillis();
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 0,0));
        assertEquals("short", d.last);
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+600, 0,0));
        assertEquals("long", d.last);
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 30,0));
        assertEquals("drag", d.last);
        detector.feed(new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 100,100));
        detector.feed(new MotionEvent(MotionEvent.ACTION_UP, t, t+100, 150,150));
        assertEquals("voidDrag", d.last);
    }

    @Test
    public void fieldDetectorFeed() {
        DummyListener d = new DummyListener();
        d.list.add(new Field(new PointF(0,0)));
        FieldDetector detector = new FieldDetector(d);
        long t = System.currentTimeMillis();
        MotionEvent down = new MotionEvent(MotionEvent.ACTION_DOWN, t, t, 0,0);
        MotionEvent up = new MotionEvent(MotionEvent.ACTION_UP, t+600, t+600, 0,0);
        detector.feed(down);
        detector.feed(up);
    }

    @Test
    public void gameHandlersExtended() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        game.onFieldDrag(a, b);
        game.onShortClick(a);
        game.onLongClick(a);
        game.onFromField(a);
        game.next();
        game.onFieldDrag(a, b);
        game.next();
        game.onFieldDrag(a, b);
        game.next();
        game.onToField(a);
        game.onVoidClick(new PointF(0,0));
    }

    @Test
    public void gameHandlers() throws Exception {
        Game game = TestUtil.createNewGame();
        Field a = game.getFields().get(0);
        Field b = game.getFields().get(1);
        game.onFieldDrag(a, b);
        game.onFromField(a);
        game.onLongClick(a);
        game.onShortClick(a);
        game.onVoidClick(new PointF(0,0));
        game.onVoidDrag(new PointF(0,0), new PointF(1,1));
        game.onAction();
    }

    @Test
    public void gameMiscMethods() throws Exception {
        Game game = TestUtil.createNewGame();
        game.setMap(new Map());
        game.setPhase(GamePhase.ATTACK);
        PlayerList list = new PlayerList();
        list.add(game.getPlayers().get(0));
        game.setPlayers(list);
        game.getParams();
        game.getMap();
        game.getPlayer();
        game.getMapSketch();
        game.setParams(game.getParams());
        game.end();
        game.next();
    }

    @Test
    public void gameAi128() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(128);
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void gameContinental() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(8);
        params.setAiFactory(i -> new ContinentAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }

    @Test
    public void gameEasy() throws URISyntaxException {
        Params params = new Params();
        params.setHumanPlayers(0);
        params.setAiPlayers(8);
        params.setAiFactory(i -> new EasyAI());
        params.setMap(new File(Map.class.getResource("world.map").toURI()).getAbsolutePath());
        TestUtil.finishGame(params);
    }
}
