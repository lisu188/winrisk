import type { GameView } from '../api/types';
import { rgb } from '../api/types';

export interface GameOverBannerProps {
    view: GameView;
    onBackToLobby: () => void;
}

export default function GameOverBanner({ view, onBackToLobby }: GameOverBannerProps) {
    const winner = view.winnerIndex >= 0 ? view.players[view.winnerIndex] : null;
    const youWon = view.winnerIndex === view.humanPlayerIndex;

    return (
        <div className="banner-backdrop">
            <div className="banner">
                {winner !== null ? (
                    <>
                        <span
                            className="swatch swatch-large"
                            style={{ background: rgb(winner.colorRgb) }}
                        />
                        <h2>
                            {youWon
                                ? 'You win!'
                                : winner.neutral
                                    ? 'Neutral wins'
                                    : `Player ${view.winnerIndex + 1} wins`}
                        </h2>
                    </>
                ) : (
                    <h2>Draw</h2>
                )}
                {view.winReason !== null && view.winReason !== undefined && (
                    <p className="muted">{view.winReason}</p>
                )}
                <button className="btn btn-accent" onClick={onBackToLobby}>
                    Back to lobby
                </button>
            </div>
        </div>
    );
}
