#pragma once

#include "engine/GameEngine.hpp"

#include <QAbstractListModel>
#include <QColor>
#include <QObject>
#include <QTimer>

class BoardModel final : public QAbstractListModel {
    Q_OBJECT

public:
    enum Role {
        TerritoryIdRole = Qt::UserRole + 1,
        TerritoryNameRole,
        XRole,
        YRole,
        ArmiesRole,
        OwnerIdRole,
        OwnerColorRole,
        HeadquartersOwnerIdRole,
        SelectedRole
    };
    Q_ENUM(Role)

    explicit BoardModel(QObject* parent = nullptr);

    int rowCount(const QModelIndex& parent = QModelIndex()) const override;
    QVariant data(const QModelIndex& index, int role) const override;
    QHash<int, QByteArray> roleNames() const override;

    void setEngine(const winrisk::GameEngine* engine);
    void setSelectedId(int selectedId);
    void refresh();

private:
    const winrisk::GameEngine* engine_ = nullptr;
    int selectedId_ = -1;
};

class AppController final : public QObject {
    Q_OBJECT
    Q_PROPERTY(bool running READ running NOTIFY stateChanged)
    Q_PROPERTY(QString modeText READ modeText NOTIFY stateChanged)
    Q_PROPERTY(QString phaseText READ phaseText NOTIFY stateChanged)
    Q_PROPERTY(QString currentPlayerText READ currentPlayerText NOTIFY stateChanged)
    Q_PROPERTY(QColor currentPlayerColor READ currentPlayerColor NOTIFY stateChanged)
    Q_PROPERTY(QString missionText READ missionText NOTIFY stateChanged)
    Q_PROPERTY(int reinforcements READ reinforcements NOTIFY stateChanged)
    Q_PROPERTY(int cardCount READ cardCount NOTIFY stateChanged)
    Q_PROPERTY(QString cardsText READ cardsText NOTIFY stateChanged)
    Q_PROPERTY(bool canTradeCards READ canTradeCards NOTIFY stateChanged)
    Q_PROPERTY(bool mustTradeCards READ mustTradeCards NOTIFY stateChanged)
    Q_PROPERTY(int nextTradeValue READ nextTradeValue NOTIFY stateChanged)
    Q_PROPERTY(qulonglong turn READ turn NOTIFY stateChanged)
    Q_PROPERTY(QString status READ status NOTIFY stateChanged)
    Q_PROPERTY(QString winnerText READ winnerText NOTIFY stateChanged)
    Q_PROPERTY(BoardModel* boardModel READ boardModel CONSTANT)

public:
    explicit AppController(QObject* parent = nullptr);

    bool running() const;
    QString modeText() const;
    QString phaseText() const;
    QString currentPlayerText() const;
    QColor currentPlayerColor() const;
    QString missionText() const;
    int reinforcements() const;
    int cardCount() const;
    QString cardsText() const;
    bool canTradeCards() const;
    bool mustTradeCards() const;
    int nextTradeValue() const;
    qulonglong turn() const;
    QString status() const;
    QString winnerText() const;
    BoardModel* boardModel();

    Q_INVOKABLE bool startNewGame(int playerCount, int humanPlayers, int gameMode);
    Q_INVOKABLE void territoryTapped(int territoryId);
    Q_INVOKABLE bool tradeCards();
    Q_INVOKABLE bool endPhase();
    Q_INVOKABLE bool quickSave();
    Q_INVOKABLE bool quickLoad();
    Q_INVOKABLE void returnToMenu();

signals:
    void stateChanged();

private:
    winrisk::GameEngine engine_;
    BoardModel boardModel_;
    QTimer aiTimer_;
    QString status_;
    int selectedId_ = -1;

    void refresh();
    void setSelected(int territoryId);
    void scheduleAi();
    void runAiStep();
    QString quickSavePath() const;
    bool saveSnapshot(const winrisk::Snapshot& snapshot, const QString& path);
    bool loadSnapshot(const QString& path, winrisk::Snapshot& snapshot, QString& error);
};
