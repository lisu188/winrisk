#include "qt/MapDefinitionJson.hpp"

#include <QJsonArray>

namespace winrisk::qt {

QJsonObject mapDefinitionToJson(const MapDefinition& map) {
    QJsonObject root;
    root.insert("id", QString::fromStdString(map.id));
    root.insert("displayName", QString::fromStdString(map.displayName));

    QJsonArray territories;
    for (const auto& territory : map.territories) {
        QJsonObject object;
        object.insert("id", territory.id);
        object.insert("name", QString::fromStdString(territory.name));
        object.insert("x", territory.x);
        object.insert("y", territory.y);
        object.insert("continent", territory.continent);
        QJsonArray adjacent;
        for (const int next : territory.adjacent) adjacent.push_back(next);
        object.insert("adjacent", adjacent);
        territories.push_back(object);
    }
    root.insert("territories", territories);

    QJsonArray continents;
    for (const auto& continent : map.continents) {
        QJsonObject object;
        object.insert("id", continent.id);
        object.insert("name", QString::fromStdString(continent.name));
        object.insert("bonus", continent.bonus);
        QJsonArray members;
        for (const int territoryId : continent.territories) members.push_back(territoryId);
        object.insert("territories", members);
        continents.push_back(object);
    }
    root.insert("continents", continents);
    return root;
}

bool mapDefinitionFromJson(const QJsonObject& root, MapDefinition& map, QString& error) {
    map = {};
    map.id = root.value("id").toString().toStdString();
    map.displayName = root.value("displayName").toString().toStdString();
    if (map.id.empty()) {
        error = "Embedded map is missing an id";
        return false;
    }
    if (map.displayName.empty()) map.displayName = map.id;

    const QJsonArray territories = root.value("territories").toArray();
    if (territories.isEmpty()) {
        error = "Embedded map has no territories";
        return false;
    }
    map.territories.reserve(static_cast<std::size_t>(territories.size()));
    for (const auto value : territories) {
        if (!value.isObject()) {
            error = "Embedded map territory is invalid";
            return false;
        }
        const QJsonObject object = value.toObject();
        Territory territory;
        territory.id = object.value("id").toInt(-1);
        territory.name = object.value("name").toString().toStdString();
        territory.x = static_cast<float>(object.value("x").toDouble(-1.0));
        territory.y = static_cast<float>(object.value("y").toDouble(-1.0));
        territory.continent = object.value("continent").toInt(-1);
        for (const auto next : object.value("adjacent").toArray()) territory.adjacent.push_back(next.toInt(-1));
        map.territories.push_back(std::move(territory));
    }

    const QJsonArray continents = root.value("continents").toArray();
    map.continents.reserve(static_cast<std::size_t>(continents.size()));
    for (const auto value : continents) {
        if (!value.isObject()) {
            error = "Embedded map continent is invalid";
            return false;
        }
        const QJsonObject object = value.toObject();
        Continent continent;
        continent.id = object.value("id").toInt(-1);
        continent.name = object.value("name").toString().toStdString();
        continent.bonus = object.value("bonus").toInt(-1);
        for (const auto member : object.value("territories").toArray()) continent.territories.push_back(member.toInt(-1));
        map.continents.push_back(std::move(continent));
    }
    return true;
}

}
