#include "qt/LegacyJavaImporter.hpp"

#include <QCryptographicHash>
#include <QJsonArray>
#include <QJsonObject>
#include <QRegularExpression>

#include <algorithm>
#include <cstdint>
#include <optional>
#include <vector>

namespace winrisk::qt {
namespace {

QString cardSymbolName(CardType type) {
    switch (type) {
        case CardType::Infantry: return "INFANTRY";
        case CardType::Cavalry: return "CAVALRY";
        case CardType::Artillery: return "ARTILLERY";
        case CardType::Wild: return "WILD";
    }
    return {};
}

bool parseMode(const QString& value, GameMode& mode) {
    if (value == "CLASSIC") { mode = GameMode::Classic; return true; }
    if (value == "SECRET_MISSION") { mode = GameMode::SecretMission; return true; }
    if (value == "CAPITAL") { mode = GameMode::Capital; return true; }
    return false;
}

bool parsePhase(const QString& value, Phase& phase) {
    if (value == "REINFORCE") { phase = Phase::Reinforce; return true; }
    if (value == "ATTACK") { phase = Phase::Attack; return true; }
    if (value == "MOVE") { phase = Phase::Maneuver; return true; }
    return false;
}

bool parseAiStrategy(const QString& className, AiStrategy& strategy) {
    if (className.endsWith(".EasyAI") || className == "EasyAI") { strategy = AiStrategy::Easy; return true; }
    if (className.endsWith(".ContinentAI") || className == "ContinentAI") { strategy = AiStrategy::Continent; return true; }
    if (className.endsWith(".BalancedAI") || className == "BalancedAI") { strategy = AiStrategy::Balanced; return true; }
    if (className.endsWith(".BorderGuardAI") || className == "BorderGuardAI") { strategy = AiStrategy::BorderGuard; return true; }
    if (className.endsWith(".RandomAI") || className == "RandomAI") { strategy = AiStrategy::Random; return true; }
    return false;
}

bool parseMission(const QJsonObject& object, MissionSpec& mission, QString& error) {
    const QString kind = object.value("kind").toString();
    if (kind == "TERRITORY") mission.kind = MissionKind::Territory;
    else if (kind == "FORTIFIED_TERRITORY") mission.kind = MissionKind::FortifiedTerritory;
    else if (kind == "CONTINENTS") mission.kind = MissionKind::Continents;
    else if (kind == "ELIMINATION") mission.kind = MissionKind::Elimination;
    else { error = "Unsupported Java mission kind"; return false; }
    mission.territories = object.value("territories").toInt();
    mission.minimumArmies = object.value("minimumArmies").toInt();
    mission.continentCount = object.value("continentCount").toInt();
    mission.eliminationTarget = object.value("eliminationTargetIndex").toInt(-1);
    return true;
}

bool mapSketchMatches(const QJsonObject& root, const MapDefinition& map) {
    const QJsonObject sketch = root.value("map").toObject();
    const QJsonArray fields = sketch.value("fields").toArray();
    const QJsonArray continents = sketch.value("continents").toArray();
    if (fields.size() != static_cast<qsizetype>(map.territories.size())
        || continents.size() != static_cast<qsizetype>(map.continents.size())) return false;

    const auto catalog = GameEngine::makeRiskDeck(static_cast<int>(map.territories.size()));
    for (qsizetype i = 0; i < fields.size(); ++i) {
        const QJsonObject field = fields[i].toObject();
        const auto& expected = map.territories[static_cast<std::size_t>(i)];
        if (field.value("fieldIndex").toInt(-1) != expected.id
            || field.value("displayName").toString() != QString::fromStdString(expected.name)
            || field.value("cardSymbol").toString() != cardSymbolName(catalog[static_cast<std::size_t>(i)].type)) return false;

        std::vector<int> actualNext;
        for (const auto value : field.value("next").toArray()) actualNext.push_back(value.toInt(-1));
        auto expectedNext = expected.adjacent;
        std::sort(actualNext.begin(), actualNext.end());
        std::sort(expectedNext.begin(), expectedNext.end());
        if (actualNext != expectedNext) return false;
    }

    for (qsizetype i = 0; i < continents.size(); ++i) {
        const QJsonObject continent = continents[i].toObject();
        const auto& expected = map.continents[static_cast<std::size_t>(i)];
        if (continent.value("bonus").toInt(-1) != expected.bonus) return false;
        std::vector<int> actualFields;
        for (const auto value : continent.value("fields").toArray()) actualFields.push_back(value.toInt(-1));
        auto expectedFields = expected.territories;
        std::sort(actualFields.begin(), actualFields.end());
        std::sort(expectedFields.begin(), expectedFields.end());
        if (actualFields != expectedFields) return false;
    }
    return true;
}

std::optional<MapDefinition> resolveBuiltinMap(const QJsonObject& root, const QJsonObject& params, QString& error) {
    const QString requested = params.value("builtinMap").toString();
    if (!requested.isEmpty()) {
        const auto map = GameEngine::makeBuiltinMap(requested.toStdString());
        if (!map || !mapSketchMatches(root, *map)) {
            error = "Java save built-in map does not match its canonical definition";
            return std::nullopt;
        }
        return map;
    }

    for (const auto& id : GameEngine::builtinMapIds()) {
        const auto map = GameEngine::makeBuiltinMap(id);
        if (map && mapSketchMatches(root, *map)) return map;
    }
    error = "Java save uses a custom, procedural or unsupported historical map";
    return std::nullopt;
}

class CardMapper {
public:
    explicit CardMapper(int territoryCount)
        : territoryCount_(territoryCount),
          catalog_(GameEngine::makeRiskDeck(territoryCount)),
          used_(catalog_.size(), false) {}

