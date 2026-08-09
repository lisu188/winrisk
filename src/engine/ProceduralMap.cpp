#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <numeric>
#include <stdexcept>
#include <string>
#include <vector>

namespace winrisk {
namespace {

constexpr int kWidth = 900;
constexpr int kHeight = 600;
constexpr int kMinDistance = 35;
constexpr int kPlacementAttempts = 200;

struct Point {
    int x = 0;
    int y = 0;
};

double distance(const Point& a, const Point& b) {
    const double dx = static_cast<double>(a.x - b.x);
    const double dy = static_cast<double>(a.y - b.y);
    return std::sqrt(dx * dx + dy * dy);
}

Point randomPoint(Random& random) {
    return {
        kMinDistance + random.uniform(kWidth - 2 * kMinDistance),
        kMinDistance + random.uniform(kHeight - 2 * kMinDistance)
    };
}

Point createPoint(Random& random, const std::vector<Point>& points) {
    for (int attempt = 0; attempt < kPlacementAttempts; ++attempt) {
        const Point candidate = randomPoint(random);
        const bool distant = std::all_of(points.begin(), points.end(), [&](const Point& point) {
            return distance(candidate, point) >= kMinDistance;
        });
        if (distant) return candidate;
    }
    return randomPoint(random);
}

void connect(std::vector<Territory>& territories, int lhs, int rhs) {
    if (lhs == rhs || lhs < 0 || rhs < 0
        || lhs >= static_cast<int>(territories.size()) || rhs >= static_cast<int>(territories.size())) return;
    auto& left = territories[static_cast<std::size_t>(lhs)].adjacent;
    auto& right = territories[static_cast<std::size_t>(rhs)].adjacent;
    if (std::find(left.begin(), left.end(), rhs) == left.end()) left.push_back(rhs);
    if (std::find(right.begin(), right.end(), lhs) == right.end()) right.push_back(lhs);
}

int nearest(const std::vector<Point>& points, int current, int limit) {
    double best = std::numeric_limits<double>::infinity();
    int bestId = 0;
    for (int i = 0; i < limit; ++i) {
        if (i == current) continue;
        const double candidate = distance(points[static_cast<std::size_t>(current)], points[static_cast<std::size_t>(i)]);
        if (candidate < best) {
            best = candidate;
            bestId = i;
        }
    }
    return bestId;
}

int nearestAvailable(const std::vector<Point>& points, const std::vector<Territory>& territories, int current) {
    double best = std::numeric_limits<double>::infinity();
    int bestId = -1;
    const auto& adjacent = territories[static_cast<std::size_t>(current)].adjacent;
    for (int i = 0; i < static_cast<int>(points.size()); ++i) {
        if (i == current || std::find(adjacent.begin(), adjacent.end(), i) != adjacent.end()) continue;
        const double candidate = distance(points[static_cast<std::size_t>(current)], points[static_cast<std::size_t>(i)]);
        if (candidate < best) {
            best = candidate;
            bestId = i;
        }
    }
    return bestId >= 0 ? bestId : nearest(points, current, static_cast<int>(points.size()));
}

std::vector<std::string> split(const std::string& value, char delimiter) {
    std::vector<std::string> parts;
    std::size_t begin = 0;
    for (;;) {
        const std::size_t end = value.find(delimiter, begin);
        parts.push_back(value.substr(begin, end == std::string::npos ? std::string::npos : end - begin));
        if (end == std::string::npos) break;
        begin = end + 1;
    }
    return parts;
}

}

std::string GameEngine::proceduralMapId(int fieldCount, int continentCount, std::uint64_t seed) {
    return "random:" + std::to_string(fieldCount) + ":" + std::to_string(continentCount) + ":" + std::to_string(seed);
}

MapDefinition GameEngine::generateProceduralMap(int fieldCount, int continentCount, std::uint64_t seed) {
    if (fieldCount < 1) throw std::invalid_argument("At least one field is required");
    if (continentCount < 1) throw std::invalid_argument("At least one continent is required");
    if (continentCount > fieldCount) throw std::invalid_argument("Continents cannot exceed fields");

    Random random(seed);
    MapDefinition map;
    map.id = proceduralMapId(fieldCount, continentCount, seed);
    map.displayName = "Random " + std::to_string(fieldCount) + " territories / " + std::to_string(continentCount) + " regions";
    map.territories.reserve(static_cast<std::size_t>(fieldCount));

    std::vector<Point> points;
    points.reserve(static_cast<std::size_t>(fieldCount));
    for (int i = 0; i < fieldCount; ++i) {
        const Point point = createPoint(random, points);
        points.push_back(point);
        Territory territory;
        territory.id = i;
        territory.name = "Territory " + std::to_string(i + 1);
        territory.x = static_cast<float>(point.x) / static_cast<float>(kWidth);
        territory.y = static_cast<float>(point.y) / static_cast<float>(kHeight);
        map.territories.push_back(std::move(territory));
    }

    for (int i = 1; i < fieldCount; ++i) connect(map.territories, i, nearest(points, i, i));

    if (fieldCount >= 3) {
        for (int i = 0; i < fieldCount; ++i) {
            while (map.territories[static_cast<std::size_t>(i)].adjacent.size() < 2) {
                const int target = nearestAvailable(points, map.territories, i);
                const auto& adjacent = map.territories[static_cast<std::size_t>(i)].adjacent;
                if (std::find(adjacent.begin(), adjacent.end(), target) != adjacent.end()) break;
                connect(map.territories, i, target);
            }
        }
        for (int i = 0; i < fieldCount; ++i) {
            if (random.uniform(2) != 0) connect(map.territories, i, nearest(points, i, fieldCount));
        }
    }

    std::vector<int> shuffled(static_cast<std::size_t>(fieldCount));
    std::iota(shuffled.begin(), shuffled.end(), 0);
    for (std::size_t i = shuffled.size(); i > 1; --i) {
        const std::size_t j = static_cast<std::size_t>(random.uniform(static_cast<int>(i)));
        std::swap(shuffled[i - 1], shuffled[j]);
    }

    const int baseSize = fieldCount / continentCount;
    const int remainder = fieldCount % continentCount;
    int offset = 0;
    map.continents.reserve(static_cast<std::size_t>(continentCount));
    for (int continentIndex = 0; continentIndex < continentCount; ++continentIndex) {
        const int continentSize = baseSize + (continentIndex < remainder ? 1 : 0);
        Continent continent;
        continent.id = continentIndex;
        continent.name = "Region " + std::to_string(continentIndex + 1);
        continent.bonus = std::max(1, continentSize / 3);
        continent.territories.reserve(static_cast<std::size_t>(continentSize));
        for (int i = 0; i < continentSize; ++i) {
            const int territoryId = shuffled[static_cast<std::size_t>(offset + i)];
            continent.territories.push_back(territoryId);
            map.territories[static_cast<std::size_t>(territoryId)].continent = continentIndex;
        }
        offset += continentSize;
        map.continents.push_back(std::move(continent));
    }
    return map;
}

std::optional<MapDefinition> GameEngine::makeMapDefinition(const std::string& mapId) {
    if (const auto builtin = makeBuiltinMap(mapId)) return builtin;
    const auto parts = split(mapId, ':');
    if (parts.size() != 4 || parts[0] != "random") return std::nullopt;
    try {
        std::size_t parsed = 0;
        const int fields = std::stoi(parts[1], &parsed);
        if (parsed != parts[1].size()) return std::nullopt;
        parsed = 0;
        const int continents = std::stoi(parts[2], &parsed);
        if (parsed != parts[2].size()) return std::nullopt;
        parsed = 0;
        const std::uint64_t seed = std::stoull(parts[3], &parsed);
        if (parsed != parts[3].size()) return std::nullopt;
        return generateProceduralMap(fields, continents, seed);
    } catch (const std::exception&) {
        return std::nullopt;
    }
}

}
