import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            GradientStop { position: 0.0; color: "#17212b" }
            GradientStop { position: 1.0; color: "#0d1319" }
        }
    }

    ColumnLayout {
        anchors.centerIn: parent
        width: Math.min(parent.width - 40, 520)
        spacing: 18

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

                Label { text: "Players"; font.bold: true }
                SpinBox {
                    id: playerCount
                    Layout.fillWidth: true
                    from: 2
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

                Button {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 52
                    text: "New Game"
                    font.bold: true
                    onClicked: appController.startNewGame(playerCount.value, humanPlayers.value)
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
    }
}
