#include "qt/LegacyJavaImporter.hpp"

#include <QCoreApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <array>
#include <iostream>
#include <set>

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

QString symbolFor(int id) {
    if (id >= 42) return "WILD";
    switch (id % 3) {
        case 0: return "INFANTRY";
        case 1: return "CAVALRY";
        default: return "ARTILLERY";
    }
}

QJsonObject card(int id) {
    QJsonObject object;
    object.insert("fieldIndex", id < 42 ? id : -1);
    object.insert("symbol", symbolFor(id));
    return object;
}

QJsonObject mission(const QString& kind, int a = 0, int b = 0) {
    QJsonObject object;
    object.insert("kind", kind);
    object.insert("territories", kind == "TERRITORY" || kind == "FORTIFIED_TERRITORY" ? a : 0);
    object.insert("minimumArmies", kind == "FORTIFIED_TERRITORY" ? b : 0);
    object.insert("continentCount", kind == "CONTINENTS" ? a : 0);
    object.insert("eliminationTargetIndex", kind == "ELIMINATION" ? a : -1);
    return object;
}

QJsonArray worldMapFields() {
    QJsonArray fields;
    const auto world = GameEngine::makeWorldTerritories();
    for (int i = 0; i < static_cast<int>(world.size()); ++i) {
        QJsonObject field;
        field.insert("fieldIndex", i);
        field.insert("displayName", QString::fromStdString(world[static_cast<std::size_t>(i)].name));
        field.insert("cardSymbol", symbolFor(i));
        fields.push_back(field);
    }
    return fields;
}

QJsonObject baseGame(const QString& mode) {
    QJsonObject root;
    QJsonObject map;
    map.insert("fields", worldMapFields());
    root.insert("map", map);

    QJsonObject params;
    params.insert("gameMode", mode);
    params.insert("fogOfWar", true);
    params.insert("attackWithAll", true);
    params.insert("skynetMode", true);
    params.insert("incrementalCardSetValues", true);
    params.insert("expandedManeuver", true);
    params.insert("attackCardReroll", true);
    params.insert("commanderDie", true);
    params.insert("humanPlayers", 1);
    params.insert("aiPlayers", 3);
    params.insert("randomSeed", 123456789);
    params.insert("builtinMap", "world.map");
    root.insert("params", params);

    QJsonArray owners;
    QJsonArray armies;
    for (int i = 0; i < 42; ++i) {
        owners.push_back(i % 4);
        armies.push_back(2 + (i % 3));
    }
    root.insert("fieldOwner", owners);
    root.insert("fieldArmy", armies);
    root.insert("curPlayer", 0);
    root.insert("phase", "ATTACK");
    root.insert("commanderDieUsed", true);
    root.insert("maneuverUsed", false);
    root.insert("neutralPlayerIndex", -1);
    root.insert("tradeCount", 7);
    return root;
}

QJsonObject player(int index, bool human) {
    QJsonObject object;
    object.insert("colorRgb", static_cast<double>(0xff000000u | static_cast<unsigned>(index * 0x00111111u)));
    object.insert("neutral", false);
    object.insert("reinforcements", index + 2);
    object.insert("conqueredTerritoryThisTurn", index == 0);
    object.insert("headquartersIndex", -1);
    object.insert("interfaceClass", human
        ? "com.winrisk.game.ai.HumanPlayer"
        : "com.winrisk.game.ai.EasyAI");
    object.insert("cards", QJsonArray{});
    return object;
}

void fillAllCards(QJsonObject& root, const std::set<int>& excluded, bool splitZones) {
    QJsonArray players = root.value("players").toArray();
    std::set<int> assigned = excluded;

    if (splitZones) {
        QJsonArray hand0;
        hand0.push_back(card(0));
        hand0.push_back(card(42));
        QJsonObject p0 = players[0].toObject();
        p0.insert("cards", hand0);
        players[0] = p0;
        assigned.insert(0);
        assigned.insert(42);
    }
    root.insert("players", players);

    QJsonArray discard;
    if (splitZones && !assigned.contains(1)) {
        discard.push_back(card(1));
        assigned.insert(1);
    }
    root.insert("discardPile", discard);

    QJsonArray draw;
    for (int id = 0; id < 44; ++id) {
        if (!assigned.contains(id)) draw.push_back(card(id));
    }
    root.insert("drawPile", draw);
}

