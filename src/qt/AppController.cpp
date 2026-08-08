#include "qt/AppController.hpp"

#include <QCryptographicHash>
#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QRandomGenerator>
#include <QStandardPaths>
#include <QStringList>

namespace {

QString diceText(const std::vector<int>& dice) {
    QStringList values;
    for (const int value : dice) {
        values.push_back(QString::number(value));
    }
    return values.join(',');
}

QJsonObject snapshotToJson(const winrisk::Snapshot& snapshot) {
    QJsonObject root;
    root.insert("schema", "winrisk-save");
    root.insert("version", snapshot.version);
    root.insert("phase", static_cast<int>(snapshot.phase));
    root.insert("currentPlayer", snapshot.currentPlayer);
    root.insert("winner", snapshot.winner);
    root.insert("turn", QString::number(snapshot.turn));

    QJsonArray rng;
    for (const auto value : snapshot.rngState) {
        rng.push_back(QString::number(value, 16));
    }
    root.insert("rng", rng);

    QJsonArray players;
    for (const auto& player : snapshot.players) {
        QJsonObject object;
        object.insert("id", player.id);
        object.insert("name", QString::fromStdString(player.name));
        object.insert("color", QString::number(player.color, 16));
        object.insert("ai", player.ai);
        object.insert("eliminated", player.eliminated);
        object.insert("reinforcements", player.reinforcements);
        players.push_back(object);
    }
    root.insert("players", players);

    QJsonArray territories;
    for (const auto& territory : snapshot.territories) {
        QJsonObject object;
        object.insert("id", territory.id);
        object.insert("owner", territory.owner);
        object.insert("armies", territory.armies);
        territories.push_back(object);
    }
    root.insert("territories", territories);
    root.insert("mapId", "world");
    return root;
}

bool jsonToSnapshotV2(const QJsonObject& root, winrisk::Snapshot& snapshot, QString& error) {
    if (root.value("version").toInt() != 2 || root.value("mapId").toString("world") != "world") {
        error = "Unsupported WinRisk save version or map";
        return false;
    }
    snapshot = {};
    snapshot.version = 2;
    snapshot.phase = static_cast<winrisk::Phase>(root.value("phase").toInt());
    snapshot.currentPlayer = root.value("currentPlayer").toInt();
    snapshot.winner = root.value("winner").toInt(-1);
    snapshot.turn = root.value("turn").toString("1").toULongLong();

    const QJsonArray rng = root.value("rng").toArray();
    if (rng.size() != 4) {
        error = "Invalid RNG state";
        return false;
    }
    for (qsizetype i = 0; i < 4; ++i) {
        bool ok = false;
        snapshot.rngState[static_cast<std::size_t>(i)] = rng[i].toString().toULongLong(&ok, 16);
        if (!ok) {
            error = "Invalid RNG state";
            return false;
        }
    }

    const QJsonArray players = root.value("players").toArray();
    for (const auto value : players) {
        const QJsonObject object = value.toObject();
        winrisk::Player player;
        player.id = object.value("id").toInt();
        player.name = object.value("name").toString().toStdString();
        bool colorOk = false;
        player.color = object.value("color").toString().toUInt(&colorOk, 16);
        if (!colorOk) {
            error = "Invalid player color";
            return false;
        }
        player.ai = object.value("ai").toBool();
        player.eliminated = object.value("eliminated").toBool();
        player.reinforcements = object.value("reinforcements").toInt();
        snapshot.players.push_back(std::move(player));
    }

    snapshot.territories = winrisk::GameEngine::makeWorldTerritories();
    const QJsonArray territories = root.value("territories").toArray();
    if (territories.size() != static_cast<qsizetype>(snapshot.territories.size())) {
        error = "Save does not contain the standard 42-territory world map";
        return false;
    }
    for (const auto value : territories) {
        const QJsonObject object = value.toObject();
        const int id = object.value("id").toInt(-1);
        if (id < 0 || id >= static_cast<int>(snapshot.territories.size())) {
            error = "Invalid territory id";
            return false;
        }
        auto& territory = snapshot.territories[static_cast<std::size_t>(id)];
        territory.owner = object.value("owner").toInt(-1);
        territory.armies = object.value("armies").toInt();
    }
    return true;
}

bool legacyJavaJsonToSnapshot(const QJsonObject& root, const QByteArray& raw, winrisk::Snapshot& snapshot, QString& error) {
    if (!root.contains("fieldOwner") || !root.contains("fieldArmy") || !root.contains("players")) {
        error = "Unsupported save format";
        return false;
    }
    const QString mode = root.value("params").toObject().value("gameMode").toString("CLASSIC");
    if (mode != "CLASSIC") {
        error = "Legacy CAPITAL and SECRET_MISSION saves require the remaining rules migration";
        return false;
    }

    const QJsonArray oldPlayers = root.value("players").toArray();
    std::vector<int> oldToNew(static_cast<std::size_t>(oldPlayers.size()), -1);
    snapshot = {};
    snapshot.version = 2;
    for (qsizetype i = 0; i < oldPlayers.size(); ++i) {
        const QJsonObject object = oldPlayers[i].toObject();
        if (object.value("neutral").toBool()) {
            error = "Legacy two-player neutral-army saves require the two-player rules migration";
            return false;
        }
        winrisk::Player player;
        player.id = static_cast<int>(snapshot.players.size());
        player.name = "Player " + std::to_string(player.id + 1);
        const qint64 signedColor = static_cast<qint64>(object.value("colorRgb").toDouble());
        player.color = static_cast<std::uint32_t>(signedColor);
        const QString interfaceClass = object.value("interfaceClass").toString();
        player.ai = !interfaceClass.contains("Human", Qt::CaseInsensitive);
        if (player.ai) {
            player.name = "AI " + std::to_string(player.id + 1);
        }
        player.reinforcements = object.value("reinforcements").toInt();
        oldToNew[static_cast<std::size_t>(i)] = player.id;
        snapshot.players.push_back(std::move(player));
    }
    if (snapshot.players.size() < 2 || snapshot.players.size() > 5) {
        error = "Unsupported legacy player count";
        return false;
    }

    snapshot.territories = winrisk::GameEngine::makeWorldTerritories();
    const QJsonArray owners = root.value("fieldOwner").toArray();
    const QJsonArray armies = root.value("fieldArmy").toArray();
    if (owners.size() != 42 || armies.size() != 42) {
        error = "Only the standard world map can currently be imported from Java saves";
        return false;
    }
    for (qsizetype i = 0; i < 42; ++i) {
        const int oldOwner = owners[i].toInt(-1);
        if (oldOwner < 0 || oldOwner >= static_cast<int>(oldToNew.size()) || oldToNew[static_cast<std::size_t>(oldOwner)] < 0) {
            error = "Invalid legacy territory owner";
            return false;
        }
        snapshot.territories[static_cast<std::size_t>(i)].owner = oldToNew[static_cast<std::size_t>(oldOwner)];
        snapshot.territories[static_cast<std::size_t>(i)].armies = armies[i].toInt();
    }

    const QString phase = root.value("phase").toString();
    if (phase == "ATTACK") snapshot.phase = winrisk::Phase::Attack;
    else if (phase == "MOVE") snapshot.phase = winrisk::Phase::Maneuver;
    else snapshot.phase = winrisk::Phase::Reinforce;

    const int oldCurrent = root.value("curPlayer").toInt();
    if (oldCurrent < 0 || oldCurrent >= static_cast<int>(oldToNew.size())) {
        error = "Invalid legacy current player";
        return false;
    }
    snapshot.currentPlayer = oldToNew[static_cast<std::size_t>(oldCurrent)];
    snapshot.winner = -1;
    snapshot.turn = 1;

    const QByteArray digest = QCryptographicHash::hash(raw, QCryptographicHash::Sha256);
    std::uint64_t seed = 0;
    for (int i = 0; i < 8; ++i) {
        seed = (seed << 8) | static_cast<unsigned char>(digest[i]);
    }
    winrisk::Random importedRandom(seed);
    snapshot.rngState = importedRandom.state();
    return true;
}

}

