import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Frame {
    id: root
    padding: 14

    ColumnLayout {
        anchors.fill: parent
        spacing: 9

        RowLayout {
            Layout.fillWidth: true
            Rectangle {
                width: 16
                height: 16
                radius: 8
                color: appController.currentPlayerColor
            }
            Label {
                Layout.fillWidth: true
                text: appController.currentPlayerText
                font.pixelSize: 20
                font.bold: true
            }
            Label {
                text: "Turn " + appController.turn
                color: "#9aa7b4"
            }
        }

        Label {
            Layout.fillWidth: true
            text: appController.winnerText.length > 0 ? appController.winnerText : appController.phaseText
            font.pixelSize: 18
            font.bold: true
        }

        Label {
            Layout.fillWidth: true
            visible: appController.phaseText === "Reinforce"
            text: "Reinforcements: " + appController.reinforcements
            color: "#d6e4ef"
        }

        Frame {
            Layout.fillWidth: true
            padding: 8

            RowLayout {
                anchors.fill: parent
                spacing: 8

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 2
                    Label {
                        text: "Cards: " + appController.cardCount
                        font.bold: appController.mustTradeCards
                        color: appController.mustTradeCards ? "#ffcf70" : "#d6e4ef"
                    }
                    Label {
                        Layout.fillWidth: true
                        text: appController.cardsText
                        color: "#9aa7b4"
                        elide: Text.ElideRight
                    }
                }

                Button {
                    visible: appController.phaseText === "Reinforce" && appController.cardCount >= 3
                    enabled: appController.canTradeCards
                    text: "Trade +" + appController.nextTradeValue
                    onClicked: appController.tradeCards()
                }
            }
        }

        Label {
            Layout.fillWidth: true
            text: appController.phaseText === "Reinforce"
                  ? (appController.mustTradeCards
                     ? "Trade a card set, then place all reinforcements."
                     : "Tap your territory to add an army.")
                  : appController.phaseText === "Attack"
                    ? "Tap a source with 2+ armies, then an adjacent enemy. Capture at least one territory to earn a card."
                    : appController.phaseText === "Maneuver"
                      ? "Tap a source, then a connected friendly territory."
                      : "Game complete."
            wrapMode: Text.Wrap
            color: "#aab7c3"
        }

        Rectangle {
            Layout.fillWidth: true
            height: 1
            color: "#3b4b57"
        }

        Label {
            Layout.fillWidth: true
            Layout.minimumHeight: 46
            text: appController.status
            wrapMode: Text.Wrap
            color: "#e3e8ed"
        }

        Item { Layout.fillHeight: true }

        Button {
            Layout.fillWidth: true
            Layout.preferredHeight: 48
            enabled: appController.phaseText !== "Finished"
            text: appController.phaseText === "Reinforce" ? "Begin Attack"
                  : appController.phaseText === "Attack" ? "Begin Maneuver"
                  : "End Turn"
            onClicked: appController.endPhase()
        }

        RowLayout {
            Layout.fillWidth: true
            Button {
                Layout.fillWidth: true
                text: "Save"
                onClicked: appController.quickSave()
            }
            Button {
                Layout.fillWidth: true
                text: "Load"
                onClicked: appController.quickLoad()
            }
        }

        Button {
            Layout.fillWidth: true
            text: "Main Menu"
            onClicked: appController.returnToMenu()
        }
    }
}
