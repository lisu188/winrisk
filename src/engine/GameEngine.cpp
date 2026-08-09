#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cmath>
#include <queue>
#include <stdexcept>

namespace winrisk {
namespace {

std::uint64_t splitMix64(std::uint64_t& x) {
    std::uint64_t z = (x += 0x9e3779b97f4a7c15ULL);
    z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9ULL;
    z = (z ^ (z >> 27)) * 0x94d049bb133111ebULL;
    return z ^ (z >> 31);
}

std::uint64_t rotateLeft(std::uint64_t value, int shift) {
    return (value << shift) | (value >> (64 - shift));
}

void link(std::vector<Territory>& territories, int a, int b) {
    territories[a].adjacent.push_back(b);
    territories[b].adjacent.push_back(a);
}

std::vector<int> range(int first, int lastInclusive) {
    std::vector<int> result;
    for (int i = first; i <= lastInclusive; ++i) {
        result.push_back(i);
    }
    return result;
}

bool validTradeTypes(const std::array<int, 4>& counts) {
    const int wild = counts[static_cast<std::size_t>(CardType::Wild)];
    if (wild > 2) {
        return false;
    }
    const int nonWild = counts[0] + counts[1] + counts[2];
    if (nonWild <= 1) {
        return true;
    }
    int distinct = 0;
    for (int type = 0; type < 3; ++type) {
        if (counts[static_cast<std::size_t>(type)] > 0) {
            ++distinct;
        }
    }
    if (wild > 0) {
        return distinct <= 2;
    }
    return distinct == 1 || distinct == 3;
}

}

Random::Random(std::uint64_t seed) {
    if (seed == 0) {
        seed = 0x6a09e667f3bcc909ULL;
    }
    for (auto& value : state_) {
        value = splitMix64(seed);
    }
}

std::uint64_t Random::next() {
    const std::uint64_t result = rotateLeft(state_[1] * 5ULL, 7) * 9ULL;
    const std::uint64_t t = state_[1] << 17;
    state_[2] ^= state_[0];
    state_[3] ^= state_[1];
    state_[1] ^= state_[2];
    state_[0] ^= state_[3];
    state_[2] ^= t;
    state_[3] = rotateLeft(state_[3], 45);
    return result;
}

int Random::uniform(int upperExclusive) {
    if (upperExclusive <= 0) {
        return 0;
    }
    const auto bound = static_cast<std::uint64_t>(upperExclusive);
    const auto threshold = -bound % bound;
    for (;;) {
        const auto value = next();
        if (value >= threshold) {
            return static_cast<int>(value % bound);
        }
    }
}

const std::array<std::uint64_t, 4>& Random::state() const {
    return state_;
}

void Random::setState(const std::array<std::uint64_t, 4>& state) {
    const bool allZero = std::all_of(state.begin(), state.end(), [](std::uint64_t value) { return value == 0; });
    state_ = allZero ? Random(1).state() : state;
}

GameEngine::GameEngine()
    : territories_(makeWorldTerritories()), continents_(makeWorldContinents()), random_(1) {
}

bool GameEngine::startNewGame(int playerCount, int humanPlayers, std::uint64_t seed) {
    if (playerCount < 2 || playerCount > 5 || humanPlayers < 0 || humanPlayers > playerCount) {
        return false;
    }
    territories_ = makeWorldTerritories();
    continents_ = makeWorldContinents();
    random_ = Random(seed);
    phase_ = Phase::Reinforce;
    currentPlayer_ = 0;
    winner_ = -1;
    turn_ = 1;
    tradeCount_ = 0;
    conqueredThisTurn_ = false;
    maneuverUsed_ = false;
    maneuverSource_ = -1;
    maneuverTarget_ = -1;
    setupPlayers(playerCount, humanPlayers);
    distributeTerritories();
    placeStartingTroops();
    initializeDeck();
    currentPlayer_ = random_.uniform(playerCount);
    beginTurn();
    return true;
}

void GameEngine::setupPlayers(int playerCount, int humanPlayers) {
    static constexpr std::array<std::uint32_t, 5> colors = {
        0xffd9534fU,
        0xff428bcaU,
        0xff5cb85cU,
        0xfff0ad4eU,
        0xff9b59b6U
    };
    players_.clear();
    players_.reserve(playerCount);
    for (int i = 0; i < playerCount; ++i) {
        Player player;
        player.id = i;
        player.name = i < humanPlayers ? "Player " + std::to_string(i + 1) : "AI " + std::to_string(i - humanPlayers + 1);
        player.color = colors[static_cast<std::size_t>(i)];
        player.ai = i >= humanPlayers;
        players_.push_back(std::move(player));
    }
}

void GameEngine::distributeTerritories() {
    std::vector<int> ids(territories_.size());
    for (std::size_t i = 0; i < ids.size(); ++i) {
        ids[i] = static_cast<int>(i);
    }
    for (std::size_t i = ids.size(); i > 1; --i) {
        const auto j = static_cast<std::size_t>(random_.uniform(static_cast<int>(i)));
        std::swap(ids[i - 1], ids[j]);
    }
    for (std::size_t i = 0; i < ids.size(); ++i) {
        auto& territory = territories_[static_cast<std::size_t>(ids[i])];
        territory.owner = static_cast<int>(i % players_.size());
        territory.armies = 1;
    }
}

void GameEngine::placeStartingTroops() {
    const int troops = startingTroops(static_cast<int>(players_.size()));
    std::vector<int> remaining(players_.size(), troops);
    for (const auto& territory : territories_) {
        if (territory.owner >= 0) {
            --remaining[static_cast<std::size_t>(territory.owner)];
        }
    }
    bool pending = true;
    while (pending) {
        pending = false;
        for (std::size_t playerId = 0; playerId < players_.size(); ++playerId) {
            if (remaining[playerId] <= 0) {
                continue;
            }
            std::vector<int> owned;
            for (const auto& territory : territories_) {
                if (territory.owner == static_cast<int>(playerId)) {
                    owned.push_back(territory.id);
                }
            }
            if (!owned.empty()) {
                const int target = owned[static_cast<std::size_t>(random_.uniform(static_cast<int>(owned.size())))];
                ++territories_[static_cast<std::size_t>(target)].armies;
            }
            --remaining[playerId];
            pending = true;
        }
    }
}

void GameEngine::initializeDeck() {
    deck_ = makeRiskDeck();
    discard_.clear();
    shuffleCards(deck_);
}

void GameEngine::shuffleCards(std::vector<Card>& cards) {
    for (std::size_t i = cards.size(); i > 1; --i) {
        const auto j = static_cast<std::size_t>(random_.uniform(static_cast<int>(i)));
        std::swap(cards[i - 1], cards[j]);
    }
}

void GameEngine::recycleDiscard() {
    if (!deck_.empty() || discard_.empty()) {
        return;
    }
    deck_ = std::move(discard_);
    discard_.clear();
    shuffleCards(deck_);
}

void GameEngine::awardTurnCard() {
    if (!conqueredThisTurn_ || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) {
        conqueredThisTurn_ = false;
        return;
    }
    recycleDiscard();
    if (!deck_.empty()) {
        players_[static_cast<std::size_t>(currentPlayer_)].cards.push_back(deck_.back().id);
        deck_.pop_back();
    }
    conqueredThisTurn_ = false;
}

void GameEngine::transferCards(int fromPlayer, int toPlayer) {
    if (fromPlayer < 0 || toPlayer < 0 || fromPlayer >= static_cast<int>(players_.size()) || toPlayer >= static_cast<int>(players_.size()) || fromPlayer == toPlayer) {
        return;
    }
    auto& source = players_[static_cast<std::size_t>(fromPlayer)].cards;
    auto& target = players_[static_cast<std::size_t>(toPlayer)].cards;
    target.insert(target.end(), source.begin(), source.end());
    source.clear();
}

bool GameEngine::reinforce(int territoryId, int count) {
    if (phase_ != Phase::Reinforce || count <= 0 || !validTerritory(territoryId)) {
        return false;
    }
    auto& player = players_[static_cast<std::size_t>(currentPlayer_)];
    auto& territory = territories_[static_cast<std::size_t>(territoryId)];
    if (territory.owner != currentPlayer_ || player.reinforcements < count) {
        return false;
    }
    territory.armies += count;
    player.reinforcements -= count;
    return true;
}

BattleResult GameEngine::attack(int sourceId, int targetId) {
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

bool GameEngine::maneuver(int sourceId, int targetId, int troops) {
    if (phase_ != Phase::Maneuver || troops <= 0 || !canManeuver(sourceId, targetId)) {
        return false;
    }
    if (maneuverUsed_ && (sourceId != maneuverSource_ || targetId != maneuverTarget_)) {
        return false;
    }
    auto& source = territories_[static_cast<std::size_t>(sourceId)];
    auto& target = territories_[static_cast<std::size_t>(targetId)];
    if (source.armies <= troops) {
        return false;
    }
    source.armies -= troops;
    target.armies += troops;
    maneuverUsed_ = true;
    maneuverSource_ = sourceId;
    maneuverTarget_ = targetId;
    return true;
}

std::optional<std::array<std::size_t, 3>> GameEngine::findTradeSet(const Player& player) const {
    if (player.cards.size() < 3) {
        return std::nullopt;
    }
    for (std::size_t a = 0; a + 2 < player.cards.size(); ++a) {
        for (std::size_t b = a + 1; b + 1 < player.cards.size(); ++b) {
            for (std::size_t c = b + 1; c < player.cards.size(); ++c) {
                const Card* first = findCard(player.cards[a]);
                const Card* second = findCard(player.cards[b]);
                const Card* third = findCard(player.cards[c]);
                if (first == nullptr || second == nullptr || third == nullptr) {
                    continue;
                }
                std::array<int, 4> counts{};
                ++counts[static_cast<std::size_t>(first->type)];
                ++counts[static_cast<std::size_t>(second->type)];
                ++counts[static_cast<std::size_t>(third->type)];
                if (validTradeTypes(counts)) {
                    return std::array<std::size_t, 3>{a, b, c};
                }
            }
        }
    }
    return std::nullopt;
}

const Card* GameEngine::findCard(int cardId) const {
    static const std::vector<Card> catalog = makeRiskDeck();
    if (cardId < 0 || cardId >= static_cast<int>(catalog.size())) {
        return nullptr;
    }
    return &catalog[static_cast<std::size_t>(cardId)];
}

int GameEngine::tradeCardsForPlayer(int playerId) {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return 0;
    }
    auto& player = players_[static_cast<std::size_t>(playerId)];
    const auto set = findTradeSet(player);
    if (!set) {
        return 0;
    }
    std::array<int, 3> ids = {
        player.cards[(*set)[0]],
        player.cards[(*set)[1]],
        player.cards[(*set)[2]]
    };
    std::array<std::size_t, 3> positions = *set;
    std::sort(positions.begin(), positions.end(), std::greater<>());
    for (const std::size_t position : positions) {
        player.cards.erase(player.cards.begin() + static_cast<std::ptrdiff_t>(position));
    }
    for (const int id : ids) {
        if (const Card* card = findCard(id)) {
            discard_.push_back(*card);
        }
    }
    const int bonus = tradeValue(tradeCount_);
    ++tradeCount_;
    player.reinforcements += bonus;
    for (const int id : ids) {
        const Card* card = findCard(id);
        if (card != nullptr && card->territoryId >= 0 && validTerritory(card->territoryId)) {
            auto& territory = territories_[static_cast<std::size_t>(card->territoryId)];
            if (territory.owner == playerId) {
                territory.armies += 2;
                break;
            }
        }
    }
    return bonus;
}

void GameEngine::tradeAfterElimination(int playerId) {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) {
        return;
    }
    auto& player = players_[static_cast<std::size_t>(playerId)];
    if (player.cards.size() < 6) {
        return;
    }
    while (player.cards.size() > 4 && findTradeSet(player).has_value()) {
        if (tradeCardsForPlayer(playerId) <= 0) {
            break;
        }
    }
}