BoardModel::BoardModel(QObject* parent)
    : QAbstractListModel(parent) {
}

int BoardModel::rowCount(const QModelIndex& parent) const {
    if (parent.isValid() || engine_ == nullptr) {
        return 0;
    }
    return static_cast<int>(engine_->territories().size());
}

QVariant BoardModel::data(const QModelIndex& index, int role) const {
    if (engine_ == nullptr || !index.isValid() || index.row() < 0 || index.row() >= rowCount()) {
        return {};
    }
    const auto& territory = engine_->territories()[static_cast<std::size_t>(index.row())];
    switch (role) {
        case TerritoryIdRole: return territory.id;
        case TerritoryNameRole: return QString::fromStdString(territory.name);
        case XRole: return territory.x;
        case YRole: return territory.y;
        case ArmiesRole: return territory.armies;
        case OwnerIdRole: return territory.owner;
        case SelectedRole: return territory.id == selectedId_;
        case OwnerColorRole:
            if (territory.owner >= 0 && territory.owner < static_cast<int>(engine_->players().size())) {
                return QColor::fromRgba(engine_->players()[static_cast<std::size_t>(territory.owner)].color);
            }
            return QColor("#70757a");
        default: return {};
    }
}

QHash<int, QByteArray> BoardModel::roleNames() const {
    return {
        {TerritoryIdRole, "territoryId"},
        {TerritoryNameRole, "territoryName"},
        {XRole, "nx"},
        {YRole, "ny"},
        {ArmiesRole, "armies"},
        {OwnerIdRole, "ownerId"},
        {OwnerColorRole, "ownerColor"},
        {SelectedRole, "selected"}
    };
}

