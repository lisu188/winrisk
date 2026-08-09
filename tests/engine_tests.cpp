#include "engine/GameEngine.hpp"

#include <algorithm>
#include <iostream>

using namespace winrisk;

namespace {

int failures = 0;

void check(bool condition, const char* expression, int line) {
    if (!condition) {
        std::cerr << "FAIL line " << line << ": " << expression << '\n';
        ++failures;
    }
}

#define CHECK(expression) check(static_cast<bool>(expression), #expression, __LINE__)

}

int main() {
    const auto world = GameEngine::makeWorldTerritories();
    CHECK(world.size() == 42);
    CHECK(world[0].name == "Alaska");
    CHECK(world[29].name == "Kamchatka");
    CHECK(std::find(world[0].adjacent.begin(), world[0].adjacent.end(), 29) != world[0].adjacent.end());

    const auto cards = GameEngine::makeRiskDeck();
    CHECK(cards.size() == 44);
    CHECK(cards[0].type == CardType::Infantry);
    CHECK(cards[1].type == CardType::Cavalry);
    CHECK(cards[2].type == CardType::Artillery);
    CHECK(cards[42].type == CardType::Wild);
    CHECK(cards[43].type == CardType::Wild);
    CHECK(GameEngine::tradeValue(0) == 4);
    CHECK(GameEngine::tradeValue(5) == 15);
    CHECK(GameEngine::tradeValue(6) == 20);

    GameEngine first;
    GameEngine second;
    CHECK(first.startNewGame(4, 1, 4242));
    CHECK(second.startNewGame(4, 1, 4242));
    CHECK(first.currentPlayerId() == second.currentPlayerId());
    CHECK(first.phase() == Phase::Reinforce);
    CHECK(first.players().size() == 4);
    CHECK(first.territories().size() == 42);
    CHECK(first.deck().size() == 44);
    CHECK(second.deck().size() == 44);

    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        CHECK(first.territories()[i].owner == second.territories()[i].owner);
        CHECK(first.territories()[i].armies == second.territories()[i].armies);
        CHECK(first.territories()[i].owner >= 0 && first.territories()[i].owner < 4);
        CHECK(first.territories()[i].armies >= 1);
    }
    for (std::size_t i = 0; i < first.deck().size(); ++i) {
        CHECK(first.deck()[i].id == second.deck()[i].id);
    }

    for (int playerId = 0; playerId < 4; ++playerId) {
        int armies = 0;
        for (const auto& territory : first.territories()) {
            if (territory.owner == playerId) {
                armies += territory.armies;
            }
        }
        CHECK(armies == GameEngine::startingTroops(4));
    }

    const int current = first.currentPlayerId();
    int owned = -1;
    for (const auto& territory : first.territories()) {
        if (territory.owner == current) {
            owned = territory.id;
            break;
        }
    }
    CHECK(owned >= 0);
    const auto* currentPlayer = first.currentPlayer();
    CHECK(currentPlayer != nullptr);
    if (currentPlayer != nullptr) {
        const int reinforcements = currentPlayer->reinforcements;
        CHECK(reinforcements >= 3);
        CHECK(first.reinforce(owned, reinforcements));
        CHECK(first.currentPlayer()->reinforcements == 0);
        CHECK(first.endPhase());
        CHECK(first.phase() == Phase::Attack);
        CHECK(first.endPhase());
        CHECK(first.phase() == Phase::Maneuver);
        CHECK(first.endPhase());
        CHECK(first.phase() == Phase::Reinforce);
        CHECK(first.turn() == 2);
    }

    const Snapshot saved = first.snapshot();
    CHECK(saved.version == 3);
    GameEngine restored;
    CHECK(restored.restore(saved));
    CHECK(restored.currentPlayerId() == first.currentPlayerId());
    CHECK(restored.phase() == first.phase());
    CHECK(restored.turn() == first.turn());
    CHECK(restored.deck().size() == first.deck().size());
    for (std::size_t i = 0; i < first.territories().size(); ++i) {
        CHECK(restored.territories()[i].owner == first.territories()[i].owner);
        CHECK(restored.territories()[i].armies == first.territories()[i].armies);
    }

    Snapshot tradeSnapshot = saved;
    const int trader = tradeSnapshot.currentPlayer;
    CHECK(trader >= 0 && trader < static_cast<int>(tradeSnapshot.players.size()));
    if (trader >= 0 && trader < static_cast<int>(tradeSnapshot.players.size())) {
        tradeSnapshot.players[static_cast<std::size_t>(trader)].cards = {0, 3, 6};
        tradeSnapshot.deck.erase(
            std::remove_if(tradeSnapshot.deck.begin(), tradeSnapshot.deck.end(), [](const Card& card) {
                return card.id == 0 || card.id == 3 || card.id == 6;
            }),
            tradeSnapshot.deck.end());
        GameEngine trading;
        CHECK(trading.restore(tradeSnapshot));
        CHECK(trading.canTradeCards());
        const auto* tradingPlayer = trading.currentPlayer();
        CHECK(tradingPlayer != nullptr);
        if (tradingPlayer != nullptr) {
            const int beforeTrade = tradingPlayer->reinforcements;
            CHECK(trading.tradeCards() == 4);
            CHECK(trading.currentPlayer()->reinforcements == beforeTrade + 4);
            CHECK(trading.currentPlayer()->cards.empty());
            CHECK(trading.discard().size() == 3);
            CHECK(trading.tradeCount() == 1);
            CHECK(trading.nextTradeValue() == 6);
        }
    }

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
    CHECK(upgraded.restore(legacyNative));
    CHECK(upgraded.deck().size() == 44);
    CHECK(upgraded.tradeCount() == 0);

    if (failures != 0) {
        std::cerr << failures << " test checks failed\n";
    }
    return failures == 0 ? 0 : 1;
}
