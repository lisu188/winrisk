#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <queue>

namespace winrisk {

std::vector<int> GameEngine::ownedTerritoryIds(int playerId) const {
    std::vector<int> result;
    for (const auto& territory : territories_) {
        if (territory.owner == playerId) result.push_back(territory.id);
    }
    return result;
}

std::vector<int> GameEngine::borderTerritoryIds(int playerId) const {
    std::vector<int> result;
    for (const auto& territory : territories_) {
        if (territory.owner != playerId) continue;
        const bool border = std::any_of(territory.adjacent.begin(), territory.adjacent.end(), [&](int next) {
            return validTerritory(next) && territories_[static_cast<std::size_t>(next)].owner != playerId;
        });
        if (border) result.push_back(territory.id);
    }
    return result;
}

std::vector<int> GameEngine::enemyTerritoryIds(int sourceId) const {
    std::vector<int> result;
    if (!validTerritory(sourceId)) return result;
    const int owner = territories_[static_cast<std::size_t>(sourceId)].owner;
    for (const int targetId : territories_[static_cast<std::size_t>(sourceId)].adjacent) {
        if (!validTerritory(targetId)) continue;
        const int targetOwner = territories_[static_cast<std::size_t>(targetId)].owner;
        if (targetOwner >= 0 && targetOwner != owner) result.push_back(targetId);
    }
    return result;
}

std::vector<int> GameEngine::visibleTerritoryIds(int playerId) const {
    if (!rules_.fogOfWar || playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        std::vector<int> all;
        all.reserve(territories_.size());
        for (const auto& territory : territories_) all.push_back(territory.id);
        return all;
    }

    std::vector<int> result;
    std::vector<bool> present(territories_.size(), false);
    for (const auto& territory : territories_) {
        if (territory.owner != playerId) continue;
        if (!present[static_cast<std::size_t>(territory.id)]) {
            present[static_cast<std::size_t>(territory.id)] = true;
            result.push_back(territory.id);
        }
        for (const int adjacentId : territory.adjacent) {
            if (!validTerritory(adjacentId) || present[static_cast<std::size_t>(adjacentId)]) continue;
            present[static_cast<std::size_t>(adjacentId)] = true;
            result.push_back(adjacentId);
        }
    }
    return result;
}

int GameEngine::graphDistance(int sourceId, int targetId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId)) return static_cast<int>(territories_.size());
    if (sourceId == targetId) return 0;

    std::vector<int> distance(territories_.size(), -1);
    std::queue<int> pending;
    distance[static_cast<std::size_t>(sourceId)] = 0;
    pending.push(sourceId);
    while (!pending.empty()) {
        const int current = pending.front();
        pending.pop();
        const int nextDistance = distance[static_cast<std::size_t>(current)] + 1;
        for (const int next : territories_[static_cast<std::size_t>(current)].adjacent) {
            if (!validTerritory(next) || distance[static_cast<std::size_t>(next)] >= 0) continue;
            if (next == targetId) return nextDistance;
            distance[static_cast<std::size_t>(next)] = nextDistance;
            pending.push(next);
        }
    }
    return static_cast<int>(territories_.size());
}

bool GameEngine::javaInteractivePlayer(int playerId) const {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) return false;
    const auto& player = players_[static_cast<std::size_t>(playerId)];
    return !player.ai || player.neutral;
}

double GameEngine::fieldScale(int territoryId) const {
    if (!validTerritory(territoryId)) return std::numeric_limits<double>::infinity();
    const int owner = territories_[static_cast<std::size_t>(territoryId)].owner;
    if (owner < 0 || owner >= static_cast<int>(players_.size())) return std::numeric_limits<double>::infinity();

    double friendly = 0.0;
    double enemy = 0.0;
    for (const int fieldId : visibleTerritoryIds(owner)) {
        const auto& field = territories_[static_cast<std::size_t>(fieldId)];
        const int distance = graphDistance(territoryId, fieldId);
        double weight = static_cast<double>(field.armies) / std::ldexp(1.0, distance);
        if (rules_.skynet && javaInteractivePlayer(field.owner)) weight *= 2.0;
        if (field.owner == owner) friendly += weight;
        else enemy += weight;
    }
    return friendly > 0.0 ? enemy / friendly : std::numeric_limits<double>::infinity();
}

