import type { GameView } from '../api/types';
import { rgb } from '../api/types';

export interface HudProps {
    view: GameView;
    placeAll: boolean;
    onTogglePlaceAll: () => void;
    onEndPhase: () => void;
    onSave: (name: string) => void;
}

function seatName(view: GameView, index: number): string {
    const player = view.players[index];
    if (!player) {
        return 'nobody';
    }
    if (player.neutral) {
        return 'Neutral';
    }
    return index === view.humanPlayerIndex ? `Player ${index + 1} (You)` : `Player ${index + 1}`;
}

export default function Hud({ view, placeAll, onTogglePlaceAll, onEndPhase, onSave }: HudProps) {
    const yourTurn = view.currentPlayerIndex === view.humanPlayerIndex;
    const gameOver = view.winnerIndex >= 0 || view.draw;

    const handleSaveClick = () => {
        const name = window.prompt('Save name (letters, digits, - and _):', 'my-game');
        if (name !== null && name.trim() !== '') {
            onSave(name.trim());
        }
    };

    return (
        <div className="hud-content">
            <h1 className="hud-title">WinRisk</h1>
            <div className="muted">Mode: {view.mode}</div>

            <div className="hud-section">
                <div className="hud-phase">Phase: {view.phase}</div>
                {yourTurn && view.phase === 'REINFORCE' && (
                    <div>Reinforcements: {view.reinforcements}</div>
                )}
            </div>

            <div className="hud-section">
                <h2 className="hud-header">Players</h2>
                {view.players.map((player, index) => (
                    <div
                        key={index}
                        className={
                            'legend-row' +
                            (index === view.currentPlayerIndex ? ' current' : '') +
                            (player.dead ? ' dead' : '')
                        }
                    >
                        <span className="swatch" style={{ background: rgb(player.colorRgb) }} />
                        <span className="legend-text">
                            {seatName(view, index)}
                            {!player.neutral && (player.ai ? ' (AI)' : '')}
                            {'  '}
                            {player.territories} terr
                            {!player.neutral ? `, ${player.cardCount} cards` : ''}
                        </span>
                    </div>
                ))}
            </div>

            {view.missionText !== null && view.missionText !== undefined && (
                <div className="hud-section muted">Mission: {view.missionText}</div>
            )}
            {view.hqName !== null && view.hqName !== undefined && (
                <div className="hud-section muted">Headquarters: {view.hqName}</div>
            )}

            {!gameOver && (
                <div className="hud-section">
                    {yourTurn && view.phase === 'REINFORCE' && (
                        <label className="place-all-toggle">
                            <input
                                type="checkbox"
                                checked={placeAll}
                                onChange={onTogglePlaceAll}
                            />{' '}
                            Place all on click
                        </label>
                    )}
                    <button
                        className="btn btn-accent"
                        onClick={onEndPhase}
                        disabled={!yourTurn}
                    >
                        {yourTurn ? 'End Phase' : 'AI thinking…'}
                    </button>
                    <button className="btn" onClick={handleSaveClick}>
                        Save game
                    </button>
                </div>
            )}

            <div className="hud-hints muted">
                {view.phase === 'REINFORCE' && yourTurn && 'Click your territory to place troops.'}
                {view.phase === 'ATTACK' && yourTurn &&
                    'Click your territory, then an adjacent enemy to attack.'}
                {view.phase === 'MOVE' && yourTurn &&
                    'Click a source, then a connected territory to fortify.'}
            </div>
        </div>
    );
}
