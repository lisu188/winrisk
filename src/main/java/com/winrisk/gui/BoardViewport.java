package com.winrisk.gui;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;

/**
 * Uniform scale-to-fit mapping between board coordinates (the space maps are
 * authored in) and screen pixels. The board rectangle is fixed at construction
 * time; only the screen size changes as the window is resized. The board is
 * letterboxed: scaled uniformly and centred within the panel.
 */
public class BoardViewport {

    /**
     * Default board rectangle. Covers every shipped board (world.map spans
     * roughly 34..768, MapGenerator up to 900x600) and gives the editor a
     * stable canvas that does not jump as territories are added.
     */
    static final int DEFAULT_WIDTH = 900;
    static final int DEFAULT_HEIGHT = 600;
    private static final int MARGIN = 50;

    private final int boardX;
    private final int boardY;
    private final int boardWidth;
    private final int boardHeight;

    private float scale = 1f;
    private float offsetX;
    private float offsetY;

    public BoardViewport(int boardX, int boardY, int boardWidth, int boardHeight) {
        this.boardX = boardX;
        this.boardY = boardY;
        this.boardWidth = Math.max(1, boardWidth);
        this.boardHeight = Math.max(1, boardHeight);
    }

    public static BoardViewport defaultBoard() {
        return new BoardViewport(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Board rectangle for a concrete map: the default rectangle grown to
     * include every field plus a margin, so no authored coordinate is clipped.
     */
    public static BoardViewport forFields(FieldList fields) {
        if (fields == null || fields.isEmpty()) {
            return defaultBoard();
        }
        int minX = 0;
        int minY = 0;
        int maxX = DEFAULT_WIDTH;
        int maxY = DEFAULT_HEIGHT;
        for (Field field : fields) {
            minX = Math.min(minX, field.getPoint().x - MARGIN);
            minY = Math.min(minY, field.getPoint().y - MARGIN);
            maxX = Math.max(maxX, field.getPoint().x + MARGIN);
            maxY = Math.max(maxY, field.getPoint().y + MARGIN);
        }
        return new BoardViewport(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Recomputes scale and centring offsets for the given panel size. A
     * non-positive size (components painted before layout) falls back to an
     * identity-like transform instead of dividing by zero.
     */
    public void setScreenSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            scale = 1f;
            offsetX = -boardX;
            offsetY = -boardY;
            return;
        }
        scale = Math.min(width / (float) boardWidth, height / (float) boardHeight);
        offsetX = ((width - (boardWidth * scale)) / 2f) - (boardX * scale);
        offsetY = ((height - (boardHeight * scale)) / 2f) - (boardY * scale);
    }

    public int boardToScreenX(int x) {
        return Math.round((x * scale) + offsetX);
    }

    public int boardToScreenY(int y) {
        return Math.round((y * scale) + offsetY);
    }

    public int screenToBoardX(int x) {
        return Math.round((x - offsetX) / scale);
    }

    public int screenToBoardY(int y) {
        return Math.round((y - offsetY) / scale);
    }

    public float getScale() {
        return scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public int getBoardX() {
        return boardX;
    }

    public int getBoardY() {
        return boardY;
    }

    public int getBoardWidth() {
        return boardWidth;
    }

    public int getBoardHeight() {
        return boardHeight;
    }
}
