#include "engine/GameEngine.hpp"

#include <algorithm>
#include <functional>

namespace winrisk {

BattleResult GameEngine::resolveOneBattle(int sourceId, int targetId) {
    BattleResult result;
    if (phase_ != Phase::Attack || !canAttack(sourceId, targetId)) {
        return result;
    }

    auto& source = territories_[static_cast<std::size_t>(sourceId)];
    auto& target = territories_[static_cast<std::size_t>(targetId)];
    const int defendingPlayer = target.owner;
    const int attackCount = std::min(3, source.armies - 1);
    const int defenseCount = std::min(2, target.armies);

    result.legal = true;
    result.attackDice.reserve(static_cast<std::size_t>(attackCount));
    result.defenseDice.reserve(static_cast<std::size_t>(defenseCount));
    for (int i = 0; i < attackCount; ++i) {
        result.attackDice.push_back(random_.uniform(6) + 1);
    }
    for (int i = 0; i < defenseCount; ++i) {
        result.defenseDice.push_back(random_.uniform(6) + 1);
    }

    if (rules_.attackCardReroll && !result.attackDice.empty()) {
        const bool hasMatchingCard = std::any_of(
            players_[static_cast<std::size_t>(currentPlayer_)].cards.begin(),
            players_[static_cast<std::size_t>(currentPlayer_)].cards.end(),
            [&](int cardId) {
                const Card* card = findCard(cardId);
                return card != nullptr
                    && card->type != CardType::Wild
                    && (card->territoryId == sourceId || card->territoryId == targetId);
            }
        );
        if (hasMatchingCard) {
            const auto lowest = std::min_element(result.attackDice.begin(), result.attackDice.end());
            const int reroll = random_.uniform(6) + 1;
            if (reroll > *lowest) {
                *lowest = reroll;
            }
        }
    }

    if (rules_.commanderDie && !commanderDieUsed_ && !result.attackDice.empty()) {
        const auto lowest = std::min_element(result.attackDice.begin(), result.attackDice.end());
        *lowest = 6;
        commanderDieUsed_ = true;
    }

    std::sort(result.attackDice.begin(), result.attackDice.end(), std::greater<>());
    std::sort(result.defenseDice.begin(), result.defenseDice.end(), std::greater<>());

    const int comparisons = std::min(attackCount, defenseCount);
    for (int i = 0; i < comparisons; ++i) {
        if (result.attackDice[static_cast<std::size_t>(i)] > result.defenseDice[static_cast<std::size_t>(i)]) {
            ++result.defenderLosses;
        } else {
            ++result.attackerLosses;
        }
    }

    source.armies -= result.attackerLosses;
    target.armies -= result.defenderLosses;
    if (target.armies <= 0) {
        const int occupation = std::clamp(attackCount - result.attackerLosses, 1, source.armies - 1);
        target.owner = currentPlayer_;
        target.armies = occupation;
        source.armies -= occupation;
        result.captured = true;
        conqueredThisTurn_ = true;
        if (defendingPlayer >= 0 && territoryCount(defendingPlayer) == 0) {
            transferCards(defendingPlayer, currentPlayer_);
            tradeAfterElimination(currentPlayer_);
        }
    }

    if (defendingPlayer >= 0) {
        updateEliminationsAndWinner();
    }
    return result;
}

}
