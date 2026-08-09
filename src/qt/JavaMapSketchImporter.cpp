#include "qt/JavaMapSketchImporter.hpp"

#include <QCryptographicHash>
#include <QJsonArray>
#include <QJsonDocument>

#include <algorithm>
#include <cmath>
#include <vector>

namespace winrisk::qt {
namespace {

QString expectedSymbol(int territoryId) {
    switch (territoryId % 3) {
        case 0: return "INFANTRY";
        case 1: return "CAVALRY";
        default: return "ARTILLERY";
    }
}

std::string customId(const QJsonObject& mapSketch) {
    const QByteArray raw = QJsonDocument(mapSketch).toJson(QJsonDocument::Compact);
    const QByteArray digest = QCryptographicHash::hash(raw, QCryptographicHash::Sha256).toHex().left(24);
    return "custom:" + digest.toStdString();
}

}

bool importJavaMapSketch(const QJsonObject& mapSketch, MapDefinition& map, QString& error) {
    const QJsonArray fields = mapSketch.value("fields").toArray();
    if (fields.isEmpty()) {
        error = "Java map has no fields";
        return false;
    }

    struct RawField {
        int x = 0;
        int y = 0;
        std::string name;
        std::vector<int> adjacent;
    };

    std::vector<RawField> rawFields;
    rawFields.reserve(static_cast<std::size_t>(fields.size()));
    int maxX = 0;
    int maxY = 0;

    for (qsizetype i = 0; i < fields.size(); ++i) {
        if (!fields[i].isObject()) {
            error = "Java map contains an invalid field";
            return false;
        }
        const QJsonObject field = fields[i].toObject();
        if (field.value("fieldIndex").toInt(-1) != i) {
            error = "Java map field indices are not sequential";
            return false;
        }
        const QString displayName = field.value("displayName").toString();
        if (displayName.isEmpty()) {
            error = "Java map contains a field without a name";
            return false;
        }
        if (field.value("cardSymbol").toString() != expectedSymbol(static_cast<int>(i))) {
            error = "Java custom map uses authored card symbols that the native card catalog cannot represent yet";
            return false;
        }

        const int x = field.value("x").toInt(-1);
        const int y = field.value("y").toInt(-1);
        if (x < 0 || y < 0) {
            error = "Java map contains invalid field coordinates";
            return false;
        }

        RawField raw;
        raw.x = x;
        raw.y = y;
        raw.name = displayName.toStdString();
        std::vector<bool> seen(static_cast<std::size_t>(fields.size()), false);
        for (const auto value : field.value("next").toArray()) {
            const int next = value.toInt(-1);
            if (next < 0 || next >= fields.size() || next == i || seen[static_cast<std::size_t>(next)]) {
                error = "Java map contains invalid or duplicate adjacency";
                return false;
            }
            seen[static_cast<std::size_t>(next)] = true;
            raw.adjacent.push_back(next);
        }
        maxX = std::max(maxX, x);
        maxY = std::max(maxY, y);
        rawFields.push_back(std::move(raw));
    }

    for (std::size_t i = 0; i < rawFields.size(); ++i) {
        for (const int next : rawFields[i].adjacent) {
            const auto& reverse = rawFields[static_cast<std::size_t>(next)].adjacent;
            if (std::find(reverse.begin(), reverse.end(), static_cast<int>(i)) == reverse.end()) {
                error = "Java map adjacency is not bidirectional";
                return false;
            }
        }
    }

    const int canvasWidth = std::max(900, maxX + 35);
    const int canvasHeight = std::max(600, maxY + 35);

    map = {};
    map.id = customId(mapSketch);
    map.displayName = "Imported Java map";
    map.territories.reserve(rawFields.size());
    for (std::size_t i = 0; i < rawFields.size(); ++i) {
        Territory territory;
        territory.id = static_cast<int>(i);
        territory.name = rawFields[i].name;
        territory.x = static_cast<float>(rawFields[i].x) / static_cast<float>(canvasWidth);
        territory.y = static_cast<float>(rawFields[i].y) / static_cast<float>(canvasHeight);
        territory.adjacent = rawFields[i].adjacent;
        map.territories.push_back(std::move(territory));
    }

    const QJsonArray continents = mapSketch.value("continents").toArray();
    std::vector<int> membership(map.territories.size(), -1);
    map.continents.reserve(static_cast<std::size_t>(continents.size()));
    for (qsizetype i = 0; i < continents.size(); ++i) {
        if (!continents[i].isObject()) {
            error = "Java map contains an invalid continent";
            return false;
        }
        const QJsonObject object = continents[i].toObject();
        const int bonus = object.value("bonus").toInt(-1);
        if (bonus < 0) {
            error = "Java map contains an invalid continent bonus";
            return false;
        }
        const QJsonArray members = object.value("fields").toArray();
        if (members.isEmpty()) {
            error = "Java map contains an empty continent";
            return false;
        }

        Continent continent;
        continent.id = static_cast<int>(i);
        continent.name = "Region " + std::to_string(i + 1);
        continent.bonus = bonus;
        for (const auto value : members) {
            const int territoryId = value.toInt(-1);
            if (territoryId < 0 || territoryId >= static_cast<int>(map.territories.size())
                || membership[static_cast<std::size_t>(territoryId)] >= 0) {
                error = "Java map continent membership is invalid";
                return false;
            }
            membership[static_cast<std::size_t>(territoryId)] = continent.id;
            continent.territories.push_back(territoryId);
        }
        map.continents.push_back(std::move(continent));
    }

    for (std::size_t i = 0; i < map.territories.size(); ++i) {
        map.territories[i].continent = membership[i];
    }
    return true;
}

}
