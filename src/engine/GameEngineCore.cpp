#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <functional>
#include <queue>
#include <stdexcept>
#include <utility>
#include <vector>

namespace winrisk {
namespace {
std::uint64_t splitMix64(std::uint64_t& x) {
    std::uint64_t z = (x += 0x9e3779b97f4a7c15ULL);
    z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9ULL;
    z = (z ^ (z >> 27)) * 0x94d049bb133111ebULL;
    return z ^ (z >> 31);
}
std::uint64_t rotateLeft(std::uint64_t value, int shift) { return (value << shift) | (value >> (64 - shift)); }
bool validTradeTypes(const std::array<int, 4>& counts) {
    const int wild = counts[static_cast<std::size_t>(CardType::Wild)];
    if (wild > 2) return false;
    const int nonWild = counts[0] + counts[1] + counts[2];
    if (nonWild <= 1) return true;
    int distinct = 0;
    for (int type = 0; type < 3; ++type) if (counts[static_cast<std::size_t>(type)] > 0) ++distinct;
    return wild > 0 ? distinct <= 2 : distinct == 1 || distinct == 3;
}

bool validMapDefinition(const MapDefinition& map) {
    if (map.id.empty() || map.territories.empty()) return false;
    const int territoryCount = static_cast<int>(map.territories.size());
    std::vector<int> membership(map.territories.size(), -1);

    for (std::size_t i = 0; i < map.territories.size(); ++i) {
        const auto& territory = map.territories[i];
        if (territory.id != static_cast<int>(i) || territory.name.empty()
            || !std::isfinite(territory.x) || !std::isfinite(territory.y)
            || territory.x < 0.0f || territory.x > 1.0f || territory.y < 0.0f || territory.y > 1.0f) return false;
        std::vector<bool> seen(map.territories.size(), false);
        for (const int next : territory.adjacent) {
            if (next < 0 || next >= territoryCount || next == territory.id || seen[static_cast<std::size_t>(next)]) return false;
            seen[static_cast<std::size_t>(next)] = true;
            const auto& reverse = map.territories[static_cast<std::size_t>(next)].adjacent;
            if (std::find(reverse.begin(), reverse.end(), territory.id) == reverse.end()) return false;
        }
    }

    for (std::size_t i = 0; i < map.continents.size(); ++i) {
        const auto& continent = map.continents[i];
        if (continent.id != static_cast<int>(i) || continent.bonus < 0 || continent.territories.empty()) return false;
        for (const int territoryId : continent.territories) {
            if (territoryId < 0 || territoryId >= territoryCount || membership[static_cast<std::size_t>(territoryId)] >= 0) return false;
            membership[static_cast<std::size_t>(territoryId)] = continent.id;
        }
    }
    for (std::size_t i = 0; i < map.territories.size(); ++i) {
        if (map.territories[i].continent != membership[i]) return false;
    }
    return true;
}
}

Random::Random(std::uint64_t seed) {
    if (seed == 0) seed = 0x6a09e667f3bcc909ULL;
    for (auto& value : state_) value = splitMix64(seed);
}
std::uint64_t Random::next() {
    const std::uint64_t result = rotateLeft(state_[1] * 5ULL, 7) * 9ULL;
    const std::uint64_t t = state_[1] << 17;
    state_[2] ^= state_[0]; state_[3] ^= state_[1]; state_[1] ^= state_[2]; state_[0] ^= state_[3]; state_[2] ^= t;
    state_[3] = rotateLeft(state_[3], 45);
    return result;
}
int Random::uniform(int upperExclusive) {
    if (upperExclusive <= 0) return 0;
    const auto bound = static_cast<std::uint64_t>(upperExclusive);
    const auto threshold = -bound % bound;
    for (;;) { const auto value = next(); if (value >= threshold) return static_cast<int>(value % bound); }
}
const std::array<std::uint64_t, 4>& Random::state() const { return state_; }
void Random::setState(const std::array<std::uint64_t, 4>& state) {
    const bool allZero = std::all_of(state.begin(), state.end(), [](std::uint64_t value) { return value == 0; });
    state_ = allZero ? Random(1).state() : state;
}

GameEngine::GameEngine()
    : territories_(makeWorldTerritories()),
      continents_(makeWorldContinents()),
      cardCatalog_(makeRiskDeck(42)),
      random_(1) {}

bool GameEngine::startNewGame(
    int playerCount,
    int humanPlayers,
    std::uint64_t seed,
    GameMode mode,
    RulesOptions rules,
    const std::string& mapId
) {
    const bool knownMode = mode == GameMode::Classic || mode == GameMode::SecretMission || mode == GameMode::Capital;
    const int minimumPlayers = mode == GameMode::Classic ? 2 : 3;
    const auto definition = makeBuiltinMap(mapId);
    if (!knownMode || playerCount < minimumPlayers || playerCount > 5 || humanPlayers < 0 || humanPlayers > playerCount || !definition) return false;

    territories_ = definition->territories;
    continents_ = definition->continents;
    mapId_ = definition->id;
    mapDisplayName_ = definition->displayName;
    cardCatalog_ = makeRiskDeck(static_cast<int>(territories_.size()));
    random_ = Random(seed); mode_ = mode; rules_ = rules;
    phase_ = Phase::Reinforce; currentPlayer_ = 0; winner_ = -1; turn_ = 1; tradeCount_ = 0; conqueredThisTurn_ = false;
    commanderDieUsed_ = false; maneuverUsed_ = false; maneuverSource_ = -1; maneuverTarget_ = -1;
    setupPlayers(playerCount, humanPlayers);
    if (mode_ == GameMode::SecretMission) {
        assignMissions(); distributeTerritories(); placeStartingTroopsOfficial(); initializeDeck(); currentPlayer_ = rollHighestPlayer();
    } else if (mode_ == GameMode::Capital) {
        currentPlayer_ = rollHighestPlayer(); claimTerritories(currentPlayer_); placeStartingTroopsOfficial(); assignHeadquarters(); initializeDeck(); removeHeadquartersFromDeck();
    } else if (playerCount == 2) {
        addNeutralPlayer(); distributeTerritories(); placeTwoPlayerStartingTroops(); initializeDeck(); currentPlayer_ = rollHighestPlayer();
    } else {
        currentPlayer_ = rollHighestPlayer(); claimTerritories(currentPlayer_); placeStartingTroopsOfficial(); initializeDeck();
    }
    beginTurn();
    return true;
}

void GameEngine::setupPlayers(int playerCount, int humanPlayers) {
    static constexpr std::array<std::uint32_t, 5> colors = {0xffd9534fU,0xff428bcaU,0xff5cb85cU,0xfff0ad4eU,0xff9b59b6U};
    players_.clear(); players_.reserve(static_cast<std::size_t>(playerCount));
    aiContinentGoals_.assign(static_cast<std::size_t>(playerCount), -1);
    for (int i = 0; i < playerCount; ++i) {
        Player player; player.id = i;
        player.name = i < humanPlayers ? "Player " + std::to_string(i + 1) : "AI " + std::to_string(i - humanPlayers + 1);
        player.color = colors[static_cast<std::size_t>(i)];
        player.ai = i >= humanPlayers;
        if (player.ai) player.aiStrategy = static_cast<AiStrategy>(i % 5);
        players_.push_back(std::move(player));
    }
}

void GameEngine::distributeTerritories() {
    std::vector<int> ids(territories_.size()); for (std::size_t i = 0; i < ids.size(); ++i) ids[i] = static_cast<int>(i);
    for (std::size_t i = ids.size(); i > 1; --i) { const auto j = static_cast<std::size_t>(random_.uniform(static_cast<int>(i))); std::swap(ids[i - 1], ids[j]); }
    for (std::size_t i = 0; i < ids.size(); ++i) { auto& territory = territories_[static_cast<std::size_t>(ids[i])]; territory.owner = static_cast<int>(i % players_.size()); territory.armies = 1; }
}

void GameEngine::placeStartingTroops() {
    const int troops = startingTroops(static_cast<int>(players_.size()));
    std::vector<int> remaining(players_.size(), troops);
    for (const auto& territory : territories_) if (territory.owner >= 0) --remaining[static_cast<std::size_t>(territory.owner)];
    bool pending = true;
    while (pending) {
        pending = false;
        for (std::size_t playerId = 0; playerId < players_.size(); ++playerId) {
            if (remaining[playerId] <= 0) continue;
            std::vector<int> owned; for (const auto& territory : territories_) if (territory.owner == static_cast<int>(playerId)) owned.push_back(territory.id);
            if (!owned.empty()) ++territories_[static_cast<std::size_t>(owned[static_cast<std::size_t>(random_.uniform(static_cast<int>(owned.size())))])].armies;
            --remaining[playerId]; pending = true;
        }
    }
}

void GameEngine::initializeDeck() {
    cardCatalog_ = makeRiskDeck(static_cast<int>(territories_.size()));
    deck_ = cardCatalog_;
    discard_.clear();
    shuffleCards(deck_);
}
void GameEngine::shuffleCards(std::vector<Card>& cards) {
    for (std::size_t i = cards.size(); i > 1; --i) { const auto j = static_cast<std::size_t>(random_.uniform(static_cast<int>(i))); std::swap(cards[i - 1], cards[j]); }
}
void GameEngine::recycleDiscard() { if (!deck_.empty() || discard_.empty()) return; deck_ = std::move(discard_); discard_.clear(); shuffleCards(deck_); }
void GameEngine::awardTurnCard() {
    if (!conqueredThisTurn_ || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size())) { conqueredThisTurn_ = false; return; }
    recycleDiscard(); if (!deck_.empty()) { players_[static_cast<std::size_t>(currentPlayer_)].cards.push_back(deck_.back().id); deck_.pop_back(); }
    conqueredThisTurn_ = false;
}
void GameEngine::transferCards(int fromPlayer, int toPlayer) {
    if (fromPlayer < 0 || toPlayer < 0 || fromPlayer >= static_cast<int>(players_.size()) || toPlayer >= static_cast<int>(players_.size()) || fromPlayer == toPlayer) return;
    auto& source = players_[static_cast<std::size_t>(fromPlayer)].cards; auto& target = players_[static_cast<std::size_t>(toPlayer)].cards;
    target.insert(target.end(), source.begin(), source.end()); source.clear();
}

