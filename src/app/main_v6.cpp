#include "qt/AppController.hpp"

#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>

int main(int argc, char* argv[]) {
    QGuiApplication app(argc, argv);
    QCoreApplication::setOrganizationName("WinRisk");
    QCoreApplication::setApplicationName("WinRisk");
    QQuickStyle::setStyle("Fusion");

    AppController controller;
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("appController", &controller);
    engine.loadFromModule("WinRisk", "MainV6");
    if (engine.rootObjects().isEmpty()) {
        return 1;
    }
    return app.exec();
}
