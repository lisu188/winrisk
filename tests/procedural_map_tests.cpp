#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cmath>
#include <iostream>
#include <queue>
#include <stdexcept>
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

bool sameMap(const MapDefinition& lhs, const MapDefinition& rhs) {
    if (lhs.id != rhs.id || lhs.territories.size() != rhs.territories.size() || lhs.continents.size() != rhs.continents.size()) return false;
    for (std::size_t i = 0; i < lhs.territories.size(); ++i) {
        const auto& a = lhs.territories[i];
        const auto& b = rhs.territories[i];
        if (a.id != b.id || a.name != b.name || a.x != b.x || a.y != b.y || a.continent != b.continent || a.adjacent != b.adjacent) return false;
    }
    for (std::size_t i = 0; i < lhs.continents.size(); ++i) {
        const auto& a = lhs.continents[i];
        const auto& b = rhs.continents[i];
        if (a.id != b.id || a.bonus != b.bonus || a.territories != b.territories) return false;
    }
    return true;
}

}

int main() {
    bool threw = false;
    try { GameEngine::generateProceduralMap(0, 1, 1); } catch (const std::invalid_argument&) { threw = true; }
    CHECK(threw);
    threw = false;
    try { GameEngine::generateProceduralMap(5, 0, 1); } catch (const std::invalid_argument&) { threw = true; }
    CHECK(threw);
    threw = false;
    try { GameEngine::generateProceduralMap(5, 6, 1); } catch (const std::invalid_argument&) { threw = true; }
    CHECK(threw);

    const std::uint64_t seed = 987654321ULL;
    const MapDefinition first = GameEngine::generateProceduralMap(30, 6, seed);
    const MapDefinition second = GameEngine::generateProceduralMap(30, 6, seed);
    const MapDefinition different = GameEngine::generateProceduralMap(30, 6, seed + 1);
    CHECK(sameMap(first, second));
    CHECK(!sameMap(first, different));
    CHECK(first.id == GameEngine::proceduralMapId(30, 6, seed));
    CHECK(first.territories.size() == 30);
    CHECK(first.continents.size() == 6);
    CHECK(connected(first));

    const float minX = 35.0f / 900.0f;
    const float maxX = 864.0f / 900.0f;
    const float minY = 35.0f / 600.0f;
    const float maxY = 564.0f / 600.0f;
    std::vector<bool> assigned(first.territories.size(), false);
    for (const auto& territory : first.territories) {
        CHECK(territory.x >= minX && territory.x <= maxX);
        CHECK(territory.y >= minY && territory.y <= maxY);
        CHECK(territory.adjacent.size() >= 2);
        CHECK(territory.continent >= 0 && territory.continent < 6);
        for (const int next : territory.adjacent) {
            const auto& reverse = first.territories[static_cast<std::size_t>(next)].adjacent;
            CHECK(std::find(reverse.begin(), reverse.end(), territory.id) != reverse.end());
        }
    }
    for (const auto& continent : first.continents) {
        CHECK(continent.territories.size() == 5);
        CHECK(continent.bonus == 1);
        for (const int territoryId : continent.territories) {
            CHECK(!assigned[static_cast<std::size_t>(territoryId)]);
            assigned[static_cast<std::size_t>(territoryId)] = true;
            CHECK(first.territories[static_cast<std::size_t>(territoryId)].continent == continent.id);
        }
    }
    CHECK(std::all_of(assigned.begin(), assigned.end(), [](bool value) { return value; }));

    const MapDefinition uneven = GameEngine::generateProceduralMap(31, 6, seed);
    CHECK(uneven.continents[0].territories.size() == 6);
    CHECK(uneven.continents[0].bonus == 2);
    for (std::size_t i = 1; i < uneven.continents.size(); ++i) {
        CHECK(uneven.continents[i].territories.size() == 5);
        CHECK(uneven.continents[i].bonus == 1);
    }

    const std::string encoded = GameEngine::proceduralMapId(30, 6, seed);
    const auto resolvedViaRegistry = GameEngine::makeBuiltinMap(encoded);
    const auto resolvedViaGeneric = GameEngine::makeMapDefinition(encoded);
    CHECK(resolvedViaRegistry.has_value());
    CHECK(resolvedViaGeneric.has_value());
    if (resolvedViaRegistry) CHECK(sameMap(first, *resolvedViaRegistry));
    if (resolvedViaGeneric) CHECK(sameMap(first, *resolvedViaGeneric));
    CHECK(!GameEngine::makeBuiltinMap("random:30:0:1").has_value());
    CHECK(!GameEngine::makeBuiltinMap("random:abc:6:1").has_value());
    CHECK(!GameEngine::makeBuiltinMap("random:30:6:not-a-seed").has_value());

    GameEngine classic;
    CHECK(classic.startNewGame(4, 1, 1234, GameMode::Classic, {}, encoded));
    CHECK(classic.mapId() == encoded);
    CHECK(classic.territories().size() == 30);
    CHECK(classic.deck().size() == 32);
    const Snapshot saved = classic.snapshot();
    CHECK(saved.mapId == encoded);
    GameEngine restored;
    CHECK(restored.restore(saved));
    CHECK(restored.mapId() == encoded);
    CHECK(restored.territories().size() == 30);
    for (std::size_t i = 0; i < classic.territories().size(); ++i) {
        CHECK(restored.territories()[i].x == classic.territories()[i].x);
        CHECK(restored.territories()[i].y == classic.territories()[i].y);
        CHECK(restored.territories()[i].adjacent == classic.territories()[i].adjacent);
        CHECK(restored.territories()[i].continent == classic.territories()[i].continent);
    }

    GameEngine capital;
    CHECK(capital.startNewGame(4, 1, 4321, GameMode::Capital, {}, encoded));
    CHECK(capital.deck().size() == 28);

    if (failures != 0) std::cerr << failures << " procedural map checks failed\n";
    return failures == 0 ? 0 : 1;
}