int GameEngine::tradeCards() {
    if (phase_ != Phase::Reinforce || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) {
        return 0;
    }
    return tradeCardsForPlayer(currentPlayer_);
}

bool GameEngine::endPhase() {
    if (!running()) {
        return false;
    }
    if (phase_ == Phase::Reinforce) {
        if (players_[static_cast<std::size_t>(currentPlayer_)].reinforcements > 0 || mustTradeCards()) {
            return false;
        }
        phase_ = Phase::Attack;
        return true;
    }
    if (phase_ == Phase::Attack) {
        phase_ = Phase::Maneuver;
        return true;
    }
    if (phase_ == Phase::Maneuver) {
        awardTurnCard();
        advancePlayer();
        return true;
    }
    return false;
}

bool GameEngine::aiStep() {
    if (!running() || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) {
        return false;
    }
    if (!players_[static_cast<std::size_t>(currentPlayer_)].ai) {
        return false;
    }
    if (phase_ == Phase::Reinforce) {
        if (canTradeCards()) {
            return tradeCards() > 0;
        }
        auto& player = players_[static_cast<std::size_t>(currentPlayer_)];
        if (player.reinforcements > 0) {
            return reinforce(chooseAiReinforcementTarget(), 1);
        }
        return endPhase();
    }
    if (phase_ == Phase::Attack) {
        const auto choice = chooseAiAttack();
        if (!choice) {
            return endPhase();
        }
        attack(choice->first, choice->second);
        return true;
    }
    if (phase_ == Phase::Maneuver) {
        return endPhase();
    }
    return false;
}

