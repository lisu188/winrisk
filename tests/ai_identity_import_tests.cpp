#include "qt/LegacyJavaImporter.hpp"
#include "JavaSketchTestSupport.hpp"

#include <QCoreApplication>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>

#include <array>
#include <cstdint>
#include <iostream>

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

}

int main(int argc, char** argv) {
    QCoreApplication app(argc, argv);
    constexpr int territoryCount = 42;

    QJsonObject root;
    root.insert("map", test::mapSketch("world"));

    QJsonObject params;
    params.insert("gameMode", "CLASSIC");
    params.insert("humanPlayers", 0);
    params.insert("aiPlayers", 5);
    params.insert("randomSeed", -9223372036854770000.0);
    params.insert("builtinMap", "world");
    root.insert("params", params);

    static const std::array<const char*, 5> classes = {
        "com.winrisk.game.ai.EasyAI",
        "com.winrisk.game.ai.ContinentAI",
        "com.winrisk.game.ai.BalancedAI",
        "com.winrisk.game.ai.BorderGuardAI",
        "com.winrisk.game.ai.RandomAI"
    };
    static const std::array<AiStrategy, 5> strategies = {
        AiStrategy::Easy,
        AiStrategy::Continent,
        AiStrategy::Balanced,
        AiStrategy::BorderGuard,
        AiStrategy::Random
    };

    QJsonArray players;
    for (int i = 0; i < 5; ++i) {
        QJsonObject player = test::basicPlayer(i, false, false, classes[static_cast<std::size_t>(i)]);
        player.insert("reinforcements", 0);
        players.push_back(player);
    }
    root.insert("players", players);

    QJsonArray owners;
    QJsonArray armies;
    for (int i = 0; i < territoryCount; ++i) {
        owners.push_back(i % 5);
        armies.push_back(2);
    }
    root.insert("fieldOwner", owners);
    root.insert("fieldArmy", armies);
    root.insert("curPlayer", 0);
    root.insert("phase", "ATTACK");
    root.insert("commanderDieUsed", false);
    root.insert("maneuverUsed", false);
    root.insert("neutralPlayerIndex", -1);
    root.insert("tradeCount", 0);
    root.insert("drawPile", test::fullDrawPile(territoryCount));
    root.insert("discardPile", QJsonArray{});

    QByteArray raw = QJsonDocument(root).toJson(QJsonDocument::Compact);
    const QByteArray preciseSeed = "-9223372036854770000";
    const int seedKey = raw.indexOf("\"randomSeed\":");
    if (seedKey >= 0) {
        const int start = seedKey + static_cast<int>(sizeof("\"randomSeed\":") - 1);
        int end = start;
        while (end < raw.size() && raw[end] != ',' && raw[end] != '}') ++end;
        raw.replace(start, end - start, preciseSeed);
    }

    QJsonParseError parseError;
    const QJsonDocument exactDocument = QJsonDocument::fromJson(raw, &parseError);
    CHECK(parseError.error == QJsonParseError::NoError);

    Snapshot snapshot;
    QString error;
    CHECK(qt::importJavaGameSketch(exactDocument.object(), raw, snapshot, error));
    CHECK(error.isEmpty());
    CHECK(snapshot.mapId == "world");
    CHECK(snapshot.players.size() == 5);
    for (std::size_t i = 0; i < strategies.size(); ++i) {
        CHECK(snapshot.players[i].ai);
        CHECK(snapshot.players[i].aiStrategy == strategies[i]);
        CHECK(snapshot.players[i].name.find(GameEngine::aiStrategyName(strategies[i])) != std::string::npos);
    }

    const auto expectedSeed = static_cast<std::uint64_t>(static_cast<std::int64_t>(-9223372036854770000LL));
    CHECK(snapshot.rngState == Random(expectedSeed).state());

    GameEngine engine;
    CHECK(engine.restore(snapshot));
    for (std::size_t i = 0; i < strategies.size(); ++i) {
        CHECK(engine.players()[i].aiStrategy == strategies[i]);
    }

    QJsonObject unsupportedRoot = exactDocument.object();
    QJsonArray unsupportedPlayers = unsupportedRoot.value("players").toArray();
    QJsonObject unsupportedPlayer = unsupportedPlayers[0].toObject();
    unsupportedPlayer.insert("interfaceClass", "com.winrisk.game.ai.UnknownAI");
    unsupportedPlayers[0] = unsupportedPlayer;
    unsupportedRoot.insert("players", unsupportedPlayers);
    const QByteArray unsupportedRaw = QJsonDocument(unsupportedRoot).toJson(QJsonDocument::Compact);

    Snapshot rejected;
    error.clear();
    CHECK(!qt::importJavaGameSketch(unsupportedRoot, unsupportedRaw, rejected, error));
    CHECK(error.contains("Unsupported Java AI class"));

    if (failures != 0) std::cerr << failures << " AI identity checks failed\n";
    return failures == 0 ? 0 : 1;
}
