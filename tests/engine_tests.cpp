#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cassert>

using namespace winrisk;

int main() {
    const auto world = GameEngine::makeWorldTerritories();
    assert(world.size() == 42);
    assert(world[0].name == "Alaska");
    assert(world[29].name == "Kamchatka");
    assert(std::find(world[0].adjacent.begin(), world[0].adjacent.end(), 29) != world[0].adjacent.end());

    GameEngine first;
    GameEngine second;
    assert(first.startNewGame(4, 1, 4242));
    assert(second.startNewGame(4, 1, 4242));
    assert(first.currentPlayerId() == second.currentPlayerId());
    assert(first.phase() == Phase::Reinforce);
    assert(first.players().size() == 4);
    assert(first.territories().size() == 42);

    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        assert(first.territories()[i].owner == second.territories()[i].owner);
        assert(first.territories()[i].armies == second.territories()[i].armies);
        assert(first.territories()[i].owner >= 0 && first.territories()[i].owner < 4);
        assert(first.territories()[i].armies >= 1);
    }

    for (int playerId = 0; playerId < 4; ++playerId) {
        int armies = 0;
        for (const auto& territory : first.territories()) {
            if (territory.owner == playerId) {
                armies += territory.armies;
            }
        }
        assert(armies == GameEngine::startingTroops(4));
    }

    const int current = first.currentPlayerId();
    int owned = -1;
    for (const auto& territory : first.territories()) {
        if (territory.owner == current) {
            owned = territory.id;
            break;
        }
    }
    assert(owned >= 0);
    const int reinforcements = first.currentPlayer()->reinforcements;
    assert(reinforcements >= 3);
    assert(first.reinforce(owned, reinforcements));
    assert(first.currentPlayer()->reinforcements == 0);
    assert(first.endPhase());
    assert(first.phase() == Phase::Attack);
    assert(first.endPhase());
    assert(first.phase() == Phase::Maneuver);
    assert(first.endPhase());
    assert(first.phase() == Phase::Reinforce);
    assert(first.turn() == 2);

    const Snapshot saved = first.snapshot();
    GameEngine restored;
    assert(restored.restore(saved));
    assert(restored.currentPlayerId() == first.currentPlayerId());
    assert(restored.phase() == first.phase());
    assert(restored.turn() == first.turn());
    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        assert(restored.territories()[i].owner == first.territories()[i].owner);
        assert(restored.territories()[i].armies == first.territories()[i].armies);
    }

    return 0;
}
