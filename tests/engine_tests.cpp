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

    const auto cards = GameEngine::makeRiskDeck();
    assert(cards.size() == 44);
    assert(cards[0].type == CardType::Infantry);
    assert(cards[1].type == CardType::Cavalry);
    assert(cards[2].type == CardType::Artillery);
    assert(cards[42].type == CardType::Wild);
    assert(cards[43].type == CardType::Wild);
    assert(GameEngine::tradeValue(0) == 4);
    assert(GameEngine::tradeValue(5) == 15);
    assert(GameEngine::tradeValue(6) == 20);

    GameEngine first;
    GameEngine second;
    assert(first.startNewGame(4, 1, 4242));
    assert(second.startNewGame(4, 1, 4242));
    assert(first.currentPlayerId() == second.currentPlayerId());
    assert(first.phase() == Phase::Reinforce);
    assert(first.players().size() == 4);
    assert(first.territories().size() == 42);
    assert(first.deck().size() == 44);
    assert(second.deck().size() == 44);

    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        assert(first.territories()[i].owner == second.territories()[i].owner);
        assert(first.territories()[i].armies == second.territories()[i].armies);
        assert(first.territories()[i].owner >= 0 && first.territories()[i].owner < 4);
        assert(first.territories()[i].armies >= 1);
    }
    for (std::size_t i = 0; i < first.deck().size(); ++i) {
        assert(first.deck()[i].id == second.deck()[i].id);
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
    assert(saved.version == 3);
    GameEngine restored;
    assert(restored.restore(saved));
    assert(restored.currentPlayerId() == first.currentPlayerId());
    assert(restored.phase() == first.phase());
    assert(restored.turn() == first.turn());
    assert(restored.deck().size() == first.deck().size());
    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        assert(restored.territories()[i].owner == first.territories()[i].owner);
        assert(restored.territories()[i].armies == first.territories()[i].armies);
    }

    Snapshot tradeSnapshot = saved;
    const int trader = tradeSnapshot.currentPlayer;
    tradeSnapshot.players[static_cast<std::size_t>(trader)].cards = {0, 3, 6};
    tradeSnapshot.deck.erase(
        std::remove_if(tradeSnapshot.deck.begin(), tradeSnapshot.deck.end(), [](const Card& card) {
            return card.id == 0 || card.id == 3 || card.id == 6;
        }),
        tradeSnapshot.deck.end());
    GameEngine trading;
    assert(trading.restore(tradeSnapshot));
    assert(trading.canTradeCards());
    const int beforeTrade = trading.currentPlayer()->reinforcements;
    assert(trading.tradeCards() == 4);
    assert(trading.currentPlayer()->reinforcements == beforeTrade + 4);
    assert(trading.currentPlayer()->cards.empty());
    assert(trading.discard().size() == 3);
    assert(trading.tradeCount() == 1);
    assert(trading.nextTradeValue() == 6);

    Snapshot legacyNative = saved;
    legacyNative.version = 2;
    legacyNative.deck.clear();
    legacyNative.discard.clear();
    legacyNative.tradeCount = 0;
    legacyNative.conqueredThisTurn = false;
    for (auto& player : legacyNative.players) {
        player.cards.clear();
    }
    GameEngine upgraded;
    assert(upgraded.restore(legacyNative));
    assert(upgraded.deck().size() == 44);
    assert(upgraded.tradeCount() == 0);

    return 0;
}
