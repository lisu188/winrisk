package com.winrisk.app;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Align;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.util.GameColor;
import com.winrisk.game.view.Game;

final class BoardActor extends Actor {
    interface FieldTapListener {
        void onTap(int fieldIndex);
    }

    private static final float PADDING = 40f;
    private static final float NODE_RADIUS = 18f;

    private final Texture pixelTexture;
    private final TextureRegion pixel;
    private final Texture circleTexture;
    private final TextureRegion circle;
    private final BitmapFont font;
    private final FieldTapListener listener;
    private Game game;
    private int selectedField = -1;

    BoardActor(Game game, FieldTapListener listener, BitmapFont font) {
        this.game = game;
        this.listener = listener;
        this.font = font;

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixelTexture = new Texture(px);
        px.dispose();
        pixel = new TextureRegion(pixelTexture);

        Pixmap node = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        node.setColor(Color.CLEAR);
        node.fill();
        node.setColor(Color.WHITE);
        node.fillCircle(32, 32, 31);
        circleTexture = new Texture(node);
        node.dispose();
        circle = new TextureRegion(circleTexture);

        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                int index = findFieldAt(x, y);
                if (index >= 0) {
                    BoardActor.this.listener.onTap(index);
                }
            }
        });
    }

    void setGame(Game game) {
        this.game = game;
    }

    void setSelectedField(int selectedField) {
        this.selectedField = selectedField;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (game == null || getWidth() <= 0f || getHeight() <= 0f) {
            return;
        }
        Bounds bounds = bounds();
        Color previous = new Color(batch.getColor());

        for (Field field : game.getFields()) {
            if (!visible(field)) {
                continue;
            }
            for (Field next : field.getNext()) {
                if (field.getFieldIndex() >= next.getFieldIndex() || !visible(next)) {
                    continue;
                }
                drawLine(batch,
                        mapX(field, bounds), mapY(field, bounds),
                        mapX(next, bounds), mapY(next, bounds));
            }
        }

        for (Field field : game.getFields()) {
            if (!visible(field)) {
                continue;
            }
            float x = mapX(field, bounds);
            float y = mapY(field, bounds);
            if (field.getFieldIndex() == selectedField) {
                batch.setColor(Color.valueOf("F0A020"));
                batch.draw(circle,
                        getX() + x - NODE_RADIUS - 4f,
                        getY() + y - NODE_RADIUS - 4f,
                        (NODE_RADIUS + 4f) * 2f,
                        (NODE_RADIUS + 4f) * 2f);
            }
            batch.setColor(ownerColor(field));
            batch.draw(circle,
                    getX() + x - NODE_RADIUS,
                    getY() + y - NODE_RADIUS,
                    NODE_RADIUS * 2f,
                    NODE_RADIUS * 2f);

            batch.setColor(textColor(field));
            font.draw(batch,
                    String.valueOf(field.getArmy()),
                    getX() + x - NODE_RADIUS,
                    getY() + y + 5f,
                    NODE_RADIUS * 2f,
                    Align.center,
                    false);
        }
        batch.setColor(previous);
    }

    private void drawLine(Batch batch, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt((dx * dx) + (dy * dy));
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        batch.setColor(Color.valueOf("596673"));
        batch.draw(pixel,
                getX() + x1,
                getY() + y1 - 1f,
                0f,
                1f,
                length,
                2f,
                1f,
                1f,
                angle);
    }

    private int findFieldAt(float x, float y) {
        if (game == null) {
            return -1;
        }
        Bounds bounds = bounds();
        float maxDistance2 = (NODE_RADIUS * 1.6f) * (NODE_RADIUS * 1.6f);
        int best = -1;
        float bestDistance2 = maxDistance2;
        for (Field field : game.getFields()) {
            if (!visible(field)) {
                continue;
            }
            float dx = x - mapX(field, bounds);
            float dy = y - mapY(field, bounds);
            float distance2 = (dx * dx) + (dy * dy);
            if (distance2 <= bestDistance2) {
                bestDistance2 = distance2;
                best = field.getFieldIndex();
            }
        }
        return best;
    }

    private boolean visible(Field field) {
        if (!game.getParams().isFogOfWar()) {
            return true;
        }
        for (Player player : game.getPlayers()) {
            if (player.getPlayerInterface().isInteractive()) {
                return player.getVis(game).contains(field);
            }
        }
        return true;
    }

    private Bounds bounds() {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Field field : game.getFields()) {
            if (!visible(field)) {
                continue;
            }
            minX = Math.min(minX, field.getPoint().x);
            maxX = Math.max(maxX, field.getPoint().x);
            minY = Math.min(minY, field.getPoint().y);
            maxY = Math.max(maxY, field.getPoint().y);
        }
        if (!Float.isFinite(minX)) {
            minX = minY = 0f;
            maxX = maxY = 1f;
        }
        return new Bounds(minX, maxX, minY, maxY);
    }

    private float scale(Bounds bounds) {
        float sourceWidth = Math.max(1f, bounds.maxX - bounds.minX);
        float sourceHeight = Math.max(1f, bounds.maxY - bounds.minY);
        float targetWidth = Math.max(1f, getWidth() - (PADDING * 2f));
        float targetHeight = Math.max(1f, getHeight() - (PADDING * 2f));
        return Math.min(targetWidth / sourceWidth, targetHeight / sourceHeight);
    }

    private float mapX(Field field, Bounds bounds) {
        float scale = scale(bounds);
        float contentWidth = (bounds.maxX - bounds.minX) * scale;
        float offset = (getWidth() - contentWidth) * 0.5f;
        return offset + ((field.getPoint().x - bounds.minX) * scale);
    }

    private float mapY(Field field, Bounds bounds) {
        float scale = scale(bounds);
        float contentHeight = (bounds.maxY - bounds.minY) * scale;
        float offset = (getHeight() - contentHeight) * 0.5f;
        return offset + ((bounds.maxY - field.getPoint().y) * scale);
    }

    private Color ownerColor(Field field) {
        if (field.getPlayer() == null) {
            return Color.valueOf("7F8C8D");
        }
        return toGdx(field.getPlayer().getColor());
    }

    private Color textColor(Field field) {
        if (field.getPlayer() == null) {
            return Color.WHITE;
        }
        GameColor color = field.getPlayer().getColor();
        double luminance = ((0.299 * color.getRed())
                + (0.587 * color.getGreen())
                + (0.114 * color.getBlue())) / 255.0;
        return luminance > 0.55 ? Color.valueOf("1B2631") : Color.WHITE;
    }

    private static Color toGdx(GameColor color) {
        return new Color(
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f);
    }

    void dispose() {
        pixelTexture.dispose();
        circleTexture.dispose();
    }

    private static final class Bounds {
        final float minX;
        final float maxX;
        final float minY;
        final float maxY;

        Bounds(float minX, float maxX, float minY, float maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}