const std::vector<Territory>& GameEngine::territories() const {
    return territories_;
}

const std::vector<Continent>& GameEngine::continents() const {
    return continents_;
}

const std::vector<Player>& GameEngine::players() const {
    return players_;
}

const std::vector<Card>& GameEngine::deck() const {
    return deck_;
}

const std::vector<Card>& GameEngine::discard() const {
    return discard_;
}

const Player* GameEngine::currentPlayer() const {
    if (currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) {
        return nullptr;
    }
    return &players_[static_cast<std::size_t>(currentPlayer_)];
}

Phase GameEngine::phase() const {
    return phase_;
}

int GameEngine::currentPlayerId() const {
    return currentPlayer_;
}

int GameEngine::winnerId() const {
    return winner_;
}

std::uint64_t GameEngine::turn() const {
    return turn_;
}

int GameEngine::tradeCount() const {
    return tradeCount_;
}

int GameEngine::nextTradeValue() const {
    return tradeValue(tradeCount_);
}

bool GameEngine::canTradeCards() const {
    const auto* player = currentPlayer();
    return player != nullptr && findTradeSet(*player).has_value();
}

bool GameEngine::mustTradeCards() const {
    const auto* player = currentPlayer();
    return player != nullptr && player->cards.size() >= 5 && canTradeCards();
}