void GameEngine::sortByScale(std::vector<int>& territoryIds) const {
    if (territoryIds.size() < 2) return;
    std::vector<double> scales;
    scales.reserve(territoryIds.size());
    for (const int id : territoryIds) scales.push_back(fieldScale(id));

    std::vector<int> sorted;
    sorted.reserve(territoryIds.size());
    for (std::size_t i = 0; i < territoryIds.size(); ++i) {
        std::size_t maxIndex = 0;
        double maxValue = scales[0];
        for (std::size_t j = 0; j < scales.size(); ++j) {
            if (scales[j] > maxValue) {
                maxValue = scales[j];
                maxIndex = j;
            }
        }
        sorted.push_back(territoryIds[maxIndex]);
        scales[maxIndex] = -1.0;
    }
    std::reverse(sorted.begin(), sorted.end());
    territoryIds = std::move(sorted);
}

int GameEngine::strongestByScale(const std::vector<int>& territoryIds) const {
    if (territoryIds.empty()) return -1;
    int bestId = territoryIds.front();
    double bestScale = fieldScale(bestId);
    for (std::size_t i = 1; i < territoryIds.size(); ++i) {
        const double scale = fieldScale(territoryIds[i]);
        if (scale < bestScale) {
            bestScale = scale;
            bestId = territoryIds[i];
        }
    }
    return bestId;
}

int GameEngine::weakestByScale(const std::vector<int>& territoryIds) const {
    if (territoryIds.empty()) return -1;
    int bestId = territoryIds.front();
    double bestScale = fieldScale(bestId);
    for (std::size_t i = 1; i < territoryIds.size(); ++i) {
        const double scale = fieldScale(territoryIds[i]);
        if (scale > bestScale) {
            bestScale = scale;
            bestId = territoryIds[i];
        }
    }
    return bestId;
}

int GameEngine::continentGoalFor(int playerId) {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) return -1;
    if (aiContinentGoals_.size() != players_.size()) aiContinentGoals_.assign(players_.size(), -1);

    int& goal = aiContinentGoals_[static_cast<std::size_t>(playerId)];
    if (goal >= 0 && goal < static_cast<int>(continents_.size())
        && !ownsContinent(playerId, continents_[static_cast<std::size_t>(goal)])) return goal;

    goal = -1;
    double bestShare = -1.0;
    for (const auto& continent : continents_) {
        if (ownsContinent(playerId, continent) || continent.territories.empty()) continue;
        int owned = 0;
        for (const int territoryId : continent.territories) {
            if (territories_[static_cast<std::size_t>(territoryId)].owner == playerId) ++owned;
        }
        const double share = static_cast<double>(owned) / static_cast<double>(continent.territories.size());
        if (share > bestShare) {
            bestShare = share;
            goal = continent.id;
        }
    }
    return goal;
}

double GameEngine::averageProximityToContinent(int territoryId, int continentId) const {
    if (!validTerritory(territoryId) || continentId < 0 || continentId >= static_cast<int>(continents_.size())) {
        return std::numeric_limits<double>::infinity();
    }
    const auto& fields = continents_[static_cast<std::size_t>(continentId)].territories;
    if (fields.empty()) return std::numeric_limits<double>::infinity();
    double sum = 0.0;
    for (const int targetId : fields) sum += graphDistance(territoryId, targetId);
    return sum / static_cast<double>(fields.size());
}