bool GameEngine::reinforce(int territoryId, int count) {
    if (phase_ != Phase::Reinforce || count <= 0 || !validTerritory(territoryId)) return false;
    auto& player = players_[static_cast<std::size_t>(currentPlayer_)]; auto& territory = territories_[static_cast<std::size_t>(territoryId)];
    if (territory.owner != currentPlayer_ || player.reinforcements < count) return false;
    territory.armies += count; player.reinforcements -= count; return true;
}

BattleResult GameEngine::attack(int sourceId, int targetId) {
    BattleResult result = resolveOneBattle(sourceId, targetId);
    if (!result.legal || !rules_.attackWithAll) return result;
    while (!result.captured && phase_ == Phase::Attack && canAttack(sourceId, targetId)) {
        BattleResult next = resolveOneBattle(sourceId, targetId); if (!next.legal) break; result = std::move(next);
    }
    return result;
}

bool GameEngine::maneuver(int sourceId, int targetId, int troops) {
    if (phase_ != Phase::Maneuver || troops <= 0 || !canManeuver(sourceId, targetId)) return false;
    if (!rules_.expandedManeuver && maneuverUsed_ && (sourceId != maneuverSource_ || targetId != maneuverTarget_)) return false;
    auto& source = territories_[static_cast<std::size_t>(sourceId)]; auto& target = territories_[static_cast<std::size_t>(targetId)];
    if (source.armies <= troops) return false;
    source.armies -= troops; target.armies += troops; maneuverUsed_ = true; maneuverSource_ = sourceId; maneuverTarget_ = targetId; return true;
}

