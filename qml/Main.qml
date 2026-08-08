import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ApplicationWindow {
    id: window
    width: 1280
    height: 800
    minimumWidth: 360
    minimumHeight: 640
    visible: true
    title: "WinRisk"
    color: "#111820"

    StackView {
        id: stack
        anchors.fill: parent
        initialItem: appController.running ? gameComponent : menuComponent
    }

    Connections {
        target: appController
        function onStateChanged() {
            const desired = appController.running ? "game" : "menu"
            if (stack.currentItem && stack.currentItem.objectName !== desired) {
                stack.replace(appController.running ? gameComponent : menuComponent)
            }
        }
    }

    Component {
        id: menuComponent
        MainMenu { objectName: "menu" }
    }

    Component {
        id: gameComponent
        GameScreen { objectName: "game" }
    }
}
