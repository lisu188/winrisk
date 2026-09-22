#include <array>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#define private public
#include "engine/GameEngine.hpp"
#undef private

#include <algorithm>
#include <cmath>
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

Snapshot activeSnapshot(AiStrategy strategy, Phase phase) {
    Snapshot snapshot;
    snapshot.version = 6;
    snapshot.mode = GameMode::Classic;
    snapshot.phase = phase;
    snapshot.currentPlayer = 0;
    snapshot.winner = -1;
    snapshot.turn = 7;
    snapshot.rngState = Random(123456789).state();
    snapshot.players.resize(3);
    for (int i = 0; i < 3; ++i) {
        auto& player = snapshot.players[static_cast<std::size_t>(i)];
        player.id = i;
        player.name = i == 0 ? "AI" : "Player";
        player.ai = i == 0;
        player.aiStrategy = i == 0 ? strategy : AiStrategy::Easy;
        player.color = 0xff000000U | static_cast<std::uint32_t>((i + 1) * 0x00202020U);
    }
    snapshot.territories = GameEngine::makeWorldTerritories();
    for (auto& territory : snapshot.territories) {
        territory.owner = territory.id % 3;
        territory.armies = 2;
    }
    return snapshot;
}

Snapshot neutralScaleSnapshot(bool skynet) {
    Snapshot snapshot;
    snapshot.version = 6;
    snapshot.mode = GameMode::Classic;
    snapshot.rules.skynet = skynet;
    snapshot.phase = Phase::Attack;
    snapshot.currentPlayer = 0;
    snapshot.rngState = Random(991).state();
    snapshot.players.resize(3);

    snapshot.players[0].id = 0;
    snapshot.players[0].name = "Easy AI";
    snapshot.players[0].ai = true;
    snapshot.players[0].aiStrategy = AiStrategy::Easy;

    snapshot.players[1].id = 1;
    snapshot.players[1].name = "Other AI";
    snapshot.players[1].ai = true;
    snapshot.players[1].aiStrategy = AiStrategy::Balanced;

    snapshot.players[2].id = 2;
    snapshot.players[2].name = "Neutral";
    snapshot.players[2].ai = true;
    snapshot.players[2].neutral = true;
    snapshot.players[2].eliminated = true;

    snapshot.territories = GameEngine::makeWorldTerritories();
    for (auto& territory : snapshot.territories) {
        territory.owner = 0;
        territory.armies = 2;
    }
    snapshot.territories[40].owner = 1;
    snapshot.territories[40].armies = 3;
    snapshot.territories[41].owner = 2;
    snapshot.territories[41].armies = 20;
    return snapshot;
}

Snapshot continentReinforcementSnapshot() {
    Snapshot snapshot = activeSnapshot(AiStrategy::Continent, Phase::Reinforce);
    snapshot.players[0].reinforcements = 5;
    for (auto& territory : snapshot.territories) {
        territory.owner = 1;
        territory.armies = 2;
    }

    const auto continents = GameEngine::makeWorldContinents();
    const auto& goal = continents.front();
    for (std::size_t i = 0; i < 3; ++i) {
        snapshot.territories[static_cast<std::size_t>(goal.territories[i])].owner = 0;
    }

    const auto outsideGoal = std::find_if(snapshot.territories.begin(), snapshot.territories.end(), [&](const Territory& territory) {
        return std::find(goal.territories.begin(), goal.territories.end(), territory.id) == goal.territories.end();
    });
    if (outsideGoal != snapshot.territories.end()) {
        outsideGoal->owner = 2;
    }
    return snapshot;
}

}

int main() {
    {
        GameEngine game;
        CHECK(game.startNewGame(5, 1, 17));
        CHECK(!game.players()[0].ai);
        CHECK(game.players()[1].aiStrategy == AiStrategy::Continent);
        CHECK(game.players()[2].aiStrategy == AiStrategy::Balanced);
        CHECK(game.players()[3].aiStrategy == AiStrategy::BorderGuard);
        CHECK(game.players()[4].aiStrategy == AiStrategy::Random);
    }

    {
        GameEngine normal;
        GameEngine skynet;
        CHECK(normal.restore(neutralScaleSnapshot(false)));
        CHECK(skynet.restore(neutralScaleSnapshot(true)));
        CHECK(skynet.javaInteractivePlayer(2));
        CHECK(!skynet.javaInteractivePlayer(1));
        const double normalScale = normal.fieldScale(0);
        const double skynetScale = skynet.fieldScale(0);
        CHECK(std::isfinite(normalScale));
        CHECK(std::isfinite(skynetScale));
        CHECK(skynetScale > normalScale);
    }

    {
        Snapshot snapshot = activeSnapshot(AiStrategy::Balanced, Phase::Reinforce);
        snapshot.players[0].reinforcements = 5;
        GameEngine game;
        CHECK(game.restore(snapshot));
        std::vector<int> before;
        before.reserve(game.territories().size());
        for (const auto& territory : game.territories()) before.push_back(territory.armies);
        CHECK(game.runAiReinforcePhase());
        CHECK(game.players()[0].reinforcements == 0);
        int changed = 0;
        int added = 0;
        for (std::size_t i = 0; i < game.territories().size(); ++i) {
            const int delta = game.territories()[i].armies - before[i];
            if (delta != 0) ++changed;
            added += delta;
        }
        CHECK(changed == 1);
        CHECK(added == 5);
    }

    {
        const Snapshot snapshot = continentReinforcementSnapshot();
        GameEngine game;
        CHECK(game.restore(snapshot));
        const auto continents = GameEngine::makeWorldContinents();
        const auto& goal = continents.front();
        CHECK(game.continentGoalFor(0) == goal.id);

        std::array<int, 3> before{};
        for (std::size_t i = 0; i < before.size(); ++i) {
            before[i] = game.territories()[static_cast<std::size_t>(goal.territories[i])].armies;
        }
        CHECK(game.runAiReinforcePhase());
        for (std::size_t i = 0; i < before.size(); ++i) {
            CHECK(game.territories()[static_cast<std::size_t>(goal.territories[i])].armies == before[i] + 1);
        }
        CHECK(game.players()[0].reinforcements == 2);

        GameEngine phaseGame;
        CHECK(phaseGame.restore(snapshot));
        CHECK(phaseGame.aiStep());
        CHECK(phaseGame.phase() == Phase::Attack);
        CHECK(phaseGame.players()[0].reinforcements == 2);
    }

    {
        Snapshot snapshot = activeSnapshot(AiStrategy::Random, Phase::Reinforce);
        snapshot.players[0].reinforcements = 0;
        snapshot.players[0].cards = {0, 3, 6};
        GameEngine game;
        CHECK(game.restore(snapshot));
        CHECK(game.canTradeCards());
        CHECK(!game.mustTradeCards());
        CHECK(game.aiStep());
        CHECK(game.phase() == Phase::Attack);
        CHECK(game.players()[0].cards.size() == 3);
        CHECK(game.tradeCount() == 0);
    }

    {
        Snapshot snapshot = activeSnapshot(AiStrategy::Random, Phase::Attack);
        GameEngine game;
        CHECK(game.restore(snapshot));
        CHECK(game.aiStep());
        CHECK(game.phase() == Phase::Maneuver || game.phase() == Phase::Finished);
    }

    if (failures != 0) std::cerr << failures << " AI strategy checks failed\n";
    return failures == 0 ? 0 : 1;
}
