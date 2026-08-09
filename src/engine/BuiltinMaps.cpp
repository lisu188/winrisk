#include "engine/GameEngine.hpp"

#include <algorithm>
#include <cctype>
#include <initializer_list>
#include <string>
#include <unordered_map>
#include <utility>

namespace winrisk {
namespace {

class MapBuilder {
public:
    MapBuilder(std::string id, std::string displayName)
        : definition_{std::move(id), std::move(displayName), {}, {}} {}

    MapBuilder& territory(const std::string& name, int x, int y) {
        const int id = static_cast<int>(definition_.territories.size());
        Territory territory;
        territory.id = id;
        territory.name = name;
        territory.x = static_cast<float>(x) / 900.0f;
        territory.y = static_cast<float>(y) / 600.0f;
        definition_.territories.push_back(std::move(territory));
        ids_.emplace(name, id);
        return *this;
    }

    MapBuilder& connect(const std::string& a, const std::string& b) {
        const int lhs = ids_.at(a);
        const int rhs = ids_.at(b);
        auto& left = definition_.territories[static_cast<std::size_t>(lhs)].adjacent;
        auto& right = definition_.territories[static_cast<std::size_t>(rhs)].adjacent;
        if (std::find(left.begin(), left.end(), rhs) == left.end()) left.push_back(rhs);
        if (std::find(right.begin(), right.end(), lhs) == right.end()) right.push_back(lhs);
        return *this;
    }

    MapBuilder& region(int bonus, std::initializer_list<const char*> names) {
        Continent continent;
        continent.id = static_cast<int>(definition_.continents.size());
        continent.name = "Region " + std::to_string(continent.id + 1);
        continent.bonus = bonus;
        for (const char* name : names) {
            const int territoryId = ids_.at(name);
            continent.territories.push_back(territoryId);
            definition_.territories[static_cast<std::size_t>(territoryId)].continent = continent.id;
        }
        definition_.continents.push_back(std::move(continent));
        return *this;
    }

