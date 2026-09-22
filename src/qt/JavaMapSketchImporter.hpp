#pragma once

#include "engine/GameEngine.hpp"

#include <QJsonObject>
#include <QString>

namespace winrisk::qt {

bool importJavaMapSketch(
    const QJsonObject& mapSketch,
    MapDefinition& map,
    QString& error
);

}
