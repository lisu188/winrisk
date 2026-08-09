#include "engine/GameEngine.hpp"

#include <array>
#include <utility>

namespace winrisk {
namespace {

void link(std::vector<Territory>& territories, int a, int b) {
    territories[static_cast<std::size_t>(a)].adjacent.push_back(b);
    territories[static_cast<std::size_t>(b)].adjacent.push_back(a);
}

std::vector<int> range(int first, int lastInclusive) {
    std::vector<int> result;
    for (int i = first; i <= lastInclusive; ++i) {
        result.push_back(i);
    }
    return result;
}

}

std::vector<Continent> GameEngine::makeWorldContinents() {
    return {
        {0, "North America", 5, range(0, 8)},
        {1, "South America", 2, range(9, 12)},
        {2, "Europe", 5, range(13, 19)},
        {3, "Africa", 3, range(20, 25)},
        {4, "Asia", 7, range(26, 37)},
        {5, "Australia", 2, range(38, 41)}
    };
}

std::vector<Territory> GameEngine::makeWorldTerritories() {
    static const std::array<const char*, 42> names = {
        "Alaska", "Northwest Territory", "Greenland", "Alberta", "Ontario", "Quebec",
        "Western United States", "Eastern United States", "Central America", "Venezuela", "Peru", "Brazil", "Argentina",
        "Iceland", "Scandinavia", "Ukraine", "Great Britain", "Northern Europe", "Western Europe", "Southern Europe",
        "North Africa", "Egypt", "East Africa", "Congo", "South Africa", "Madagascar",
        "Ural", "Siberia", "Yakutsk", "Kamchatka", "Irkutsk", "Mongolia", "Japan", "Afghanistan", "China", "Middle East", "India", "Siam",
        "Indonesia", "New Guinea", "Western Australia", "Eastern Australia"
    };
    static const std::array<std::array<float, 2>, 42> positions = {{
        {0.06f,0.16f},{0.17f,0.11f},{0.34f,0.08f},{0.14f,0.24f},{0.23f,0.22f},{0.31f,0.21f},{0.17f,0.32f},{0.26f,0.32f},{0.20f,0.43f},
        {0.28f,0.52f},{0.27f,0.64f},{0.36f,0.58f},{0.29f,0.77f},
        {0.43f,0.16f},{0.51f,0.16f},{0.59f,0.21f},{0.45f,0.25f},{0.51f,0.27f},{0.46f,0.35f},{0.54f,0.35f},
        {0.46f,0.49f},{0.54f,0.46f},{0.58f,0.54f},{0.50f,0.58f},{0.52f,0.71f},{0.61f,0.68f},
        {0.66f,0.21f},{0.73f,0.14f},{0.81f,0.10f},{0.91f,0.17f},{0.80f,0.21f},{0.82f,0.29f},{0.92f,0.32f},{0.65f,0.31f},{0.75f,0.36f},{0.61f,0.42f},{0.68f,0.43f},{0.76f,0.49f},
        {0.79f,0.62f},{0.89f,0.61f},{0.82f,0.74f},{0.91f,0.73f}
    }};

    std::vector<Territory> territories;
    territories.reserve(names.size());
    for (int i = 0; i < static_cast<int>(names.size()); ++i) {
        Territory territory;
        territory.id = i;
        territory.name = names[static_cast<std::size_t>(i)];
        territory.x = positions[static_cast<std::size_t>(i)][0];
        territory.y = positions[static_cast<std::size_t>(i)][1];
        if (i <= 8) territory.continent = 0;
        else if (i <= 12) territory.continent = 1;
        else if (i <= 19) territory.continent = 2;
        else if (i <= 25) territory.continent = 3;
        else if (i <= 37) territory.continent = 4;
        else territory.continent = 5;
        territories.push_back(std::move(territory));
    }

    static const std::array<std::pair<int, int>, 83> edges = {{
        {0,1},{0,3},{0,29},{1,2},{1,3},{1,4},{2,4},{2,5},{2,13},{3,4},{3,6},{4,5},{4,6},{4,7},{5,7},{6,7},{6,8},{7,8},{8,9},
        {9,10},{9,11},{10,11},{10,12},{11,12},{11,20},
        {13,14},{13,16},{14,15},{14,16},{14,17},{15,17},{15,19},{15,26},{15,33},{15,35},{16,17},{16,18},{17,18},{17,19},{18,19},{18,20},{19,20},{19,21},{19,35},
        {20,21},{20,22},{20,23},{21,22},{21,35},{22,23},{22,24},{22,25},{22,35},{23,24},{24,25},
        {26,27},{26,33},{26,34},{27,28},{27,30},{27,31},{27,34},{28,29},{28,30},{29,30},{29,31},{29,32},{30,31},{31,32},{31,34},{33,34},{33,35},{33,36},{34,36},{34,37},{35,36},{36,37},
        {37,38},{38,39},{38,40},{39,40},{39,41},{40,41}
    }};
    for (const auto& edge : edges) {
        link(territories, edge.first, edge.second);
    }
    return territories;
}

}