std::optional<std::array<std::size_t, 3>> GameEngine::findTradeSet(const Player& player) const {
    if (player.cards.size() < 3) return std::nullopt;
    for (std::size_t a = 0; a + 2 < player.cards.size(); ++a) for (std::size_t b = a + 1; b + 1 < player.cards.size(); ++b) for (std::size_t c = b + 1; c < player.cards.size(); ++c) {
        const Card* first = findCard(player.cards[a]); const Card* second = findCard(player.cards[b]); const Card* third = findCard(player.cards[c]);
        if (first == nullptr || second == nullptr || third == nullptr) continue;
        std::array<int, 4> counts{}; ++counts[static_cast<std::size_t>(first->type)]; ++counts[static_cast<std::size_t>(second->type)]; ++counts[static_cast<std::size_t>(third->type)];
        if (validTradeTypes(counts)) return std::array<std::size_t, 3>{a,b,c};
    }
    return std::nullopt;
}

const Card* GameEngine::findCard(int cardId) const {
    return cardId < 0 || cardId >= static_cast<int>(cardCatalog_.size()) ? nullptr : &cardCatalog_[static_cast<std::size_t>(cardId)];
}

int GameEngine::tradeCardsForPlayer(int playerId) {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) return 0;
    auto& player = players_[static_cast<std::size_t>(playerId)]; const auto set = findTradeSet(player); if (!set) return 0;
    std::array<int, 3> ids = {player.cards[(*set)[0]], player.cards[(*set)[1]], player.cards[(*set)[2]]};
    std::array<std::size_t, 3> positions = *set; std::sort(positions.begin(), positions.end(), std::greater<>());
    for (const std::size_t position : positions) player.cards.erase(player.cards.begin() + static_cast<std::ptrdiff_t>(position));
    for (const int id : ids) if (const Card* card = findCard(id)) discard_.push_back(*card);
    const int bonus = nextTradeValue(); ++tradeCount_; player.reinforcements += bonus;
    for (const int id : ids) {
        const Card* card = findCard(id);
        if (card != nullptr && card->territoryId >= 0 && validTerritory(card->territoryId)) {
            auto& territory = territories_[static_cast<std::size_t>(card->territoryId)];
            if (territory.owner == playerId) { territory.armies += 2; break; }
        }
    }
    return bonus;
}