bool GameEngine::running() const {
    return !players_.empty() && phase_ != Phase::Finished;
}

bool GameEngine::canAttack(int sourceId, int targetId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId) || !isAdjacent(sourceId, targetId)) {
        return false;
    }
    const auto& source = territories_[static_cast<std::size_t>(sourceId)];
    const auto& target = territories_[static_cast<std::size_t>(targetId)];
    return source.owner == currentPlayer_ && target.owner >= 0 && target.owner != currentPlayer_ && source.armies >= 2 && target.armies >= 1;
}

bool GameEngine::canManeuver(int sourceId, int targetId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId) || sourceId == targetId) {
        return false;
    }
    const auto& source = territories_[static_cast<std::size_t>(sourceId)];
    const auto& target = territories_[static_cast<std::size_t>(targetId)];
    return source.owner == currentPlayer_ && target.owner == currentPlayer_ && source.armies >= 2 && ownsConnectedPath(sourceId, targetId, currentPlayer_);
}

bool GameEngine::ownsConnectedPath(int sourceId, int targetId, int ownerId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId)) {
        return false;
    }
    std::vector<bool> visited(territories_.size(), false);
    std::queue<int> pending;
    pending.push(sourceId);
    visited[static_cast<std::size_t>(sourceId)] = true;
    while (!pending.empty()) {
        const int current = pending.front();
        pending.pop();
        if (current == targetId) {
            return true;
        }
        for (const int next : territories_[static_cast<std::size_t>(current)].adjacent) {
            if (!visited[static_cast<std::size_t>(next)] && territories_[static_cast<std::size_t>(next)].owner == ownerId) {
                visited[static_cast<std::size_t>(next)] = true;
                pending.push(next);
            }
        }
    }
    return false;
}