void BoardModel::setEngine(const winrisk::GameEngine* engine) {
    beginResetModel();
    engine_ = engine;
    endResetModel();
}

void BoardModel::setSelectedId(int selectedId) {
    if (selectedId_ == selectedId) {
        return;
    }
    selectedId_ = selectedId;
    refresh();
}

void BoardModel::refresh() {
    if (rowCount() > 0) {
        emit dataChanged(index(0, 0), index(rowCount() - 1, 0));
    }
}

AppController::AppController(QObject* parent)
    : QObject(parent), boardModel_(this) {
    boardModel_.setEngine(&engine_);
    aiTimer_.setSingleShot(true);
    aiTimer_.setInterval(140);
    connect(&aiTimer_, &QTimer::timeout, this, &AppController::runAiStep);
}

bool AppController::running() const {
    return !engine_.players().empty();
}

QString AppController::phaseText() const {
    return QString::fromStdString(winrisk::phaseName(engine_.phase()));
}

QString AppController::currentPlayerText() const {
    const auto* player = engine_.currentPlayer();
    return player == nullptr ? QString() : QString::fromStdString(player->name);
}

QColor AppController::currentPlayerColor() const {
    const auto* player = engine_.currentPlayer();
    return player == nullptr ? QColor("#808080") : QColor::fromRgba(player->color);
}

int AppController::reinforcements() const {
    const auto* player = engine_.currentPlayer();
    return player == nullptr ? 0 : player->reinforcements;
}

qulonglong AppController::turn() const {
    return engine_.turn();
}

QString AppController::status() const {
    return status_;
}

QString AppController::winnerText() const {
    const int winner = engine_.winnerId();
    if (winner < 0 || winner >= static_cast<int>(engine_.players().size())) {
        return {};
    }
    return QString::fromStdString(engine_.players()[static_cast<std::size_t>(winner)].name) + " wins";
}

BoardModel* AppController::boardModel() {
    return &boardModel_;
}

bool AppController::startNewGame(int playerCount, int humanPlayers) {
    aiTimer_.stop();
    const auto seed = QRandomGenerator::global()->generate64();
    if (!engine_.startNewGame(playerCount, humanPlayers, seed)) {
        status_ = "Invalid player configuration";
        refresh();
        return false;
    }
    setSelected(-1);
    status_ = "Game started";
    refresh();
    scheduleAi();
    return true;
}

void AppController::territoryTapped(int territoryId) {
    const auto* player = engine_.currentPlayer();
    if (player == nullptr || player->ai || engine_.phase() == winrisk::Phase::Finished) {
        return;
    }
    if (territoryId < 0 || territoryId >= static_cast<int>(engine_.territories().size())) {
        return;
    }
    const auto& clicked = engine_.territories()[static_cast<std::size_t>(territoryId)];
    if (engine_.phase() == winrisk::Phase::Reinforce) {
        if (engine_.reinforce(territoryId)) {
            status_ = QString("Reinforced %1").arg(QString::fromStdString(clicked.name));
        } else {
            status_ = "Select one of your territories";
        }
        refresh();
        return;
    }
    if (engine_.phase() == winrisk::Phase::Attack) {
        if (selectedId_ < 0 || clicked.owner == engine_.currentPlayerId()) {
            if (clicked.owner == engine_.currentPlayerId() && clicked.armies >= 2) {
                setSelected(territoryId);
                status_ = QString("Attack from %1").arg(QString::fromStdString(clicked.name));
            } else {
                status_ = "Select one of your territories with at least 2 armies";
            }
            refresh();
            return;
        }
        const auto result = engine_.attack(selectedId_, territoryId);
        if (!result.legal) {
            status_ = "That territory cannot be attacked from the selected source";
        } else {
            status_ = QString("Attack %1 vs %2 — losses %3/%4%5")
                    .arg(diceText(result.attackDice), diceText(result.defenseDice))
                    .arg(result.attackerLosses)
                    .arg(result.defenderLosses)
                    .arg(result.captured ? " — captured" : "");
            if (engine_.territories()[static_cast<std::size_t>(selectedId_)].armies < 2) {
                setSelected(-1);
            }
        }
        refresh();
        return;
    }
    if (engine_.phase() == winrisk::Phase::Maneuver) {
        if (selectedId_ < 0) {
            if (clicked.owner == engine_.currentPlayerId() && clicked.armies >= 2) {
                setSelected(territoryId);
                status_ = QString("Move from %1").arg(QString::fromStdString(clicked.name));
            } else {
                status_ = "Select one of your territories with at least 2 armies";
            }
            refresh();
            return;
        }
        if (territoryId == selectedId_) {
            setSelected(-1);
            status_ = "Selection cleared";
        } else if (engine_.maneuver(selectedId_, territoryId, 1)) {
            status_ = QString("Moved 1 army to %1").arg(QString::fromStdString(clicked.name));
        } else if (clicked.owner == engine_.currentPlayerId() && clicked.armies >= 2) {
            setSelected(territoryId);
            status_ = QString("Move from %1").arg(QString::fromStdString(clicked.name));
        } else {
            status_ = "Destination must be connected through your territories";
        }
        refresh();
    }
}

