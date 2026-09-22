#pragma once

#include "engine/GameEngine.hpp"

#include <QJsonObject>
#include <QString>

namespace winrisk::qt {

QJsonObject mapDefinitionToJson(const MapDefinition& map);
bool mapDefinitionFromJson(const QJsonObject& object, MapDefinition& map, QString& error);

}
