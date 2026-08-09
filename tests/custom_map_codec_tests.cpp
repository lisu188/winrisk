#include "qt/JavaMapSketchImporter.hpp"
#include "qt/MapDefinitionJson.hpp"

#include <QCoreApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <algorithm>
#include <iostream>
#include <string>

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
    switch (id % 3) {
        case 0: return "INFANTRY";
        case 1: return "CAVALRY";
        default: return "ARTILLERY";
    }
}

QJsonObject customSketch() {
    static const int next[][3] = {
        {1, 5, -1},
        {0, 2, -1},
        {1, 3, -1},
        {2, 4, -1},
        {3, 5, -1},
        {4, 0, -1}
    };

    QJsonArray fields;
    for (int i = 0; i < 6; ++i) {
        QJsonObject field;
        field.insert("fieldIndex", i);
        field.insert("displayName", QString("Custom %1").arg(i + 1));
        field.insert("cardSymbol", symbolFor(i));
        field.insert("x", 80 + (i % 3) * 260);
        field.insert("y", 100 + (i / 3) * 300);
        QJsonArray adjacent;
        for (const int value : next[i]) if (value >= 0) adjacent.push_back(value);
        field.insert("next", adjacent);
        fields.push_back(field);
    }

    QJsonArray continents;
    for (int c = 0; c < 2; ++c) {
        QJsonObject continent;
        continent.insert("bonus", c + 2);
        continent.insert("color", c);
        QJsonArray members;
        for (int i = 0; i < 3; ++i) members.push_back(c * 3 + i);
        continent.insert("fields", members);
        continents.push_back(continent);
    }

    QJsonObject map;
    map.insert("fields", fields);
    map.insert("continents", continents);
    map.insert("image", QJsonArray{});
    return map;
}

bool sameDefinition(const MapDefinition& a, const MapDefinition& b) {
    if (a.id != b.id || a.displayName != b.displayName
        || a.territories.size() != b.territories.size()
        || a.continents.size() != b.continents.size()) return false;
    for (std::size_t i = 0; i < a.territories.size(); ++i) {
        const auto& lhs = a.territories[i];
        const auto& rhs = b.territories[i];
        if (lhs.id != rhs.id || lhs.name != rhs.name || lhs.x != rhs.x || lhs.y != rhs.y
            || lhs.continent != rhs.continent || lhs.adjacent != rhs.adjacent) return false;
    }
    for (std::size_t i = 0; i < a.continents.size(); ++i) {
        const auto& lhs = a.continents[i];
        const auto& rhs = b.continents[i];
        if (lhs.id != rhs.id || lhs.name != rhs.name || lhs.bonus != rhs.bonus
            || lhs.territories != rhs.territories) return false;
    }
    return true;
}