bool GameEngine::runAiReinforcePhase() {
    if (currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) return false;
    auto& player = players_[static_cast<std::size_t>(currentPlayer_)];
    if (player.reinforcements <= 0) return false;

    switch (player.aiStrategy) {
        case AiStrategy::Easy: {
            bool acted = false;
            while (player.reinforcements > 0) {
                const int target = weakestByScale(ownedTerritoryIds(currentPlayer_));
                if (target < 0 || !reinforce(target, 1)) break;
                acted = true;
            }
            return acted;
        }
        case AiStrategy::Continent: {
            const int goal = continentGoalFor(currentPlayer_);
            if (goal < 0) return false;
            std::vector<int> targets;
            for (const int territoryId : continents_[static_cast<std::size_t>(goal)].territories) {
                if (territories_[static_cast<std::size_t>(territoryId)].owner == currentPlayer_) targets.push_back(territoryId);
            }
            if (targets.empty()) {
                targets = ownedTerritoryIds(currentPlayer_);
                std::stable_sort(targets.begin(), targets.end(), [&](int lhs, int rhs) {
                    return fieldScale(lhs) < fieldScale(rhs);
                });
            }
            bool acted = false;
            for (const int target : targets) {
                if (player.reinforcements <= 0) break;
                acted = reinforce(target, 1) || acted;
            }
            return acted;
        }
        case AiStrategy::Balanced: {
            auto targets = borderTerritoryIds(currentPlayer_);
            if (targets.empty()) targets = ownedTerritoryIds(currentPlayer_);
            const int target = weakestByScale(targets);
            const int count = player.reinforcements;
            return target >= 0 && count > 0 && reinforce(target, count);
        }
        case AiStrategy::BorderGuard: {
            const auto targets = borderTerritoryIds(currentPlayer_);
            const int target = weakestByScale(targets);
            const int count = player.reinforcements;
            return target >= 0 && count > 0 && reinforce(target, count);
        }
        case AiStrategy::Random: {
            const auto fields = ownedTerritoryIds(currentPlayer_);
            if (fields.empty()) return false;
            bool acted = false;
            while (player.reinforcements > 0) {
                const int target = fields[static_cast<std::size_t>(random_.uniform(static_cast<int>(fields.size())))];
                if (!reinforce(target, 1)) break;
                acted = true;
            }
            return acted;
        }
    }
    return false;
}

void GameEngine::runAiAttackPhase() {
    if (currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) return;
    const AiStrategy strategy = players_[static_cast<std::size_t>(currentPlayer_)].aiStrategy;

    if (strategy == AiStrategy::Easy) {
        auto borders = borderTerritoryIds(currentPlayer_);
        sortByScale(borders);
        std::size_t index = 0;
        while (index < borders.size()) {
            const int sourceId = borders[index];
            auto enemies = enemyTerritoryIds(sourceId);
            if (rules_.skynet) {
                const bool hasInteractive = std::any_of(enemies.begin(), enemies.end(), [&](int targetId) {
                    return javaInteractivePlayer(territories_[static_cast<std::size_t>(targetId)].owner);
                });
                if (hasInteractive) {
                    enemies.erase(std::remove_if(enemies.begin(), enemies.end(), [&](int targetId) {
                        return !javaInteractivePlayer(territories_[static_cast<std::size_t>(targetId)].owner);
                    }), enemies.end());
                }
            }
            sortByScale(enemies);
            std::reverse(enemies.begin(), enemies.end());

            bool attacked = false;
            for (const int targetId : enemies) {
                const auto& source = territories_[static_cast<std::size_t>(sourceId)];
                const auto& target = territories_[static_cast<std::size_t>(targetId)];
                if ((target.armies * 2) < source.armies) {
                    const BattleResult result = attack(sourceId, targetId);
                    if (result.legal) {
                        borders = borderTerritoryIds(currentPlayer_);
                        sortByScale(borders);
                        index = 0;
                        attacked = true;
                    }
                    break;
                }
            }
            if (!attacked) ++index;
        }
        return;
    }

    if (strategy == AiStrategy::Continent) {
        const int goal = continentGoalFor(currentPlayer_);
        if (goal < 0) return;
        const auto playerFields = ownedTerritoryIds(currentPlayer_);
        std::vector<int> fieldsToAttack;
        for (const auto& territory : territories_) {
            if (territory.owner != currentPlayer_) fieldsToAttack.push_back(territory.id);
        }
        std::stable_sort(fieldsToAttack.begin(), fieldsToAttack.end(), [&](int lhs, int rhs) {
            return averageProximityToContinent(lhs, goal) < averageProximityToContinent(rhs, goal);
        });
        for (const int sourceId : playerFields) {
            for (const int targetId : fieldsToAttack) attack(sourceId, targetId);
        }
        return;
    }

    if (strategy == AiStrategy::Random) {
        const auto borders = borderTerritoryIds(currentPlayer_);
        if (borders.empty()) return;
        const int sourceId = borders[static_cast<std::size_t>(random_.uniform(static_cast<int>(borders.size())))];
        const auto enemies = enemyTerritoryIds(sourceId);
        if (enemies.empty()) return;
        const int targetId = enemies[static_cast<std::size_t>(random_.uniform(static_cast<int>(enemies.size())))];
        attack(sourceId, targetId);
        return;
    }

    auto borders = borderTerritoryIds(currentPlayer_);
    if (strategy == AiStrategy::Balanced) sortByScale(borders);
    for (const int sourceId : borders) {
        auto enemies = enemyTerritoryIds(sourceId);
        if (enemies.empty()) continue;
        sortByScale(enemies);
        const int targetId = enemies.front();
        const auto& source = territories_[static_cast<std::size_t>(sourceId)];
        const auto& target = territories_[static_cast<std::size_t>(targetId)];
        const bool shouldAttack = strategy == AiStrategy::Balanced
            ? source.armies > target.armies + 1
            : source.armies > target.armies * 2;
        if (shouldAttack) attack(sourceId, targetId);
    }
}

