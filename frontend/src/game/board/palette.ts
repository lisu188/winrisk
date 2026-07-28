export const INK = '#1B2631';
export const PARCHMENT = '#F2EBD9';
export const SLATE = '#26313C';
export const SLATE_LIGHT = '#34495E';
export const ACCENT = '#D68910';
export const TEXT_LIGHT = '#ECF0F1';
export const TEXT_MUTED = '#95A5A6';
export const CROSS_LINK = 'rgba(120,110,95,0.35)';
export const REGION_LINK_ALPHA = 0.59;

/** Disc fill diameter in board units. */
export const DISC = 32;
/** Ink ring diameter in board units. */
export const RING = 38;
/** Vertical offset of the territory name below the field center, board units. */
export const NAME_OFFSET = 28;

/** java.awt.Color.getRGB() packed int + alpha to a CSS rgba() string. */
export function rgba(packedRgb: number, alpha: number): string {
    const r = (packedRgb >> 16) & 255;
    const g = (packedRgb >> 8) & 255;
    const b = packedRgb & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}
