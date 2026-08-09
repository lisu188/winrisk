import QtQuick
import QtQuick.Controls

ApplicationWindow {
    id: window
    width: 1280
    height: 800
    minimumWidth: 360
    minimumHeight: 600
    visible: true
    title: "WinRisk"
    color: "#0d1319"

    Loader {
        anchors.fill: parent
        sourceComponent: appController.running ? gameComponent : menuComponent
    }

    Component {
        id: menuComponent
        MainMenuV6 { }
    }

    Component {
        id: gameComponent
        GameScreenV6 { }
    }
}
