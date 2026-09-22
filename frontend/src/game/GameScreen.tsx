import { useCallback, useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import type { FieldView, GameView } from '../api/types';
import * as api from '../api/client';
import { ApiError } from '../api/client';
import { subscribeToGame } from '../ws/gameSocket';
import BoardCanvas from './board/BoardCanvas';
import Hud from './Hud';
import TradePanel from './TradePanel';
import GameOverBanner from './GameOverBanner';

export interface GameScreenProps {
    gameId: string;
}

type ToastKind = 'info' | 'error' | 'combat';

interface ToastState {
    text: string;
    kind: ToastKind;
}

const AI_OVERLAY_STYLE: CSSProperties = {
    position: 'absolute',
    top: '0.8rem',
    left: '50%',
    transform: 'translateX(-50%)',
    background: 'rgba(27, 38, 49, 0.85)',
    padding: '0.35rem 1rem',
    borderRadius: 6,
    zIndex: 40,
};

function fieldByIndex(view: GameView, index: number): FieldView | undefined {
    return view.fields.find(f => f.index === index);
}

function areLinked(view: GameView, a: number, b: number): boolean {
    return view.links.some(([x, y]) => (x === a && y === b) || (x === b && y === a));
}

function toastClass(kind: ToastKind): string {
    if (kind === 'error') {
        return 'toast toast-error';
    }
    if (kind === 'combat') {
        return 'toast toast-combat';
    }
    return 'toast';
}

export default function GameScreen({ gameId }: GameScreenProps) {
    const [view, setView] = useState<GameView | null>(null);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [toast, setToast] = useState<ToastState | null>(null);
    const [selected, setSelected] = useState(-1);
    const [placeAll, setPlaceAll] = useState(false);
    const [moveSource, setMoveSource] = useState(-1);
    const [backgroundImg, setBackgroundImg] = useState<HTMLImageElement | null>(null);
    const busyRef = useRef(false);
    const toastTimer = useRef<number | null>(null);

    /** Merge rule: ignore any view whose version <= current version. */
    const mergeView = useCallback((next: GameView) => {
        setView(prev => (prev !== null && next.version <= prev.version ? prev : next));
    }, []);

    const showToast = useCallback((text: string, kind: ToastKind = 'info') => {
        setToast({ text, kind });
        if (toastTimer.current !== null) {
            window.clearTimeout(toastTimer.current);
        }
        toastTimer.current = window.setTimeout(() => setToast(null), 4000);
    }, []);

    const handleError = useCallback(
        (e: unknown) => {
            if (e instanceof ApiError) {
                showToast(e.message, 'error');
            } else {
                console.error(e);
                showToast('Network error', 'error');
            }
        },
        [showToast],
    );

    const runAction = useCallback(
        (action: Promise<GameView>) => {
            busyRef.current = true;
            action
                .then(mergeView)
                .catch(handleError)
                .finally(() => {
                    busyRef.current = false;
                });
        },
        [mergeView, handleError],
    );

    useEffect(() => {
        let cancelled = false;
        api.getGame(gameId)
            .then(v => {
                if (!cancelled) {
                    mergeView(v);
                }
            })
            .catch(e => {
                if (!cancelled) {
                    setLoadError(e instanceof ApiError ? e.message : 'Failed to load game');
                }
            });
        const unsubscribe = subscribeToGame(gameId, mergeView);
        return () => {
            cancelled = true;
            unsubscribe();
        };
    }, [gameId, mergeView]);

    useEffect(
        () => () => {
            if (toastTimer.current !== null) {
                window.clearTimeout(toastTimer.current);
            }
        },
        [],
    );

    const hasBackground = view?.hasBackgroundImage === true;
    useEffect(() => {
        if (!hasBackground) {
            return;
        }
        let cancelled = false;
        const img = new Image();
        img.onload = () => {
            if (!cancelled) {
                setBackgroundImg(img);
            }
        };
        img.src = api.backgroundUrl(gameId);
        return () => {
            cancelled = true;
        };
    }, [hasBackground, gameId]);

    const phase = view?.phase;
    const currentPlayerIndex = view?.currentPlayerIndex;
    useEffect(() => {
        setSelected(-1);
        setMoveSource(-1);
    }, [phase, currentPlayerIndex]);

    const handleVoidClick = useCallback(() => {
        setSelected(-1);
        setMoveSource(-1);
    }, []);

    const handleFieldClick = useCallback(
        (index: number) => {
            if (view === null || busyRef.current) {
                return;
            }
            if (view.winnerIndex >= 0 || view.draw) {
                return;
            }
            if (view.currentPlayerIndex !== view.humanPlayerIndex) {
                return;
            }
            const me = view.humanPlayerIndex;
            const field = fieldByIndex(view, index);
            if (!field) {
                return;
            }

            switch (view.phase) {
                case 'REINFORCE': {
                    if (field.ownerIndex !== me) {
                        return;
                    }
                    runAction(api.place(view.id, index, placeAll));
                    return;
                }
                case 'ATTACK': {
                    if (selected < 0) {
                        if (field.ownerIndex === me && field.army > 1) {
                            setSelected(index);
                        }
                        return;
                    }
                    if (index === selected) {
                        setSelected(-1);
                        return;
                    }
                    if (field.ownerIndex === me) {
                        setSelected(field.army > 1 ? index : -1);
                        return;
                    }
                    if (!areLinked(view, selected, index)) {
                        setSelected(-1);
                        return;
                    }
                    const from = selected;
                    busyRef.current = true;
                    api.attack(view.id, from, index)
                        .then(res => {
                            mergeView(res.view);
                            const c = res.combat;
                            showToast(
                                `Dice ${c.attackerDice.join(' ')} vs ${c.defenderDice.join(' ')}` +
                                    ` — losses ${c.attackerLosses} / ${c.defenderLosses}` +
                                    (c.captured ? ' — territory captured!' : ''),
                                'combat',
                            );
                            const f = fieldByIndex(res.view, from);
                            if (!f || f.ownerIndex !== res.view.humanPlayerIndex || f.army <= 1) {
                                setSelected(-1);
                            }
                        })
                        .catch(handleError)
                        .finally(() => {
                            busyRef.current = false;
                        });
                    return;
                }
                case 'MOVE': {
                    if (moveSource < 0) {
                        if (field.ownerIndex === me && field.army > 1) {
                            setMoveSource(index);
                        }
                        return;
                    }
                    if (index === moveSource) {
                        setMoveSource(-1);
                        return;
                    }
                    if (field.ownerIndex !== me) {
                        setMoveSource(-1);
                        return;
                    }
                    const src = fieldByIndex(view, moveSource);
                    if (!src || src.army <= 1) {
                        setMoveSource(-1);
                        return;
                    }
                    const max = src.army - 1;
                    const input = window.prompt(
                        `Move troops from ${src.name} to ${field.name} (1-${max}):`,
                        String(max),
                    );
                    if (input === null) {
                        return;
                    }
                    const troops = Number.parseInt(input, 10);
                    if (!Number.isFinite(troops) || troops < 1 || troops > max) {
                        showToast(`Enter a number between 1 and ${max}`, 'error');
                        return;
                    }
                    busyRef.current = true;
                    api.maneuver(view.id, moveSource, index, troops)
                        .then(v => {
                            mergeView(v);
                            setMoveSource(-1);
                        })
                        .catch(handleError)
                        .finally(() => {
                            busyRef.current = false;
                        });
                    return;
                }
                default:
                    return;
            }
        },
        [view, placeAll, selected, moveSource, runAction, mergeView, showToast, handleError],
    );

    const handleEndPhase = useCallback(() => {
        if (view === null || busyRef.current) {
            return;
        }
        setSelected(-1);
        setMoveSource(-1);
        runAction(api.endPhase(view.id));
    }, [view, runAction]);

    const handleSave = useCallback(
        (name: string) => {
            if (view === null) {
                return;
            }
            api.saveGame(view.id, name)
                .then(() => showToast(`Saved as "${name}"`))
                .catch(handleError);
        },
        [view, showToast, handleError],
    );

    const backToLobby = useCallback(() => {
        window.location.hash = '#/';
    }, []);

    if (loadError !== null) {
        return (
            <div className="game-screen">
                <div className="center" style={{ margin: 'auto' }}>
                    <p className="error-text">{loadError}</p>
                    <button className="btn" onClick={backToLobby}>
                        Back to lobby
                    </button>
                </div>
            </div>
        );
    }

    if (view === null) {
        return (
            <div className="game-screen">
                <div className="center" style={{ margin: 'auto' }}>
                    <span className="spinner" /> Loading game…
                </div>
            </div>
        );
    }

    const gameOver = view.winnerIndex >= 0 || view.draw;
    const yourTurn = view.currentPlayerIndex === view.humanPlayerIndex;
    const highlight = view.phase === 'MOVE' ? moveSource : selected;
    const showTrade =
        !gameOver && yourTurn && view.phase === 'REINFORCE' && view.hand.length >= 3;

    return (
        <div className="game-screen">
            <div className="board-wrap">
                <BoardCanvas
                    view={view}
                    backgroundImg={backgroundImg}
                    selected={highlight}
                    onFieldClick={handleFieldClick}
                    onVoidClick={handleVoidClick}
                />
                {!gameOver && !yourTurn && (
                    <div className="ai-thinking" style={AI_OVERLAY_STYLE}>
                        AI thinking…
                    </div>
                )}
                {gameOver && <GameOverBanner view={view} onBackToLobby={backToLobby} />}
            </div>
            <aside className="hud">
                <Hud
                    view={view}
                    placeAll={placeAll}
                    onTogglePlaceAll={() => setPlaceAll(v => !v)}
                    onEndPhase={handleEndPhase}
                    onSave={handleSave}
                />
                {showTrade && <TradePanel view={view} onTraded={mergeView} />}
            </aside>
            {toast !== null && (
                <div className="toast-stack">
                    <div className={toastClass(toast.kind)}>{toast.text}</div>
                </div>
            )}
        </div>
    );
}
