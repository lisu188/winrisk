import { FormEvent, useState } from 'react';
import * as api from '../api/client';
import type { CreateGameRequest, GameMode, MapSpec, MapsResponse, Rules } from '../api/types';

interface CreateGameFormProps {
    maps: MapsResponse;
    onCreated: (id: string) => void;
}

const RANDOM_VALUE = 'random';

const RULE_ITEMS: { key: keyof Rules; label: string }[] = [
    { key: 'attackWithAll', label: 'Attack with all' },
    { key: 'fogOfWar', label: 'Fog of war' },
    { key: 'skynet', label: 'Skynet' },
    { key: 'incrementalCardSetValues', label: 'Incremental card values' },
    { key: 'expandedManeuver', label: 'Expanded maneuver' },
    { key: 'attackCardReroll', label: 'Attack card reroll' },
    { key: 'commanderDie', label: 'Commander die' },
];

const DEFAULT_RULES: Rules = {
    attackWithAll: false,
    fogOfWar: false,
    skynet: false,
    incrementalCardSetValues: false,
    expandedManeuver: false,
    attackCardReroll: false,
    commanderDie: false,
};

function defaultMapValue(maps: MapsResponse): string {
    if (maps.builtin.length > 0) {
        return `builtin:${maps.builtin[0]}`;
    }
    if (maps.files.length > 0) {
        return `file:${maps.files[0]}`;
    }
    return RANDOM_VALUE;
}

function parseBoundedInt(raw: string, min: number, max: number): number | null {
    const n = Number(raw.trim());
    if (raw.trim() === '' || !Number.isFinite(n)) {
        return null;
    }
    const i = Math.round(n);
    if (i < min || i > max) {
        return null;
    }
    return i;
}

export default function CreateGameForm({ maps, onCreated }: CreateGameFormProps) {
    const [mode, setMode] = useState<GameMode>('classic');
    const [mapValue, setMapValue] = useState<string>(() => defaultMapValue(maps));
    const [randomFields, setRandomFields] = useState('40');
    const [randomContinents, setRandomContinents] = useState('6');
    const [aiPlayers, setAiPlayers] = useState(3);
    const [seed, setSeed] = useState('');
    const [rules, setRules] = useState<Rules>(DEFAULT_RULES);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const minAi = mode === 'classic' ? 1 : 2;
    const aiOptions: number[] = [];
    for (let n = minAi; n <= 4; n++) {
        aiOptions.push(n);
    }

    function handleModeChange(next: GameMode) {
        setMode(next);
        const min = next === 'classic' ? 1 : 2;
        if (aiPlayers < min) {
            setAiPlayers(min);
        }
    }

    function toggleRule(key: keyof Rules) {
        setRules(prev => ({ ...prev, [key]: !prev[key] }));
    }

    function buildMapSpec(): MapSpec | null {
        if (mapValue === RANDOM_VALUE) {
            const fields = parseBoundedInt(randomFields, 8, 200);
            const continents = parseBoundedInt(randomContinents, 1, 20);
            if (fields === null) {
                setError('Random map: fields must be a number between 8 and 200.');
                return null;
            }
            if (continents === null) {
                setError('Random map: continents must be a number between 1 and 20.');
                return null;
            }
            return { random: { fields, continents } };
        }
        if (mapValue.startsWith('builtin:')) {
            return { builtin: mapValue.slice('builtin:'.length) };
        }
        return { file: mapValue.slice('file:'.length) };
    }

    async function handleSubmit(e: FormEvent<HTMLFormElement>) {
        e.preventDefault();
        if (busy) {
            return;
        }
        setError(null);
        const map = buildMapSpec();
        if (map === null) {
            return;
        }
        const req: CreateGameRequest = { mode, aiPlayers, map, rules };
        if (seed.trim() !== '') {
            const seedNum = Number(seed.trim());
            if (!Number.isFinite(seedNum)) {
                setError('Seed must be a number.');
                return;
            }
            req.seed = seedNum;
        }
        setBusy(true);
        try {
            const res = await api.createGame(req);
            onCreated(res.id);
        } catch (err) {
            setError(err instanceof Error ? err.message : String(err));
            setBusy(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="form-row">
                <label htmlFor="cgf-mode">Mode</label>
                <select
                    id="cgf-mode"
                    value={mode}
                    onChange={e => handleModeChange(e.target.value as GameMode)}
                >
                    <option value="classic">Classic</option>
                    <option value="secret">Secret mission</option>
                    <option value="capital">Capital</option>
                </select>
            </div>

            <div className="form-row">
                <label htmlFor="cgf-map">Map</label>
                <select id="cgf-map" value={mapValue} onChange={e => setMapValue(e.target.value)}>
                    {maps.builtin.length > 0 && (
                        <optgroup label="Built-in">
                            {maps.builtin.map(name => (
                                <option key={`builtin:${name}`} value={`builtin:${name}`}>
                                    {name}
                                </option>
                            ))}
                        </optgroup>
                    )}
                    {maps.files.length > 0 && (
                        <optgroup label="Map files">
                            {maps.files.map(name => (
                                <option key={`file:${name}`} value={`file:${name}`}>
                                    {name}
                                </option>
                            ))}
                        </optgroup>
                    )}
                    <option value={RANDOM_VALUE}>Random map</option>
                </select>
            </div>

            {mapValue === RANDOM_VALUE && (
                <div className="form-row">
                    <label htmlFor="cgf-random-fields">Random size</label>
                    <input
                        id="cgf-random-fields"
                        type="number"
                        min={8}
                        max={200}
                        value={randomFields}
                        onChange={e => setRandomFields(e.target.value)}
                    />
                    <span className="muted">fields</span>
                    <input
                        type="number"
                        min={1}
                        max={20}
                        value={randomContinents}
                        onChange={e => setRandomContinents(e.target.value)}
                        aria-label="Random map continents"
                    />
                    <span className="muted">continents</span>
                </div>
            )}

            <div className="form-row">
                <label htmlFor="cgf-ai">AI opponents</label>
                <select
                    id="cgf-ai"
                    value={aiPlayers}
                    onChange={e => setAiPlayers(Number(e.target.value))}
                >
                    {aiOptions.map(n => (
                        <option key={n} value={n}>
                            {n}
                        </option>
                    ))}
                </select>
            </div>

            <div className="form-row">
                <label htmlFor="cgf-seed">Seed (optional)</label>
                <input
                    id="cgf-seed"
                    type="number"
                    value={seed}
                    onChange={e => setSeed(e.target.value)}
                    placeholder="random"
                />
            </div>

            <fieldset>
                <legend>Rules</legend>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '0.3rem 1rem',
                    }}
                >
                    {RULE_ITEMS.map(({ key, label }) => (
                        <label
                            key={key}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: '0.4rem',
                                cursor: 'pointer',
                                fontSize: '0.9rem',
                            }}
                        >
                            <input
                                type="checkbox"
                                checked={rules[key]}
                                onChange={() => toggleRule(key)}
                            />
                            {label}
                        </label>
                    ))}
                </div>
            </fieldset>

            {error && <p className="error-text">{error}</p>}

            <div className="btn-row">
                <button type="submit" className="btn btn-accent" disabled={busy}>
                    {busy ? 'Creating…' : 'Start Game'}
                </button>
                {busy && <span className="spinner" />}
            </div>
        </form>
    );
}