void GameEngine::tradeAfterElimination(int playerId) {
    if (playerId < 0 || playerId >= static_cast<int>(players_.size())) return;
    auto& player = players_[static_cast<std::size_t>(playerId)]; if (player.cards.size() < 6) return;
    while (player.cards.size() > 4 && findTradeSet(player).has_value()) if (tradeCardsForPlayer(playerId) <= 0) break;
}
int GameEngine::tradeCards() { return phase_ != Phase::Reinforce || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size()) ? 0 : tradeCardsForPlayer(currentPlayer_); }

bool GameEngine::endPhase() {
    if (!running()) return false;
    if (phase_ == Phase::Reinforce) {
        if (players_[static_cast<std::size_t>(currentPlayer_)].reinforcements > 0 || mustTradeCards()) return false;
        phase_ = Phase::Attack; return true;
    }
    if (phase_ == Phase::Attack) { phase_ = Phase::Maneuver; return true; }
    if (phase_ == Phase::Maneuver) { awardTurnCard(); advancePlayer(); return true; }
    return false;
}

bool GameEngine::aiStep() {
    if (!running() || currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size()) || !players_[static_cast<std::size_t>(currentPlayer_)].ai) return false;
    if (phase_ == Phase::Reinforce) {
        if (mustTradeCards()) return tradeCards() > 0;
        runAiReinforcePhase();
        phase_ = Phase::Attack;
        return true;
    }
    if (phase_ == Phase::Attack) {
        runAiAttackPhase();
        if (phase_ != Phase::Finished) phase_ = Phase::Maneuver;
        return true;
    }
    if (phase_ == Phase::Maneuver) {
        runAiManeuverPhase();
        return endPhase();
    }
    return false;
}

