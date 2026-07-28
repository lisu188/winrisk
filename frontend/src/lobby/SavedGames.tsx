import { useState } from 'react';
import * as api from '../api/client';

interface SavedGamesProps {
    saves: string[];
    onCreated: (id: string) => void;
}

export default function SavedGames({ saves, onCreated }: SavedGamesProps) {
    const [busyName, setBusyName] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    async function handleLoad(name: string) {
        if (busyName !== null) {
            return;
        }
        setBusyName(name);
        setError(null);
        try {
            const res = await api.loadGame(name);
            onCreated(res.id);
        } catch (err) {
            setError(err instanceof Error ? err.message : String(err));
            setBusyName(null);
        }
    }

    if (saves.length === 0) {
        return <p className="muted">No saved games yet.</p>;
    }
    return (
        <>
            <ul className="game-list">
                {saves.map(name => (
                    <li key={name}>
                        <span>{name}</span>
                        <button
                            type="button"
                            className="btn btn-accent btn-small"
                            disabled={busyName !== null}
                            onClick={() => handleLoad(name)}
                        >
                            {busyName === name ? 'Loading…' : 'Load'}
                        </button>
                    </li>
                ))}
            </ul>
            {error && <p className="error-text">{error}</p>}
        </>
    );
}