void GameEngine::runAiManeuverPhase() {
    if (currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) return;
    const AiStrategy strategy = players_[static_cast<std::size_t>(currentPlayer_)].aiStrategy;
    const auto fields = ownedTerritoryIds(currentPlayer_);
    if (fields.size() < 2) return;

    if (strategy == AiStrategy::Easy) {
        const int source = strongestByScale(fields);
        const auto borders = borderTerritoryIds(currentPlayer_);
        const int destination = weakestByScale(borders.empty() ? fields : borders);
        if (source >= 0 && destination >= 0 && source != destination
            && territories_[static_cast<std::size_t>(source)].armies > 1) {
            maneuver(source, destination, territories_[static_cast<std::size_t>(source)].armies - 1);
        }
        return;
    }

    if (strategy == AiStrategy::Continent) {
        continentGoalFor(currentPlayer_);
        return;
    }

    if (strategy == AiStrategy::Random) {
        const int source = fields[static_cast<std::size_t>(random_.uniform(static_cast<int>(fields.size())))];
        const int destination = fields[static_cast<std::size_t>(random_.uniform(static_cast<int>(fields.size())))];
        if (source != destination && territories_[static_cast<std::size_t>(source)].armies > 1) {
            const int troops = random_.uniform(territories_[static_cast<std::size_t>(source)].armies);
            maneuver(source, destination, troops);
        }
        return;
    }

    const auto borders = borderTerritoryIds(currentPlayer_);
    if (strategy == AiStrategy::Balanced) {
        const int destination = weakestByScale(borders.empty() ? fields : borders);
        for (const int source : fields) {
            if (std::find(borders.begin(), borders.end(), source) != borders.end()) continue;
            if (territories_[static_cast<std::size_t>(source)].armies > 1) {
                maneuver(source, destination, territories_[static_cast<std::size_t>(source)].armies - 1);
            }
        }
        return;
    }

    for (const int source : fields) {
        if (std::find(borders.begin(), borders.end(), source) != borders.end()) continue;
        if (territories_[static_cast<std::size_t>(source)].armies <= 1) continue;
        const int destination = weakestByScale(borders.empty() ? fields : borders);
        maneuver(source, destination, territories_[static_cast<std::size_t>(source)].armies - 1);
    }
}

}