const std::vector<Territory>& GameEngine::territories() const { return territories_; }
const std::vector<Continent>& GameEngine::continents() const { return continents_; }
const std::vector<Player>& GameEngine::players() const { return players_; }
const std::vector<Card>& GameEngine::deck() const { return deck_; }
const std::vector<Card>& GameEngine::discard() const { return discard_; }
const Player* GameEngine::currentPlayer() const { return currentPlayer_ < 0 || currentPlayer_ >= static_cast<int>(players_.size()) ? nullptr : &players_[static_cast<std::size_t>(currentPlayer_)]; }
const std::string& GameEngine::mapId() const { return mapId_; }
const std::string& GameEngine::mapDisplayName() const { return mapDisplayName_; }
GameMode GameEngine::mode() const { return mode_; }
const RulesOptions& GameEngine::rules() const { return rules_; }
bool GameEngine::commanderDieUsed() const { return commanderDieUsed_; }
Phase GameEngine::phase() const { return phase_; }
int GameEngine::currentPlayerId() const { return currentPlayer_; }
int GameEngine::winnerId() const { return winner_; }
std::uint64_t GameEngine::turn() const { return turn_; }
int GameEngine::tradeCount() const { return tradeCount_; }
int GameEngine::nextTradeValue() const { return rules_.incrementalCardSetValues ? 4 + tradeCount_ : tradeValue(tradeCount_); }
bool GameEngine::canTradeCards() const { const auto* player = currentPlayer(); return player != nullptr && findTradeSet(*player).has_value(); }
bool GameEngine::mustTradeCards() const { const auto* player = currentPlayer(); return player != nullptr && player->cards.size() >= 5 && canTradeCards(); }
bool GameEngine::running() const { return !players_.empty() && phase_ != Phase::Finished; }

bool GameEngine::canAttack(int sourceId, int targetId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId) || !isAdjacent(sourceId, targetId)) return false;
    const auto& source = territories_[static_cast<std::size_t>(sourceId)]; const auto& target = territories_[static_cast<std::size_t>(targetId)];
    return source.owner == currentPlayer_ && target.owner >= 0 && target.owner != currentPlayer_ && source.armies >= 2 && target.armies >= 1;
}
bool GameEngine::canManeuver(int sourceId, int targetId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId) || sourceId == targetId) return false;
    const auto& source = territories_[static_cast<std::size_t>(sourceId)]; const auto& target = territories_[static_cast<std::size_t>(targetId)];
    return source.owner == currentPlayer_ && target.owner == currentPlayer_ && source.armies >= 2 && ownsConnectedPath(sourceId, targetId, currentPlayer_);
}
bool GameEngine::ownsConnectedPath(int sourceId, int targetId, int ownerId) const {
    if (!validTerritory(sourceId) || !validTerritory(targetId)) return false;
    std::vector<bool> visited(territories_.size(), false); std::queue<int> pending; pending.push(sourceId); visited[static_cast<std::size_t>(sourceId)] = true;
    while (!pending.empty()) {
        const int current = pending.front(); pending.pop(); if (current == targetId) return true;
        for (const int next : territories_[static_cast<std::size_t>(current)].adjacent) if (!visited[static_cast<std::size_t>(next)] && territories_[static_cast<std::size_t>(next)].owner == ownerId) { visited[static_cast<std::size_t>(next)] = true; pending.push(next); }
    }
    return false;
}

Snapshot GameEngine::snapshot() const {
    Snapshot result;
    result.version = 6; result.mapId = mapId_; result.mode = mode_; result.rules = rules_; result.phase = phase_; result.currentPlayer = currentPlayer_; result.winner = winner_; result.turn = turn_; result.rngState = random_.state();
    result.players = players_; result.territories = territories_; result.deck = deck_; result.discard = discard_; result.tradeCount = tradeCount_; result.conqueredThisTurn = conqueredThisTurn_;
    result.commanderDieUsed = commanderDieUsed_; result.maneuverUsed = maneuverUsed_; result.maneuverSource = maneuverSource_; result.maneuverTarget = maneuverTarget_;
    if (!makeBuiltinMap(mapId_)) {
        MapDefinition embedded;
        embedded.id = mapId_;
        embedded.displayName = mapDisplayName_;
        embedded.territories = territories_;
        embedded.continents = continents_;
        for (auto& territory : embedded.territories) {
            territory.owner = -1;
            territory.armies = 0;
        }
        result.mapDefinition = std::move(embedded);
    }
    return result;
}

