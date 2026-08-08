package com.winrisk.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.winrisk.game.data.GamePhase;
import com.winrisk.game.data.Params;
import com.winrisk.game.map.BuiltinMaps;
import com.winrisk.game.object.Field;
import com.winrisk.game.object.Player;
import com.winrisk.game.rules.CombatResult;
import com.winrisk.game.rules.RiskCard;
import com.winrisk.game.view.Game;

import java.util.Arrays;
import java.util.List;

public final class WinRiskApplication extends ApplicationAdapter {
    private static final float AI_STEP_SECONDS = 0.35f;

    private Stage stage;
    private Skin skin;
    private BoardActor board;
    private Table root;
    private Table hud;
    private Label phaseLabel;
    private Label playerLabel;
    private Label reinforceLabel;
    private Label cardsLabel;
    private Label statusLabel;
    private TextButton placeAllButton;
    private TextButton tradeButton;
    private TextButton endPhaseButton;
    private Game game;
    private int selectedField = -1;
    private boolean placeAll;
    private float aiTimer;

    @Override
    public void create() {
        skin = createSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        buildHud();
        newGame();
        rebuildLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Skin createSkin() {
        Skin result = new Skin();
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        Texture white = new Texture(pixel);
        pixel.dispose();
        result.add("white", white);

        BitmapFont font = new BitmapFont();
        result.add("default-font", font);
        result.add("default", new Label.LabelStyle(font, Color.valueOf("ECF0F1")));

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = result.newDrawable("white", Color.valueOf("34495E"));
        buttonStyle.over = result.newDrawable("white", Color.valueOf("48678A"));
        buttonStyle.down = result.newDrawable("white", Color.valueOf("1B2631"));
        buttonStyle.checked = result.newDrawable("white", Color.valueOf("D68910"));
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.valueOf("ECF0F1");
        buttonStyle.checkedFontColor = Color.valueOf("1B2631");
        result.add("default", buttonStyle);
        return result;
    }

    private void buildHud() {
        hud = new Table();
        hud.setBackground(skin.newDrawable("white", Color.valueOf("26313C")));
        hud.defaults().pad(6f).growX();

        Label title = new Label("WINRISK", skin);
        title.setAlignment(Align.center);
        title.setFontScale(1.45f);
        hud.add(title).padTop(12f).row();

        phaseLabel = centeredLabel(1.15f);
        playerLabel = centeredLabel(1f);
        reinforceLabel = centeredLabel(1f);
        cardsLabel = centeredLabel(1f);
        hud.add(phaseLabel).row();
        hud.add(playerLabel).row();
        hud.add(reinforceLabel).row();
        hud.add(cardsLabel).row();

        placeAllButton = button("Place all: OFF", this::togglePlaceAll);
        tradeButton = button("Trade cards", this::tradeCards);
        endPhaseButton = button("End phase", this::endPhase);
        hud.add(placeAllButton).height(48f).row();
        hud.add(tradeButton).height(48f).row();
        hud.add(endPhaseButton).height(52f).row();

        Table persistence = new Table();
        persistence.defaults().pad(4f).growX();
        persistence.add(button("Save", this::saveGame)).height(44f);
        persistence.add(button("Load", this::loadGame)).height(44f);
        hud.add(persistence).row();
        hud.add(button("New game", this::newGame)).height(44f).row();

        statusLabel = new Label("", skin);
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.topLeft);
        hud.add(statusLabel).pad(10f).grow().top().row();
    }

