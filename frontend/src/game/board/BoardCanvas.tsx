import { useCallback, useEffect, useRef, useState } from 'react';
import type { GameView } from '../../api/types';
import { drawBoard } from './draw';
import { computeBoardRect, fitTransform, hitTest, toBoard } from './viewport';

export interface BoardCanvasProps {
    view: GameView;
    backgroundImg: HTMLImageElement | null;
    selected: number;
    onFieldClick: (index: number) => void;
    onVoidClick: () => void;
}

interface Size {
    width: number;
    height: number;
}

export default function BoardCanvas({ view, backgroundImg, selected, onFieldClick, onVoidClick }: BoardCanvasProps) {
    const containerRef = useRef<HTMLDivElement | null>(null);
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const [size, setSize] = useState<Size>({ width: 0, height: 0 });
    const [hover, setHover] = useState(-1);

    useEffect(() => {
        const el = containerRef.current;
        if (!el) {
            return;
        }
        const observer = new ResizeObserver(entries => {
            for (const entry of entries) {
                const { width, height } = entry.contentRect;
                setSize(prev => {
                    const w = Math.round(width);
                    const h = Math.round(height);
                    return prev.width === w && prev.height === h ? prev : { width: w, height: h };
                });
            }
        });
        observer.observe(el);
        return () => observer.disconnect();
    }, []);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas || size.width <= 0 || size.height <= 0) {
            return;
        }
        const dpr = window.devicePixelRatio || 1;
        const pw = Math.round(size.width * dpr);
        const ph = Math.round(size.height * dpr);
        if (canvas.width !== pw) {
            canvas.width = pw;
        }
        if (canvas.height !== ph) {
            canvas.height = ph;
        }
        const ctx = canvas.getContext('2d');
        if (!ctx) {
            return;
        }
        drawBoard(ctx, view, {
            width: size.width,
            height: size.height,
            backgroundImg,
            selectedIndex: selected,
            hoverIndex: hover,
        });
    }, [view, backgroundImg, selected, hover, size]);

    const pickField = useCallback(
        (e: React.MouseEvent<HTMLCanvasElement>): number => {
            const canvas = canvasRef.current;
            if (!canvas) {
                return -1;
            }
            const bounds = canvas.getBoundingClientRect();
            if (bounds.width <= 0 || bounds.height <= 0) {
                return -1;
            }
            const rect = computeBoardRect(view.fields);
            const t = fitTransform(rect, bounds.width, bounds.height);
            const boardPt = toBoard(t, { x: e.clientX - bounds.left, y: e.clientY - bounds.top });
            return hitTest(view.fields, boardPt);
        },
        [view.fields],
    );

    const handleMouseMove = useCallback(
        (e: React.MouseEvent<HTMLCanvasElement>) => {
            const hit = pickField(e);
            setHover(prev => (prev === hit ? prev : hit));
        },
        [pickField],
    );

    const handleMouseLeave = useCallback(() => {
        setHover(-1);
    }, []);

    const handleClick = useCallback(
        (e: React.MouseEvent<HTMLCanvasElement>) => {
            const hit = pickField(e);
            if (hit >= 0) {
                onFieldClick(hit);
            } else {
                onVoidClick();
            }
        },
        [pickField, onFieldClick, onVoidClick],
    );

    return (
        <div ref={containerRef} className="board-canvas-container" style={{ width: '100%', height: '100%', overflow: 'hidden' }}>
            <canvas
                ref={canvasRef}
                style={{
                    width: size.width > 0 ? `${size.width}px` : '100%',
                    height: size.height > 0 ? `${size.height}px` : '100%',
                    display: 'block',
                    cursor: hover >= 0 ? 'pointer' : 'default',
                }}
                onMouseMove={handleMouseMove}
                onMouseLeave={handleMouseLeave}
                onClick={handleClick}
            />
        </div>
    );
}
