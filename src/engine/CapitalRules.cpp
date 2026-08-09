#include "engine/GameEngine.hpp"

#include <algorithm>

namespace winrisk {

void GameEngine::claimTerritories(int startingPlayer) {
    if (players_.empty()) {
        return;
    }
    int playerId = std::clamp(startingPlayer, 0, static_cast<int>(players_.size()) - 1);
    for (auto& territory : territories_) {
        territory.owner = playerId;
        territory.armies = 1;
        playerId = (playerId + 1) % static_cast<int>(players_.size());
    }
}

void GameEngine::placeStartingTroopsOfficial() {
    const int total = startingTroops(static_cast<int>(players_.size()));
    for (const auto& player : players_) {
        int owned = 0;
        int first = -1;
        for (const auto& territory : territories_) {
            if (territory.owner == player.id) {
                ++owned;
                if (first < 0) {
                    first = territory.id;
                }
            }
        }
        if (first >= 0 && owned < total) {
            territories_[static_cast<std::size_t>(first)].armies += total - owned;
        }
    }
}

void GameEngine::assignHeadquarters() {
    for (auto& player : players_) {
        player.headquarters = -1;
        for (const auto& territory : territories_) {
            if (territory.owner == player.id) {
                player.headquarters = territory.id;
                break;
            }
        }
    }
}

void GameEngine::removeHeadquartersFromDeck() {
    deck_.erase(std::remove_if(deck_.begin(), deck_.end(), [&](const Card& card) {
        return card.territoryId >= 0 && headquartersOwner(card.territoryId) >= 0;
    }), deck_.end());
}

int GameEngine::capitalWinner() const {
    if (mode_ != GameMode::Capital || players_.empty()) {
        return -1;
    }
    for (const auto& player : players_) {
        if (!validTerritory(player.headquarters)
            || territories_[static_cast<std::size_t>(player.headquarters)].owner != player.id) {
            continue;
        }
        bool controlsAll = true;
        for (const auto& opponent : players_) {
            if (!validTerritory(opponent.headquarters)
                || territories_[static_cast<std::size_t>(opponent.headquarters)].owner != player.id) {
                controlsAll = false;
                break;
            }
        }
        if (controlsAll) {
            return player.id;
        }
    }
    return -1;
}

int GameEngine::headquartersOwner(int territoryId) const {
    if (!validTerritory(territoryId)) {
        return -1;
    }
    for (const auto& player : players_) {
        if (player.headquarters == territoryId) {
            return player.id;
        }
    }
    return -1;
}

int GameEngine::headquartersControlledBy(int playerId) const {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return 0;
    }
    int controlled = 0;
    for (const auto& player : players_) {
        if (validTerritory(player.headquarters)
            && territories_[static_cast<std::size_t>(player.headquarters)].owner == playerId) {
            ++controlled;
        }
    }
    return controlled;
}

std::string GameEngine::capitalObjectiveText(int playerId) const {
    if (mode_ != GameMode::Capital || playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return {};
    }
    const auto& player = players_[static_cast<std::size_t>(playerId)];
    if (!validTerritory(player.headquarters)) {
        return "Headquarters not assigned";
    }
    return "Control all " + std::to_string(players_.size()) + " headquarters. Your HQ: "
        + territories_[static_cast<std::size_t>(player.headquarters)].name + ". Controlled: "
        + std::to_string(headquartersControlledBy(playerId)) + "/" + std::to_string(players_.size());
}

}
