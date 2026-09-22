#include "engine/GameEngine.hpp"

#include <algorithm>
#include <array>
#include <iostream>
#include <queue>
#include <string>
#include <vector>

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

bool connected(const MapDefinition& map) {
    if (map.territories.empty()) return false;
    std::vector<bool> seen(map.territories.size(), false);
    std::queue<int> pending;
    seen[0] = true;
    pending.push(0);
    while (!pending.empty()) {
        const int current = pending.front();
        pending.pop();
        for (const int next : map.territories[static_cast<std::size_t>(current)].adjacent) {
            if (next < 0 || next >= static_cast<int>(map.territories.size())) return false;
            if (!seen[static_cast<std::size_t>(next)]) {
                seen[static_cast<std::size_t>(next)] = true;
                pending.push(next);
            }
        }
    }
    return std::all_of(seen.begin(), seen.end(), [](bool value) { return value; });
}

void validateMap(const std::string& id, int territoryCount, int continentCount) {
    const auto map = GameEngine::makeBuiltinMap(id);
    CHECK(map.has_value());
    if (!map) return;
    CHECK(map->id == id);
    CHECK(static_cast<int>(map->territories.size()) == territoryCount);
    CHECK(static_cast<int>(map->continents.size()) == continentCount);
    CHECK(connected(*map));

    for (std::size_t i = 0; i < map->territories.size(); ++i) {
        const auto& territory = map->territories[i];
        CHECK(territory.id == static_cast<int>(i));
        CHECK(!territory.name.empty());
        CHECK(territory.x >= 0.0f && territory.x <= 1.0f);
        CHECK(territory.y >= 0.0f && territory.y <= 1.0f);
        CHECK(territory.continent >= 0 && territory.continent < continentCount);
        for (const int next : territory.adjacent) {
            CHECK(next >= 0 && next < territoryCount);
            if (next >= 0 && next < territoryCount) {
                const auto& reverse = map->territories[static_cast<std::size_t>(next)].adjacent;
                CHECK(std::find(reverse.begin(), reverse.end(), territory.id) != reverse.end());
            }
        }
    }

    for (std::size_t i = 0; i < map->continents.size(); ++i) {
        const auto& continent = map->continents[i];
        CHECK(continent.id == static_cast<int>(i));
        CHECK(continent.bonus > 0);
        CHECK(!continent.territories.empty());
        for (const int territoryId : continent.territories) {
            CHECK(map->territories[static_cast<std::size_t>(territoryId)].continent == continent.id);
        }
    }

    const auto deck = GameEngine::makeRiskDeck(territoryCount);
    CHECK(static_cast<int>(deck.size()) == territoryCount + 2);
    CHECK(deck[static_cast<std::size_t>(territoryCount)].type == CardType::Wild);
    CHECK(deck[static_cast<std::size_t>(territoryCount + 1)].type == CardType::Wild);

    GameEngine classic;
    CHECK(classic.startNewGame(4, 1, 1000 + territoryCount, GameMode::Classic, {}, id));
    CHECK(classic.mapId() == id);
    CHECK(static_cast<int>(classic.territories().size()) == territoryCount);
    CHECK(static_cast<int>(classic.deck().size()) == territoryCount + 2);
    const Snapshot saved = classic.snapshot();
    CHECK(saved.mapId == id);
    GameEngine restored;
    CHECK(restored.restore(saved));
    CHECK(restored.mapId() == id);
    CHECK(restored.territories().size() == classic.territories().size());
    for (std::size_t i = 0; i < classic.territories().size(); ++i) {
        CHECK(restored.territories()[i].name == classic.territories()[i].name);
        CHECK(restored.territories()[i].owner == classic.territories()[i].owner);
        CHECK(restored.territories()[i].armies == classic.territories()[i].armies);
    }

    GameEngine secret;
    CHECK(secret.startNewGame(4, 1, 2000 + territoryCount, GameMode::SecretMission, {}, id));
    CHECK(static_cast<int>(secret.deck().size()) == territoryCount + 2);

    GameEngine capital;
    CHECK(capital.startNewGame(4, 1, 3000 + territoryCount, GameMode::Capital, {}, id));
    CHECK(static_cast<int>(capital.deck().size()) == territoryCount + 2 - 4);
    for (const auto& player : capital.players()) {
        CHECK(player.headquarters >= 0 && player.headquarters < territoryCount);
    }
}

}

int main() {
    const std::vector<std::string> expectedIds = {"world", "pangaea", "laurasia", "gondwana", "rodinia"};
    CHECK(GameEngine::builtinMapIds() == expectedIds);
    CHECK(!GameEngine::makeBuiltinMap("unknown").has_value());
    CHECK(GameEngine::makeBuiltinMap(" PANGAEA ").has_value());

    validateMap("world", 42, 6);
    validateMap("pangaea", 32, 6);
    validateMap("laurasia", 24, 5);
    validateMap("gondwana", 26, 5);
    validateMap("rodinia", 20, 4);

    GameEngine twoPlayerRodinia;
    CHECK(twoPlayerRodinia.startNewGame(2, 1, 4567, GameMode::Classic, {}, "rodinia"));
    CHECK(twoPlayerRodinia.players().size() == 3);
    for (const auto& player : twoPlayerRodinia.players()) {
        int armies = 0;
        int fields = 0;
        for (const auto& territory : twoPlayerRodinia.territories()) {
            if (territory.owner == player.id) {
                ++fields;
                armies += territory.armies;
            }
        }
        CHECK(fields == 6 || fields == 7);
        CHECK(armies == 40);
    }

    if (failures != 0) std::cerr << failures << " built-in map checks failed\n";
    return failures == 0 ? 0 : 1;
}
