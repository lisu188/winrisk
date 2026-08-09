cmake_minimum_required(VERSION 3.24)

project(WinRisk VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

find_package(Qt6 6.10 REQUIRED COMPONENTS Core Gui Qml Quick QuickControls2)
qt_standard_project_setup(REQUIRES 6.10)

add_library(winrisk_engine STATIC
    src/engine/GameEngineCore.cpp
    src/engine/WorldMap.cpp
    src/engine/MissionRules.cpp
    src/engine/CapitalRules.cpp
    src/engine/TwoPlayerRules.cpp
    src/engine/CombatRules.cpp
)

target_include_directories(winrisk_engine PUBLIC src)
target_compile_features(winrisk_engine PUBLIC cxx_std_20)

add_library(winrisk_qt STATIC
    src/qt/AppControllerV6.cpp
    src/qt/LegacyJavaImporter.cpp
)

target_include_directories(winrisk_qt PUBLIC src)
target_link_libraries(winrisk_qt PUBLIC winrisk_engine Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick)

qt_add_executable(winrisk_app
    src/app/main_v6.cpp
)

set(V6_QML_FILES
    qml/MainV6.qml
    qml/screens/MainMenuV6.qml
    qml/screens/GameScreenV6.qml
    qml/components/BoardPaneV6.qml
    qml/components/ControlPanel.qml
)
set_source_files_properties(qml/MainV6.qml PROPERTIES QT_RESOURCE_ALIAS "MainV6.qml")
set_source_files_properties(qml/screens/MainMenuV6.qml PROPERTIES QT_RESOURCE_ALIAS "MainMenuV6.qml")
set_source_files_properties(qml/screens/GameScreenV6.qml PROPERTIES QT_RESOURCE_ALIAS "GameScreenV6.qml")
set_source_files_properties(qml/components/BoardPaneV6.qml PROPERTIES QT_RESOURCE_ALIAS "BoardPaneV6.qml")
set_source_files_properties(qml/components/ControlPanel.qml PROPERTIES QT_RESOURCE_ALIAS "ControlPanel.qml")

qt_add_qml_module(winrisk_app
    URI WinRisk
    VERSION 1.0
    QML_FILES ${V6_QML_FILES}
)

target_link_libraries(winrisk_app PRIVATE winrisk_qt Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick Qt6::QuickControls2)

set_target_properties(winrisk_app PROPERTIES
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
    MACOSX_BUNDLE_BUNDLE_NAME WinRisk
    MACOSX_BUNDLE_GUI_IDENTIFIER com.winrisk.game
    QT_ANDROID_PACKAGE_NAME com.winrisk.game
)

if(ANDROID)
    set_target_properties(winrisk_app PROPERTIES
        QT_ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/platform/android"
        QT_ANDROID_MIN_SDK_VERSION 28
        QT_ANDROID_TARGET_SDK_VERSION 36
        QT_ANDROID_COMPILE_SDK_VERSION 36
        QT_ANDROID_VERSION_NAME "${PROJECT_VERSION}"
        QT_ANDROID_VERSION_CODE 1
    )
endif()

if(EMSCRIPTEN)
    set_target_properties(winrisk_app PROPERTIES
        QT_WASM_INITIAL_MEMORY "96MB"
    )
endif()

include(CTest)
if(BUILD_TESTING)
    add_executable(winrisk_engine_tests tests/engine_tests_v6.cpp)
    target_link_libraries(winrisk_engine_tests PRIVATE winrisk_engine)
    add_test(NAME winrisk_engine_tests COMMAND winrisk_engine_tests)

    add_executable(winrisk_fog_model_tests tests/fog_model_tests.cpp)
    target_link_libraries(winrisk_fog_model_tests PRIVATE winrisk_qt Qt6::Core)
    add_test(NAME winrisk_fog_model_tests COMMAND winrisk_fog_model_tests)

    add_executable(winrisk_legacy_import_tests tests/legacy_java_import_tests.cpp)
    target_link_libraries(winrisk_legacy_import_tests PRIVATE winrisk_qt Qt6::Core)
    add_test(NAME winrisk_legacy_import_tests COMMAND winrisk_legacy_import_tests)
endif()

install(TARGETS winrisk_app
    BUNDLE DESTINATION .
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
)
