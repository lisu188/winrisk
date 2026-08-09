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
    if (a.has_value() != b.has_value()) return false;
    if (!a) return true;
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
    CHECK(std::find(world[21].adjacent.begin(), world[21].adjacent.end(), 22) != world[21].adjacent.end());
    CHECK(std::find(world[30].adjacent.begin(), world[30].adjacent.end(), 31) != world[30].adjacent.end());
    std::size_t adjacencyEntries = 0;
    for (const auto& territory : world) adjacencyEntries += territory.adjacent.size();
    CHECK(adjacencyEntries == 166);

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

    GameEngine classicA;
    GameEngine classicB;
    CHECK(classicA.startNewGame(4, 1, 4242));
    CHECK(classicB.startNewGame(4, 1, 4242));
    CHECK(classicA.mode() == GameMode::Classic);
    CHECK(classicA.currentPlayerId() == classicB.currentPlayerId());
    CHECK(classicA.phase() == Phase::Reinforce);
    CHECK(classicA.players().size() == 4);
    CHECK(classicA.territories().size() == 42);
    CHECK(classicA.deck().size() == 44);
    for (std::size_t i = 0; i < classicA.territories().size(); ++i) {
        CHECK(classicA.territories()[i].owner == classicB.territories()[i].owner);
        CHECK(classicA.territories()[i].armies == classicB.territories()[i].armies);
        CHECK(classicA.territories()[i].owner >= 0 && classicA.territories()[i].owner < 4);
        CHECK(classicA.territories()[i].armies >= 1);
    }
    for (int playerId = 0; playerId < 4; ++playerId) {
        int armies = 0;
        for (const auto& territory : classicA.territories()) {
            if (territory.owner == playerId) armies += territory.armies;
        }
        CHECK(armies == GameEngine::startingTroops(4));
    }

    const int current = classicA.currentPlayerId();
    int owned = -1;
    for (const auto& territory : classicA.territories()) {
        if (territory.owner == current) {
            owned = territory.id;
            break;
        }
    }
    CHECK(owned >= 0);
    if (classicA.currentPlayer() != nullptr) {
        const int reinforcements = classicA.currentPlayer()->reinforcements;
        CHECK(reinforcements >= 3);
        CHECK(classicA.reinforce(owned, reinforcements));
        CHECK(classicA.endPhase());
        CHECK(classicA.phase() == Phase::Attack);
        CHECK(classicA.endPhase());
        CHECK(classicA.phase() == Phase::Maneuver);
        CHECK(classicA.endPhase());
        CHECK(classicA.phase() == Phase::Reinforce);
        CHECK(classicA.turn() == 2);
    }

    const Snapshot saved = classicA.snapshot();
    CHECK(saved.version == 4);
    GameEngine restored;
    CHECK(restored.restore(saved));
    CHECK(restored.mode() == GameMode::Classic);
    CHECK(restored.currentPlayerId() == classicA.currentPlayerId());
    CHECK(restored.turn() == classicA.turn());

    Snapshot tradeSnapshot = saved;
    const int trader = tradeSnapshot.currentPlayer;
    if (trader >= 0 && trader < static_cast<int>(tradeSnapshot.players.size())) {
        tradeSnapshot.players[static_cast<std::size_t>(trader)].cards = {0, 3, 6};
        tradeSnapshot.deck.erase(std::remove_if(tradeSnapshot.deck.begin(), tradeSnapshot.deck.end(), [](const Card& card) {
            return card.id == 0 || card.id == 3 || card.id == 6;
        }), tradeSnapshot.deck.end());
        GameEngine trading;
        CHECK(trading.restore(tradeSnapshot));
        CHECK(trading.canTradeCards());
        const int beforeTrade = trading.currentPlayer()->reinforcements;
        CHECK(trading.tradeCards() == 4);
        CHECK(trading.currentPlayer()->reinforcements == beforeTrade + 4);
        CHECK(trading.currentPlayer()->cards.empty());
        CHECK(trading.discard().size() == 3);
        CHECK(trading.tradeCount() == 1);
        CHECK(trading.nextTradeValue() == 6);

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
    for (auto& player : legacyV3.players) player.mission.reset();
    GameEngine upgradedV3;
    CHECK(upgradedV3.restore(legacyV3));
    CHECK(upgradedV3.mode() == GameMode::Classic);

    Snapshot legacyV2 = saved;
    legacyV2.version = 2;
    legacyV2.mode = GameMode::SecretMission;
    legacyV2.deck.clear();
    legacyV2.discard.clear();
    legacyV2.tradeCount = 0;
    legacyV2.conqueredThisTurn = false;
    for (auto& player : legacyV2.players) {
        player.cards.clear();
        player.mission.reset();
    }
    GameEngine upgradedV2;
    CHECK(upgradedV2.restore(legacyV2));
    CHECK(upgradedV2.mode() == GameMode::Classic);
    CHECK(upgradedV2.deck().size() == 44);

    GameEngine twoA;
    GameEngine twoB;
    CHECK(twoA.startNewGame(2, 1, 777, GameMode::Classic));
    CHECK(twoB.startNewGame(2, 1, 777, GameMode::Classic));
    CHECK(twoA.players().size() == 3);
    CHECK(twoA.currentPlayerId() == twoB.currentPlayerId());
    int neutralId = -1;
    int activePlayers = 0;
    for (const auto& player : twoA.players()) {
        if (player.neutral) {
            neutralId = player.id;
            CHECK(player.ai);
            CHECK(player.eliminated);
            CHECK(player.cards.empty());
        } else {
            ++activePlayers;
        }
    }
    CHECK(activePlayers == 2);
    CHECK(neutralId == 2);
    CHECK(twoA.currentPlayerId() != neutralId);
    CHECK(twoA.deck().size() == 44);
    for (int playerId = 0; playerId < 3; ++playerId) {
        int territories = 0;
        int armies = 0;
        for (const auto& territory : twoA.territories()) {
            if (territory.owner == playerId) {
                ++territories;
                armies += territory.armies;
            }
        }
        CHECK(territories == 14);
        CHECK(armies == 40);
    }
    for (std::size_t i = 0; i < twoA.territories().size(); ++i) {
        CHECK(twoA.territories()[i].owner == twoB.territories()[i].owner);
        CHECK(twoA.territories()[i].armies == twoB.territories()[i].armies);
    }

    const Snapshot twoSaved = twoA.snapshot();
    GameEngine twoRestored;
    CHECK(twoRestored.restore(twoSaved));
    CHECK(twoRestored.players().size() == 3);
    CHECK(twoRestored.players()[static_cast<std::size_t>(neutralId)].neutral);
    CHECK(twoRestored.players()[static_cast<std::size_t>(neutralId)].eliminated);
    CHECK(twoRestored.currentPlayerId() != neutralId);

    Snapshot twoTurn = twoSaved;
    twoTurn.phase = Phase::Maneuver;
    twoTurn.winner = -1;
    twoTurn.currentPlayer = 0;
    twoTurn.conqueredThisTurn = false;
    GameEngine twoTurnEngine;
    CHECK(twoTurnEngine.restore(twoTurn));
    CHECK(twoTurnEngine.endPhase());
    CHECK(twoTurnEngine.currentPlayerId() == 1);
    CHECK(!twoTurnEngine.currentPlayer()->neutral);

    Snapshot twoWinnerSnapshot = twoSaved;
    twoWinnerSnapshot.phase = Phase::Attack;
    twoWinnerSnapshot.winner = -1;
    twoWinnerSnapshot.currentPlayer = 0;
    for (auto& territory : twoWinnerSnapshot.territories) {
        if (territory.owner == 1) territory.owner = neutralId;
    }
    GameEngine twoWinner;
    CHECK(twoWinner.restore(twoWinnerSnapshot));
    CHECK(twoWinner.winnerId() == 0);
    CHECK(twoWinner.phase() == Phase::Finished);
    int neutralTerritories = 0;
    for (const auto& territory : twoWinner.territories()) {
        if (territory.owner == neutralId) ++neutralTerritories;
    }
    CHECK(neutralTerritories > 0);

    Snapshot invalidNeutral = twoSaved;
    invalidNeutral.players[0].neutral = true;
    GameEngine invalidNeutralEngine;
    CHECK(!invalidNeutralEngine.restore(invalidNeutral));

    GameEngine secretA;
    GameEngine secretB;
    CHECK(!secretA.startNewGame(2, 1, 9001, GameMode::SecretMission));
    CHECK(secretA.startNewGame(4, 1, 9001, GameMode::SecretMission));
    CHECK(secretB.startNewGame(4, 1, 9001, GameMode::SecretMission));
    CHECK(secretA.mode() == GameMode::SecretMission);
    CHECK(secretA.currentPlayerId() == secretB.currentPlayerId());
    for (std::size_t i = 0; i < secretA.players().size(); ++i) {
        const auto& a = secretA.players()[i];
        const auto& b = secretB.players()[i];
        CHECK(a.mission.has_value());
        CHECK(sameMission(a.mission, b.mission));
        if (a.mission && a.mission->kind == MissionKind::Elimination) {
            CHECK(a.mission->eliminationTarget != a.id);
        }
    }
    const Snapshot secretSaved = secretA.snapshot();
    GameEngine secretRestored;
    CHECK(secretRestored.restore(secretSaved));
    CHECK(secretRestored.mode() == GameMode::SecretMission);
    for (std::size_t i = 0; i < secretA.players().size(); ++i) {
        CHECK(sameMission(secretA.players()[i].mission, secretRestored.players()[i].mission));
    }

    Snapshot winningMission = secretSaved;
    winningMission.phase = Phase::Attack;
    winningMission.winner = -1;
    for (auto& player : winningMission.players) {
        player.mission = MissionSpec{MissionKind::Territory, 42, 0, 0, -1};
    }
    winningMission.players[0].mission = MissionSpec{MissionKind::Territory, 24, 0, 0, -1};
    for (int territoryId = 0; territoryId < 42; ++territoryId) {
        auto& territory = winningMission.territories[static_cast<std::size_t>(territoryId)];
        territory.owner = territoryId < 24 ? 0 : territoryId < 30 ? 1 : territoryId < 36 ? 2 : 3;
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

    std::array<bool, 42> seenHeadquarters{};
    for (const auto& player : capitalA.players()) {
        CHECK(player.headquarters >= 0 && player.headquarters < 42);
        if (player.headquarters < 0 || player.headquarters >= 42) continue;
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
    for (const auto& card : capitalA.deck()) {
        CHECK(card.territoryId < 0 || capitalA.headquartersOwner(card.territoryId) < 0);
    }

    const Snapshot capitalSaved = capitalA.snapshot();
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
    for (const auto& player : capitalWin.players) {
        capitalWin.territories[static_cast<std::size_t>(player.headquarters)].owner = 0;
    }
    GameEngine capitalWinner;
    CHECK(capitalWinner.restore(capitalWin));
    CHECK(capitalWinner.winnerId() == 0);
    CHECK(capitalWinner.phase() == Phase::Finished);

    Snapshot lostOwnHq = capitalWin;
    lostOwnHq.phase = Phase::Attack;
    lostOwnHq.winner = -1;
    lostOwnHq.territories[static_cast<std::size_t>(lostOwnHq.players[0].headquarters)].owner = 1;
    GameEngine noCapitalWinner;
    CHECK(noCapitalWinner.restore(lostOwnHq));
    CHECK(noCapitalWinner.winnerId() < 0);
    CHECK(noCapitalWinner.phase() == Phase::Attack);

    if (failures != 0) std::cerr << failures << " test checks failed\n";
    return failures == 0 ? 0 : 1;
}