bool GameEngine::restore(const Snapshot& snapshot) {
    if (snapshot.version < 2 || snapshot.version > 6 || snapshot.players.size() < 2 || snapshot.players.size() > 5) return false;

    const std::string restoredMapId = snapshot.mapId.empty() ? "world" : snapshot.mapId;
    std::optional<MapDefinition> definition = makeBuiltinMap(restoredMapId);
    if (!definition) {
        if (!snapshot.mapDefinition || snapshot.mapDefinition->id != restoredMapId || !validMapDefinition(*snapshot.mapDefinition)) return false;
        definition = snapshot.mapDefinition;
    }
    if (snapshot.territories.size() != definition->territories.size()) return false;
    const int territoryCountValue = static_cast<int>(definition->territories.size());
    const auto catalog = makeRiskDeck(territoryCountValue);
    const auto validCardId = [&](int cardId) { return cardId >= 0 && cardId < static_cast<int>(catalog.size()); };

    const GameMode restoredMode = snapshot.version >= 4 ? snapshot.mode : GameMode::Classic;
    if (restoredMode != GameMode::Classic && restoredMode != GameMode::SecretMission && restoredMode != GameMode::Capital) return false;
    int neutralCount = 0, activeCount = 0;
    for (const auto& player : snapshot.players) {
        if (static_cast<int>(player.aiStrategy) < static_cast<int>(AiStrategy::Easy)
            || static_cast<int>(player.aiStrategy) > static_cast<int>(AiStrategy::Random)) return false;
        player.neutral ? ++neutralCount : ++activeCount;
    }
    if (neutralCount > 0) { if (restoredMode != GameMode::Classic || neutralCount != 1 || activeCount != 2 || snapshot.players.size() != 3) return false; }
    else if ((restoredMode == GameMode::SecretMission || restoredMode == GameMode::Capital) && snapshot.players.size() < 3) return false;
    if (snapshot.currentPlayer < 0 || snapshot.currentPlayer >= static_cast<int>(snapshot.players.size()) || snapshot.players[static_cast<std::size_t>(snapshot.currentPlayer)].neutral) return false;

    std::vector<Territory> restoredTerritories = definition->territories;
    for (std::size_t i = 0; i < snapshot.territories.size(); ++i) {
        const auto& state = snapshot.territories[i];
        if (state.id != static_cast<int>(i) || state.owner < -1 || state.owner >= static_cast<int>(snapshot.players.size()) || state.armies < 0) return false;
        restoredTerritories[i].owner = state.owner;
        restoredTerritories[i].armies = state.armies;
    }

    if (snapshot.version >= 5 && snapshot.maneuverUsed) {
        const bool consumed = snapshot.maneuverSource == -1 && snapshot.maneuverTarget == -1;
        const bool route = snapshot.maneuverSource >= 0 && snapshot.maneuverSource < territoryCountValue
            && snapshot.maneuverTarget >= 0 && snapshot.maneuverTarget < territoryCountValue
            && snapshot.maneuverSource != snapshot.maneuverTarget;
        if (snapshot.phase != Phase::Maneuver || (!consumed && !route)) return false;
    }

    std::vector<bool> headquarters(static_cast<std::size_t>(territoryCountValue), false);
    for (const auto& player : snapshot.players) {
        for (const int cardId : player.cards) if (!validCardId(cardId)) return false;
        if (player.neutral && (!player.cards.empty() || player.mission.has_value() || player.headquarters >= 0)) return false;
        if (restoredMode == GameMode::SecretMission && !player.neutral && !player.mission.has_value()) return false;
        if (player.mission && player.mission->kind == MissionKind::Elimination && (player.mission->eliminationTarget < 0 || player.mission->eliminationTarget >= static_cast<int>(snapshot.players.size()) || snapshot.players[static_cast<std::size_t>(player.mission->eliminationTarget)].neutral)) return false;
        if (restoredMode == GameMode::Capital) {
            if (player.headquarters < 0 || player.headquarters >= territoryCountValue || headquarters[static_cast<std::size_t>(player.headquarters)]) return false;
            headquarters[static_cast<std::size_t>(player.headquarters)] = true;
        }
    }

    std::vector<Card> restoredDeck;
    std::vector<Card> restoredDiscard;
    if (snapshot.version >= 3) {
        restoredDeck.reserve(snapshot.deck.size());
        for (const auto& card : snapshot.deck) {
            if (!validCardId(card.id)) return false;
            restoredDeck.push_back(catalog[static_cast<std::size_t>(card.id)]);
        }
        restoredDiscard.reserve(snapshot.discard.size());
        for (const auto& card : snapshot.discard) {
            if (!validCardId(card.id)) return false;
            restoredDiscard.push_back(catalog[static_cast<std::size_t>(card.id)]);
        }
    }

    if (restoredMode == GameMode::Capital) {
        const auto isHeadquartersCard = [&](int cardId) {
            if (!validCardId(cardId)) return false;
            const Card& card = catalog[static_cast<std::size_t>(cardId)];
            return card.territoryId >= 0 && headquarters[static_cast<std::size_t>(card.territoryId)];
        };
        for (const auto& player : snapshot.players) if (std::any_of(player.cards.begin(), player.cards.end(), isHeadquartersCard)) return false;
        for (const auto& card : restoredDeck) if (card.territoryId >= 0 && headquarters[static_cast<std::size_t>(card.territoryId)]) return false;
        for (const auto& card : restoredDiscard) if (card.territoryId >= 0 && headquarters[static_cast<std::size_t>(card.territoryId)]) return false;
    }

    mapId_ = definition->id;
    mapDisplayName_ = definition->displayName.empty() ? definition->id : definition->displayName;
    mode_ = restoredMode;
    rules_ = snapshot.version >= 5 ? snapshot.rules : RulesOptions{};
    if (snapshot.version < 6) {
        rules_.fogOfWar = false;
        rules_.skynet = false;
    }
    players_ = snapshot.players;
    territories_ = std::move(restoredTerritories);
    continents_ = definition->continents;
    cardCatalog_ = catalog;
    phase_ = snapshot.phase;
    aiContinentGoals_.assign(players_.size(), -1);
    currentPlayer_ = snapshot.currentPlayer; winner_ = snapshot.winner; turn_ = snapshot.turn; random_.setState(snapshot.rngState); tradeCount_ = std::max(0, snapshot.tradeCount); conqueredThisTurn_ = snapshot.conqueredThisTurn;
    commanderDieUsed_ = rules_.commanderDie && snapshot.commanderDieUsed;
    if (snapshot.version >= 5) { maneuverUsed_ = snapshot.maneuverUsed; maneuverSource_ = snapshot.maneuverSource; maneuverTarget_ = snapshot.maneuverTarget; }
    else { maneuverUsed_ = false; maneuverSource_ = -1; maneuverTarget_ = -1; }
    if (maneuverUsed_ && maneuverSource_ >= 0
        && (territories_[static_cast<std::size_t>(maneuverSource_)].owner != currentPlayer_
            || territories_[static_cast<std::size_t>(maneuverTarget_)].owner != currentPlayer_)) return false;
    if (snapshot.version >= 3) { deck_ = std::move(restoredDeck); discard_ = std::move(restoredDiscard); }
    else { const auto savedRng = random_.state(); initializeDeck(); random_.setState(savedRng); tradeCount_ = 0; conqueredThisTurn_ = false; }
    updateEliminationsAndWinner();
    return currentPlayer_ >= 0 && currentPlayer_ < static_cast<int>(players_.size());
}

