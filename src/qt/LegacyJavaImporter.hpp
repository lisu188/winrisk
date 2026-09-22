#pragma once

#include "engine/GameEngine.hpp"

#include <QByteArray>
#include <QJsonObject>
#include <QString>

namespace winrisk::qt {

bool importJavaGameSketch(
    const QJsonObject& root,
    const QByteArray& raw,
    Snapshot& snapshot,
    QString& error
);

}