    private Label centeredLabel(float scale) {
        Label label = new Label("", skin);
        label.setAlignment(Align.center);
        label.setFontScale(scale);
        return label;
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int mouseButton) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int mouseButton) {
                if (x >= 0f && y >= 0f && x <= button.getWidth() && y <= button.getHeight()) {
                    action.run();
                }
            }
        });
        return button;
    }

    private void newGame() {
        Params params = new Params();
        params.setHumanPlayers(1);
        params.setAiPlayers(3);
        params.setBuiltinMap(BuiltinMaps.WORLD);
        game = new Game(params);
        selectedField = -1;
        placeAll = false;
        aiTimer = 0f;
        if (board == null) {
            board = new BoardActor(game, this::fieldTapped, skin.getFont("default-font"));
        } else {
            board.setGame(game);
        }
        board.setSelectedField(-1);
        if (placeAllButton != null) {
            placeAllButton.setChecked(false);
            placeAllButton.setText("Place all: OFF");
        }
        status("New local game. No server is running.");
        updateHud();
        if (root != null) {
            rebuildLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    private void fieldTapped(int fieldIndex) {
        if (game == null || game.end()) {
            return;
        }
        if (!isHumanTurn()) {
            status("AI is playing.");
            return;
        }

        Field tapped = field(fieldIndex);
        Player human = game.getPlayer();
        switch (game.getPhase()) {
            case REINFORCE:
                if (tapped.getPlayer() != human) {
                    status("Reinforcements can only be placed on your territory.");
                    return;
                }
                if (placeAll) {
                    game.onLongClick(tapped);
                } else {
                    game.onShortClick(tapped);
                }
                status(tapped.getDisplayName() + ": " + tapped.getArmy() + " armies");
                break;
            case ATTACK:
                handleAttackTap(tapped);
                break;
            case MOVE:
                handleMoveTap(tapped);
                break;
            default:
                break;
        }
        updateHud();
    }

    private void handleAttackTap(Field tapped) {
        Player human = game.getPlayer();
        if (selectedField < 0) {
            if (tapped.getPlayer() == human && tapped.getArmy() > 1) {
                select(tapped);
                status("Attack from " + tapped.getDisplayName() + ". Select an adjacent enemy.");
            }
            return;
        }

        Field source = field(selectedField);
        if (tapped == source) {
            clearSelection();
            return;
        }
        if (tapped.getPlayer() == human) {
            if (tapped.getArmy() > 1) {
                select(tapped);
            } else {
                clearSelection();
            }
            return;
        }
        if (!source.getNext().contains(tapped)) {
            status("Those territories are not adjacent.");
            clearSelection();
            return;
        }

        CombatResult result = game.attack(source, tapped);
        if (!result.isLegal()) {
            status("Illegal attack.");
            clearSelection();
            return;
        }
        status("Dice " + Arrays.toString(result.getAttackerDice())
                + " vs " + Arrays.toString(result.getDefenderDice())
                + " | losses " + result.getAttackerLosses() + "/" + result.getDefenderLosses()
                + (result.isCaptured() ? " | captured " + tapped.getDisplayName() : ""));
        if (source.getPlayer() != human || source.getArmy() <= 1) {
            clearSelection();
        }
    }

    private void handleMoveTap(Field tapped) {
        Player human = game.getPlayer();
        if (selectedField < 0) {
            if (tapped.getPlayer() == human && tapped.getArmy() > 1) {
                select(tapped);
                status("Move from " + tapped.getDisplayName() + ". Tap a connected territory.");
            }
            return;
        }

        Field source = field(selectedField);
        if (tapped == source) {
            clearSelection();
            return;
        }
        if (tapped.getPlayer() != human) {
            status("You can only maneuver between your territories.");
            clearSelection();
            return;
        }
        if (game.maneuver(source, tapped, 1)) {
            status("Moved 1 army to " + tapped.getDisplayName() + ". Tap again to move another.");
            if (source.getArmy() <= 1) {
                clearSelection();
            }
        } else {
            status("No legal maneuver route between those territories.");
            clearSelection();
        }
    }

    private void select(Field field) {
        selectedField = field.getFieldIndex();
        board.setSelectedField(selectedField);
    }

    private void clearSelection() {
        selectedField = -1;
        if (board != null) {
            board.setSelectedField(-1);
        }
    }

    private Field field(int index) {
        return game.getFields().get(index);
    }

    private void togglePlaceAll() {
        placeAll = !placeAll;
        placeAllButton.setChecked(placeAll);
        placeAllButton.setText(placeAll ? "Place all: ON" : "Place all: OFF");
    }

    private void tradeCards() {
        if (!isHumanTurn() || game.getPhase() != GamePhase.REINFORCE) {
            status("Cards can be traded during your reinforcement phase.");
            return;
        }
        Player player = game.getPlayer();
        List<List<RiskCard>> sets = game.getCardService().findTradeSets(player.getRiskCards());
        if (sets.isEmpty()) {
            status("No legal card set available.");
            return;
        }
        int value = game.getCardService().nextTradeValue();
        game.getCardService().trade(game, player, sets.get(0));
        status("Traded a card set for " + value + " reinforcements.");
        updateHud();
    }

    private void endPhase() {
        if (!isHumanTurn() || game.end()) {
            return;
        }
        clearSelection();
        game.next();
        aiTimer = 0f;
        updateHud();
    }

    private void saveGame() {
        try {
            Gdx.files.local("saves").mkdirs();
            String path = Gdx.files.local("saves/quick.save.json").file().getAbsolutePath();
            game.save(path);
            status("Saved locally as quick.save.json.");
        } catch (RuntimeException e) {
            status("Save failed: " + e.getMessage());
        }
    }

    private void loadGame() {
        try {
            if (!Gdx.files.local("saves/quick.save.json").exists()) {
                status("No quick save exists yet.");
                return;
            }
            String path = Gdx.files.local("saves/quick.save.json").file().getAbsolutePath();
            game = Game.loadGame(path);
            selectedField = -1;
            aiTimer = 0f;
            board.setGame(game);
            board.setSelectedField(-1);
            status("Loaded quick.save.json.");
            updateHud();
        } catch (RuntimeException e) {
            status("Load failed: " + e.getMessage());
        }
    }

    private boolean isHumanTurn() {
        return game != null && game.getPlayer().getPlayerInterface().isInteractive();
    }

    private int humanIndex() {
        if (game == null) {
            return -1;
        }
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getPlayerInterface().isInteractive()) {
                return i;
            }
        }
        return -1;
    }

    private void updateHud() {
        if (game == null) {
            return;
        }
        boolean over = game.end();
        phaseLabel.setText(over ? "GAME OVER" : game.getPhase().name());
        playerLabel.setText(over
                ? winnerText()
                : "Player " + (game.getCurPlayer() + 1) + (isHumanTurn() ? " — YOU" : " — AI"));
        reinforceLabel.setText(over ? "" : "Reinforcements: " + game.getPlayer().getCurrentReinforcements());
        int human = humanIndex();
        cardsLabel.setText(human < 0 ? "" : "Cards: " + game.getPlayers().get(human).getRiskCards().size());
        boolean canAct = !over && isHumanTurn();
        endPhaseButton.setDisabled(!canAct);
        placeAllButton.setDisabled(!canAct || game.getPhase() != GamePhase.REINFORCE);
        tradeButton.setDisabled(!canAct || game.getPhase() != GamePhase.REINFORCE);
        board.setGame(game);
        board.setSelectedField(selectedField);
    }

    private String winnerText() {
        Player winner = game.getWinner();
        if (winner == null) {
            return "Draw";
        }
        int index = game.getPlayers().indexOf(winner);
        return "Player " + (index + 1) + " wins — " + game.getWinReason();
    }

    private void status(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? "" : text);
        }
    }

    private void stepAi(float delta) {
        if (game == null || game.end() || isHumanTurn()) {
            aiTimer = 0f;
            return;
        }
        aiTimer += delta;
        if (aiTimer < AI_STEP_SECONDS) {
            return;
        }
        aiTimer = 0f;
        game.next();
        clearSelection();
        updateHud();
    }

    private void rebuildLayout(int width, int height) {
        if (root == null || board == null || hud == null) {
            return;
        }
        root.clearChildren();
        root.setFillParent(true);
        if (width >= height) {
            root.add(board).grow().minWidth(300f);
            root.add(hud).width(Math.min(340f, width * 0.36f)).growY();
        } else {
            root.add(board).grow().minHeight(320f).row();
            root.add(hud).growX().height(Math.min(360f, height * 0.40f));
        }
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.1f);
        stepAi(delta);
        Gdx.gl.glClearColor(0.055f, 0.075f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        rebuildLayout(width, height);
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (board != null) {
            board.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }
}
