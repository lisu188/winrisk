#include "engine/GameEngine.hpp"

#include <algorithm>
#include <array>
#include <iostream>
#include <numeric>

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

bool allRulesEnabled(const RulesOptions& rules) {
    return rules.incrementalCardSetValues
        && rules.expandedManeuver
        && rules.attackCardReroll
        && rules.commanderDie
        && rules.attackWithAll
        && rules.fogOfWar
        && rules.skynet;
}

void removeCard(Snapshot& snapshot, int cardId) {
    snapshot.deck.erase(
        std::remove_if(snapshot.deck.begin(), snapshot.deck.end(), [cardId](const Card& card) {
            return card.id == cardId;
        }),
        snapshot.deck.end()
    );
}

Snapshot actionSnapshot(const Snapshot& source, Phase phase) {
    Snapshot snapshot = source;
    snapshot.version = 6;
    snapshot.mode = GameMode::Classic;
    snapshot.rules = {};
    snapshot.phase = phase;
    snapshot.currentPlayer = 0;
    snapshot.winner = -1;
    snapshot.tradeCount = 0;
    snapshot.conqueredThisTurn = false;
    snapshot.commanderDieUsed = false;
    snapshot.maneuverUsed = false;
    snapshot.maneuverSource = -1;
    snapshot.maneuverTarget = -1;
    snapshot.deck = GameEngine::makeRiskDeck();
    snapshot.discard.clear();

    for (auto& player : snapshot.players) {
        player.neutral = false;
        player.eliminated = false;
        player.reinforcements = 0;
        player.cards.clear();
        player.mission.reset();
        player.headquarters = -1;
    }

    for (std::size_t i = 0; i < snapshot.territories.size(); ++i) {
        auto& territory = snapshot.territories[i];
        territory.owner = static_cast<int>(i % snapshot.players.size());
        territory.armies = 2;
    }

    snapshot.territories[0].owner = 0;
    snapshot.territories[0].armies = 50;
    snapshot.territories[1].owner = 1;
    snapshot.territories[1].armies = 8;
    snapshot.territories[3].owner = 0;
    snapshot.territories[3].armies = 4;
    snapshot.territories[4].owner = 0;
    snapshot.territories[4].armies = 4;
    return snapshot;
}