bool import(const QJsonObject& root, Snapshot& snapshot, QString& error) {
    const QByteArray raw = QJsonDocument(root).toJson(QJsonDocument::Compact);
    return winrisk::qt::importJavaGameSketch(root, raw, snapshot, error);
}

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);

    QJsonObject classic = baseGame("CLASSIC");
    QJsonArray classicPlayers;
    for (int i = 0; i < 4; ++i) classicPlayers.push_back(player(i, i == 0));
    classic.insert("players", classicPlayers);
    fillAllCards(classic, {}, true);

    Snapshot classicSnapshot;
    QString error;
    CHECK(import(classic, classicSnapshot, error));
    CHECK(error.isEmpty());
    CHECK(classicSnapshot.version == 6);
    CHECK(classicSnapshot.mode == GameMode::Classic);
    CHECK(classicSnapshot.rules.incrementalCardSetValues);
    CHECK(classicSnapshot.rules.expandedManeuver);
    CHECK(classicSnapshot.rules.attackCardReroll);
    CHECK(classicSnapshot.rules.commanderDie);
    CHECK(classicSnapshot.rules.attackWithAll);
    CHECK(classicSnapshot.rules.fogOfWar);
    CHECK(classicSnapshot.rules.skynet);
    CHECK(classicSnapshot.commanderDieUsed);
    CHECK(classicSnapshot.tradeCount == 7);
    CHECK(classicSnapshot.conqueredThisTurn);
    CHECK(classicSnapshot.players[0].cards.size() == 2);
    CHECK(classicSnapshot.players[0].cards[0] == 0);
    CHECK(classicSnapshot.players[0].cards[1] == 42);
    CHECK(classicSnapshot.discard.size() == 1 && classicSnapshot.discard[0].id == 1);
    CHECK(!classicSnapshot.deck.empty());
    CHECK(classicSnapshot.deck.back().id == 2);
    GameEngine classicEngine;
    CHECK(classicEngine.restore(classicSnapshot));
    CHECK(classicEngine.rules().skynet);

    QJsonObject secret = baseGame("SECRET_MISSION");
    QJsonArray secretPlayers;
    for (int i = 0; i < 4; ++i) {
        QJsonObject p = player(i, i == 0);
        if (i == 0) p.insert("mission", mission("TERRITORY", 24));
        if (i == 1) p.insert("mission", mission("FORTIFIED_TERRITORY", 18, 2));
        if (i == 2) p.insert("mission", mission("CONTINENTS", 2));
        if (i == 3) p.insert("mission", mission("ELIMINATION", 0));
        secretPlayers.push_back(p);
    }
    secret.insert("players", secretPlayers);
    fillAllCards(secret, {}, false);

    Snapshot secretSnapshot;
    error.clear();
    CHECK(import(secret, secretSnapshot, error));
    CHECK(secretSnapshot.mode == GameMode::SecretMission);
    CHECK(secretSnapshot.players[0].mission.has_value());
    CHECK(secretSnapshot.players[0].mission->kind == MissionKind::Territory);
    CHECK(secretSnapshot.players[0].mission->territories == 24);
    CHECK(secretSnapshot.players[3].mission.has_value());
    CHECK(secretSnapshot.players[3].mission->kind == MissionKind::Elimination);
    CHECK(secretSnapshot.players[3].mission->eliminationTarget == 0);
    GameEngine secretEngine;
    CHECK(secretEngine.restore(secretSnapshot));
    CHECK(secretEngine.mode() == GameMode::SecretMission);

    QJsonObject capital = baseGame("CAPITAL");
    QJsonArray capitalPlayers;
    std::set<int> headquarters;
    for (int i = 0; i < 4; ++i) {
        QJsonObject p = player(i, i == 0);
        p.insert("headquartersIndex", i);
        capitalPlayers.push_back(p);
        headquarters.insert(i);
    }
    capital.insert("players", capitalPlayers);
    fillAllCards(capital, headquarters, false);

    Snapshot capitalSnapshot;
    error.clear();
    CHECK(import(capital, capitalSnapshot, error));
    CHECK(capitalSnapshot.mode == GameMode::Capital);
    CHECK(capitalSnapshot.deck.size() == 40);
    for (int i = 0; i < 4; ++i) CHECK(capitalSnapshot.players[static_cast<std::size_t>(i)].headquarters == i);
    GameEngine capitalEngine;
    CHECK(capitalEngine.restore(capitalSnapshot));
    CHECK(capitalEngine.mode() == GameMode::Capital);

    if (failures != 0) std::cerr << failures << " Java import checks failed\n";
    return failures == 0 ? 0 : 1;
}
