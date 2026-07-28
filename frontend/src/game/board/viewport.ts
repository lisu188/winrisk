import type { FieldView } from '../../api/types';

/** Extra board units added around the field bounding box. */
export const MARGIN = 50;

/** Hit-test radius around a field center, in board units. */
export const HIT_RADIUS = 25;

export interface BoardRect {
    x: number;
    y: number;
    w: number;
    h: number;
}

export interface FitTransform {
    scale: number;
    ox: number;
    oy: number;
}

export interface Point {
    x: number;
    y: number;
}

/**
 * Union of the base rect (0,0,900,600) with the bounding box of all
 * field centers grown by MARGIN on every side.
 */
export function computeBoardRect(fields: FieldView[]): BoardRect {
    let minX = 0;
    let minY = 0;
    let maxX = 900;
    let maxY = 600;
    for (const f of fields) {
        minX = Math.min(minX, f.x - MARGIN);
        minY = Math.min(minY, f.y - MARGIN);
        maxX = Math.max(maxX, f.x + MARGIN);
        maxY = Math.max(maxY, f.y + MARGIN);
    }
    return { x: minX, y: minY, w: maxX - minX, h: maxY - minY };
}

/**
 * Uniform scale-to-fit of the board rect into a cw x ch canvas,
 * centered (letterboxed) on both axes.
 */
export function fitTransform(rect: BoardRect, cw: number, ch: number): FitTransform {
    if (rect.w <= 0 || rect.h <= 0 || cw <= 0 || ch <= 0) {
        return { scale: 1, ox: -rect.x, oy: -rect.y };
    }
    const scale = Math.min(cw / rect.w, ch / rect.h);
    const ox = (cw - rect.w * scale) / 2 - rect.x * scale;
    const oy = (ch - rect.h * scale) / 2 - rect.y * scale;
    return { scale, ox, oy };
}

/** Board-unit point to screen (canvas CSS pixel) point. */
export function toScreen(t: FitTransform, p: Point): Point {
    return { x: p.x * t.scale + t.ox, y: p.y * t.scale + t.oy };
}

/** Screen (canvas CSS pixel) point to board-unit point. */
export function toBoard(t: FitTransform, p: Point): Point {
    if (t.scale === 0) {
        return { x: 0, y: 0 };
    }
    return { x: (p.x - t.ox) / t.scale, y: (p.y - t.oy) / t.scale };
}

/**
 * Index of the field whose center is nearest to boardPt and within
 * HIT_RADIUS board units, or -1 if none qualifies.
 */
export function hitTest(fields: FieldView[], boardPt: Point): number {
    let best = -1;
    let bestDist = HIT_RADIUS * HIT_RADIUS;
    for (const f of fields) {
        const dx = f.x - boardPt.x;
        const dy = f.y - boardPt.y;
        const d2 = dx * dx + dy * dy;
        if (d2 <= bestDist) {
            bestDist = d2;
            best = f.index;
        }
    }
    return best;
}
