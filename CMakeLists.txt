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
    src/engine/GameEngine.cpp
)

target_include_directories(winrisk_engine PUBLIC src)
target_compile_features(winrisk_engine PUBLIC cxx_std_20)

add_library(winrisk_qt STATIC
    src/qt/AppController.cpp
)

target_include_directories(winrisk_qt PUBLIC src)
target_link_libraries(winrisk_qt PUBLIC winrisk_engine Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick)

qt_add_executable(winrisk_app
    src/app/main.cpp
)

qt_add_qml_module(winrisk_app
    URI WinRisk
    VERSION 1.0
    QML_FILES
        qml/Main.qml
        qml/screens/MainMenu.qml
        qml/screens/GameScreen.qml
        qml/components/BoardPane.qml
        qml/components/ControlPanel.qml
)

target_link_libraries(winrisk_app PRIVATE winrisk_qt Qt6::Core Qt6::Gui Qt6::Qml Qt6::Quick Qt6::QuickControls2)

set_target_properties(winrisk_app PROPERTIES
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
    MACOSX_BUNDLE_BUNDLE_NAME WinRisk
    MACOSX_BUNDLE_GUI_IDENTIFIER com.winrisk.game
    QT_ANDROID_PACKAGE_NAME com.winrisk.game
)

include(CTest)
if(BUILD_TESTING)
    add_executable(winrisk_engine_tests tests/engine_tests.cpp)
    target_link_libraries(winrisk_engine_tests PRIVATE winrisk_engine)
    add_test(NAME winrisk_engine_tests COMMAND winrisk_engine_tests)
endif()

install(TARGETS winrisk_app
    BUNDLE DESTINATION .
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
)
