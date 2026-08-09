#pragma once

#include <array>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace winrisk {

enum class Phase {
    Reinforce = 0,
    Attack = 1,
    Maneuver = 2,
    Finished = 3
};

enum class GameMode {
    Classic = 0,
    SecretMission = 1,
    Capital = 2
};

enum class CardType {
    Infantry = 0,
    Cavalry = 1,
    Artillery = 2,
    Wild = 3
};

enum class MissionKind {
    Territory = 0,
    FortifiedTerritory = 1,
    Continents = 2,
    Elimination = 3
};

struct MissionSpec {
    MissionKind kind = MissionKind::Territory;
    int territories = 0;
    int minimumArmies = 0;
    int continentCount = 0;
    int eliminationTarget = -1;
};

struct Card {
    int id = -1;
    CardType type = CardType::Infantry;
    int territoryId = -1;
};

struct Territory {
    int id = -1;
    std::string name;
    float x = 0.0f;
    float y = 0.0f;
    int continent = -1;
    std::vector<int> adjacent;
    int owner = -1;
    int armies = 0;
};

struct Continent {
    int id = -1;
    std::string name;
    int bonus = 0;
    std::vector<int> territories;
};

struct Player {
    int id = -1;
    std::string name;
    std::uint32_t color = 0xff808080u;
    bool ai = false;
    bool eliminated = false;
    int reinforcements = 0;
    std::vector<int> cards;
    std::optional<MissionSpec> mission;
};

struct BattleResult {
    bool legal = false;
    bool captured = false;
    int attackerLosses = 0;
    int defenderLosses = 0;
    std::vector<int> attackDice;
    std::vector<int> defenseDice;
};

struct Snapshot {
    int version = 4;
    GameMode mode = GameMode::Classic;
    Phase phase = Phase::Reinforce;
    int currentPlayer = 0;
    int winner = -1;
    std::uint64_t turn = 1;
    std::array<std::uint64_t, 4> rngState{};
    std::vector<Player> players;
    std::vector<Territory> territories;
    std::vector<Card> deck;
    std::vector<Card> discard;
    int tradeCount = 0;
    bool conqueredThisTurn = false;
};

class Random {
public:
    explicit Random(std::uint64_t seed = 1);
    std::uint64_t next();
    int uniform(int upperExclusive);
    const std::array<std::uint64_t, 4>& state() const;
    void setState(const std::array<std::uint64_t, 4>& state);

private:
    std::array<std::uint64_t, 4> state_{};
};

class GameEngine {
public:
    GameEngine();

    bool startNewGame(int playerCount, int humanPlayers, std::uint64_t seed, GameMode mode = GameMode::Classic);
    bool reinforce(int territoryId, int count = 1);
    BattleResult attack(int sourceId, int targetId);
    bool maneuver(int sourceId, int targetId, int troops = 1);
    int tradeCards();
    bool endPhase();
    bool aiStep();

    const std::vector<Territory>& territories() const;
    const std::vector<Continent>& continents() const;
    const std::vector<Player>& players() const;
    const std::vector<Card>& deck() const;
    const std::vector<Card>& discard() const;
    const Player* currentPlayer() const;
    GameMode mode() const;
    Phase phase() const;
    int currentPlayerId() const;
    int winnerId() const;
    std::uint64_t turn() const;
    int tradeCount() const;
    int nextTradeValue() const;
    bool canTradeCards() const;
    bool mustTradeCards() const;
    bool running() const;
    std::string missionText(int playerId) const;

    bool canAttack(int sourceId, int targetId) const;
    bool canManeuver(int sourceId, int targetId) const;
    bool ownsConnectedPath(int sourceId, int targetId, int ownerId) const;

    Snapshot snapshot() const;
    bool restore(const Snapshot& snapshot);

    static std::vector<Territory> makeWorldTerritories();
    static std::vector<Continent> makeWorldContinents();
    static std::vector<Card> makeRiskDeck();
    static std::vector<MissionSpec> makeMissionDeck(int playerCount);
    static std::string missionDescription(const MissionSpec& mission);
    static int startingTroops(int playerCount);
    static int tradeValue(int completedTrades);

private:
    std::vector<Territory> territories_;
    std::vector<Continent> continents_;
    std::vector<Player> players_;
    std::vector<Card> deck_;
    std::vector<Card> discard_;
    Random random_;
    GameMode mode_ = GameMode::Classic;
    Phase phase_ = Phase::Finished;
    int currentPlayer_ = 0;
    int winner_ = -1;
    std::uint64_t turn_ = 0;
    int tradeCount_ = 0;
    bool conqueredThisTurn_ = false;
    bool maneuverUsed_ = false;
    int maneuverSource_ = -1;
    int maneuverTarget_ = -1;

    void setupPlayers(int playerCount, int humanPlayers);
    void assignMissions();
    int rollHighestPlayer();
    void distributeTerritories();
    void placeStartingTroops();
    void initializeDeck();
    void shuffleCards(std::vector<Card>& cards);
    void recycleDiscard();
    void awardTurnCard();
    void transferCards(int fromPlayer, int toPlayer);
    int tradeCardsForPlayer(int playerId);
    void tradeAfterElimination(int playerId);
    void beginTurn();
    void advancePlayer();
    void updateEliminationsAndWinner();
    bool missionCompleted(int playerId, const MissionSpec& mission) const;
    int reinforcementCount(int playerId) const;
    int territoryCount(int playerId) const;
    bool ownsContinent(int playerId, const Continent& continent) const;
    bool validTerritory(int id) const;
    bool isAdjacent(int sourceId, int targetId) const;
    int chooseAiReinforcementTarget() const;
    std::optional<std::pair<int, int>> chooseAiAttack() const;
    std::optional<std::array<std::size_t, 3>> findTradeSet(const Player& player) const;
    const Card* findCard(int cardId) const;
};

std::string phaseName(Phase phase);
std::string modeName(GameMode mode);
std::string cardTypeName(CardType type);

}
