import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

Item {
    id: root

    Rectangle {
        anchors.fill: parent
        color: "#0d141b"
    }

    Loader {
        anchors.fill: parent
        anchors.margins: width < 760 ? 8 : 14
        sourceComponent: width < 760 ? phoneLayout : desktopLayout
    }

    Component {
        id: desktopLayout
        RowLayout {
            spacing: 12

            BoardPane {
                Layout.fillWidth: true
                Layout.fillHeight: true
            }

            ControlPanel {
                Layout.preferredWidth: Math.max(280, Math.min(340, parent.width * 0.28))
                Layout.fillHeight: true
            }
        }
    }

    Component {
        id: phoneLayout
        ColumnLayout {
            spacing: 8

            BoardPane {
                Layout.fillWidth: true
                Layout.fillHeight: true
                Layout.minimumHeight: 360
            }

            ControlPanel {
                Layout.fillWidth: true
                Layout.preferredHeight: Math.min(300, parent.height * 0.37)
            }
        }
    }
}
