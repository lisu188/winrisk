#include "engine/GameEngine.hpp"

#include <algorithm>
#include <array>
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

bool sameMission(const std::optional<MissionSpec>& a, const std::optional<MissionSpec>& b) {
    if (a.has_value() != b.has_value()) {
        return false;
    }
    if (!a) {
        return true;
    }
    return a->kind == b->kind
        && a->territories == b->territories
        && a->minimumArmies == b->minimumArmies
        && a->continentCount == b->continentCount
        && a->eliminationTarget == b->eliminationTarget;
}

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

    const auto missions = GameEngine::makeMissionDeck(4);
    CHECK(missions.size() == 9);
    CHECK(missions[0].kind == MissionKind::Territory && missions[0].territories == 24);
    CHECK(missions[1].kind == MissionKind::FortifiedTerritory && missions[1].territories == 18 && missions[1].minimumArmies == 2);
    CHECK(missions[2].kind == MissionKind::Continents && missions[2].continentCount == 2);
    CHECK(missions[3].kind == MissionKind::Continents && missions[3].continentCount == 3);
    CHECK(missions[4].kind == MissionKind::FortifiedTerritory && missions[4].territories == 15 && missions[4].minimumArmies == 3);

    GameEngine first;
    GameEngine second;
    CHECK(first.startNewGame(4, 1, 4242));
    CHECK(second.startNewGame(4, 1, 4242));
    CHECK(first.mode() == GameMode::Classic);
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
    CHECK(saved.version == 4);
    CHECK(saved.mode == GameMode::Classic);
    GameEngine restored;
    CHECK(restored.restore(saved));
    CHECK(restored.mode() == GameMode::Classic);
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

        Snapshot wildSnapshot = saved;
        wildSnapshot.players[static_cast<std::size_t>(trader)].cards = {42, 43, 0};
        GameEngine wildTrading;
        CHECK(wildTrading.restore(wildSnapshot));
        CHECK(wildTrading.canTradeCards());

        Snapshot threeWildSnapshot = saved;
        threeWildSnapshot.players[static_cast<std::size_t>(trader)].cards = {42, 43, 42};
        GameEngine threeWildTrading;
        CHECK(threeWildTrading.restore(threeWildSnapshot));
        CHECK(!threeWildTrading.canTradeCards());
    }

    Snapshot legacyV3 = saved;
    legacyV3.version = 3;
    legacyV3.mode = GameMode::SecretMission;
    for (auto& player : legacyV3.players) {
        player.mission.reset();
    }
    GameEngine upgradedV3;
    CHECK(upgradedV3.restore(legacyV3));
    CHECK(upgradedV3.mode() == GameMode::Classic);

    Snapshot legacyNative = saved;
    legacyNative.version = 2;
    legacyNative.mode = GameMode::SecretMission;
    legacyNative.deck.clear();
    legacyNative.discard.clear();
    legacyNative.tradeCount = 0;
    legacyNative.conqueredThisTurn = false;
    for (auto& player : legacyNative.players) {
        player.cards.clear();
        player.mission.reset();
    }
    GameEngine upgraded;
    CHECK(upgraded.restore(legacyNative));
    CHECK(upgraded.mode() == GameMode::Classic);
    CHECK(upgraded.deck().size() == 44);
    CHECK(upgraded.tradeCount() == 0);

    GameEngine secretA;
    GameEngine secretB;
    CHECK(!secretA.startNewGame(2, 1, 9001, GameMode::SecretMission));
    CHECK(secretA.startNewGame(4, 1, 9001, GameMode::SecretMission));
    CHECK(secretB.startNewGame(4, 1, 9001, GameMode::SecretMission));
    CHECK(secretA.mode() == GameMode::SecretMission);
    CHECK(secretA.currentPlayerId() == secretB.currentPlayerId());
    CHECK(secretA.players().size() == 4);
    for (std::size_t i = 0; i < secretA.players().size(); ++i) {
        const auto& a = secretA.players()[i];
        const auto& b = secretB.players()[i];
        CHECK(a.mission.has_value());
        CHECK(sameMission(a.mission, b.mission));
        if (a.mission && a.mission->kind == MissionKind::Elimination) {
            CHECK(a.mission->eliminationTarget != a.id);
        }
        CHECK(!secretA.missionText(a.id).empty());
    }

    const Snapshot secretSaved = secretA.snapshot();
    CHECK(secretSaved.version == 4);
    CHECK(secretSaved.mode == GameMode::SecretMission);
    GameEngine secretRestored;
    CHECK(secretRestored.restore(secretSaved));
    CHECK(secretRestored.mode() == GameMode::SecretMission);
    for (std::size_t i = 0; i < secretA.players().size(); ++i) {
        CHECK(sameMission(secretA.players()[i].mission, secretRestored.players()[i].mission));
    }

    Snapshot winningMission = secretSaved;
    winningMission.phase = Phase::Attack;
    winningMission.winner = -1;
    for (std::size_t i = 0; i < winningMission.players.size(); ++i) {
        winningMission.players[i].mission = MissionSpec{MissionKind::Territory, 42, 0, 0, -1};
    }
    winningMission.players[0].mission = MissionSpec{MissionKind::Territory, 24, 0, 0, -1};
    for (int territoryId = 0; territoryId < 42; ++territoryId) {
        auto& territory = winningMission.territories[static_cast<std::size_t>(territoryId)];
        if (territoryId < 24) territory.owner = 0;
        else if (territoryId < 30) territory.owner = 1;
        else if (territoryId < 36) territory.owner = 2;
        else territory.owner = 3;
        territory.armies = 1;
    }
    GameEngine missionWinner;
    CHECK(missionWinner.restore(winningMission));
    CHECK(missionWinner.winnerId() == 0);
    CHECK(missionWinner.phase() == Phase::Finished);

    GameEngine capitalA;
    GameEngine capitalB;
    CHECK(!capitalA.startNewGame(2, 1, 12345, GameMode::Capital));
    CHECK(capitalA.startNewGame(4, 1, 12345, GameMode::Capital));
    CHECK(capitalB.startNewGame(4, 1, 12345, GameMode::Capital));
    CHECK(capitalA.mode() == GameMode::Capital);
    CHECK(capitalA.currentPlayerId() == capitalB.currentPlayerId());
    CHECK(capitalA.deck().size() == 40);
    CHECK(capitalA.players().size() == 4);

    std::array<bool, 42> seenHeadquarters{};
    for (const auto& player : capitalA.players()) {
        CHECK(player.headquarters >= 0 && player.headquarters < 42);
        if (player.headquarters >= 0 && player.headquarters < 42) {
            CHECK(!seenHeadquarters[static_cast<std::size_t>(player.headquarters)]);
            seenHeadquarters[static_cast<std::size_t>(player.headquarters)] = true;
            CHECK(capitalA.territories()[static_cast<std::size_t>(player.headquarters)].owner == player.id);
            CHECK(capitalA.headquartersOwner(player.headquarters) == player.id);
            CHECK(capitalA.headquartersControlledBy(player.id) == 1);
            CHECK(!capitalA.capitalObjectiveText(player.id).empty());
            int firstOwned = -1;
            for (const auto& territory : capitalA.territories()) {
                if (territory.owner == player.id) {
                    firstOwned = territory.id;
                    break;
                }
            }
            CHECK(player.headquarters == firstOwned);
        }
    }
    for (const auto& card : capitalA.deck()) {
        CHECK(card.territoryId < 0 || capitalA.headquartersOwner(card.territoryId) < 0);
    }

    const Snapshot capitalSaved = capitalA.snapshot();
    CHECK(capitalSaved.version == 4);
    CHECK(capitalSaved.mode == GameMode::Capital);
    GameEngine capitalRestored;
    CHECK(capitalRestored.restore(capitalSaved));
    CHECK(capitalRestored.mode() == GameMode::Capital);
    CHECK(capitalRestored.deck().size() == capitalA.deck().size());
    for (std::size_t i = 0; i < capitalA.players().size(); ++i) {
        CHECK(capitalRestored.players()[i].headquarters == capitalA.players()[i].headquarters);
    }

    Snapshot capitalWin = capitalSaved;
    capitalWin.phase = Phase::Attack;
    capitalWin.winner = -1;
    for (std::size_t i = 0; i < capitalWin.players.size(); ++i) {
        const int hq = capitalWin.players[i].headquarters;
        capitalWin.territories[static_cast<std::size_t>(hq)].owner = 0;
    }
    for (std::size_t playerId = 1; playerId < capitalWin.players.size(); ++playerId) {
        for (auto& territory : capitalWin.territories) {
            if (capitalWin.headquarters < 0) {
                break;
            }
            if (territory.owner == 0 && !seenHeadquarters[static_cast<std::size_t>(territory.id)]) {
                territory.owner = static_cast<int>(playerId);
                break;
            }
        }
    }
    GameEngine capitalWinner;
    CHECK(capitalWinner.restore(capitalWin));
    CHECK(capitalWinner.winnerId() == 0);
    CHECK(capitalWinner.phase() == Phase::Finished);

    Snapshot lostOwnHq = capitalWin;
    lostOwnHq.phase = Phase::Attack;
    lostOwnHq.winner = -1;
    const int playerZeroHq = lostOwnHq.players[0].headquarters;
    lostOwnHq.territories[static_cast<std::size_t>(playerZeroHq)].owner = 1;
    GameEngine noCapitalWinner;
    CHECK(noCapitalWinner.restore(lostOwnHq));
    CHECK(noCapitalWinner.winnerId() < 0);
    CHECK(noCapitalWinner.phase() == Phase::Attack);

    if (failures != 0) {
        std::cerr << failures << " test checks failed\n";
    }
    return failures == 0 ? 0 : 1;
}