int armiesOn(const GameEngine& engine, int a, int b) {
    return engine.territories()[static_cast<std::size_t>(a)].armies
        + engine.territories()[static_cast<std::size_t>(b)].armies;
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
    for (const auto& territory : world) {
        adjacencyEntries += territory.adjacent.size();
    }
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

    GameEngine classicA;
    GameEngine classicB;
    CHECK(classicA.startNewGame(4, 1, 4242));
    CHECK(classicB.startNewGame(4, 1, 4242));
    CHECK(classicA.currentPlayerId() == classicB.currentPlayerId());
    CHECK(classicA.mode() == GameMode::Classic);
    CHECK(classicA.phase() == Phase::Reinforce);
    CHECK(classicA.players().size() == 4);
    CHECK(classicA.deck().size() == 44);

    for (std::size_t i = 0; i < classicA.territories().size(); ++i) {
        CHECK(classicA.territories()[i].owner == classicB.territories()[i].owner);
        CHECK(classicA.territories()[i].armies == classicB.territories()[i].armies);
    }
    for (int playerId = 0; playerId < 4; ++playerId) {
        int armies = 0;
        for (const auto& territory : classicA.territories()) {
            if (territory.owner == playerId) armies += territory.armies;
        }
        CHECK(armies == GameEngine::startingTroops(4));
    }

    const Snapshot classicSaved = classicA.snapshot();
    CHECK(classicSaved.version == 6);
    CHECK(!classicSaved.rules.fogOfWar);
    CHECK(!classicSaved.rules.skynet);
    GameEngine classicRestored;
    CHECK(classicRestored.restore(classicSaved));
    CHECK(classicRestored.currentPlayerId() == classicA.currentPlayerId());

    Snapshot legacyV5 = classicSaved;
    legacyV5.version = 5;
    legacyV5.rules.fogOfWar = true;
    legacyV5.rules.skynet = true;
    GameEngine upgradedV5;
    CHECK(upgradedV5.restore(legacyV5));
    CHECK(!upgradedV5.rules().fogOfWar);
    CHECK(!upgradedV5.rules().skynet);

    RulesOptions everyRule;
    everyRule.incrementalCardSetValues = true;
    everyRule.expandedManeuver = true;
    everyRule.attackCardReroll = true;
    everyRule.commanderDie = true;
    everyRule.attackWithAll = true;
    everyRule.fogOfWar = true;
    everyRule.skynet = true;

    GameEngine optionGame;
    CHECK(optionGame.startNewGame(4, 1, 5150, GameMode::Classic, everyRule));
    CHECK(allRulesEnabled(optionGame.rules()));
    const Snapshot optionSaved = optionGame.snapshot();
    CHECK(optionSaved.version == 6);
    CHECK(allRulesEnabled(optionSaved.rules));
    GameEngine optionRestored;
    CHECK(optionRestored.restore(optionSaved));
    CHECK(allRulesEnabled(optionRestored.rules()));

    Snapshot trade = actionSnapshot(classicSaved, Phase::Reinforce);
    trade.rules.incrementalCardSetValues = true;
    trade.tradeCount = 1;
    trade.players[0].cards = {0, 3, 6};
    removeCard(trade, 0);
    removeCard(trade, 3);
    removeCard(trade, 6);
    GameEngine tradeEngine;
    CHECK(tradeEngine.restore(trade));
    CHECK(tradeEngine.nextTradeValue() == 5);
    CHECK(tradeEngine.tradeCards() == 5);
    CHECK(tradeEngine.nextTradeValue() == 6);

    Snapshot maneuver = actionSnapshot(classicSaved, Phase::Maneuver);
    GameEngine standardManeuver;
    CHECK(standardManeuver.restore(maneuver));
    CHECK(standardManeuver.maneuver(0, 3, 1));
    CHECK(!standardManeuver.maneuver(3, 4, 1));

    const Snapshot lockedManeuver = standardManeuver.snapshot();
    CHECK(lockedManeuver.maneuverUsed);
    CHECK(lockedManeuver.maneuverSource == 0);
    CHECK(lockedManeuver.maneuverTarget == 3);
    GameEngine lockedReload;
    CHECK(lockedReload.restore(lockedManeuver));
    CHECK(!lockedReload.maneuver(3, 4, 1));

    maneuver.rules.expandedManeuver = true;
    GameEngine expandedManeuver;
    CHECK(expandedManeuver.restore(maneuver));
    CHECK(expandedManeuver.maneuver(0, 3, 1));
    const Snapshot expandedSaved = expandedManeuver.snapshot();
    GameEngine expandedReload;
    CHECK(expandedReload.restore(expandedSaved));
    CHECK(expandedReload.maneuver(3, 4, 1));

    Snapshot commander = actionSnapshot(classicSaved, Phase::Attack);
    commander.rules.commanderDie = true;
    commander.rngState = Random(1001).state();
    GameEngine commanderEngine;
    CHECK(commanderEngine.restore(commander));
    const BattleResult commanderResult = commanderEngine.attack(0, 1);
    CHECK(commanderResult.legal);
    CHECK(commanderEngine.commanderDieUsed());
    CHECK(!commanderResult.attackDice.empty());
    CHECK(commanderResult.attackDice.front() == 6);

    Snapshot commanderEnd = commanderEngine.snapshot();
    commanderEnd.phase = Phase::Maneuver;
    commanderEnd.winner = -1;
    GameEngine commanderReset;
    CHECK(commanderReset.restore(commanderEnd));
    CHECK(commanderReset.commanderDieUsed());
    CHECK(commanderReset.endPhase());
    CHECK(!commanderReset.commanderDieUsed());

    Snapshot oneBattle = actionSnapshot(classicSaved, Phase::Attack);
    oneBattle.rngState = Random(2222).state();
    GameEngine singleAttack;
    CHECK(singleAttack.restore(oneBattle));
    const BattleResult singleResult = singleAttack.attack(0, 1);
    CHECK(singleResult.legal);
    const int singleTotal = armiesOn(singleAttack, 0, 1);

    Snapshot allBattle = oneBattle;
    allBattle.rules.attackWithAll = true;
    GameEngine allAttack;
    CHECK(allAttack.restore(allBattle));
    const BattleResult allResult = allAttack.attack(0, 1);
    CHECK(allResult.legal);
    CHECK(armiesOn(allAttack, 0, 1) <= singleTotal);
    CHECK(allResult.captured
        || allAttack.territories()[0].armies <= 1
        || allAttack.territories()[1].owner == 0);

    bool foundRerollChange = false;
    for (std::uint64_t seed = 1; seed <= 512 && !foundRerollChange; ++seed) {
        Snapshot baseline = actionSnapshot(classicSaved, Phase::Attack);
        baseline.territories[0].armies = 7;
        baseline.players[0].cards = {0};
        removeCard(baseline, 0);
        baseline.rngState = Random(seed).state();

        Snapshot reroll = baseline;
        reroll.rules.attackCardReroll = true;

        GameEngine baselineEngine;
        GameEngine rerollEngine;
        CHECK(baselineEngine.restore(baseline));
        CHECK(rerollEngine.restore(reroll));
        const BattleResult baselineResult = baselineEngine.attack(0, 1);
        const BattleResult rerollResult = rerollEngine.attack(0, 1);
        if (baselineResult.attackDice != rerollResult.attackDice) {
            const int baselineSum = std::accumulate(baselineResult.attackDice.begin(), baselineResult.attackDice.end(), 0);
            const int rerollSum = std::accumulate(rerollResult.attackDice.begin(), rerollResult.attackDice.end(), 0);
            CHECK(rerollSum > baselineSum);
            foundRerollChange = true;
        }
    }
    CHECK(foundRerollChange);

    Snapshot normalAi = actionSnapshot(classicSaved, Phase::Attack);
    normalAi.currentPlayer = 2;
    normalAi.territories[0].owner = 2;
    normalAi.territories[0].armies = 20;
    normalAi.territories[1].owner = 0;
    normalAi.territories[1].armies = 15;
    normalAi.territories[5].owner = 3;
    normalAi.territories[5].armies = 1;
    normalAi.territories[29].owner = 2;
    normalAi.territories[29].armies = 2;
    normalAi.rngState = Random(9191).state();

    Snapshot skynetAi = normalAi;
    skynetAi.rules.skynet = true;

    GameEngine normalAiEngine;
    GameEngine skynetAiEngine;
    CHECK(normalAiEngine.restore(normalAi));
    CHECK(skynetAiEngine.restore(skynetAi));
    CHECK(normalAiEngine.currentPlayer() != nullptr && normalAiEngine.currentPlayer()->ai);
    CHECK(skynetAiEngine.currentPlayer() != nullptr && skynetAiEngine.currentPlayer()->ai);

    const int normalHumanBefore = normalAiEngine.territories()[1].armies;
    const int normalAiBefore = normalAiEngine.territories()[5].armies;
    CHECK(normalAiEngine.aiStep());
    CHECK(normalAiEngine.territories()[1].armies == normalHumanBefore);
    CHECK(normalAiEngine.territories()[5].armies != normalAiBefore
        || normalAiEngine.territories()[5].owner == 2);

    const int skynetHumanBefore = skynetAiEngine.territories()[1].armies;
    const int skynetAiBefore = skynetAiEngine.territories()[5].armies;
    CHECK(skynetAiEngine.aiStep());
    CHECK(skynetAiEngine.territories()[1].armies != skynetHumanBefore
        || skynetAiEngine.territories()[1].owner == 2);
    CHECK(skynetAiEngine.territories()[5].armies == skynetAiBefore);
    CHECK(skynetAiEngine.territories()[5].owner == 3);

    GameEngine twoPlayer;
    CHECK(twoPlayer.startNewGame(2, 1, 777, GameMode::Classic));
    CHECK(twoPlayer.players().size() == 3);
    int neutralId = -1;
    for (const auto& player : twoPlayer.players()) {
        if (player.neutral) {
            neutralId = player.id;
            CHECK(player.eliminated);
            CHECK(player.ai);
        }
    }
    CHECK(neutralId == 2);
    CHECK(twoPlayer.currentPlayerId() != neutralId);
    for (int playerId = 0; playerId < 3; ++playerId) {
        int territoryCount = 0;
        int armyCount = 0;
        for (const auto& territory : twoPlayer.territories()) {
            if (territory.owner == playerId) {
                ++territoryCount;
                armyCount += territory.armies;
            }
        }
        CHECK(territoryCount == 14);
        CHECK(armyCount == 40);
    }

    const Snapshot twoSaved = twoPlayer.snapshot();
    GameEngine twoReload;
    CHECK(twoReload.restore(twoSaved));
    CHECK(twoReload.players()[static_cast<std::size_t>(neutralId)].neutral);

    const auto missionDeck = GameEngine::makeMissionDeck(4);
    CHECK(missionDeck.size() == 9);
    GameEngine secretA;
    GameEngine secretB;
    CHECK(secretA.startNewGame(4, 1, 9001, GameMode::SecretMission));
    CHECK(secretB.startNewGame(4, 1, 9001, GameMode::SecretMission));
    for (std::size_t i = 0; i < secretA.players().size(); ++i) {
        CHECK(sameMission(secretA.players()[i].mission, secretB.players()[i].mission));
        if (secretA.players()[i].mission
            && secretA.players()[i].mission->kind == MissionKind::Elimination) {
            CHECK(secretA.players()[i].mission->eliminationTarget != secretA.players()[i].id);
        }
    }

    const Snapshot secretSaved = secretA.snapshot();
    GameEngine secretReload;
    CHECK(secretReload.restore(secretSaved));
    CHECK(secretReload.mode() == GameMode::SecretMission);

    GameEngine capital;
    CHECK(capital.startNewGame(4, 1, 12345, GameMode::Capital));
    CHECK(capital.mode() == GameMode::Capital);
    CHECK(capital.deck().size() == 40);
    std::array<bool, 42> headquarters{};
    for (const auto& player : capital.players()) {
        CHECK(player.headquarters >= 0 && player.headquarters < 42);
        if (player.headquarters >= 0 && player.headquarters < 42) {
            CHECK(!headquarters[static_cast<std::size_t>(player.headquarters)]);
            headquarters[static_cast<std::size_t>(player.headquarters)] = true;
            CHECK(capital.territories()[static_cast<std::size_t>(player.headquarters)].owner == player.id);
        }
    }
    for (const auto& card : capital.deck()) {
        CHECK(card.territoryId < 0 || capital.headquartersOwner(card.territoryId) < 0);
    }

    if (failures != 0) {
        std::cerr << failures << " test checks failed\n";
    }
    return failures == 0 ? 0 : 1;
}
