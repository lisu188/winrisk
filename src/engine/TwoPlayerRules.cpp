#include "engine/GameEngine.hpp"

#include <utility>

namespace winrisk {

void GameEngine::addNeutralPlayer() {
    Player neutral;
    neutral.id = static_cast<int>(players_.size());
    neutral.name = "Neutral";
    neutral.color = 0xff777777U;
    neutral.ai = true;
    neutral.neutral = true;
    neutral.eliminated = true;
    players_.push_back(std::move(neutral));
}

void GameEngine::placeTwoPlayerStartingTroops() {
    constexpr int total = 40;
    for (const auto& player : players_) {
        int owned = 0;
        int firstOwned = -1;
        for (const auto& territory : territories_) {
            if (territory.owner == player.id) {
                ++owned;
                if (firstOwned < 0) {
                    firstOwned = territory.id;
                }
            }
        }
        if (firstOwned >= 0 && owned < total) {
            territories_[static_cast<std::size_t>(firstOwned)].armies += total - owned;
        }
    }
}

}