Snapshot GameEngine::snapshot() const {
    Snapshot result;
    result.version = 3;
    result.phase = phase_;
    result.currentPlayer = currentPlayer_;
    result.winner = winner_;
    result.turn = turn_;
    result.rngState = random_.state();
    result.players = players_;
    result.territories = territories_;
    result.deck = deck_;
    result.discard = discard_;
    result.tradeCount = tradeCount_;
    result.conqueredThisTurn = conqueredThisTurn_;
    return result;
}

bool GameEngine::restore(const Snapshot& snapshot) {
    if ((snapshot.version != 2 && snapshot.version != 3) || snapshot.players.size() < 2 || snapshot.players.size() > 5 || snapshot.territories.size() != 42) {
        return false;
    }
    if (snapshot.currentPlayer < 0 || snapshot.currentPlayer >= static_cast<int>(snapshot.players.size())) {
        return false;
    }
    for (const auto& territory : snapshot.territories) {
        if (territory.id < 0 || territory.id >= 42 || territory.owner < -1 || territory.owner >= static_cast<int>(snapshot.players.size()) || territory.armies < 0) {
            return false;
        }
    }
    for (const auto& player : snapshot.players) {
        for (const int cardId : player.cards) {
            if (findCard(cardId) == nullptr) {
                return false;
            }
        }
    }
    players_ = snapshot.players;
    territories_ = snapshot.territories;
    continents_ = makeWorldContinents();
    phase_ = snapshot.phase;
    currentPlayer_ = snapshot.currentPlayer;
    winner_ = snapshot.winner;
    turn_ = snapshot.turn;
    random_.setState(snapshot.rngState);
    tradeCount_ = std::max(0, snapshot.tradeCount);
    conqueredThisTurn_ = snapshot.conqueredThisTurn;
    if (snapshot.version >= 3) {
        deck_ = snapshot.deck;
        discard_ = snapshot.discard;
    } else {
        const auto savedRng = random_.state();
        initializeDeck();
        random_.setState(savedRng);
        tradeCount_ = 0;
        conqueredThisTurn_ = false;
    }
    maneuverUsed_ = false;
    maneuverSource_ = -1;
    maneuverTarget_ = -1;
    updateEliminationsAndWinner();
    return currentPlayer_ >= 0 && currentPlayer_ < static_cast<int>(players_.size());
}

void GameEngine::beginTurn() {
    updateEliminationsAndWinner();
    if (winner_ >= 0) {
        phase_ = Phase::Finished;
        return;
    }
    phase_ = Phase::Reinforce;
    conqueredThisTurn_ = false;
    maneuverUsed_ = false;
    maneuverSource_ = -1;
    maneuverTarget_ = -1;
    players_[static_cast<std::size_t>(currentPlayer_)].reinforcements += reinforcementCount(currentPlayer_);
}

void GameEngine::advancePlayer() {
    updateEliminationsAndWinner();
    if (winner_ >= 0) {
        phase_ = Phase::Finished;
        return;
    }
    const int count = static_cast<int>(players_.size());
    for (int i = 0; i < count; ++i) {
        currentPlayer_ = (currentPlayer_ + 1) % count;
        if (!players_[static_cast<std::size_t>(currentPlayer_)].eliminated) {
            ++turn_;
            beginTurn();
            return;
        }
    }
    phase_ = Phase::Finished;
}

void GameEngine::updateEliminationsAndWinner() {
    if (players_.empty()) {
        winner_ = -1;
        return;
    }
    int alive = 0;
    int candidate = -1;
    for (auto& player : players_) {
        player.eliminated = territoryCount(player.id) == 0;
        if (!player.eliminated) {
            ++alive;
            candidate = player.id;
        }
    }
    if (alive == 1) {
        winner_ = candidate;
        phase_ = Phase::Finished;
    } else {
        winner_ = -1;
    }
}

int GameEngine::reinforcementCount(int playerId) const {
    int result = std::max(3, territoryCount(playerId) / 3);
    for (const auto& continent : continents_) {
        if (ownsContinent(playerId, continent)) {
            result += continent.bonus;
        }
    }
    return result;
}

