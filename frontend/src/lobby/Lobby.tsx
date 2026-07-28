import { useEffect, useState } from 'react';
import * as api from '../api/client';
import type { GameSummary, MapsResponse } from '../api/types';
import CreateGameForm from './CreateGameForm';
import OpenGames from './OpenGames';
import SavedGames from './SavedGames';

function messageOf(err: unknown): string {
    return err instanceof Error ? err.message : String(err);
}

export default function Lobby() {
    const [maps, setMaps] = useState<MapsResponse | null>(null);
    const [games, setGames] = useState<GameSummary[] | null>(null);
    const [saves, setSaves] = useState<string[] | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const [m, g, s] = await Promise.all([
                    api.listMaps(),
                    api.listGames(),
                    api.listSaves(),
                ]);
                if (cancelled) {
                    return;
                }
                setMaps(m);
                setGames(g);
                setSaves(s);
            } catch (err) {
                if (!cancelled) {
                    setError(messageOf(err));
                }
            }
        })();
        return () => {
            cancelled = true;
        };
    }, []);

    function openGame(id: string) {
        window.location.hash = '#/game/' + encodeURIComponent(id);
    }

    async function handleDelete(id: string) {
        setError(null);
        try {
            await api.deleteGame(id);
            setGames(prev => (prev ? prev.filter(g => g.id !== id) : prev));
        } catch (err) {
            setError(messageOf(err));
        }
    }

    return (
        <div className="lobby">
            <header className="lobby-header">
                <h1>WinRisk</h1>
                <div className="subtitle">Conquer the world, one province at a time</div>
            </header>

            {error && <p className="error-text center">{error}</p>}

            <div className="lobby-grid">
                <section className="panel">
                    <div className="panel-title">New Game</div>
                    {maps ? (
                        <CreateGameForm maps={maps} onCreated={openGame} />
                    ) : (
                        <p className="muted">
                            <span className="spinner" /> Loading maps…
                        </p>
                    )}
                </section>

                <div>
                    <section className="panel">
                        <div className="panel-title">Open Games</div>
                        {games ? (
                            <OpenGames games={games} onOpen={openGame} onDelete={handleDelete} />
                        ) : (
                            <p className="muted">
                                <span className="spinner" /> Loading…
                            </p>
                        )}
                    </section>

                    <section className="panel">
                        <div className="panel-title">Saved Games</div>
                        {saves ? (
                            <SavedGames saves={saves} onCreated={openGame} />
                        ) : (
                            <p className="muted">
                                <span className="spinner" /> Loading…
                            </p>
                        )}
                    </section>
                </div>
            </div>
        </div>
    );
}