    MapDefinition build() && { return std::move(definition_); }

private:
    MapDefinition definition_;
    std::unordered_map<std::string, int> ids_;
};

MapDefinition pangaea() {
    MapBuilder b("pangaea", "Pangaea");
    b.territory("Laurentia",120,90).territory("Greenland Shield",230,60).territory("Superior Craton",200,140)
     .territory("Wyoming Craton",90,160).territory("Ouachita",150,230).territory("Appalachia",270,160)
     .territory("Fennoscandia",340,60).territory("Baltica",420,90).territory("Avalonia",350,140)
     .territory("Armorica",420,170).territory("Iberia",360,230)
     .territory("Siberia",560,60).territory("Kazakhstania",620,120).territory("Tarim",680,170)
     .territory("North China",740,110).territory("South China",740,210)
     .territory("Anatolia",520,260).territory("Iran",590,280).territory("Tibet",660,300).territory("Indochina",730,300)
     .territory("Amazonia",170,330).territory("Rio de la Plata",120,420).territory("Patagonia",180,500)
     .territory("West Africa",280,310).territory("Congo Craton",330,390).territory("Kalahari",300,480)
     .territory("Arabia",450,330).territory("India",540,380).territory("Madagascar",430,460)
     .territory("Antarctica",520,520).territory("Australia",630,470).territory("Zealandia",720,520);

    b.connect("Laurentia","Greenland Shield").connect("Laurentia","Superior Craton").connect("Laurentia","Wyoming Craton")
     .connect("Wyoming Craton","Ouachita").connect("Superior Craton","Appalachia").connect("Ouachita","Appalachia")
     .connect("Greenland Shield","Superior Craton").connect("Fennoscandia","Baltica").connect("Baltica","Avalonia")
     .connect("Avalonia","Armorica").connect("Armorica","Iberia").connect("Avalonia","Iberia")
     .connect("Siberia","Kazakhstania").connect("Kazakhstania","Tarim").connect("Tarim","North China")
     .connect("North China","South China").connect("Tarim","South China").connect("Anatolia","Iran")
     .connect("Iran","Tibet").connect("Tibet","Indochina").connect("Amazonia","Rio de la Plata")
     .connect("Rio de la Plata","Patagonia").connect("Amazonia","West Africa").connect("West Africa","Congo Craton")
     .connect("Congo Craton","Kalahari").connect("Patagonia","Kalahari").connect("Arabia","India")
     .connect("India","Madagascar").connect("Madagascar","Antarctica").connect("Antarctica","Australia")
     .connect("Australia","Zealandia").connect("India","Antarctica")
     .connect("Greenland Shield","Fennoscandia").connect("Appalachia","Avalonia").connect("Appalachia","West Africa")
     .connect("Ouachita","Amazonia").connect("Iberia","West Africa").connect("Baltica","Kazakhstania")
     .connect("Siberia","Baltica").connect("Kazakhstania","Iran").connect("Tarim","Tibet")
     .connect("South China","Indochina").connect("Anatolia","Arabia").connect("Iran","Arabia")
     .connect("Tibet","India").connect("Congo Craton","Arabia").connect("Kalahari","Madagascar")
     .connect("Kalahari","Antarctica");

    b.region(4,{"Laurentia","Greenland Shield","Superior Craton","Wyoming Craton","Ouachita","Appalachia"})
     .region(3,{"Fennoscandia","Baltica","Avalonia","Armorica","Iberia"})
     .region(3,{"Siberia","Kazakhstania","Tarim","North China","South China"})
     .region(2,{"Anatolia","Iran","Tibet","Indochina"})
     .region(4,{"Amazonia","Rio de la Plata","Patagonia","West Africa","Congo Craton","Kalahari"})
     .region(4,{"Arabia","India","Madagascar","Antarctica","Australia","Zealandia"});
    return std::move(b).build();
}

MapDefinition laurasia() {
    MapBuilder b("laurasia", "Laurasia");
    b.territory("Alaska",80,120).territory("Cordillera",140,190).territory("Laurentia",230,160)
     .territory("Sonora",150,280).territory("Wyoming Craton",230,250)
     .territory("Superior Craton",310,200).territory("Labrador",370,130).territory("Greenland",440,70).territory("Appalachia",350,290)
     .territory("Fennoscandia",520,110).territory("Baltica",560,180).territory("Avalonia",480,240).territory("Bohemia",560,280).territory("Iberia",480,330)
     .territory("Taimyr",660,80).territory("West Siberia",630,150).territory("Angara",700,160).territory("Kolyma",750,90)
     .territory("Kazakhstania",630,250).territory("Tarim",690,300).territory("Mongolia",740,230)
     .territory("North China",740,330).territory("South China",700,400).territory("Indochina",620,430);

    b.connect("Alaska","Cordillera").connect("Cordillera","Laurentia").connect("Cordillera","Sonora")
     .connect("Sonora","Wyoming Craton").connect("Laurentia","Wyoming Craton")
     .connect("Superior Craton","Labrador").connect("Labrador","Greenland").connect("Superior Craton","Appalachia").connect("Labrador","Appalachia")
     .connect("Fennoscandia","Baltica").connect("Baltica","Avalonia").connect("Baltica","Bohemia").connect("Avalonia","Iberia").connect("Bohemia","Iberia")
     .connect("Taimyr","West Siberia").connect("West Siberia","Angara").connect("Angara","Kolyma").connect("Taimyr","Angara")
     .connect("Kazakhstania","Tarim").connect("Tarim","Mongolia").connect("Mongolia","North China")
     .connect("Tarim","North China").connect("North China","South China").connect("South China","Indochina")
     .connect("Laurentia","Superior Craton").connect("Wyoming Craton","Superior Craton").connect("Alaska","Kolyma")
     .connect("Greenland","Fennoscandia").connect("Appalachia","Avalonia").connect("Baltica","West Siberia")
     .connect("Bohemia","Kazakhstania").connect("Kazakhstania","West Siberia").connect("Mongolia","Angara");

    b.region(3,{"Alaska","Cordillera","Laurentia","Sonora","Wyoming Craton"})
     .region(2,{"Superior Craton","Labrador","Greenland","Appalachia"})
     .region(3,{"Fennoscandia","Baltica","Avalonia","Bohemia","Iberia"})
     .region(2,{"Taimyr","West Siberia","Angara","Kolyma"})
     .region(4,{"Kazakhstania","Tarim","Mongolia","North China","South China","Indochina"});
    return std::move(b).build();
}

MapDefinition gondwana() {
    MapBuilder b("gondwana", "Gondwana");
    b.territory("Amazonia",120,140).territory("Sao Francisco",210,190).territory("Pampia",110,240)
     .territory("Rio de la Plata",180,290).territory("Patagonia",140,380)
     .territory("West Africa",330,120).territory("Sahara Metacraton",430,110).territory("Congo Craton",380,210)
     .territory("Tanzania Craton",470,250).territory("Kalahari",400,320).territory("Kaapvaal",330,350)
     .territory("Levant",530,80).territory("Nubian Shield",520,160).territory("Arabia",600,130).territory("Oman",660,190)
     .territory("Madagascar",500,330).territory("India",610,280).territory("Enderby Land",580,380)
     .territory("East Antarctica",520,450).territory("Queen Maud Land",430,430).territory("West Antarctica",330,470)
     .territory("Pilbara",700,330).territory("Yilgarn",680,420).territory("North Australia",750,260)
     .territory("Gawler",740,440).territory("Tasmanides",740,520);

    b.connect("Amazonia","Sao Francisco").connect("Amazonia","Pampia").connect("Pampia","Rio de la Plata")
     .connect("Sao Francisco","Rio de la Plata").connect("Rio de la Plata","Patagonia")
     .connect("West Africa","Sahara Metacraton").connect("West Africa","Congo Craton").connect("Sahara Metacraton","Congo Craton")
     .connect("Congo Craton","Tanzania Craton").connect("Congo Craton","Kalahari").connect("Tanzania Craton","Kalahari").connect("Kalahari","Kaapvaal")
     .connect("Levant","Nubian Shield").connect("Levant","Arabia").connect("Nubian Shield","Arabia").connect("Arabia","Oman")
     .connect("Madagascar","India").connect("Madagascar","Enderby Land").connect("India","Enderby Land")
     .connect("Enderby Land","East Antarctica").connect("East Antarctica","Queen Maud Land").connect("Queen Maud Land","West Antarctica")
     .connect("North Australia","Pilbara").connect("Pilbara","Yilgarn").connect("Yilgarn","Gawler")
     .connect("Gawler","Tasmanides").connect("North Australia","Gawler")
     .connect("Amazonia","West Africa").connect("Sao Francisco","Congo Craton").connect("Pampia","Kaapvaal")
     .connect("Patagonia","West Antarctica").connect("Kalahari","Queen Maud Land").connect("Tanzania Craton","Madagascar")
     .connect("Sahara Metacraton","Nubian Shield").connect("Oman","India").connect("East Antarctica","Gawler").connect("India","Pilbara");

    b.region(3,{"Amazonia","Sao Francisco","Pampia","Rio de la Plata","Patagonia"})
     .region(4,{"West Africa","Sahara Metacraton","Congo Craton","Tanzania Craton","Kalahari","Kaapvaal"})
     .region(2,{"Levant","Nubian Shield","Arabia","Oman"})
     .region(4,{"Madagascar","India","Enderby Land","East Antarctica","Queen Maud Land","West Antarctica"})
     .region(3,{"Pilbara","Yilgarn","North Australia","Gawler","Tasmanides"});
    return std::move(b).build();
}

MapDefinition rodinia() {
    MapBuilder b("rodinia", "Rodinia");
    b.territory("Slave",180,120).territory("Laurentia",260,200).territory("Wyoming Craton",170,260)
     .territory("Superior Craton",350,150).territory("Grenville Belt",400,250).territory("Siberia",150,60)
     .territory("Baltica",520,130).territory("Amazonia",560,230).territory("West Africa",640,160)
     .territory("Sao Francisco",650,300).territory("Congo Craton",720,230)
     .territory("Kalahari",690,400).territory("Zimbabwe Craton",740,330).territory("India",600,420).territory("Tarim",520,470)
     .territory("North China",420,60).territory("South China",330,400).territory("Australia",250,450)
     .territory("Mawson Craton",140,400).territory("East Antarctica",230,520);

    b.connect("Slave","Laurentia").connect("Laurentia","Wyoming Craton").connect("Laurentia","Superior Craton")
     .connect("Laurentia","Grenville Belt").connect("Superior Craton","Grenville Belt").connect("Slave","Superior Craton").connect("Slave","Siberia")
     .connect("Baltica","Amazonia").connect("Baltica","West Africa").connect("Amazonia","Sao Francisco")
     .connect("West Africa","Congo Craton").connect("Sao Francisco","Congo Craton")
     .connect("Kalahari","Zimbabwe Craton").connect("Kalahari","India").connect("Zimbabwe Craton","India").connect("India","Tarim")
     .connect("North China","South China").connect("South China","Australia").connect("Australia","Mawson Craton")
     .connect("Australia","East Antarctica").connect("Mawson Craton","East Antarctica")
     .connect("Siberia","North China").connect("Grenville Belt","Baltica").connect("Grenville Belt","Amazonia")
     .connect("Congo Craton","Zimbabwe Craton").connect("India","East Antarctica").connect("Tarim","South China")
     .connect("Wyoming Craton","Mawson Craton");

    b.region(4,{"Slave","Laurentia","Wyoming Craton","Superior Craton","Grenville Belt","Siberia"})
     .region(3,{"Baltica","Amazonia","West Africa","Sao Francisco","Congo Craton"})
     .region(2,{"Kalahari","Zimbabwe Craton","India","Tarim"})
     .region(3,{"North China","South China","Australia","Mawson Craton","East Antarctica"});
    return std::move(b).build();
}

std::string normalize(std::string value) {
    value.erase(value.begin(), std::find_if(value.begin(), value.end(), [](unsigned char c) { return !std::isspace(c); }));
    value.erase(std::find_if(value.rbegin(), value.rend(), [](unsigned char c) { return !std::isspace(c); }).base(), value.end());
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return value;
}

}

std::vector<std::string> GameEngine::builtinMapIds() {
    return {"world", "pangaea", "laurasia", "gondwana", "rodinia"};
}

std::optional<MapDefinition> GameEngine::makeBuiltinMap(const std::string& mapId) {
    const std::string id = normalize(mapId);
    if (id == "world") return MapDefinition{"world", "World", makeWorldTerritories(), makeWorldContinents()};
    if (id == "pangaea") return pangaea();
    if (id == "laurasia") return laurasia();
    if (id == "gondwana") return gondwana();
    if (id == "rodinia") return rodinia();
    return std::nullopt;
}

}