void GameEngine::beginTurn() {
    updateEliminationsAndWinner(); if (winner_ >= 0) { phase_ = Phase::Finished; return; }
    phase_ = Phase::Reinforce; conqueredThisTurn_ = false; commanderDieUsed_ = false; maneuverUsed_ = false; maneuverSource_ = -1; maneuverTarget_ = -1;
    players_[static_cast<std::size_t>(currentPlayer_)].reinforcements += reinforcementCount(currentPlayer_);
}
void GameEngine::advancePlayer() {
    updateEliminationsAndWinner(); if (winner_ >= 0) { phase_ = Phase::Finished; return; }
    const int count = static_cast<int>(players_.size());
    for (int i = 0; i < count; ++i) { currentPlayer_ = (currentPlayer_ + 1) % count; if (!players_[static_cast<std::size_t>(currentPlayer_)].eliminated) { ++turn_; beginTurn(); return; } }
    phase_ = Phase::Finished;
}
void GameEngine::updateEliminationsAndWinner() {
    if (players_.empty()) { winner_ = -1; return; }
    int alive = 0, candidate = -1;
    for (auto& player : players_) {
        if (player.neutral) { player.eliminated = true; continue; }
        player.eliminated = territoryCount(player.id) == 0; if (!player.eliminated) { ++alive; candidate = player.id; }
    }
    if (mode_ == GameMode::SecretMission) {
        for (const auto& player : players_) if (!player.neutral && !player.eliminated && player.mission && missionCompleted(player.id, *player.mission)) { winner_ = player.id; phase_ = Phase::Finished; return; }
        winner_ = -1; return;
    }
    if (mode_ == GameMode::Capital) { winner_ = capitalWinner(); if (winner_ >= 0) phase_ = Phase::Finished; return; }
    if (alive == 1) { winner_ = candidate; phase_ = Phase::Finished; } else winner_ = -1;
}