int GameEngine::territoryCount(int playerId) const {
    return static_cast<int>(std::count_if(territories_.begin(), territories_.end(), [playerId](const Territory& territory) {
        return territory.owner == playerId;
    }));
}

bool GameEngine::ownsContinent(int playerId, const Continent& continent) const {
    return std::all_of(continent.territories.begin(), continent.territories.end(), [&](int territoryId) {
        return territories_[static_cast<std::size_t>(territoryId)].owner == playerId;
    });
}

bool GameEngine::validTerritory(int id) const {
    return id >= 0 && id < static_cast<int>(territories_.size());
}

bool GameEngine::isAdjacent(int sourceId, int targetId) const {
    const auto& adjacent = territories_[static_cast<std::size_t>(sourceId)].adjacent;
    return std::find(adjacent.begin(), adjacent.end(), targetId) != adjacent.end();
}

int GameEngine::chooseAiReinforcementTarget() const {
    int bestId = -1;
    int bestScore = -1000000;
    for (const auto& territory : territories_) {
        if (territory.owner != currentPlayer_) {
            continue;
        }
        int enemyNeighbors = 0;
        for (const int next : territory.adjacent) {
            if (territories_[static_cast<std::size_t>(next)].owner != currentPlayer_) {
                ++enemyNeighbors;
            }
        }
        const int score = enemyNeighbors * 100 - territory.armies;
        if (score > bestScore) {
            bestScore = score;
            bestId = territory.id;
        }
    }
    return bestId;
}

std::optional<std::pair<int, int>> GameEngine::chooseAiAttack() const {
    int bestScore = 1;
    std::optional<std::pair<int, int>> result;
    for (const auto& source : territories_) {
        if (source.owner != currentPlayer_ || source.armies < 2) {
            continue;
        }
        for (const int targetId : source.adjacent) {
            const auto& target = territories_[static_cast<std::size_t>(targetId)];
            if (target.owner == currentPlayer_ || target.owner < 0) {
                continue;
            }
            const int score = source.armies - target.armies;
            if (score > bestScore) {
                bestScore = score;
                result = std::make_pair(source.id, target.id);
            }
        }
    }
    return result;
}

int GameEngine::startingTroops(int playerCount) {
    switch (playerCount) {
        case 2: return 40;
        case 3: return 35;
        case 4: return 30;
        case 5: return 25;
        default: throw std::invalid_argument("WinRisk supports 2-5 players");
    }
}

int GameEngine::tradeValue(int completedTrades) {
    static constexpr std::array<int, 6> values = {4, 6, 8, 10, 12, 15};
    if (completedTrades < 0) {
        return values.front();
    }
    if (completedTrades < static_cast<int>(values.size())) {
        return values[static_cast<std::size_t>(completedTrades)];
    }
    return 15 + (completedTrades - 5) * 5;
}

std::vector<Card> GameEngine::makeRiskDeck() {
    std::vector<Card> cards;
    cards.reserve(44);
    for (int territory = 0; territory < 42; ++territory) {
        cards.push_back({territory, static_cast<CardType>(territory % 3), territory});
    }
    cards.push_back({42, CardType::Wild, -1});
    cards.push_back({43, CardType::Wild, -1});
    return cards;
}

std::vector<Continent> GameEngine::makeWorldContinents() {
    return {
        {0, "North America", 5, range(0, 8)},
        {1, "South America", 2, range(9, 12)},
        {2, "Europe", 5, range(13, 19)},
        {3, "Africa", 3, range(20, 25)},
        {4, "Asia", 7, range(26, 37)},
        {5, "Australia", 2, range(38, 41)}
    };
}

