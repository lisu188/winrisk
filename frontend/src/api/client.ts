import type {
    AttackResponse,
    CreateGameRequest,
    CreateGameResponse,
    GameSummary,
    GameView,
    MapsResponse,
    TradeSet,
} from './types';

const BASE = '/api';

export class ApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const res = await fetch(`${BASE}${path}`, init);
    if (!res.ok) {
        let message = res.statusText || `HTTP ${res.status}`;
        try {
            const body: unknown = await res.json();
            if (
                typeof body === 'object' &&
                body !== null &&
                typeof (body as { error?: unknown }).error === 'string'
            ) {
                message = (body as { error: string }).error;
            }
        } catch {
            // non-JSON error body; keep the status text
        }
        throw new ApiError(res.status, message);
    }
    if (res.status === 204) {
        return undefined as T;
    }
    return (await res.json()) as T;
}

function get<T>(path: string): Promise<T> {
    return request<T>(path);
}

function post<T>(path: string, body: unknown): Promise<T> {
    return request<T>(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

function del<T>(path: string): Promise<T> {
    return request<T>(path, { method: 'DELETE' });
}

export function createGame(req: CreateGameRequest): Promise<CreateGameResponse> {
    return post('/games', req);
}

export function listGames(): Promise<GameSummary[]> {
    return get('/games');
}

export function getGame(id: string): Promise<GameView> {
    return get(`/games/${id}`);
}

export function backgroundUrl(id: string): string {
    return `${BASE}/games/${id}/background`;
}

export function place(id: string, fieldIndex: number, all: boolean): Promise<GameView> {
    return post(`/games/${id}/place`, { fieldIndex, all });
}

export function attack(id: string, from: number, to: number): Promise<AttackResponse> {
    return post(`/games/${id}/attack`, { from, to });
}

export function maneuver(id: string, from: number, to: number, troops: number): Promise<GameView> {
    return post(`/games/${id}/maneuver`, { from, to, troops });
}

export function endPhase(id: string): Promise<GameView> {
    return post(`/games/${id}/end-phase`, {});
}

export function tradeSets(id: string): Promise<TradeSet[]> {
    return get(`/games/${id}/trade-sets`);
}

export function trade(id: string, cardIndexes: number[]): Promise<GameView> {
    return post(`/games/${id}/trade`, { cardIndexes });
}

export function saveGame(id: string, name: string): Promise<void> {
    return post(`/games/${id}/save`, { name });
}

export function listSaves(): Promise<string[]> {
    return get('/saves');
}

export function loadGame(name: string): Promise<CreateGameResponse> {
    return post('/games/load', { name });
}

export function listMaps(): Promise<MapsResponse> {
    return get('/maps');
}

export function deleteGame(id: string): Promise<void> {
    return del(`/games/${id}`);
}