    bool idFor(const QJsonObject& object, int& id, QString& error) {
        const int fieldIndex = object.value("fieldIndex").toInt(-1);
        const QString symbol = object.value("symbol").toString();
        if (fieldIndex >= 0) {
            if (fieldIndex >= territoryCount_) { error = "Invalid Java territory card index"; return false; }
            id = fieldIndex;
            if (symbol != cardSymbolName(catalog_[static_cast<std::size_t>(id)].type)) {
                error = "Java card symbol does not match the selected built-in map";
                return false;
            }
        } else {
            if (symbol != "WILD") { error = "Invalid Java wild card"; return false; }
            const int firstWild = territoryCount_;
            const int secondWild = territoryCount_ + 1;
            if (!used_[static_cast<std::size_t>(firstWild)]) id = firstWild;
            else if (!used_[static_cast<std::size_t>(secondWild)]) id = secondWild;
            else { error = "Java save contains more than two wild cards"; return false; }
        }
        if (used_[static_cast<std::size_t>(id)]) { error = "Java save contains a duplicate Risk card"; return false; }
        used_[static_cast<std::size_t>(id)] = true;
        return true;
    }

    const Card& card(int id) const { return catalog_[static_cast<std::size_t>(id)]; }

    bool validateComplete(GameMode mode, const std::vector<Player>& players, QString& error) const {
        std::vector<bool> headquarters(static_cast<std::size_t>(territoryCount_), false);
        if (mode == GameMode::Capital) {
            for (const auto& player : players) {
                if (player.neutral) continue;
                if (player.headquarters < 0 || player.headquarters >= territoryCount_) {
                    error = "Java Capital save has an invalid headquarters";
                    return false;
                }
                headquarters[static_cast<std::size_t>(player.headquarters)] = true;
            }
        }
        for (int id = 0; id < static_cast<int>(catalog_.size()); ++id) {
            if (used_[static_cast<std::size_t>(id)]) continue;
            if (mode == GameMode::Capital && id < territoryCount_ && headquarters[static_cast<std::size_t>(id)]) continue;
            error = "Java save is missing a Risk card";
            return false;
        }
        return true;
    }

private:
    int territoryCount_;
    std::vector<Card> catalog_;
    std::vector<bool> used_;
};

std::uint64_t conversionSeed(const QByteArray& raw) {
    static const QRegularExpression pattern(QStringLiteral("\\\"randomSeed\\\"\\s*:\\s*(-?\\d+)"));
    const auto match = pattern.match(QString::fromUtf8(raw));
    if (match.hasMatch()) {
        bool ok = false;
        const qlonglong signedSeed = match.captured(1).toLongLong(&ok);
        if (ok) return static_cast<std::uint64_t>(signedSeed);
    }
    const QByteArray digest = QCryptographicHash::hash(raw, QCryptographicHash::Sha256);
    std::uint64_t seed = 0;
    for (int i = 0; i < 8; ++i) seed = (seed << 8) | static_cast<unsigned char>(digest[i]);
    return seed;
}

}

bool importJavaGameSketch(const QJsonObject& root, const QByteArray& raw, Snapshot& snapshot, QString& error) {
    if (!root.contains("params") || !root.contains("players") || !root.contains("fieldOwner") || !root.contains("fieldArmy")) {
        error = "Unsupported save format";
        return false;
    }

    const QJsonObject params = root.value("params").toObject();
    const auto map = resolveBuiltinMap(root, params, error);
    if (!map) return false;
    const int territoryCount = static_cast<int>(map->territories.size());

    GameMode mode;
    if (!parseMode(params.value("gameMode").toString(), mode)) { error = "Unsupported Java game mode"; return false; }

    snapshot = {};
    snapshot.version = 6;
    snapshot.mapId = map->id;
    snapshot.mode = mode;
    snapshot.rules.incrementalCardSetValues = params.value("incrementalCardSetValues").toBool();
    snapshot.rules.expandedManeuver = params.value("expandedManeuver").toBool();
    snapshot.rules.attackCardReroll = params.value("attackCardReroll").toBool();
    snapshot.rules.commanderDie = params.value("commanderDie").toBool();
    snapshot.rules.attackWithAll = params.value("attackWithAll").toBool();
    snapshot.rules.fogOfWar = params.value("fogOfWar").toBool();
    snapshot.rules.skynet = params.value("skynetMode").toBool();

    if (!parsePhase(root.value("phase").toString(), snapshot.phase)) {
        error = "Java save is in an unsupported transient phase";
        return false;
    }

    const QJsonArray oldPlayers = root.value("players").toArray();
    if (oldPlayers.size() < 2 || oldPlayers.size() > 5) { error = "Unsupported Java player roster size"; return false; }

    snapshot.players.reserve(static_cast<std::size_t>(oldPlayers.size()));
    CardMapper cards(territoryCount);
    for (qsizetype i = 0; i < oldPlayers.size(); ++i) {
        const QJsonObject object = oldPlayers[i].toObject();
        Player player;
        player.id = static_cast<int>(i);
        player.neutral = object.value("neutral").toBool();
        player.color = static_cast<std::uint32_t>(static_cast<qint64>(object.value("colorRgb").toDouble()));
        player.reinforcements = object.value("reinforcements").toInt();
        player.headquarters = object.value("headquartersIndex").toInt(-1);
        const QString interfaceClass = object.value("interfaceClass").toString();
        player.ai = player.neutral || !interfaceClass.contains("Human", Qt::CaseInsensitive);
        if (player.ai && !player.neutral && !parseAiStrategy(interfaceClass, player.aiStrategy)) {
            error = "Unsupported Java AI class: " + interfaceClass;
            return false;
        }
        player.eliminated = player.neutral;
        player.name = player.neutral ? "Neutral"
            : player.ai ? GameEngine::aiStrategyName(player.aiStrategy) + " AI " + std::to_string(player.id + 1)
                        : "Player " + std::to_string(player.id + 1);

        if (object.contains("mission") && !object.value("mission").isNull()) {
            MissionSpec mission;
            if (!parseMission(object.value("mission").toObject(), mission, error)) return false;
            player.mission = mission;
        }
        for (const auto value : object.value("cards").toArray()) {
            int id = -1;
            if (!cards.idFor(value.toObject(), id, error)) return false;
            player.cards.push_back(id);
        }
        snapshot.players.push_back(std::move(player));
    }

    const int activePlayers = params.value("humanPlayers").toInt() + params.value("aiPlayers").toInt();
    const int neutralIndex = root.value("neutralPlayerIndex").toInt(-1);
    int actualNeutral = -1;
    for (const auto& player : snapshot.players) {
        if (!player.neutral) continue;
        if (actualNeutral >= 0) { error = "Java save contains multiple neutral players"; return false; }
        actualNeutral = player.id;
    }
    if (actualNeutral != neutralIndex) { error = "Java neutral-player index is inconsistent"; return false; }
    if (mode == GameMode::Classic && activePlayers == 2) {
        if (snapshot.players.size() != 3 || actualNeutral < 0) { error = "Java two-player Classic save is missing Neutral"; return false; }
    } else if (actualNeutral >= 0 || snapshot.players.size() != activePlayers) {
        error = "Java player roster does not match game parameters";
        return false;
    }
    if ((mode == GameMode::SecretMission || mode == GameMode::Capital) && activePlayers < 3) {
        error = "Java game mode has too few active players";
        return false;
    }

    for (auto& player : snapshot.players) {
        if (player.mission && player.mission->kind == MissionKind::Elimination) {
            const int target = player.mission->eliminationTarget;
            if (target < 0 || target >= static_cast<int>(snapshot.players.size())
                || snapshot.players[static_cast<std::size_t>(target)].neutral) {
                error = "Java elimination mission has an invalid target";
                return false;
            }
        }
        if (mode == GameMode::SecretMission && !player.neutral && !player.mission) {
            error = "Java Secret Mission save is missing a mission";
            return false;
        }
        if (mode == GameMode::Capital && !player.neutral
            && (player.headquarters < 0 || player.headquarters >= territoryCount)) {
            error = "Java Capital save is missing a headquarters";
            return false;
        }
    }

    snapshot.territories = map->territories;
    const QJsonArray owners = root.value("fieldOwner").toArray();
    const QJsonArray armies = root.value("fieldArmy").toArray();
    if (owners.size() != territoryCount || armies.size() != territoryCount) {
        error = "Java save territory state does not match its built-in map";
        return false;
    }
    for (int i = 0; i < territoryCount; ++i) {
        const int owner = owners[i].toInt(-1);
        const int army = armies[i].toInt(-1);
        if (owner < -1 || owner >= static_cast<int>(snapshot.players.size()) || army < 0) {
            error = "Java territory state is invalid";
            return false;
        }
        snapshot.territories[static_cast<std::size_t>(i)].owner = owner;
        snapshot.territories[static_cast<std::size_t>(i)].armies = army;
    }

    snapshot.currentPlayer = root.value("curPlayer").toInt(-1);
    if (snapshot.currentPlayer < 0 || snapshot.currentPlayer >= static_cast<int>(snapshot.players.size())
        || snapshot.players[static_cast<std::size_t>(snapshot.currentPlayer)].neutral) {
        error = "Java current player is invalid";
        return false;
    }
    snapshot.winner = -1;
    snapshot.turn = 1;
    snapshot.tradeCount = root.value("tradeCount").toInt();
    snapshot.commanderDieUsed = root.value("commanderDieUsed").toBool();
    snapshot.maneuverUsed = root.value("maneuverUsed").toBool();
    snapshot.maneuverSource = -1;
    snapshot.maneuverTarget = -1;
    snapshot.conqueredThisTurn = oldPlayers[snapshot.currentPlayer].toObject().value("conqueredTerritoryThisTurn").toBool();

    const QJsonArray javaDraw = root.value("drawPile").toArray();
    snapshot.deck.clear();
    snapshot.deck.reserve(static_cast<std::size_t>(javaDraw.size()));
    for (qsizetype i = javaDraw.size(); i > 0; --i) {
        int id = -1;
        if (!cards.idFor(javaDraw[i - 1].toObject(), id, error)) return false;
        snapshot.deck.push_back(cards.card(id));
    }

    const QJsonArray javaDiscard = root.value("discardPile").toArray();
    snapshot.discard.clear();
    snapshot.discard.reserve(static_cast<std::size_t>(javaDiscard.size()));
    for (const auto value : javaDiscard) {
        int id = -1;
        if (!cards.idFor(value.toObject(), id, error)) return false;
        snapshot.discard.push_back(cards.card(id));
    }

    if (!cards.validateComplete(mode, snapshot.players, error)) return false;

    Random convertedRandom(conversionSeed(raw));
    snapshot.rngState = convertedRandom.state();
    return true;
}

}
