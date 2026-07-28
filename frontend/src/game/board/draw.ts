import type { FieldView, GameView } from '../../api/types';
import { rgb } from '../../api/types';
import { computeBoardRect, fitTransform } from './viewport';
import {
    ACCENT,
    CROSS_LINK,
    DISC,
    INK,
    NAME_OFFSET,
    PARCHMENT,
    REGION_LINK_ALPHA,
    RING,
    SLATE,
    rgba,
} from './palette';

export interface DrawOptions {
    width: number;
    height: number;
    backgroundImg: HTMLImageElement | null;
    selectedIndex: number;
    hoverIndex: number;
}

function fillCircle(ctx: CanvasRenderingContext2D, x: number, y: number, diameter: number, color: string): void {
    ctx.beginPath();
    ctx.arc(x, y, diameter / 2, 0, Math.PI * 2);
    ctx.fillStyle = color;
    ctx.fill();
}

function discFill(field: FieldView, view: GameView, continentColor: Map<number, number>): string {
    if (field.ownerIndex >= 0 && field.ownerIndex < view.players.length) {
        return rgb(view.players[field.ownerIndex].colorRgb);
    }
    const cc = continentColor.get(field.continentIndex);
    if (field.continentIndex >= 0 && cc !== undefined) {
        return rgb(cc);
    }
    return '#FFFFFF';
}

/**
 * Renders the whole board: slate page fill, parchment board rect,
 * optional background image, links, territory discs, army badges,
 * names and the accent highlight ring on the selected/hovered field.
 *
 * width/height are CSS pixels; the canvas backing store is expected to
 * be width * devicePixelRatio by height * devicePixelRatio.
 */
export function drawBoard(ctx: CanvasRenderingContext2D, view: GameView, opts: DrawOptions): void {
    const { width, height, backgroundImg, selectedIndex, hoverIndex } = opts;
    const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;

    // Page fill in CSS-pixel space.
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.fillStyle = SLATE;
    ctx.fillRect(0, 0, width, height);

    const rect = computeBoardRect(view.fields);
    const t = fitTransform(rect, width, height);

    // Board rect (screen coords, still CSS-pixel space).
    const bx = rect.x * t.scale + t.ox;
    const by = rect.y * t.scale + t.oy;
    const bw = rect.w * t.scale;
    const bh = rect.h * t.scale;
    ctx.fillStyle = PARCHMENT;
    ctx.fillRect(bx, by, bw, bh);

    if (view.hasBackgroundImage && backgroundImg) {
        ctx.drawImage(backgroundImg, bx, by, bw, bh);
    }

    // Everything below is in board units.
    ctx.setTransform(dpr * t.scale, 0, 0, dpr * t.scale, dpr * t.ox, dpr * t.oy);

    const fieldByIndex = new Map<number, FieldView>();
    for (const f of view.fields) {
        fieldByIndex.set(f.index, f);
    }
    const continentColor = new Map<number, number>();
    for (const c of view.continents) {
        continentColor.set(c.index, c.colorRgb);
    }

    // Links: each edge once; continent-tinted when both ends share a continent.
    ctx.lineWidth = 2;
    for (const [a, b] of view.links) {
        const fa = fieldByIndex.get(a);
        const fb = fieldByIndex.get(b);
        if (!fa || !fb) {
            continue;
        }
        const cc = continentColor.get(fa.continentIndex);
        if (fa.continentIndex >= 0 && fa.continentIndex === fb.continentIndex && cc !== undefined) {
            ctx.strokeStyle = rgba(cc, REGION_LINK_ALPHA);
        } else {
            ctx.strokeStyle = CROSS_LINK;
        }
        ctx.beginPath();
        ctx.moveTo(fa.x, fa.y);
        ctx.lineTo(fb.x, fb.y);
        ctx.stroke();
    }

    // Territory discs.
    for (const f of view.fields) {
        const highlighted = f.index === selectedIndex || f.index === hoverIndex;
        if (highlighted) {
            fillCircle(ctx, f.x, f.y, RING + 8, ACCENT);
        }
        fillCircle(ctx, f.x, f.y, RING, INK);
        fillCircle(ctx, f.x, f.y, DISC, discFill(f, view, continentColor));
    }

    // Text: army badges and territory names.
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    for (const f of view.fields) {
        ctx.font = 'bold 13px sans-serif';
        ctx.fillStyle =
            f.ownerIndex >= 0 && f.ownerIndex < view.players.length
                ? rgb(view.players[f.ownerIndex].textColorRgb)
                : INK;
        ctx.fillText(String(f.army), f.x, f.y);

        ctx.font = '10px sans-serif';
        ctx.fillStyle = INK;
        ctx.fillText(f.name, f.x, f.y + NAME_OFFSET);
    }

    ctx.setTransform(1, 0, 0, 1, 0, 0);
}
