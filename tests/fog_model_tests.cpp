#include "qt/AppController.hpp"

#include <QCoreApplication>
#include <iostream>

namespace {

int failures = 0;

void check(bool condition, const char* expression, int line) {
    if (!condition) {
        std::cerr << "FAIL line " << line << ": " << expression << '\n';
        ++failures;
    }
}

#define CHECK(expression) check(static_cast<bool>(expression), #expression, __LINE__)

bool adjacentToOwner(const winrisk::GameEngine& engine, int territoryId, int ownerId) {
    const auto& territory = engine.territories()[static_cast<std::size_t>(territoryId)];
    for (const int adjacentId : territory.adjacent) {
        if (engine.territories()[static_cast<std::size_t>(adjacentId)].owner == ownerId) {
            return true;
        }
    }
    return false;
}

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);

    winrisk::RulesOptions rules;
    rules.fogOfWar = true;

    winrisk::GameEngine engine;
    CHECK(engine.startNewGame(4, 1, 123456, winrisk::GameMode::Classic, rules));

    BoardModel model;
    model.setEngine(&engine);
    model.setViewerPlayerId(0);

    int owned = -1;
    int adjacentEnemy = -1;
    int hiddenEnemy = -1;

    for (const auto& territory : engine.territories()) {
        if (territory.owner == 0 && owned < 0) {
            owned = territory.id;
        }
        if (territory.owner != 0 && adjacentToOwner(engine, territory.id, 0) && adjacentEnemy < 0) {
            adjacentEnemy = territory.id;
        }
        if (territory.owner != 0 && !adjacentToOwner(engine, territory.id, 0) && hiddenEnemy < 0) {
            hiddenEnemy = territory.id;
        }
    }

    CHECK(owned >= 0);
    CHECK(adjacentEnemy >= 0);
    CHECK(hiddenEnemy >= 0);

    if (owned >= 0) {
        const QModelIndex index = model.index(owned, 0);
        CHECK(model.data(index, BoardModel::FieldVisibleRole).toBool());
        CHECK(model.data(index, BoardModel::OwnerIdRole).toInt() == 0);
        CHECK(model.data(index, BoardModel::ArmiesRole).toInt() == engine.territories()[static_cast<std::size_t>(owned)].armies);
    }

    if (adjacentEnemy >= 0) {
        const QModelIndex index = model.index(adjacentEnemy, 0);
        CHECK(model.data(index, BoardModel::FieldVisibleRole).toBool());
        CHECK(model.data(index, BoardModel::OwnerIdRole).toInt() == engine.territories()[static_cast<std::size_t>(adjacentEnemy)].owner);
        CHECK(model.data(index, BoardModel::ArmiesRole).toInt() == engine.territories()[static_cast<std::size_t>(adjacentEnemy)].armies);
    }

    if (hiddenEnemy >= 0) {
        const QModelIndex index = model.index(hiddenEnemy, 0);
        CHECK(!model.data(index, BoardModel::FieldVisibleRole).toBool());
        CHECK(model.data(index, BoardModel::OwnerIdRole).toInt() == -1);
        CHECK(model.data(index, BoardModel::ArmiesRole).toInt() == 0);
        CHECK(model.data(index, BoardModel::HeadquartersOwnerIdRole).toInt() == -1);

        model.setViewerPlayerId(-1);
        CHECK(model.data(index, BoardModel::FieldVisibleRole).toBool());
        CHECK(model.data(index, BoardModel::OwnerIdRole).toInt() == engine.territories()[static_cast<std::size_t>(hiddenEnemy)].owner);
    }

    winrisk::GameEngine standardEngine;
    CHECK(standardEngine.startNewGame(4, 1, 123456));
    model.setEngine(&standardEngine);
    model.setViewerPlayerId(0);
    for (int row = 0; row < model.rowCount(); ++row) {
        CHECK(model.data(model.index(row, 0), BoardModel::FieldVisibleRole).toBool());
    }

    if (failures != 0) {
        std::cerr << failures << " fog model checks failed\n";
    }
    return failures == 0 ? 0 : 1;
}
