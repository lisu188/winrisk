import QtQuick
import QtQuick.Controls

Item {
    id: root
    clip: true

    Rectangle {
        anchors.fill: parent
        radius: 12
        color: "#17232d"
        border.color: "#334553"
        border.width: 1

        gradient: Gradient {
            GradientStop { position: 0.0; color: "#1d2b36" }
            GradientStop { position: 1.0; color: "#101820" }
        }
    }

    Item {
        id: boardSurface
        anchors.fill: parent
        anchors.margins: 14
        transformOrigin: Item.Center

        Text { x: parent.width * 0.12; y: parent.height * 0.08; text: "NORTH AMERICA"; color: "#526879"; font.pixelSize: 12; font.bold: true }
        Text { x: parent.width * 0.24; y: parent.height * 0.82; text: "SOUTH AMERICA"; color: "#526879"; font.pixelSize: 12; font.bold: true }
        Text { x: parent.width * 0.45; y: parent.height * 0.08; text: "EUROPE"; color: "#526879"; font.pixelSize: 12; font.bold: true }
        Text { x: parent.width * 0.47; y: parent.height * 0.77; text: "AFRICA"; color: "#526879"; font.pixelSize: 12; font.bold: true }
        Text { x: parent.width * 0.72; y: parent.height * 0.06; text: "ASIA"; color: "#526879"; font.pixelSize: 12; font.bold: true }
        Text { x: parent.width * 0.82; y: parent.height * 0.82; text: "AUSTRALIA"; color: "#526879"; font.pixelSize: 12; font.bold: true }

        Repeater {
            model: appController.boardModel

            delegate: Item {
                id: territory
                required property int territoryId
                required property string territoryName
                required property real nx
                required property real ny
                required property int armies
                required property int ownerId
                required property color ownerColor
                required property int headquartersOwnerId
                required property bool fieldVisible
                required property bool selected

                width: Math.max(34, Math.min(48, boardSurface.width / 22))
                height: width
                x: nx * Math.max(1, boardSurface.width - width)
                y: ny * Math.max(1, boardSurface.height - height)
                z: selected ? 3 : 2
                visible: fieldVisible

                Rectangle {
                    anchors.fill: parent
                    radius: width / 2
                    color: territory.ownerColor
                    border.color: territory.selected ? "white" : "#182028"
                    border.width: territory.selected ? 4 : 2
                    scale: tapHandler.pressed ? 0.9 : 1.0

                    Behavior on scale { NumberAnimation { duration: 80 } }

                    Text {
                        anchors.centerIn: parent
                        text: territory.armies
                        color: "white"
                        font.pixelSize: Math.max(13, parent.width * 0.38)
                        font.bold: true
                        style: Text.Outline
                        styleColor: "#55000000"
                    }

                    Rectangle {
                        visible: territory.headquartersOwnerId >= 0
                        width: 15
                        height: 15
                        radius: 2
                        anchors.right: parent.right
                        anchors.top: parent.top
                        color: "#f8d66d"
                        border.color: "#261b00"
                        border.width: 1

                        Text {
                            anchors.centerIn: parent
                            text: "H"
                            font.pixelSize: 10
                            font.bold: true
                            color: "#261b00"
                        }
                    }
                }

                TapHandler {
                    id: tapHandler
                    enabled: territory.fieldVisible
                    onTapped: appController.territoryTapped(territory.territoryId)
                }

                HoverHandler { id: hover; enabled: territory.fieldVisible }
                ToolTip.visible: territory.fieldVisible && hover.hovered
                ToolTip.text: territory.territoryName + " — " + territory.armies
                ToolTip.delay: 350
            }
        }

        PinchHandler {
            target: boardSurface
            minimumScale: 0.85
            maximumScale: 2.2
        }
    }
}
