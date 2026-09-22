#include "engine/GameEngine.hpp"

#include <algorithm>

namespace winrisk {

std::vector<MissionSpec> GameEngine::makeMissionDeck(int playerCount) {
    std::vector<MissionSpec> missions;
    missions.reserve(static_cast<std::size_t>(playerCount) + 5);
    missions.push_back({MissionKind::Territory, 24, 0, 0, -1});
    missions.push_back({MissionKind::FortifiedTerritory, 18, 2, 0, -1});
    missions.push_back({MissionKind::Continents, 0, 0, 2, -1});
    missions.push_back({MissionKind::Continents, 0, 0, 3, -1});
    missions.push_back({MissionKind::FortifiedTerritory, 15, 3, 0, -1});
    for (int playerId = 0; playerId < playerCount; ++playerId) {
        missions.push_back({MissionKind::Elimination, 0, 0, 0, playerId});
    }
    return missions;
}

std::string GameEngine::missionDescription(const MissionSpec& mission) {
    switch (mission.kind) {
        case MissionKind::Territory:
            return "Conquer " + std::to_string(mission.territories) + " territories";
        case MissionKind::FortifiedTerritory:
            return "Conquer " + std::to_string(mission.territories) + " territories with at least "
                + std::to_string(mission.minimumArmies) + " armies each";
        case MissionKind::Continents:
            return "Conquer " + std::to_string(mission.continentCount) + " continents";
        case MissionKind::Elimination:
            return "Eliminate Player " + std::to_string(mission.eliminationTarget + 1);
    }
    return {};
}

void GameEngine::assignMissions() {
    auto missions = makeMissionDeck(static_cast<int>(players_.size()));
    for (std::size_t i = missions.size(); i > 1; --i) {
        const auto j = static_cast<std::size_t>(random_.uniform(static_cast<int>(i)));
        std::swap(missions[i - 1], missions[j]);
    }

    for (auto& player : players_) {
        std::vector<std::size_t> candidates;
        for (std::size_t i = 0; i < missions.size(); ++i) {
            if (missions[i].kind != MissionKind::Elimination || missions[i].eliminationTarget != player.id) {
                candidates.push_back(i);
            }
        }
        if (candidates.empty()) {
            for (std::size_t i = 0; i < missions.size(); ++i) {
                candidates.push_back(i);
            }
        }
        if (candidates.empty()) {
            player.mission = MissionSpec{MissionKind::Territory, 24, 0, 0, -1};
            continue;
        }
        const auto candidate = candidates[static_cast<std::size_t>(random_.uniform(static_cast<int>(candidates.size())))];
        player.mission = missions[candidate];
        missions.erase(missions.begin() + static_cast<std::ptrdiff_t>(candidate));
    }
}

int GameEngine::rollHighestPlayer() {
    if (players_.empty()) {
        return -1;
    }
    for (;;) {
        int highest = -1;
        int winner = -1;
        bool tied = false;
        for (const auto& player : players_) {
            if (player.eliminated) {
                continue;
            }
            const int roll = random_.uniform(6) + 1;
            if (roll > highest) {
                highest = roll;
                winner = player.id;
                tied = false;
            } else if (roll == highest) {
                tied = true;
            }
        }
        if (!tied && winner >= 0) {
            return winner;
        }
    }
}

bool GameEngine::missionCompleted(int playerId, const MissionSpec& mission) const {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return false;
    }
    switch (mission.kind) {
        case MissionKind::Territory:
            return territoryCount(playerId) >= mission.territories;
        case MissionKind::FortifiedTerritory: {
            int fortified = 0;
            for (const auto& territory : territories_) {
                if (territory.owner == playerId && territory.armies >= mission.minimumArmies) {
                    ++fortified;
                }
            }
            return fortified >= mission.territories;
        }
        case MissionKind::Continents: {
            int owned = 0;
            for (const auto& continent : continents_) {
                if (ownsContinent(playerId, continent)) {
                    ++owned;
                }
            }
            return owned >= mission.continentCount;
        }
        case MissionKind::Elimination:
            if (mission.eliminationTarget == playerId) {
                return territoryCount(playerId) >= 24;
            }
            return mission.eliminationTarget >= 0
                && mission.eliminationTarget < static_cast<int>(players_.size())
                && territoryCount(mission.eliminationTarget) == 0;
    }
    return false;
}

std::string GameEngine::missionText(int playerId) const {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return {};
    }
    const auto& mission = players_[static_cast<std::size_t>(playerId)].mission;
    return mission ? missionDescription(*mission) : std::string{};
}

std::string modeName(GameMode mode) {
    switch (mode) {
        case GameMode::Classic: return "Classic";
        case GameMode::SecretMission: return "Secret Mission";
        case GameMode::Capital: return "Capital";
    }
    return "Unknown";
}

}
