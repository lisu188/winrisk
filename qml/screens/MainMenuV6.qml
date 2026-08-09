import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    id: root

    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            GradientStop { position: 0.0; color: "#17212b" }
            GradientStop { position: 1.0; color: "#0d1319" }
        }
    }

    ScrollView {
        anchors.fill: parent
        clip: true
        contentWidth: availableWidth

        ColumnLayout {
            x: Math.max(20, (root.width - width) / 2)
            width: Math.min(root.width - 40, 540)
            spacing: 18

            Item { Layout.preferredHeight: 12 }

            Label {
                Layout.alignment: Qt.AlignHCenter
                text: "WINRISK"
                color: "#f2f4f8"
                font.pixelSize: 48
                font.bold: true
                font.letterSpacing: 4
            }

            Label {
                Layout.alignment: Qt.AlignHCenter
                text: "Native C++ / Qt Quick"
                color: "#9aa7b4"
                font.pixelSize: 15
            }

            Frame {
                Layout.fillWidth: true
                padding: 20

                ColumnLayout {
                    anchors.fill: parent
                    spacing: 14

                    Label { text: "Map"; font.bold: true }
                    ComboBox {
                        id: mapChoice
                        Layout.fillWidth: true
                        textRole: "text"
                        valueRole: "value"
                        model: [
                            { text: "World", value: "world" },
                            { text: "Pangaea (~250 Ma)", value: "pangaea" },
                            { text: "Laurasia (~150 Ma)", value: "laurasia" },
                            { text: "Gondwana (~420 Ma)", value: "gondwana" },
                            { text: "Rodinia (~1 Ga)", value: "rodinia" },
                            { text: "Random / procedural", value: "random" }
                        ]
                    }

                    GridLayout {
                        Layout.fillWidth: true
                        visible: mapChoice.currentValue === "random"
                        columns: root.width < 480 ? 1 : 2
                        columnSpacing: 10
                        rowSpacing: 6

                        ColumnLayout {
                            Layout.fillWidth: true
                            Label { text: "Territories"; font.bold: true }
                            SpinBox {
                                id: randomFields
                                Layout.fillWidth: true
                                from: Math.max(3, playerCount.value)
                                to: 100
                                value: 30
                            }
                        }

                        ColumnLayout {
                            Layout.fillWidth: true
                            Label { text: "Regions"; font.bold: true }
                            SpinBox {
                                id: randomContinents
                                Layout.fillWidth: true
                                from: 1
                                to: Math.min(12, randomFields.value)
                                value: Math.min(6, to)
                            }
                        }
                    }

                    Label {
                        Layout.fillWidth: true
                        visible: mapChoice.currentValue === "random"
                        text: "Each new game generates a connected board from a fresh seed. The generated topology is encoded in the save identity so quick-save/load recreates the same board."
                        color: "#9aa7b4"
                        wrapMode: Text.Wrap
                    }

                    Label { text: "Game mode"; font.bold: true }
                    ComboBox {
                        id: gameMode
                        Layout.fillWidth: true
                        model: ["Classic", "Secret Mission", "Capital"]
                        onCurrentIndexChanged: {
                            if (playerCount.value < playerCount.from)
                                playerCount.value = playerCount.from
                        }
                    }

                    Label { text: "Players"; font.bold: true }
                    SpinBox {
                        id: playerCount
                        Layout.fillWidth: true
                        from: gameMode.currentIndex === 0 ? 2 : 3
                        to: 5
                        value: 4
                    }

                    Label { text: "Human players"; font.bold: true }
                    SpinBox {
                        id: humanPlayers
                        Layout.fillWidth: true
                        from: 1
                        to: playerCount.value
                        value: 1
                    }

                    Label {
                        Layout.fillWidth: true
                        visible: gameMode.currentIndex !== 0
                        text: gameMode.currentIndex === 1
                              ? "Each player receives a hidden objective. First player to complete their mission wins."
                              : "Each player has an original headquarters. Capture every HQ while retaining your own to win."
                        color: "#9aa7b4"
                        wrapMode: Text.Wrap
                    }

                    Label {
                        Layout.fillWidth: true
                        visible: gameMode.currentIndex === 1 && mapChoice.currentValue !== "world"
                        text: "Java parity note: non-world boards keep the original mission deck. On smaller boards, some fixed 24/18/15-territory objectives can be impossible."
                        color: "#d7b46a"
                        wrapMode: Text.Wrap
                    }

                    Label { text: "Optional rules"; font.bold: true }

                    GridLayout {
                        Layout.fillWidth: true
                        columns: root.width < 480 ? 1 : 2
                        columnSpacing: 10
                        rowSpacing: 2

                        CheckBox { id: incrementalCards; text: "Incremental card values" }
                        CheckBox { id: expandedManeuver; text: "Expanded maneuver" }
                        CheckBox { id: attackCardReroll; text: "Attack-card reroll" }
                        CheckBox { id: commanderDie; text: "Commander die" }
                        CheckBox { id: attackWithAll; text: "Attack with all" }
                        CheckBox { id: fogOfWar; text: "Fog of war" }
                        CheckBox { id: skynet; text: "Skynet" }
                    }

                    Label {
                        Layout.fillWidth: true
                        visible: fogOfWar.checked
                        text: "Fog shows your territories and their immediate neighbors. During AI turns the board remains scoped to the last human viewer."
                        color: "#9aa7b4"
                        wrapMode: Text.Wrap
                    }

                    Label {
                        Layout.fillWidth: true
                        visible: skynet.checked
                        text: "Skynet preserves Java behavior: AI scale calculations double Java-interactive fields, and EasyAI prefers adjacent interactive targets. In two-player Classic, Java Neutral is also classified as interactive."
                        color: "#9aa7b4"
                        wrapMode: Text.Wrap
                    }

                    Button {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 52
                        text: "New Game"
                        font.bold: true
                        onClicked: {
                            const selectedMap = mapChoice.currentValue === "random"
                                ? "random:" + randomFields.value + ":" + randomContinents.value + ":" + Math.floor(Date.now())
                                : mapChoice.currentValue
                            appController.startNewGame(
                                playerCount.value,
                                humanPlayers.value,
                                gameMode.currentIndex,
                                incrementalCards.checked,
                                expandedManeuver.checked,
                                attackCardReroll.checked,
                                commanderDie.checked,
                                attackWithAll.checked,
                                fogOfWar.checked,
                                skynet.checked,
                                selectedMap
                            )
                        }
                    }

                    Button {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 48
                        text: "Continue Quick Save"
                        onClicked: appController.quickLoad()
                    }

                    Label {
                        Layout.fillWidth: true
                        text: appController.status
                        color: "#d5dde5"
                        wrapMode: Text.Wrap
                        horizontalAlignment: Text.AlignHCenter
                        visible: text.length > 0
                    }
                }
            }

            Item { Layout.preferredHeight: 18 }
        }
    }
}