bool AppController::endPhase() {
    if (!engine_.endPhase()) {
        status_ = engine_.phase() == winrisk::Phase::Reinforce ? "Place all reinforcements first" : "Cannot advance phase";
        refresh();
        return false;
    }
    setSelected(-1);
    status_ = QString("Phase: %1").arg(phaseText());
    refresh();
    scheduleAi();
    return true;
}

bool AppController::quickSave() {
    if (!running()) {
        status_ = "No game to save";
        refresh();
        return false;
    }
    const bool ok = saveSnapshot(engine_.snapshot(), quickSavePath());
    status_ = ok ? "Game saved" : "Could not save game";
    refresh();
    return ok;
}

bool AppController::quickLoad() {
    winrisk::Snapshot snapshot;
    QString error;
    if (!loadSnapshot(quickSavePath(), snapshot, error) || !engine_.restore(snapshot)) {
        status_ = error.isEmpty() ? "Could not restore save" : error;
        refresh();
        return false;
    }
    setSelected(-1);
    status_ = "Game loaded";
    refresh();
    scheduleAi();
    return true;
}

void AppController::returnToMenu() {
    aiTimer_.stop();
    engine_ = winrisk::GameEngine();
    setSelected(-1);
    status_.clear();
    boardModel_.setEngine(&engine_);
    emit stateChanged();
}

void AppController::refresh() {
    boardModel_.refresh();
    emit stateChanged();
}

void AppController::setSelected(int territoryId) {
    selectedId_ = territoryId;
    boardModel_.setSelectedId(territoryId);
}

void AppController::scheduleAi() {
    const auto* player = engine_.currentPlayer();
    if (engine_.running() && player != nullptr && player->ai && !aiTimer_.isActive()) {
        aiTimer_.start();
    }
}

void AppController::runAiStep() {
    const auto* player = engine_.currentPlayer();
    if (!engine_.running() || player == nullptr || !player->ai) {
        return;
    }
    engine_.aiStep();
    status_ = QString("%1 — %2").arg(currentPlayerText(), phaseText());
    refresh();
    scheduleAi();
}

QString AppController::quickSavePath() const {
    const QString directory = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(directory);
    return directory + QDir::separator() + "quick-save.json";
}

bool AppController::saveSnapshot(const winrisk::Snapshot& snapshot, const QString& path) {
    QFile file(path);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
        return false;
    }
    return file.write(QJsonDocument(snapshotToJson(snapshot)).toJson(QJsonDocument::Indented)) > 0;
}

bool AppController::loadSnapshot(const QString& path, winrisk::Snapshot& snapshot, QString& error) {
    QFile file(path);
    if (!file.open(QIODevice::ReadOnly)) {
        error = "No quick save exists yet";
        return false;
    }
    const QByteArray raw = file.readAll();
    QJsonParseError parseError;
    const QJsonDocument document = QJsonDocument::fromJson(raw, &parseError);
    if (parseError.error != QJsonParseError::NoError || !document.isObject()) {
        error = "Save is not valid JSON";
        return false;
    }
    const QJsonObject root = document.object();
    if (root.value("version").toInt() == 2) {
        return jsonToSnapshotV2(root, snapshot, error);
    }
    return legacyJavaJsonToSnapshot(root, raw, snapshot, error);
}
