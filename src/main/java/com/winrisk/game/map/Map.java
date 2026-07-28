package com.winrisk.game.map;

import com.winrisk.game.cluster.ContinentList;
import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Continent;
import com.winrisk.game.object.Field;
import com.winrisk.game.serialization.*;
import com.winrisk.game.util.PointF;
import com.winrisk.game.view.Game;
import com.winrisk.game.view.GameSurface;

import java.util.ArrayList;
import java.util.Collections;

public class Map implements Saveable {

    private final ContinentList continents = new ContinentList();

    private final FieldList fields = new FieldList();
    private final Serializer serializer = new JavaSerializer();
    private byte[] image;
    private int[][] proxCache;

    public Map() {

    }

    public Map(String map) {
        load(map);
    }

    public void addContinent(int i) {
        continents.add(new Continent(continents.size(), i));
    }

    public void addField(PointF point) {
        fields.add(new Field(new PointF(point.x, point.y)));
        applyMetadata();
    }

    public void draw(GameSurface graphics, Game game) {
        graphics.drawBackground(image);
        for (Field field : fields) {
            field.drawLines(graphics);
        }

        for (Field field : (game == null) || !game.getParams().isFogOfWar() ? fields
                : game.getPlayers().get(0)
                .getVis(game)) {
            field.drawFields(graphics);
        }
    }

    @Override
    public void fromSketch(Sketch sketch) {
        ((MapSketch) sketch).toMap(this);
        applyMetadata();

    }

    public ContinentList getContinents() {
        return continents;
    }

    public FieldList getFields() {
        return fields;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    @Override
    public Class<? extends Sketch> getSketchClass() {
        return MapSketch.class;
    }

    public void load(String path) {
        serializer.load(this, path);
        applyMetadata();
    }

    /**
     * Loads a map from a stream, e.g. a classpath resource inside a jar.
     */
    public void load(java.io.InputStream input, String sourceName) {
        serializer.load(this, input, sourceName);
        applyMetadata();
    }

    public void save(String path) {
        serializer.save(this, path);
    }

    @Override
    public Sketch toSketch() {
        return new MapSketch(this);
    }

    private int[][] initCache() {
        int size = getFields().size();
        int[][] cache = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                cache[i][j] = -1;
            }
        }
        return cache;
    }

    int proximity(ArrayList<Field> prev, Field a, Field b) {
        if (proxCache == null) {
            proxCache = initCache();
        }
        if (prev == null) {
            prev = new ArrayList<>();
        }
        prev.add(a);
        int x = getFields().indexOf(a);
        int y = getFields().indexOf(b);
        if (proxCache[x][y] != -1) {
            return proxCache[y][x] = proxCache[x][y];
        }
        if (proxCache[y][x] != -1) {
            return proxCache[x][y] = proxCache[y][x];
        }

        if (x == y) {
            return proxCache[x][y] = proxCache[y][x] = 0;
        }

        if (a.getNext().contains(b)) {
            return proxCache[y][x] = proxCache[x][y] = 1;
        }
        if (b.getNext().contains(a)) {
            return proxCache[x][y] = proxCache[y][x] = 1;
        }

        ArrayList<Integer> tab = new ArrayList<>();
        for (int i = 0; i < a.getNext().size(); i++) {
            if (!prev.contains(a.getNext().get(i))) {
                tab.add(1 + proximity(prev, a.getNext().get(i), b));
            }
        }
        if (tab.size() == 0) {
            return getFields().size();
        }
        return proxCache[x][y] = proxCache[y][x] = Collections.min(tab);
    }

    public int proximity(Field a, Field b) {
        return proximity(null, a, b);
    }

    public int proximity(int i, int j) {
        return proximity(null, fields.get(i), fields.get(j));
    }

    public void applyMetadata() {
        WorldTerritoryMetadata.apply(this);
    }
}
