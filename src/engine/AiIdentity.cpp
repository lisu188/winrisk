#include "engine/GameEngine.hpp"

namespace winrisk {

std::string GameEngine::aiStrategyName(AiStrategy strategy) {
    switch (strategy) {
        case AiStrategy::Easy: return "Easy";
        case AiStrategy::Continent: return "Continent";
        case AiStrategy::Balanced: return "Balanced";
        case AiStrategy::BorderGuard: return "Border Guard";
        case AiStrategy::Random: return "Random";
    }
    return "Easy";
}

}
