import { Client } from '@stomp/stompjs';
import type { GameView } from '../api/types';

function brokerUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    return `${protocol}://${window.location.host}/ws`;
}

/**
 * Subscribe to live GameView frames for a game. Each STOMP frame on
 * /topic/games/{gameId} carries a GameView JSON body and is passed to onView.
 * Returns a cleanup function that unsubscribes and deactivates the client.
 */
export function subscribeToGame(gameId: string, onView: (v: GameView) => void): () => void {
    const client = new Client({
        brokerURL: brokerUrl(),
        reconnectDelay: 2000,
        onConnect: () => {
            client.subscribe(`/topic/games/${gameId}`, message => {
                try {
                    const view = JSON.parse(message.body) as GameView;
                    onView(view);
                } catch (e) {
                    console.error('Failed to parse GameView frame', e);
                }
            });
        },
        onStompError: frame => {
            console.error('STOMP error', frame.headers['message'], frame.body);
        },
    });

    client.activate();

    return () => {
        void client.deactivate();
    };
}
