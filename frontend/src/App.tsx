import { useEffect, useState } from 'react';
import Lobby from './lobby/Lobby';
import GameScreen from './game/GameScreen';

type Route =
    | { view: 'lobby' }
    | { view: 'game'; gameId: string };

function parseHash(hash: string): Route {
    const match = /^#\/game\/([^/]+)$/.exec(hash);
    if (match) {
        return { view: 'game', gameId: decodeURIComponent(match[1]) };
    }
    return { view: 'lobby' };
}

export default function App() {
    const [route, setRoute] = useState<Route>(() => parseHash(window.location.hash));

    useEffect(() => {
        const onHashChange = () => setRoute(parseHash(window.location.hash));
        window.addEventListener('hashchange', onHashChange);
        return () => window.removeEventListener('hashchange', onHashChange);
    }, []);

    if (route.view === 'game') {
        return <GameScreen key={route.gameId} gameId={route.gameId} />;
    }
    return <Lobby />;
}
