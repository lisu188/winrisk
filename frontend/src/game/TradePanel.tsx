import { useEffect, useState } from 'react';
import type { GameView, TradeSet } from '../api/types';
import * as api from '../api/client';
import { ApiError } from '../api/client';

export interface TradePanelProps {
    view: GameView;
    onTraded: (view: GameView) => void;
}

const SYMBOL_ICONS: Record<string, string> = {
    INFANTRY: '⚔',
    CAVALRY: '♞',
    ARTILLERY: '♜',
    WILD: '★',
};

export default function TradePanel({ view, onTraded }: TradePanelProps) {
    const [sets, setSets] = useState<TradeSet[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);

    const handSize = view.hand.length;
    useEffect(() => {
        let cancelled = false;
        api.tradeSets(view.id)
            .then(result => {
                if (!cancelled) {
                    setSets(result);
                    setError(null);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setError('Could not load trade sets');
                }
            });
        return () => {
            cancelled = true;
        };
    }, [view.id, handSize, view.version]);

    const doTrade = (set: TradeSet) => {
        setBusy(true);
        api.trade(view.id, set.cardIndexes)
            .then(onTraded)
            .catch(e => setError(e instanceof ApiError ? e.message : 'Trade failed'))
            .finally(() => setBusy(false));
    };

    return (
        <div className="panel trade-panel">
            <h2 className="hud-header">Cards</h2>
            {view.tradeForced && (
                <div className="trade-forced">You must trade a set this turn.</div>
            )}
            <div className="hand">
                {view.hand.map((card, index) => (
                    <span key={index} className="card" title={card.territoryName}>
                        {SYMBOL_ICONS[card.symbol] ?? '?'}{' '}
                        {card.symbol === 'WILD' ? 'Wild' : card.territoryName}
                    </span>
                ))}
            </div>
            {error !== null && <div className="error-text">{error}</div>}
            {sets.length === 0 && <div className="muted">No tradeable set yet.</div>}
            {sets.map((set, index) => (
                <div key={index} className="trade-set">
                    <span className="trade-set-desc">
                        {set.symbols.map(symbol => SYMBOL_ICONS[symbol] ?? '?').join(' ')}
                        {'  +'}
                        {set.value}
                    </span>
                    <button
                        className="btn btn-accent"
                        disabled={busy}
                        onClick={() => doTrade(set)}
                    >
                        Trade
                    </button>
                </div>
            ))}
        </div>
    );
}
