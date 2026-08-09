#include "qt/AppController.hpp"
#include "qt/LegacyJavaImporter.hpp"

#include <QDir>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QRandomGenerator>
#include <QStandardPaths>
#include <QStringList>

#include <array>
#include <utility>

namespace {

QString diceText(const std::vector<int>& dice) {
    QStringList values;
    for (const int value : dice) values.push_back(QString::number(value));
    return values.join(',');
}

QJsonArray intVectorToJson(const std::vector<int>& values) {
    QJsonArray array;
    for (const int value : values) array.push_back(value);
    return array;
}

QJsonObject missionToJson(const winrisk::MissionSpec& mission) {
    QJsonObject object;
    object.insert("kind", static_cast<int>(mission.kind));
    object.insert("territories", mission.territories);
    object.insert("minimumArmies", mission.minimumArmies);
    object.insert("continentCount", mission.continentCount);
    object.insert("eliminationTarget", mission.eliminationTarget);
    return object;
}

bool jsonToMission(const QJsonObject& object, winrisk::MissionSpec& mission, QString& error) {
    const int kind = object.value("kind").toInt(-1);
    if (kind < static_cast<int>(winrisk::MissionKind::Territory)
        || kind > static_cast<int>(winrisk::MissionKind::Elimination)) {
        error = "Invalid mission kind";
        return false;
    }
    mission.kind = static_cast<winrisk::MissionKind>(kind);
    mission.territories = object.value("territories").toInt();
    mission.minimumArmies = object.value("minimumArmies").toInt();
    mission.continentCount = object.value("continentCount").toInt();
    mission.eliminationTarget = object.value("eliminationTarget").toInt(-1);
    return true;
}

QJsonObject rulesToJson(const winrisk::RulesOptions& rules) {
    QJsonObject object;
    object.insert("incrementalCardSetValues", rules.incrementalCardSetValues);
    object.insert("expandedManeuver", rules.expandedManeuver);
    object.insert("attackCardReroll", rules.attackCardReroll);
    object.insert("commanderDie", rules.commanderDie);
    object.insert("attackWithAll", rules.attackWithAll);
    object.insert("fogOfWar", rules.fogOfWar);
    object.insert("skynet", rules.skynet);
    return object;
}

winrisk::RulesOptions jsonToRules(const QJsonObject& object) {
    winrisk::RulesOptions rules;
    rules.incrementalCardSetValues = object.value("incrementalCardSetValues").toBool();
    rules.expandedManeuver = object.value("expandedManeuver").toBool();
    rules.attackCardReroll = object.value("attackCardReroll").toBool();
    rules.commanderDie = object.value("commanderDie").toBool();
    rules.attackWithAll = object.value("attackWithAll").toBool();
    rules.fogOfWar = object.value("fogOfWar").toBool();
    rules.skynet = object.value("skynet").toBool();
    return rules;
}

bool jsonToCardVector(const QJsonArray& array, std::vector<winrisk::Card>& cards, QString& error) {
    const auto catalog = winrisk::GameEngine::makeRiskDeck();
    cards.clear();
    cards.reserve(static_cast<std::size_t>(array.size()));
    for (const auto value : array) {
        const int id = value.toInt(-1);
        if (id < 0 || id >= static_cast<int>(catalog.size())) {
            error = "Invalid card id";
            return false;
        }
        cards.push_back(catalog[static_cast<std::size_t>(id)]);
    }
    return true;
}

QJsonObject snapshotToJson(const winrisk::Snapshot& snapshot) {
    QJsonObject root;
    root.insert("schema", "winrisk-save");
    root.insert("version", snapshot.version);
    root.insert("mode", static_cast<int>(snapshot.mode));
    root.insert("rules", rulesToJson(snapshot.rules));
    root.insert("phase", static_cast<int>(snapshot.phase));
    root.insert("currentPlayer", snapshot.currentPlayer);
    root.insert("winner", snapshot.winner);
    root.insert("turn", QString::number(snapshot.turn));
    root.insert("tradeCount", snapshot.tradeCount);
    root.insert("conqueredThisTurn", snapshot.conqueredThisTurn);
    root.insert("commanderDieUsed", snapshot.commanderDieUsed);
    root.insert("maneuverUsed", snapshot.maneuverUsed);
    root.insert("maneuverSource", snapshot.maneuverSource);
    root.insert("maneuverTarget", snapshot.maneuverTarget);

    QJsonArray rng;
    for (const auto value : snapshot.rngState) rng.push_back(QString::number(value, 16));
    root.insert("rng", rng);

    QJsonArray players;
    for (const auto& player : snapshot.players) {
        QJsonObject object;
        object.insert("id", player.id);
        object.insert("name", QString::fromStdString(player.name));
        object.insert("color", QString::number(player.color, 16));
        object.insert("ai", player.ai);
        object.insert("aiStrategy", static_cast<int>(player.aiStrategy));
        object.insert("neutral", player.neutral);
        object.insert("eliminated", player.eliminated);
        object.insert("reinforcements", player.reinforcements);
        object.insert("cards", intVectorToJson(player.cards));
        object.insert("headquarters", player.headquarters);
        if (player.mission) object.insert("mission", missionToJson(*player.mission));
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

    QJsonArray deck;
    for (const auto& card : snapshot.deck) deck.push_back(card.id);
    root.insert("deck", deck);

    QJsonArray discard;
    for (const auto& card : snapshot.discard) discard.push_back(card.id);
    root.insert("discard", discard);
    root.insert("mapId", "world");
    return root;
}

bool jsonToSnapshot(const QJsonObject& root, winrisk::Snapshot& snapshot, QString& error) {
    const int version = root.value("version").toInt();
    if (version < 2 || version > 6 || root.value("mapId").toString("world") != "world") {
        error = "Unsupported WinRisk save version or map";
        return false;
    }

    snapshot = {};
    snapshot.version = version;
    if (version >= 4) {
        const int mode = root.value("mode").toInt(-1);
        if (mode < static_cast<int>(winrisk::GameMode::Classic)
            || mode > static_cast<int>(winrisk::GameMode::Capital)) {
            error = "Invalid game mode";
            return false;
        }
        snapshot.mode = static_cast<winrisk::GameMode>(mode);
    } else {
        snapshot.mode = winrisk::GameMode::Classic;
    }

    if (version >= 5) {
        snapshot.rules = jsonToRules(root.value("rules").toObject());
        snapshot.commanderDieUsed = root.value("commanderDieUsed").toBool();
        snapshot.maneuverUsed = root.value("maneuverUsed").toBool();
        snapshot.maneuverSource = root.value("maneuverSource").toInt(-1);
        snapshot.maneuverTarget = root.value("maneuverTarget").toInt(-1);
    }
    if (version < 6) {
        snapshot.rules.fogOfWar = false;
        snapshot.rules.skynet = false;
    }

    const int phase = root.value("phase").toInt(-1);
    if (phase < static_cast<int>(winrisk::Phase::Reinforce)
        || phase > static_cast<int>(winrisk::Phase::Finished)) {
        error = "Invalid game phase";
        return false;
    }
    snapshot.phase = static_cast<winrisk::Phase>(phase);
    snapshot.currentPlayer = root.value("currentPlayer").toInt();
    snapshot.winner = root.value("winner").toInt(-1);
    snapshot.turn = root.value("turn").toString("1").toULongLong();
    snapshot.tradeCount = root.value("tradeCount").toInt();
    snapshot.conqueredThisTurn = root.value("conqueredThisTurn").toBool();

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
        const int strategy = object.value("aiStrategy").toInt(static_cast<int>(winrisk::AiStrategy::Easy));
        if (strategy < static_cast<int>(winrisk::AiStrategy::Easy)
            || strategy > static_cast<int>(winrisk::AiStrategy::Random)) {
            error = "Invalid AI strategy";
            return false;
        }
        player.aiStrategy = static_cast<winrisk::AiStrategy>(strategy);
        player.neutral = object.value("neutral").toBool();
        player.eliminated = object.value("eliminated").toBool();
        player.reinforcements = object.value("reinforcements").toInt();
        player.headquarters = object.value("headquarters").toInt(-1);
        for (const auto cardValue : object.value("cards").toArray()) {
            const int cardId = cardValue.toInt(-1);
            if (cardId < 0 || cardId >= 44) {
                error = "Invalid player card";
                return false;
            }
            player.cards.push_back(cardId);
        }
        if (version >= 4 && object.contains("mission")) {
            winrisk::MissionSpec mission;
            if (!jsonToMission(object.value("mission").toObject(), mission, error)) return false;
            player.mission = mission;
        }
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

    if (version >= 3) {
        if (!jsonToCardVector(root.value("deck").toArray(), snapshot.deck, error)
            || !jsonToCardVector(root.value("discard").toArray(), snapshot.discard, error)) {
            return false;
        }
    }
    return true;
}

}

BoardModel::BoardModel(QObject* parent) : QAbstractListModel(parent) {}

int BoardModel::rowCount(const QModelIndex& parent) const {
    if (parent.isValid() || engine_ == nullptr) return 0;
    return static_cast<int>(engine_->territories().size());
}

bool BoardModel::fieldVisible(const winrisk::Territory& territory) const {
    if (engine_ == nullptr || !engine_->rules().fogOfWar) return true;
    if (viewerPlayerId_ < 0 || viewerPlayerId_ >= static_cast<int>(engine_->players().size())) return true;
    if (territory.owner == viewerPlayerId_) return true;
    for (const int adjacentId : territory.adjacent) {
        if (adjacentId >= 0
            && adjacentId < static_cast<int>(engine_->territories().size())
            && engine_->territories()[static_cast<std::size_t>(adjacentId)].owner == viewerPlayerId_) {
            return true;
        }
    }
    return false;
}

QVariant BoardModel::data(const QModelIndex& index, int role) const {
    if (engine_ == nullptr || !index.isValid() || index.row() < 0 || index.row() >= rowCount()) return {};
    const auto& territory = engine_->territories()[static_cast<std::size_t>(index.row())];
    const bool visible = fieldVisible(territory);
    switch (role) {
        case TerritoryIdRole: return territory.id;
        case TerritoryNameRole: return QString::fromStdString(territory.name);
        case XRole: return territory.x;
        case YRole: return territory.y;
        case FieldVisibleRole: return visible;
        case ArmiesRole: return visible ? territory.armies : 0;
        case OwnerIdRole: return visible ? territory.owner : -1;
        case HeadquartersOwnerIdRole: return visible ? engine_->headquartersOwner(territory.id) : -1;
        case SelectedRole: return visible && territory.id == selectedId_;
        case OwnerColorRole:
            if (!visible) return QColor("#26313a");
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
        {HeadquartersOwnerIdRole, "headquartersOwnerId"},
        {FieldVisibleRole, "fieldVisible"},
        {SelectedRole, "selected"}
    };
}

void BoardModel::setEngine(const winrisk::GameEngine* engine) {
    beginResetModel();
    engine_ = engine;
    endResetModel();
}

void BoardModel::setViewerPlayerId(int viewerPlayerId) {
    if (viewerPlayerId_ == viewerPlayerId) return;
    viewerPlayerId_ = viewerPlayerId;
    refresh();
}

void BoardModel::setSelectedId(int selectedId) {
    if (selectedId_ == selectedId) return;
    selectedId_ = selectedId;
    refresh();
}

void BoardModel::refresh() {
    if (rowCount() > 0) emit dataChanged(index(0, 0), index(rowCount() - 1, 0));
}

AppController::AppController(QObject* parent) : QObject(parent), boardModel_(this) {
    boardModel_.setEngine(&engine_);
    aiTimer_.setSingleShot(true);
    aiTimer_.setInterval(140);
    connect(&aiTimer_, &QTimer::timeout, this, &AppController::runAiStep);
}

bool AppController::running() const { return !engine_.players().empty(); }
QString AppController::modeText() const { return QString::fromStdString(winrisk::modeName(engine_.mode())); }

QString AppController::rulesText() const {
    const auto& rules = engine_.rules();
    QStringList names;
    if (rules.incrementalCardSetValues) names << "Incremental cards";
    if (rules.expandedManeuver) names << "Expanded maneuver";
    if (rules.attackCardReroll) names << "Attack reroll";
    if (rules.commanderDie) names << "Commander die";
    if (rules.attackWithAll) names << "Attack with all";
    if (rules.fogOfWar) names << "Fog of war";
    if (rules.skynet) names << "Skynet";
    return names.isEmpty() ? QString("Standard rules") : names.join(", ");
}

QString AppController::phaseText() const { return QString::fromStdString(winrisk::phaseName(engine_.phase())); }
QString AppController::currentPlayerText() const {
    const auto* player = engine_.currentPlayer();
    return player == nullptr ? QString() : QString::fromStdString(player->name);
}
QColor AppController::currentPlayerColor() const {
    const auto* player = engine_.currentPlayer();
    return player == nullptr ? QColor("#808080") : QColor::fromRgba(player->color);
}
QString AppController::missionText() const {
    const auto* player = engine_.currentPlayer();
    if (player == nullptr) return {};
    if (engine_.mode() == winrisk::GameMode::Capital) return QString::fromStdString(engine_.capitalObjectiveText(player->id));
    if (engine_.mode() != winrisk::GameMode::SecretMission) return {};
    return player->ai ? QString("AI mission hidden") : QString::fromStdString(engine_.missionText(player->id));
}
int AppController::reinforcements() const { const auto* p = engine_.currentPlayer(); return p == nullptr ? 0 : p->reinforcements; }
int AppController::cardCount() const { const auto* p = engine_.currentPlayer(); return p == nullptr ? 0 : static_cast<int>(p->cards.size()); }

QString AppController::cardsText() const {
    const auto* player = engine_.currentPlayer();
    if (player == nullptr || player->cards.empty()) return "No cards";
    const auto catalog = winrisk::GameEngine::makeRiskDeck();
    std::array<int, 4> counts{};
    for (const int cardId : player->cards) {
        if (cardId >= 0 && cardId < static_cast<int>(catalog.size())) {
            ++counts[static_cast<std::size_t>(catalog[static_cast<std::size_t>(cardId)].type)];
        }
    }
    return QString("Inf %1  Cav %2  Art %3  Wild %4").arg(counts[0]).arg(counts[1]).arg(counts[2]).arg(counts[3]);
}

bool AppController::canTradeCards() const { return engine_.phase() == winrisk::Phase::Reinforce && engine_.canTradeCards(); }
bool AppController::mustTradeCards() const { return engine_.phase() == winrisk::Phase::Reinforce && engine_.mustTradeCards(); }
int AppController::nextTradeValue() const { return engine_.nextTradeValue(); }
qulonglong AppController::turn() const { return engine_.turn(); }
QString AppController::status() const { return status_; }
QString AppController::winnerText() const {
    const int winner = engine_.winnerId();
    return winner < 0 || winner >= static_cast<int>(engine_.players().size())
        ? QString()
        : QString::fromStdString(engine_.players()[static_cast<std::size_t>(winner)].name) + " wins";
}
BoardModel* AppController::boardModel() { return &boardModel_; }

bool AppController::startNewGame(
    int playerCount,
    int humanPlayers,
    int gameMode,
    bool incrementalCardSetValues,
    bool expandedManeuver,
    bool attackCardReroll,
    bool commanderDie,
    bool attackWithAll,
    bool fogOfWar,
    bool skynet
) {
    aiTimer_.stop();
    if (gameMode < static_cast<int>(winrisk::GameMode::Classic)
        || gameMode > static_cast<int>(winrisk::GameMode::Capital)) {
        status_ = "Invalid game mode";
        refresh();
        return false;
    }

    winrisk::RulesOptions rules;
    rules.incrementalCardSetValues = incrementalCardSetValues;
    rules.expandedManeuver = expandedManeuver;
    rules.attackCardReroll = attackCardReroll;
    rules.commanderDie = commanderDie;
    rules.attackWithAll = attackWithAll;
    rules.fogOfWar = fogOfWar;
    rules.skynet = skynet;

    const auto mode = static_cast<winrisk::GameMode>(gameMode);
    const auto seed = QRandomGenerator::global()->generate64();
    if (!engine_.startNewGame(playerCount, humanPlayers, seed, mode, rules)) {
        status_ = mode == winrisk::GameMode::Classic
            ? "Classic requires 2-5 players"
            : QString("%1 requires 3-5 players").arg(QString::fromStdString(winrisk::modeName(mode)));
        refresh();
        return false;
    }

    viewerPlayerId_ = -1;
    setSelected(-1);
    status_ = QString("%1 game started").arg(modeText());
    refresh();
    scheduleAi();
    return true;
}

void AppController::territoryTapped(int territoryId) {
    const auto* player = engine_.currentPlayer();
    if (player == nullptr || player->ai || engine_.phase() == winrisk::Phase::Finished
        || territoryId < 0 || territoryId >= static_cast<int>(engine_.territories().size())) return;
    const auto& clicked = engine_.territories()[static_cast<std::size_t>(territoryId)];

    if (engine_.phase() == winrisk::Phase::Reinforce) {
        status_ = engine_.reinforce(territoryId)
            ? QString("Reinforced %1").arg(QString::fromStdString(clicked.name))
            : "Select one of your territories";
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
            if (selectedId_ >= 0
                && engine_.territories()[static_cast<std::size_t>(selectedId_)].armies < 2) setSelected(-1);
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

bool AppController::tradeCards() {
    const int bonus = engine_.tradeCards();
    if (bonus <= 0) {
        status_ = "No valid card set to trade";
        refresh();
        return false;
    }
    status_ = QString("Traded cards for %1 armies").arg(bonus);
    refresh();
    return true;
}

bool AppController::endPhase() {
    if (!engine_.endPhase()) {
        status_ = engine_.phase() == winrisk::Phase::Reinforce
            ? (engine_.mustTradeCards() ? "Trade a card set first" : "Place all reinforcements first")
            : "Cannot advance phase";
        refresh();
        return false;
    }
    setSelected(-1);
    status_ = engine_.phase() == winrisk::Phase::Finished ? winnerText() : QString("Phase: %1").arg(phaseText());
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
    viewerPlayerId_ = -1;
    setSelected(-1);
    status_ = snapshot.version < 6 ? "Game loaded and upgraded to save v6" : "Game loaded";
    refresh();
    scheduleAi();
    return true;
}

void AppController::returnToMenu() {
    aiTimer_.stop();
    engine_ = winrisk::GameEngine();
    viewerPlayerId_ = -1;
    boardModel_.setViewerPlayerId(-1);
    setSelected(-1);
    status_.clear();
    boardModel_.setEngine(&engine_);
    emit stateChanged();
}

void AppController::updateViewer() {
    const auto* current = engine_.currentPlayer();
    if (current != nullptr && !current->ai && !current->neutral) viewerPlayerId_ = current->id;

    const auto validViewer = [&]() {
        return viewerPlayerId_ >= 0
            && viewerPlayerId_ < static_cast<int>(engine_.players().size())
            && !engine_.players()[static_cast<std::size_t>(viewerPlayerId_)].ai
            && !engine_.players()[static_cast<std::size_t>(viewerPlayerId_)].neutral;
    };

    if (!validViewer()) {
        viewerPlayerId_ = -1;
        for (const auto& candidate : engine_.players()) {
            if (!candidate.ai && !candidate.neutral) {
                viewerPlayerId_ = candidate.id;
                break;
            }
        }
    }
    boardModel_.setViewerPlayerId(viewerPlayerId_);
}

void AppController::refresh() {
    updateViewer();
    boardModel_.refresh();
    emit stateChanged();
}

void AppController::setSelected(int territoryId) {
    selectedId_ = territoryId;
    boardModel_.setSelectedId(territoryId);
}

void AppController::scheduleAi() {
    const auto* player = engine_.currentPlayer();
    if (engine_.running() && player != nullptr && player->ai && !aiTimer_.isActive()) aiTimer_.start();
}

void AppController::runAiStep() {
    const auto* player = engine_.currentPlayer();
    if (!engine_.running() || player == nullptr || !player->ai) return;
    engine_.aiStep();
    status_ = engine_.phase() == winrisk::Phase::Finished
        ? winnerText()
        : QString("%1 — %2").arg(currentPlayerText(), phaseText());
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
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) return false;
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
    const int version = root.value("version").toInt();
    if (version >= 2 && version <= 6) return jsonToSnapshot(root, snapshot, error);
    return winrisk::qt::importJavaGameSketch(root, raw, snapshot, error);
}
