import QtQuick
import QtQuick.Controls

Item {
    id: root
    property bool portraitLayout: width < 900

    Rectangle {
        anchors.fill: parent
        color: "#0d1319"
    }

    ScrollView {
        id: panelScroll
        anchors.right: parent.right
        anchors.bottom: parent.bottom
        anchors.left: root.portraitLayout ? parent.left : undefined
        anchors.top: root.portraitLayout ? undefined : parent.top
        width: root.portraitLayout ? parent.width : Math.min(340, parent.width * 0.32)
        height: root.portraitLayout ? Math.max(250, parent.height * 0.42) : parent.height
        clip: true
        contentWidth: availableWidth

        ControlPanel {
            width: panelScroll.availableWidth
            height: Math.max(panelScroll.availableHeight, 620)
        }
    }

    BoardPaneV6 {
        anchors.left: parent.left
        anchors.top: parent.top
        anchors.right: root.portraitLayout ? parent.right : panelScroll.left
        anchors.bottom: root.portraitLayout ? panelScroll.top : parent.bottom
        anchors.margins: 8
    }
}
