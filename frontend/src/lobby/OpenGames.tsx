import type { GameSummary } from '../api/types';

interface OpenGamesProps {
    games: GameSummary[];
    onOpen: (id: string) => void;
    onDelete: (id: string) => void;
}

function statusText(game: GameSummary): string {
    if (game.winnerIndex >= 0) {
        return `won by player ${game.winnerIndex + 1}`;
    }
    return game.phase.toLowerCase();
}

export default function OpenGames({ games, onOpen, onDelete }: OpenGamesProps) {
    if (games.length === 0) {
        return <p className="muted">No games in progress. Create one to get started.</p>;
    }
    return (
        <ul className="game-list">
            {games.map(game => (
                <li key={game.id}>
                    <span>
                        <strong>{game.mapName}</strong>
                        <br />
                        <span className="muted">
                            {game.mode} · {game.players} players · {statusText(game)}
                        </span>
                    </span>
                    <span className="btn-row">
                        <button
                            type="button"
                            className="btn btn-accent btn-small"
                            onClick={() => onOpen(game.id)}
                        >
                            Open
                        </button>
                        <button
                            type="button"
                            className="btn btn-danger btn-small"
                            onClick={() => onDelete(game.id)}
                        >
                            Delete
                        </button>
                    </span>
                </li>
            ))}
        </ul>
    );
}
