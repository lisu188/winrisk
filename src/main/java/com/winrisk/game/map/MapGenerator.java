package com.winrisk.game.map;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MapGenerator {

    private static final int DEFAULT_HEIGHT = 600;
    private static final int DEFAULT_WIDTH = 900;
    private static final int MIN_DISTANCE = 35;

    private final Random random;

    public MapGenerator() {
        this(new Random());
    }

    public MapGenerator(long seed) {
        this(new Random(seed));
    }

    private MapGenerator(Random random) {
        this.random = random;
    }

    public Map generate(int fieldCount, int continentCount) {
        if (fieldCount < 1) {
            throw new IllegalArgumentException("At least one field is required");
        }
        if (continentCount < 1) {
            throw new IllegalArgumentException("At least one continent is required");
        }
        if (continentCount > fieldCount) {
            throw new IllegalArgumentException("Continents cannot exceed fields");
        }

        Map map = new Map();
        for (int i = 0; i < fieldCount; i++) {
            map.addField(createPoint(map.getFields()));
        }

        connectFields(map.getFields());
        assignContinents(map, continentCount);
        return map;
    }

    private void assignContinents(Map map, int continentCount) {
        List<Field> shuffledFields = new ArrayList<>(map.getFields());
        Collections.shuffle(shuffledFields, random);

        int baseSize = shuffledFields.size() / continentCount;
        int remainder = shuffledFields.size() % continentCount;
        int offset = 0;

        for (int continentIndex = 0; continentIndex < continentCount; continentIndex++) {
            int continentSize = baseSize + (continentIndex < remainder ? 1 : 0);
            map.addContinent(Math.max(1, continentSize / 3));
            for (int i = 0; i < continentSize; i++) {
                Field field = shuffledFields.get(offset + i);
                map.getContinents().get(continentIndex).addField(field);
            }
            offset += continentSize;
        }
    }

    private PointF createPoint(FieldList fields) {
        for (int attempt = 0; attempt < 200; attempt++) {
            PointF candidate = randomPoint();
            if (isDistant(candidate, fields)) {
                return candidate;
            }
        }
        return randomPoint();
    }

    private void connectFields(FieldList fields) {
        if (fields.isEmpty()) {
            return;
        }
        for (int i = 1; i < fields.size(); i++) {
            Field current = fields.get(i);
            Field nearest = findNearest(current, fields, i);
            current.addNext(nearest);
        }

        if (fields.size() < 3) {
            return;
        }

        for (Field field : fields) {
            while (field.getNext().size() < 2) {
                Field closest = findNearestAvailable(field, fields);
                if (field.getNext().contains(closest)) {
                    break;
                }
                field.addNext(closest);
            }
        }

        for (Field field : fields) {
            if (random.nextBoolean()) {
                Field extra = findNearest(field, fields, fields.size());
                field.addNext(extra);
            }
        }
    }

    private Field findNearest(Field current, FieldList fields, int limit) {
        float bestDistance = Float.MAX_VALUE;
        Field bestField = fields.get(0);
        for (int i = 0; i < limit; i++) {
            Field candidate = fields.get(i);
            if (candidate == current) {
                continue;
            }
            float distance = PointF.dist(current.getPoint(), candidate.getPoint());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestField = candidate;
            }
        }
        return bestField;
    }

    private Field findNearestAvailable(Field current, FieldList fields) {
        float bestDistance = Float.MAX_VALUE;
        Field bestField = null;
        for (Field candidate : fields) {
            if ((candidate == current) || current.getNext().contains(candidate)) {
                continue;
            }
            float distance = PointF.dist(current.getPoint(), candidate.getPoint());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestField = candidate;
            }
        }
        return bestField == null ? findNearest(current, fields, fields.size()) : bestField;
    }

    private boolean isDistant(PointF candidate, FieldList fields) {
        for (Field field : fields) {
            if (PointF.dist(candidate, field.getPoint()) < MIN_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    private PointF randomPoint() {
        int x = MIN_DISTANCE + random.nextInt(DEFAULT_WIDTH - (2 * MIN_DISTANCE));
        int y = MIN_DISTANCE + random.nextInt(DEFAULT_HEIGHT - (2 * MIN_DISTANCE));
        return new PointF(x, y);
    }
}
