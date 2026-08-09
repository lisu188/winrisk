#include "qt/LegacyJavaImporter.hpp"
#include "JavaSketchTestSupport.hpp"

#include <QCoreApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <iostream>
#include <set>
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

QJsonObject gameFor(const std::string& mapId, const QString& mode = "CLASSIC") {
    const auto map = GameEngine::makeBuiltinMap(mapId);
    if (!map) return {};
    const int territoryCount = static_cast<int>(map->territories.size());

    QJsonObject root;
    root.insert("map", test::mapSketch(mapId));

    QJsonObject params;
    params.insert("gameMode", mode);
    params.insert("humanPlayers", 1);
    params.insert("aiPlayers", 3);
    params.insert("randomSeed", 86420);
    params.insert("builtinMap", QString::fromStdString(mapId));
    params.insert("fogOfWar", false);
    params.insert("attackWithAll", false);
    params.insert("skynetMode", false);
    params.insert("incrementalCardSetValues", false);
    params.insert("expandedManeuver", false);
    params.insert("attackCardReroll", false);
    params.insert("commanderDie", false);
    root.insert("params", params);

    QJsonArray players;
    players.push_back(test::basicPlayer(0, true));
    players.push_back(test::basicPlayer(1, false, false, "com.winrisk.game.ai.ContinentAI"));
    players.push_back(test::basicPlayer(2, false, false, "com.winrisk.game.ai.BalancedAI"));
    players.push_back(test::basicPlayer(3, false, false, "com.winrisk.game.ai.BorderGuardAI"));
    root.insert("players", players);

    QJsonArray owners;
    QJsonArray armies;
    for (int i = 0; i < territoryCount; ++i) {
        owners.push_back(i % 4);
        armies.push_back(2 + (i % 2));
    }
    root.insert("fieldOwner", owners);
    root.insert("fieldArmy", armies);
    root.insert("curPlayer", 0);
    root.insert("phase", "ATTACK");
    root.insert("commanderDieUsed", false);
    root.insert("maneuverUsed", false);
    root.insert("neutralPlayerIndex", -1);
    root.insert("tradeCount", 2);
    root.insert("drawPile", test::fullDrawPile(territoryCount));
    root.insert("discardPile", QJsonArray{});
    return root;
}

bool import(const QJsonObject& root, Snapshot& snapshot, QString& error) {
    const QByteArray raw = QJsonDocument(root).toJson(QJsonDocument::Compact);
    return qt::importJavaGameSketch(root, raw, snapshot, error);
}

void checkClassic(const std::string& mapId) {
    const auto definition = GameEngine::makeBuiltinMap(mapId);
    CHECK(definition.has_value());
    if (!definition) return;

    QJsonObject root = gameFor(mapId);
    Snapshot snapshot;
    QString error;
    CHECK(import(root, snapshot, error));
    CHECK(error.isEmpty());
    CHECK(snapshot.mapId == mapId);
    CHECK(snapshot.mode == GameMode::Classic);
    CHECK(snapshot.territories.size() == definition->territories.size());
    CHECK(snapshot.deck.size() == definition->territories.size() + 2);
    CHECK(snapshot.deck.back().id == 0);
    CHECK(snapshot.players[1].aiStrategy == AiStrategy::Continent);
    CHECK(snapshot.players[2].aiStrategy == AiStrategy::Balanced);
    CHECK(snapshot.players[3].aiStrategy == AiStrategy::BorderGuard);

    GameEngine engine;
    CHECK(engine.restore(snapshot));
    CHECK(engine.mapId() == mapId);
    CHECK(engine.territories().size() == definition->territories.size());
    CHECK(engine.territories()[0].name == definition->territories[0].name);

    QJsonObject fallback = root;
    QJsonObject params = fallback.value("params").toObject();
    params.remove("builtinMap");
    fallback.insert("params", params);
    Snapshot fallbackSnapshot;
    error.clear();
    CHECK(import(fallback, fallbackSnapshot, error));
    CHECK(fallbackSnapshot.mapId == mapId);
}

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);

    const std::vector<std::string> historical = {"pangaea", "laurasia", "gondwana", "rodinia"};
    for (const auto& mapId : historical) checkClassic(mapId);

    {
        constexpr int playerCount = 4;
        const std::string mapId = "rodinia";
        const auto definition = GameEngine::makeBuiltinMap(mapId);
        CHECK(definition.has_value());
        if (definition) {
            const int territoryCount = static_cast<int>(definition->territories.size());
            QJsonObject root = gameFor(mapId, "CAPITAL");
            QJsonArray players = root.value("players").toArray();
            std::set<int> headquarters;
            for (int i = 0; i < playerCount; ++i) {
                QJsonObject player = players[i].toObject();
                player.insert("headquartersIndex", i);
                players[i] = player;
                headquarters.insert(i);
            }
            root.insert("players", players);

            QJsonArray draw;
            for (int id = 0; id < territoryCount + 2; ++id) {
                if (!headquarters.contains(id)) draw.push_back(test::card(id, territoryCount));
            }
            root.insert("drawPile", draw);

            Snapshot snapshot;
            QString error;
            CHECK(import(root, snapshot, error));
            CHECK(error.isEmpty());
            CHECK(snapshot.mode == GameMode::Capital);
            CHECK(snapshot.mapId == mapId);
            CHECK(static_cast<int>(snapshot.deck.size()) == territoryCount + 2 - playerCount);
            for (int i = 0; i < playerCount; ++i) CHECK(snapshot.players[static_cast<std::size_t>(i)].headquarters == i);

            GameEngine engine;
            CHECK(engine.restore(snapshot));
            CHECK(engine.mode() == GameMode::Capital);
            CHECK(engine.mapId() == mapId);
        }
    }

    {
        QJsonObject drifted = gameFor("pangaea");
        QJsonObject map = drifted.value("map").toObject();
        QJsonArray fields = map.value("fields").toArray();
        QJsonObject first = fields[0].toObject();
        first.insert("displayName", "Not Laurentia");
        fields[0] = first;
        map.insert("fields", fields);
        drifted.insert("map", map);

        Snapshot rejected;
        QString error;
        CHECK(!import(drifted, rejected, error));
        CHECK(error.contains("canonical", Qt::CaseInsensitive));
    }

    if (failures != 0) std::cerr << failures << " historical Java import checks failed\n";
    return failures == 0 ? 0 : 1;
}
