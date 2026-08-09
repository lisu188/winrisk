#pragma once

#include "engine/GameEngine.hpp"

#include <QJsonArray>
#include <QJsonObject>
#include <QString>

#include <stdexcept>
#include <string>

namespace winrisk::test {

inline QString symbolFor(int id, int territoryCount) {
    if (id >= territoryCount) return "WILD";
    switch (id % 3) {
        case 0: return "INFANTRY";
        case 1: return "CAVALRY";
        default: return "ARTILLERY";
    }
}

inline QJsonObject card(int id, int territoryCount) {
    QJsonObject object;
    object.insert("fieldIndex", id < territoryCount ? id : -1);
    object.insert("symbol", symbolFor(id, territoryCount));
    return object;
}

inline QJsonObject mapSketch(const std::string& mapId) {
    const auto definition = GameEngine::makeBuiltinMap(mapId);
    if (!definition) throw std::runtime_error("Unknown built-in map test fixture");
    const int territoryCount = static_cast<int>(definition->territories.size());

    QJsonArray fields;
    for (const auto& territory : definition->territories) {
        QJsonObject field;
        field.insert("fieldIndex", territory.id);
        field.insert("displayName", QString::fromStdString(territory.name));
        field.insert("cardSymbol", symbolFor(territory.id, territoryCount));
        QJsonArray next;
        for (const int adjacent : territory.adjacent) next.push_back(adjacent);
        field.insert("next", next);
        fields.push_back(field);
    }

    QJsonArray continents;
    for (const auto& continent : definition->continents) {
        QJsonObject object;
        object.insert("bonus", continent.bonus);
        object.insert("color", continent.id);
        QJsonArray members;
        for (const int territoryId : continent.territories) members.push_back(territoryId);
        object.insert("fields", members);
        continents.push_back(object);
    }

    QJsonObject map;
    map.insert("fields", fields);
    map.insert("continents", continents);
    map.insert("image", QJsonArray{});
    return map;
}

inline QJsonObject basicPlayer(int index, bool human, bool neutral = false, const QString& aiClass = "com.winrisk.game.ai.EasyAI") {
    QJsonObject object;
    object.insert("colorRgb", static_cast<double>(static_cast<qint32>(0xff000000u | static_cast<unsigned>(index * 0x00111111u))));
    object.insert("neutral", neutral);
    object.insert("reinforcements", neutral ? 0 : index + 2);
    object.insert("conqueredTerritoryThisTurn", false);
    object.insert("headquartersIndex", -1);
    object.insert("interfaceClass", neutral
        ? "com.winrisk.game.ai.PlayerAI"
        : human ? "com.winrisk.game.ai.HumanPlayer" : aiClass);
    object.insert("cards", QJsonArray{});
    return object;
}

inline QJsonArray fullDrawPile(int territoryCount) {
    QJsonArray draw;
    for (int id = 0; id < territoryCount + 2; ++id) draw.push_back(card(id, territoryCount));
    return draw;
}

}