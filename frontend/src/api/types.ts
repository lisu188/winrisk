export type GameMode = 'classic' | 'secret' | 'capital';

export type GamePhase = 'REINFORCE' | 'ATTACK' | 'MOVE' | 'UNDEFINED';

export type CardSymbol = 'INFANTRY' | 'CAVALRY' | 'ARTILLERY' | 'WILD';

export interface Rules {
    attackWithAll: boolean;
    fogOfWar: boolean;
    skynet: boolean;
    incrementalCardSetValues: boolean;
    expandedManeuver: boolean;
    attackCardReroll: boolean;
    commanderDie: boolean;
}

export interface FieldView {
    index: number;
    name: string;
    x: number;
    y: number;
    ownerIndex: number; // -1 unowned/hidden
    army: number;
    continentIndex: number;
}

export interface ContinentView {
    index: number;
    bonus: number;
    colorRgb: number;
}

export interface PlayerView {
    colorRgb: number;
    textColorRgb: number;
    neutral: boolean;
    ai: boolean;
    dead: boolean;
    territories: number;
    cardCount: number;
    reinforcements: number;
}

export interface CardView {
    fieldIndex: number;
    symbol: CardSymbol;
    territoryName: string;
}

export interface GameView {
    id: string;
    version: number;
    mode: GameMode;
    phase: GamePhase;
    currentPlayerIndex: number;
    humanPlayerIndex: number;
    reinforcements: number;
    maneuverUsed: boolean;
    commanderDieUsed: boolean;
    winnerIndex: number; // -1 while playing
    winReason: string | null;
    draw: boolean;
    hasBackgroundImage: boolean;
    missionText: string | null;
    hqName: string | null;
    hqIndex: number;
    rules: Rules;
    fields: FieldView[];
    continents: ContinentView[];
    links: [number, number][]; // field index pairs, a<b, each edge once
    players: PlayerView[];
    hand: CardView[];
    tradeForced: boolean;
}

export interface GameSummary {
    id: string;
    mode: GameMode;
    mapName: string;
    players: number;
    phase: GamePhase;
    winnerIndex: number;
    createdAt: string;
}

export interface MapSpec {
    builtin?: string;
    file?: string;
    random?: {
        fields: number;
        continents: number;
    };
}

export interface CreateGameRequest {
    mode: GameMode;
    aiPlayers: number;
    map: MapSpec;
    rules: Rules;
    seed?: number;
}

export interface CreateGameResponse {
    id: string;
    view: GameView;
}

export interface PlaceRequest {
    fieldIndex: number;
    all: boolean;
}

export interface AttackRequest {
    from: number;
    to: number;
}

export interface CombatResult {
    captured: boolean;
    attackerDice: number[];
    defenderDice: number[];
    attackerLosses: number;
    defenderLosses: number;
}

export interface AttackResponse {
    view: GameView;
    combat: CombatResult;
}

export interface ManeuverRequest {
    from: number;
    to: number;
    troops: number;
}

export interface TradeSet {
    cardIndexes: number[];
    symbols: string[];
    territoryNames: string[];
    value: number;
}

export interface TradeRequest {
    cardIndexes: number[];
}

export interface SaveGameRequest {
    name: string;
}

export interface LoadGameRequest {
    name: string;
}

export interface MapsResponse {
    builtin: string[];
    files: string[];
}

export interface ApiErrorBody {
    error: string;
}

/** Convert a java.awt.Color.getRGB() packed int to a CSS rgb() string. */
export function rgb(v: number): string {
    const r = (v >> 16) & 255;
    const g = (v >> 8) & 255;
    const b = v & 255;
    return `rgb(${r}, ${g}, ${b})`;
}