Snapshot playableSnapshot(const MapDefinition& map) {
    Snapshot snapshot;
    snapshot.version = 6;
    snapshot.mapId = map.id;
    snapshot.mapDefinition = map;
    snapshot.mode = GameMode::Classic;
    snapshot.phase = Phase::Attack;
    snapshot.currentPlayer = 0;
    snapshot.turn = 3;
    snapshot.rngState = Random(444).state();
    snapshot.players.resize(3);
    for (int i = 0; i < 3; ++i) {
        snapshot.players[static_cast<std::size_t>(i)].id = i;
        snapshot.players[static_cast<std::size_t>(i)].name = i == 0 ? "Player" : "AI";
        snapshot.players[static_cast<std::size_t>(i)].ai = i != 0;
        snapshot.players[static_cast<std::size_t>(i)].aiStrategy = static_cast<AiStrategy>(i % 5);
    }
    snapshot.territories = map.territories;
    for (std::size_t i = 0; i < snapshot.territories.size(); ++i) {
        snapshot.territories[i].owner = static_cast<int>(i % 3);
        snapshot.territories[i].armies = 3;
    }
    snapshot.deck = GameEngine::makeRiskDeck(static_cast<int>(map.territories.size()));
    return snapshot;
}

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);

    MapDefinition imported;
    QString error;
    const QJsonObject source = customSketch();
    CHECK(qt::importJavaMapSketch(source, imported, error));
    CHECK(error.isEmpty());
    CHECK(imported.id.rfind("custom:", 0) == 0);
    CHECK(imported.displayName == "Imported Java map");
    CHECK(imported.territories.size() == 6);
    CHECK(imported.continents.size() == 2);
    CHECK(imported.territories[0].name == "Custom 1");
    CHECK(imported.territories[0].continent == 0);
    CHECK(imported.territories[5].continent == 1);
    CHECK(imported.continents[0].bonus == 2);
    CHECK(imported.continents[1].bonus == 3);
    CHECK(imported.territories[0].x > 0.0f && imported.territories[0].x < 1.0f);
    CHECK(imported.territories[0].y > 0.0f && imported.territories[0].y < 1.0f);

    MapDefinition importedAgain;
    error.clear();
    CHECK(qt::importJavaMapSketch(source, importedAgain, error));
    CHECK(sameDefinition(imported, importedAgain));

    const QJsonObject encoded = qt::mapDefinitionToJson(imported);
    MapDefinition decoded;
    error.clear();
    CHECK(qt::mapDefinitionFromJson(encoded, decoded, error));
    CHECK(error.isEmpty());
    CHECK(sameDefinition(imported, decoded));

    Snapshot snapshot = playableSnapshot(imported);
    GameEngine engine;
    CHECK(engine.restore(snapshot));
    CHECK(engine.mapId() == imported.id);
    CHECK(engine.mapDisplayName() == imported.displayName);
    CHECK(engine.territories().size() == imported.territories.size());
    const Snapshot roundTrip = engine.snapshot();
    CHECK(roundTrip.mapDefinition.has_value());
    if (roundTrip.mapDefinition) CHECK(sameDefinition(imported, *roundTrip.mapDefinition));

    QJsonObject badEdge = source;
    QJsonArray badEdgeFields = badEdge.value("fields").toArray();
    QJsonObject first = badEdgeFields[0].toObject();
    QJsonArray oneWay = first.value("next").toArray();
    oneWay.push_back(2);
    first.insert("next", oneWay);
    badEdgeFields[0] = first;
    badEdge.insert("fields", badEdgeFields);
    MapDefinition rejected;
    error.clear();
    CHECK(!qt::importJavaMapSketch(badEdge, rejected, error));
    CHECK(error.contains("bidirectional", Qt::CaseInsensitive));

    QJsonObject duplicateMembership = source;
    QJsonArray duplicateContinents = duplicateMembership.value("continents").toArray();
    QJsonObject second = duplicateContinents[1].toObject();
    QJsonArray secondMembers = second.value("fields").toArray();
    secondMembers.push_back(0);
    second.insert("fields", secondMembers);
    duplicateContinents[1] = second;
    duplicateMembership.insert("continents", duplicateContinents);
    error.clear();
    CHECK(!qt::importJavaMapSketch(duplicateMembership, rejected, error));
    CHECK(error.contains("membership", Qt::CaseInsensitive));

    QJsonObject authoredSymbol = source;
    QJsonArray authoredFields = authoredSymbol.value("fields").toArray();
    QJsonObject authoredFirst = authoredFields[0].toObject();
    authoredFirst.insert("cardSymbol", "ARTILLERY");
    authoredFields[0] = authoredFirst;
    authoredSymbol.insert("fields", authoredFields);
    error.clear();
    CHECK(!qt::importJavaMapSketch(authoredSymbol, rejected, error));
    CHECK(error.contains("card symbols", Qt::CaseInsensitive));

    Snapshot malformed = playableSnapshot(imported);
    malformed.mapDefinition->territories[0].adjacent.push_back(0);
    GameEngine malformedEngine;
    CHECK(!malformedEngine.restore(malformed));

    if (failures != 0) std::cerr << failures << " custom map codec checks failed\n";
    return failures == 0 ? 0 : 1;
}
