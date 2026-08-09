#include "qt/LegacyJavaImporter.hpp"
#include "JavaSketchTestSupport.hpp"

#include <QCoreApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <iostream>

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

QJsonObject twoPlayerClassic(bool maneuverUsed = false) {
    constexpr int territoryCount = 42;
    QJsonObject root;
    root.insert("map", test::mapSketch("world"));

    QJsonObject params;
    params.insert("gameMode", "CLASSIC");
    params.insert("fogOfWar", false);
    params.insert("attackWithAll", false);
    params.insert("skynetMode", false);
    params.insert("incrementalCardSetValues", false);
    params.insert("expandedManeuver", false);
    params.insert("attackCardReroll", false);
    params.insert("commanderDie", false);
    params.insert("humanPlayers", 1);
    params.insert("aiPlayers", 1);
    params.insert("randomSeed", 424242);
    params.insert("builtinMap", "world");
    root.insert("params", params);

    QJsonArray players;
    players.push_back(test::basicPlayer(0, true));
    players.push_back(test::basicPlayer(1, false));
    players.push_back(test::basicPlayer(2, false, true));
    root.insert("players", players);

    QJsonArray owners;
    QJsonArray armies;
    for (int i = 0; i < territoryCount; ++i) {
        owners.push_back(i % 3);
        armies.push_back(i < 3 ? 14 : 2);
    }
    root.insert("fieldOwner", owners);
    root.insert("fieldArmy", armies);
    root.insert("curPlayer", 0);
    root.insert("phase", maneuverUsed ? "MOVE" : "ATTACK");
    root.insert("commanderDieUsed", false);
    root.insert("maneuverUsed", maneuverUsed);
    root.insert("neutralPlayerIndex", 2);
    root.insert("tradeCount", 0);
    root.insert("drawPile", test::fullDrawPile(territoryCount));
    root.insert("discardPile", QJsonArray{});
    return root;
}

bool import(const QJsonObject& root, Snapshot& snapshot, QString& error) {
    const QByteArray raw = QJsonDocument(root).toJson(QJsonDocument::Compact);
    return qt::importJavaGameSketch(root, raw, snapshot, error);
}

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);

    Snapshot neutralSnapshot;
    QString error;
    const QJsonObject neutralGame = twoPlayerClassic();
    CHECK(import(neutralGame, neutralSnapshot, error));
    CHECK(error.isEmpty());
    CHECK(neutralSnapshot.mode == GameMode::Classic);
    CHECK(neutralSnapshot.mapId == "world");
    CHECK(neutralSnapshot.players.size() == 3);
    CHECK(neutralSnapshot.players[2].neutral);
    CHECK(neutralSnapshot.players[2].ai);
    CHECK(neutralSnapshot.currentPlayer != 2);
    CHECK(neutralSnapshot.deck.size() == 44);
    CHECK(neutralSnapshot.deck.back().id == 0);

    GameEngine neutralEngine;
    CHECK(neutralEngine.restore(neutralSnapshot));
    CHECK(neutralEngine.players().size() == 3);
    CHECK(neutralEngine.players()[2].neutral);
    CHECK(neutralEngine.players()[2].eliminated);

    Snapshot maneuverSnapshot;
    error.clear();
    const QJsonObject maneuverGame = twoPlayerClassic(true);
    CHECK(import(maneuverGame, maneuverSnapshot, error));
    CHECK(maneuverSnapshot.phase == Phase::Maneuver);
    CHECK(maneuverSnapshot.maneuverUsed);
    CHECK(maneuverSnapshot.maneuverSource == -1);
    CHECK(maneuverSnapshot.maneuverTarget == -1);

    GameEngine maneuverEngine;
    CHECK(maneuverEngine.restore(maneuverSnapshot));
    int source = -1;
    int target = -1;
    for (const auto& territory : maneuverEngine.territories()) {
        if (territory.owner != maneuverEngine.currentPlayerId() || territory.armies < 2) continue;
        for (const int adjacent : territory.adjacent) {
            if (maneuverEngine.territories()[static_cast<std::size_t>(adjacent)].owner == maneuverEngine.currentPlayerId()) {
                source = territory.id;
                target = adjacent;
                break;
            }
        }
        if (source >= 0) break;
    }
    if (source >= 0) CHECK(!maneuverEngine.maneuver(source, target, 1));

    QJsonObject duplicate = twoPlayerClassic();
    QJsonArray duplicatePlayers = duplicate.value("players").toArray();
    QJsonObject first = duplicatePlayers[0].toObject();
    QJsonArray hand;
    hand.push_back(test::card(0, 42));
    first.insert("cards", hand);
    duplicatePlayers[0] = first;
    duplicate.insert("players", duplicatePlayers);

    Snapshot rejected;
    error.clear();
    CHECK(!import(duplicate, rejected, error));
    CHECK(error.contains("duplicate", Qt::CaseInsensitive));

    if (failures != 0) std::cerr << failures << " Java import edge checks failed\n";
    return failures == 0 ? 0 : 1;
}