int GameEngine::reinforcementCount(int playerId) const {
    int result = std::max(3, territoryCount(playerId) / 3); for (const auto& continent : continents_) if (ownsContinent(playerId, continent)) result += continent.bonus; return result;
}
int GameEngine::territoryCount(int playerId) const { return static_cast<int>(std::count_if(territories_.begin(), territories_.end(), [playerId](const Territory& territory) { return territory.owner == playerId; })); }
bool GameEngine::ownsContinent(int playerId, const Continent& continent) const { return std::all_of(continent.territories.begin(), continent.territories.end(), [&](int territoryId) { return territories_[static_cast<std::size_t>(territoryId)].owner == playerId; }); }
bool GameEngine::validTerritory(int id) const { return id >= 0 && id < static_cast<int>(territories_.size()); }
bool GameEngine::isAdjacent(int sourceId, int targetId) const { const auto& adjacent = territories_[static_cast<std::size_t>(sourceId)].adjacent; return std::find(adjacent.begin(), adjacent.end(), targetId) != adjacent.end(); }

int GameEngine::startingTroops(int playerCount) {
    switch (playerCount) { case 2: return 40; case 3: return 35; case 4: return 30; case 5: return 25; default: throw std::invalid_argument("WinRisk supports 2-5 players"); }
}
int GameEngine::tradeValue(int completedTrades) {
    static constexpr std::array<int, 6> values = {4,6,8,10,12,15};
    if (completedTrades < 0) return values.front();
    return completedTrades < static_cast<int>(values.size()) ? values[static_cast<std::size_t>(completedTrades)] : 15 + (completedTrades - 5) * 5;
}
std::vector<Card> GameEngine::makeRiskDeck(int territoryCount) {
    if (territoryCount < 0) return {};
    std::vector<Card> cards;
    cards.reserve(static_cast<std::size_t>(territoryCount + 2));
    for (int territory = 0; territory < territoryCount; ++territory) cards.push_back({territory, static_cast<CardType>(territory % 3), territory});
    cards.push_back({territoryCount, CardType::Wild, -1});
    cards.push_back({territoryCount + 1, CardType::Wild, -1});
    return cards;
}

std::string phaseName(Phase phase) {
    switch (phase) { case Phase::Reinforce: return "Reinforce"; case Phase::Attack: return "Attack"; case Phase::Maneuver: return "Maneuver"; case Phase::Finished: return "Finished"; }
    return "Unknown";
}
std::string cardTypeName(CardType type) {
    switch (type) { case CardType::Infantry: return "Infantry"; case CardType::Cavalry: return "Cavalry"; case CardType::Artillery: return "Artillery"; case CardType::Wild: return "Wild"; }
    return "Unknown";
}

}