std::vector<Territory> GameEngine::makeWorldTerritories() {
    static const std::array<const char*, 42> names = {
        "Alaska", "Northwest Territory", "Greenland", "Alberta", "Ontario", "Quebec",
        "Western United States", "Eastern United States", "Central America", "Venezuela", "Peru", "Brazil", "Argentina",
        "Iceland", "Scandinavia", "Ukraine", "Great Britain", "Northern Europe", "Western Europe", "Southern Europe",
        "North Africa", "Egypt", "East Africa", "Congo", "South Africa", "Madagascar",
        "Ural", "Siberia", "Yakutsk", "Kamchatka", "Irkutsk", "Mongolia", "Japan", "Afghanistan", "China", "Middle East", "India", "Siam",
        "Indonesia", "New Guinea", "Western Australia", "Eastern Australia"
    };
    static const std::array<std::array<float, 2>, 42> positions = {{
        {0.06f,0.16f},{0.17f,0.11f},{0.34f,0.08f},{0.14f,0.24f},{0.23f,0.22f},{0.31f,0.21f},{0.17f,0.32f},{0.26f,0.32f},{0.20f,0.43f},
        {0.28f,0.52f},{0.27f,0.64f},{0.36f,0.58f},{0.29f,0.77f},
        {0.43f,0.16f},{0.51f,0.16f},{0.59f,0.21f},{0.45f,0.25f},{0.51f,0.27f},{0.46f,0.35f},{0.54f,0.35f},
        {0.46f,0.49f},{0.54f,0.46f},{0.58f,0.54f},{0.50f,0.58f},{0.52f,0.71f},{0.61f,0.68f},
        {0.66f,0.21f},{0.73f,0.14f},{0.81f,0.10f},{0.91f,0.17f},{0.80f,0.21f},{0.82f,0.29f},{0.92f,0.32f},{0.65f,0.31f},{0.75f,0.36f},{0.61f,0.42f},{0.68f,0.43f},{0.76f,0.49f},
        {0.79f,0.62f},{0.89f,0.61f},{0.82f,0.74f},{0.91f,0.73f}
    }};
    std::vector<Territory> territories;
    territories.reserve(names.size());
    for (int i = 0; i < static_cast<int>(names.size()); ++i) {
        Territory territory;
        territory.id = i;
        territory.name = names[static_cast<std::size_t>(i)];
        territory.x = positions[static_cast<std::size_t>(i)][0];
        territory.y = positions[static_cast<std::size_t>(i)][1];
        if (i <= 8) territory.continent = 0;
        else if (i <= 12) territory.continent = 1;
        else if (i <= 19) territory.continent = 2;
        else if (i <= 25) territory.continent = 3;
        else if (i <= 37) territory.continent = 4;
        else territory.continent = 5;
        territories.push_back(std::move(territory));
    }
    const std::array<std::pair<int,int>, 83> edges = {{
        {0,1},{0,3},{0,29},{1,2},{1,3},{1,4},{2,4},{2,5},{2,13},{3,4},{3,6},{4,5},{4,6},{4,7},{5,7},{6,7},{6,8},{7,8},{8,9},
        {9,10},{9,11},{10,11},{10,12},{11,12},{11,20},
        {13,14},{13,16},{14,15},{14,16},{14,17},{15,17},{15,19},{15,26},{15,33},{15,35},{16,17},{16,18},{17,18},{17,19},{18,19},{18,20},{19,20},{19,21},{19,35},
        {20,21},{20,22},{20,23},{21,22},{21,35},{22,23},{22,24},{22,25},{22,35},{23,24},{24,25},
        {26,27},{26,33},{26,34},{27,28},{27,30},{27,31},{27,34},{28,29},{28,30},{29,30},{29,31},{29,32},{30,31},{31,32},{31,34},{33,34},{33,35},{33,36},{34,36},{34,37},{35,36},{36,37},
        {37,38},{38,39},{38,40},{39,40},{39,41},{40,41}
    }};
    for (const auto& edge : edges) {
        link(territories, edge.first, edge.second);
    }
    return territories;
}

std::string phaseName(Phase phase) {
    switch (phase) {
        case Phase::Reinforce: return "Reinforce";
        case Phase::Attack: return "Attack";
        case Phase::Maneuver: return "Maneuver";
        case Phase::Finished: return "Finished";
    }
    return "Unknown";
}

std::string cardTypeName(CardType type) {
    switch (type) {
        case CardType::Infantry: return "Infantry";
        case CardType::Cavalry: return "Cavalry";
        case CardType::Artillery: return "Artillery";
        case CardType::Wild: return "Wild";
    }
    return "Unknown";
}

}
