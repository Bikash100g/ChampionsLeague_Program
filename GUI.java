// GUI.java
// Single-file JavaFX GUI that REPLACES the console menu from Part 1.
// Reuses: League, Team, Match, FileStore (must be on the same classpath/module).
// No FXML, no extra source files.

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GUI extends Application {

    // ---- Part 1 reused objects ----
    private final League league = new League();
    private final FileStore store = new FileStore();

    // ---- UI state ----
    private boolean unsavedChanges = false;

    // Main containers
    private BorderPane root;
    private StackPane centerStack;
    private Label statusLabel;

    // Reusable panes
    private Pane addTeamPane;
    private Pane viewTeamsPane;
    private Pane recordMatchPane;
    private Pane tablePane;

    // Controls used across panes
    private TableView<Team> teamsTable;       // left list for View Teams
    private TableView<Team> standingsTable;   // league table
    private final ObservableList<Team> teamsObs = FXCollections.observableArrayList();

    // Formatter
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        // Build shell
        root = new BorderPane();
        root.setTop(buildTopBar());
        centerStack = new StackPane();
        centerStack.setPadding(new Insets(10));
        root.setCenter(centerStack);
        root.setBottom(buildStatusBar());

        // Build panes
        addTeamPane     = buildAddTeamPane();
        viewTeamsPane   = buildViewTeamsPane();
        recordMatchPane = buildRecordMatchPane();
        tablePane       = buildTablePane();

        // Load files safely
        safeLoad();

        // Default center
        showPane(addTeamPane);

        Scene scene = new Scene(root, 980, 620);
        stage.setTitle("League Manager (JavaFX) – Part 2");
        stage.setScene(scene);

        // Confirm save on close if dirty (optional)
        stage.setOnCloseRequest(e -> {
            if (unsavedChanges) {
                e.consume();
                boolean cont = confirmSaveIfDirtyThen(stage::close);
                if (cont) stage.close();
            }
        });

        stage.show();
    }

    // ----------------------- Top Area (Toolbar) -----------------------

    private Pane buildTopBar() {
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(10));
        bar.setAlignment(Pos.CENTER_LEFT);

        Button btnAddTeam     = new Button("Add Team");
        Button btnViewTeams   = new Button("View Teams");
        Button btnRecordMatch = new Button("Record Match");
        Button btnShowTable   = new Button("Show Table");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSave = new Button("Save");
        Button btnExit = new Button("Exit");

        btnAddTeam.setOnAction(e -> showPane(addTeamPane));
        btnViewTeams.setOnAction(e -> {
            refreshTeamsTable();
            showPane(viewTeamsPane);
        });
        btnRecordMatch.setOnAction(e -> {
            refreshTeamPickersInRecordPane();
            showPane(recordMatchPane);
        });
        btnShowTable.setOnAction(e -> {
            refreshStandingsTable();
            showPane(tablePane);
        });

        btnSave.setOnAction(e -> saveToDisk());
        btnExit.setOnAction(e -> {
            if (unsavedChanges) {
                if (!confirmSaveIfDirtyThen(Platform::exit)) return;
            }
            Platform.exit();
        });

        bar.getChildren().addAll(
                btnAddTeam, btnViewTeams, btnRecordMatch, btnShowTable,
                spacer, btnSave, btnExit
        );
        return new VBox(bar, new Separator());
    }

    private Pane buildStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(6, 10, 6, 10));
        statusLabel = new Label("Ready.");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    private void showPane(Pane pane) { centerStack.getChildren().setAll(pane); }

    // ----------------------- Add Team Pane -----------------------

    private Pane buildAddTeamPane() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10));

        Label header = new Label("Add New Team");
        header.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        TextField tfName = new TextField();
        tfName.setPromptText("Team name (mandatory) – e.g., Manchester FC");

        TextField tfOrigin = new TextField();
        tfOrigin.setPromptText("Team origin / country (mandatory)");

        // Enter players now?
        ToggleGroup tg = new ToggleGroup();
        RadioButton rbYes = new RadioButton("Enter players now");
        RadioButton rbNo  = new RadioButton("Keep default players (Player 1..16)");
        rbNo.setSelected(true);
        rbYes.setToggleGroup(tg);
        rbNo.setToggleGroup(tg);

        GridPane playersGrid = new GridPane();
        playersGrid.setHgap(8);
        playersGrid.setVgap(6);
        playersGrid.setPadding(new Insets(6));
        playersGrid.setDisable(true); // disabled until Yes is selected

        List<TextField> playerFields = IntStream.rangeClosed(1, 16)
                .mapToObj(i -> {
                    TextField tf = new TextField();
                    tf.setPromptText("Player " + i);
                    return tf;
                })
                .collect(Collectors.toList());

        for (int i = 0; i < playerFields.size(); i++) {
            int row = i / 2;
            int col = (i % 2) * 2;
            playersGrid.add(new Label("Player " + (i + 1) + ":"), col, row);
            playersGrid.add(playerFields.get(i), col + 1, row);
        }

        rbYes.setOnAction(e -> playersGrid.setDisable(false));
        rbNo.setOnAction(e -> playersGrid.setDisable(true));

        HBox buttons = new HBox(10);
        Button btnAdd = new Button("Add Team");
        Button btnReset = new Button("Reset");
        buttons.getChildren().addAll(btnAdd, btnReset);

        btnReset.setOnAction(e -> {
            tfName.clear();
            tfOrigin.clear();
            tg.selectToggle(rbNo);
            playersGrid.setDisable(true);
            playerFields.forEach(TextField::clear);
            setStatus("Form cleared.");
        });

        btnAdd.setOnAction(e -> {
            String rawName   = tfName.getText()   == null ? "" : tfName.getText().trim();
            String rawOrigin = tfOrigin.getText() == null ? "" : tfOrigin.getText().trim();

            if (rawName.isEmpty()) {
                message("Validation", "Team name cannot be empty.", Alert.AlertType.WARNING);
                tfName.requestFocus();
                return;
            }
            if (rawOrigin.isEmpty()) {
                message("Validation", "Origin cannot be empty.", Alert.AlertType.WARNING);
                tfOrigin.requestFocus();
                return;
            }

            String name   = formatTeamName(rawName);
            String origin = capitalize(rawOrigin);

            List<String> players;
            if (rbNo.isSelected()) {
                players = defaultPlayers();
            } else {
                players = IntStream.rangeClosed(1, 16)
                        .mapToObj(i -> {
                            String s = playerFields.get(i - 1).getText();
                            s = (s == null) ? "" : s.trim();
                            return s.isEmpty() ? ("Player " + i) : capitalize(s);
                        })
                        .collect(Collectors.toList());
            }

            boolean ok = league.addTeam(name, origin, players);
            if (!ok) {
                message("Add Team", "A team with this name already exists.", Alert.AlertType.ERROR);
                return;
            }
            setDirty(true);
            setStatus("Team added: " + name + " (" + origin + ")");

            // Save? prompt
            promptSaveAfterModify();

            // refresh open lists/tables
            refreshTeamsTable();
            refreshStandingsTable();
        });

        VBox radios = new VBox(4, rbYes, rbNo);
        box.getChildren().addAll(
                header,
                new Label("Team name:"), tfName,
                new Label("Team origin / country:"), tfOrigin,
                radios,
                new TitledPane("Players (16)", playersGrid),
                buttons
        );

        return box;
    }

    // ----------------------- View Teams Pane -----------------------

    // ----------------------- View Teams Pane -----------------------

    private Label perfPlayed, perfWon, perfDrawn, perfLost, perfGF, perfGA, perfGD, perfPts;
    private Label originLabel;
    private ListView<String> playersList;

    private Pane buildViewTeamsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        // Left: teams table (name only)
        teamsTable = new TableView<>();
        TableColumn<Team, String> cName = new TableColumn<>("Team");
        cName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getName()));
        cName.setPrefWidth(220);
        teamsTable.getColumns().add(cName);
        refreshTeamsTable();

        // Remove button under the table
        Button btnRemoveSelected = new Button("Remove Selected Team");
        btnRemoveSelected.setMaxWidth(Double.MAX_VALUE);
        btnRemoveSelected.setOnAction(e -> handleRemoveSelectedTeam());

        VBox leftBox = new VBox(8, teamsTable, btnRemoveSelected);
        VBox.setVgrow(teamsTable, Priority.ALWAYS);

        // Right: TabPane with reports
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Performance tab
        GridPane perf = new GridPane();
        perf.setHgap(10); perf.setVgap(6); perf.setPadding(new Insets(8));
        perfPlayed = new Label(); perfWon = new Label(); perfDrawn = new Label(); perfLost = new Label();
        perfGF = new Label(); perfGA = new Label(); perfGD = new Label(); perfPts = new Label();
        int r = 0;
        perf.add(row("Played:", perfPlayed), 0, r++);
        perf.add(row("Won:",    perfWon),    0, r++);
        perf.add(row("Drawn:",  perfDrawn),  0, r++);
        perf.add(row("Lost:",   perfLost),   0, r++);
        perf.add(row("Goals For (GF):", perfGF), 0, r++);
        perf.add(row("Goals Against (GA):", perfGA), 0, r++);
        perf.add(row("Goal Difference (GD):", perfGD), 0, r++);
        perf.add(row("Points:", perfPts), 0, r++);

        // Origin tab
        VBox originBox = new VBox(8);
        originBox.setPadding(new Insets(8));
        originLabel = new Label("-");
        originBox.getChildren().addAll(new Label("Origin / Country:"), originLabel);

        // Players tab
        VBox playersBox = new VBox(8);
        playersBox.setPadding(new Insets(8));
        playersList = new ListView<>();
        playersBox.getChildren().addAll(new Label("Players:"), playersList);

        tabs.getTabs().addAll(
                new Tab("Performance", perf),
                new Tab("Origin", originBox),
                new Tab("Players", playersBox)
        );

        // Selection listener
        teamsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, team) -> {
            if (team != null) populateTeamDetails(team);
        });

        // Layout
        pane.setLeft(leftBox);
        BorderPane.setMargin(leftBox, new Insets(0, 10, 0, 0));
        pane.setCenter(tabs);

        return pane;
    }

    private HBox row(String label, Label value) {
        Label l = new Label(label);
        l.setMinWidth(170);
        return new HBox(10, l, value);
    }

    private void populateTeamDetails(Team t) {
        perfPlayed.setText(String.valueOf(t.getPlayed()));
        perfWon.setText(String.valueOf(t.getWon()));
        perfDrawn.setText(String.valueOf(t.getDrawn()));
        perfLost.setText(String.valueOf(t.getLost()));
        perfGF.setText(String.valueOf(t.getGoalsFor()));
        perfGA.setText(String.valueOf(t.getGoalsAgainst()));
        perfGD.setText(String.valueOf(t.getGoalDifference()));
        perfPts.setText(String.valueOf(t.getPoints()));

        originLabel.setText(t.getOrigin());
        playersList.getItems().setAll(t.getPlayers());
    }

    // ----------------------- Record Match Pane -----------------------

    private ComboBox<String> cbHome, cbAway;
    private DatePicker dpDate;
    private Spinner<Integer> spHomeGoals, spAwayGoals;

    private Pane buildRecordMatchPane() {
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.setPadding(new Insets(10));

        dpDate = new DatePicker(LocalDate.now());
        dpDate.setConverter(new StringConverter<LocalDate>() {
            @Override public String toString(LocalDate date) {
                return (date == null) ? "" : DATE_FMT.format(date);
            }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.trim().isEmpty()) return null;
                try { return LocalDate.parse(s.trim(), DATE_FMT); }
                catch (DateTimeParseException ex) { return null; }
            }
        });

        cbHome = new ComboBox<>();
        cbAway = new ComboBox<>();
        refreshTeamPickersInRecordPane();

        spHomeGoals = new Spinner<>(0, 999, 0);
        spAwayGoals = new Spinner<>(0, 999, 0);
        spHomeGoals.setEditable(true);
        spAwayGoals.setEditable(true);

        Button btnRecord = new Button("Record Result");
        btnRecord.setOnAction(e -> handleRecordMatch());

        int r = 0;
        g.add(new Label("Date (yyyy-MM-dd):"), 0, r); g.add(dpDate, 1, r++);
        g.add(new Label("Home Team:"), 0, r); g.add(cbHome, 1, r++);
        g.add(new Label("Away Team:"), 0, r); g.add(cbAway, 1, r++);
        g.add(new Label("Home Goals:"), 0, r); g.add(spHomeGoals, 1, r++);
        g.add(new Label("Away Goals:"), 0, r); g.add(spAwayGoals, 1, r++);
        g.add(btnRecord, 1, r);

        return g;
    }

    private void handleRecordMatch() {
        try {
            LocalDate d = dpDate.getValue();
            if (d == null) {
                message("Validation", "Please enter a valid date (yyyy-MM-dd).", Alert.AlertType.WARNING);
                return;
            }
            String dateStr = DATE_FMT.format(d);

            String home = cbHome.getValue();
            String away = cbAway.getValue();
            if (home == null || away == null) {
                message("Validation", "Please choose both home and away teams.", Alert.AlertType.WARNING);
                return;
            }
            if (home.equalsIgnoreCase(away)) {
                message("Validation", "Home and away teams must be different.", Alert.AlertType.WARNING);
                return;
            }
            if (league.getTeam(home) == null) {
                message("Validation", "Unknown home team: " + home, Alert.AlertType.WARNING);
                return;
            }
            if (league.getTeam(away) == null) {
                message("Validation", "Unknown away team: " + away, Alert.AlertType.WARNING);
                return;
            }

            int hg = spHomeGoals.getValue();
            int ag = spAwayGoals.getValue();
            if (hg < 0 || ag < 0) {
                message("Validation", "Goals must be non-negative.", Alert.AlertType.WARNING);
                return;
            }

            Match m = new Match(dateStr, home, away, hg, ag);
            if (league.recordMatch(m)) {
                setDirty(true);
                setStatus("Result recorded: " + home + " " + hg + " - " + ag + " " + away);
                refreshStandingsTable();
                promptSaveAfterModify();
            } else {
                message("Record Match", "Failed to record match. Check team names.", Alert.AlertType.ERROR);
            }
        } catch (Exception ex) {
            message("Record Match", "Unexpected error: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void refreshTeamPickersInRecordPane() {
        List<String> names = league.getTeams().stream()
                .map(Team::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        cbHome.getItems().setAll(names);
        cbAway.getItems().setAll(names);
        cbHome.setValue(names.isEmpty() ? null : names.get(0));
        cbAway.setValue(names.size() > 1 ? names.get(1) : null);
    }

    // ----------------------- Table Pane (Standings) -----------------------

    private Pane buildTablePane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        standingsTable = new TableView<>();
        // Widely supported (works on JavaFX 8 too)
        standingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Team, String> cName = new TableColumn<>("Team");
        cName.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getName()));

        TableColumn<Team, Integer> cP   = intCol("P",   Team::getPlayed);
        TableColumn<Team, Integer> cW   = intCol("W",   Team::getWon);
        TableColumn<Team, Integer> cD   = intCol("D",   Team::getDrawn);
        TableColumn<Team, Integer> cL   = intCol("L",   Team::getLost);
        TableColumn<Team, Integer> cGF  = intCol("GF",  Team::getGoalsFor);
        TableColumn<Team, Integer> cGA  = intCol("GA",  Team::getGoalsAgainst);
        TableColumn<Team, Integer> cGD  = intCol("GD",  Team::getGoalDifference);
        TableColumn<Team, Integer> cPts = intCol("Pts", Team::getPoints);

        standingsTable.getColumns().addAll(cName, cP, cW, cD, cL, cGF, cGA, cGD, cPts);

        refreshStandingsTable();

        box.getChildren().addAll(new Label("League Table"), standingsTable);
        return box;
    }

    private TableColumn<Team, Integer> intCol(String title, java.util.function.ToIntFunction<Team> getter) {
        TableColumn<Team, Integer> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(getter.applyAsInt(data.getValue())));
        col.setPrefWidth(56);
        return col;
    }

    private void refreshStandingsTable() {
        List<Team> standings = league.getStandings(); // already sorted via Team::compareTo
        standingsTable.getItems().setAll(standings);
    }

    // ----------------------- Teams list (left of View Teams) -----------------------

    private void refreshTeamsTable() {
        List<Team> list = league.getTeams().stream()
                .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        teamsObs.setAll(list);
        if (teamsTable != null) {
            teamsTable.getItems().setAll(teamsObs);
            if (!teamsObs.isEmpty()) {
                teamsTable.getSelectionModel().selectFirst();
                populateTeamDetails(teamsObs.get(0)); // Java 8 friendly (no getFirst())
            } else {
                // Clear details if nothing selected
                if (perfPlayed != null) {
                    perfPlayed.setText(""); perfWon.setText(""); perfDrawn.setText(""); perfLost.setText("");
                    perfGF.setText(""); perfGA.setText(""); perfGD.setText(""); perfPts.setText("");
                    if (originLabel != null) originLabel.setText("-");
                    if (playersList != null) playersList.getItems().clear();
                }
            }
        }
    }

    private void handleRemoveSelectedTeam() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            message("Remove Team", "Please select a team to remove.", Alert.AlertType.INFORMATION);
            return;
        }

        // Confirm removal
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm Removal");
        a.setHeaderText("Remove team: " + selected.getName() + "?");
        a.setContentText("This will remove the team from the league. Matches already recorded remain unchanged.");
        ButtonType REMOVE = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        ButtonType CANCEL = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(REMOVE, CANCEL);

        Optional<ButtonType> res = a.showAndWait();
        if (!res.isPresent() || res.get() == CANCEL) {
            return; // user cancelled
        }

        boolean ok = league.removeTeam(selected.getName());
        if (!ok) {
            message("Remove Team", "Failed to remove team (not found).", Alert.AlertType.ERROR);
            return;
        }

        setDirty(true);
        setStatus("Removed team: " + selected.getName());

        // Refresh UI
        refreshTeamsTable();
        refreshStandingsTable();

        // Prompt to save (consistent with other modifying actions)
        promptSaveAfterModify();
    }

    // ----------------------- Save / Load -----------------------

    private void setDirty(boolean dirty) {
        unsavedChanges = dirty;
        setStatus(dirty ? "Unsaved changes." : "Saved.");
    }

    private void promptSaveAfterModify() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Save Changes");
        a.setHeaderText("Save changes to disk?");
        a.setContentText("Choose Save to write to files, or Don’t Save to keep changes only in memory.");
        ButtonType SAVE = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType DONT = new ButtonType("Don’t Save", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(SAVE, DONT);
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == SAVE) {
            saveToDisk();
        } else {
            setStatus("Changes kept in memory.");
        }
    }

    // *** Java 8–friendly version (no Optional.isEmpty()) ***
    private boolean confirmSaveIfDirtyThen(Runnable afterSaveOrSkip) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Unsaved Changes");
        a.setHeaderText("You have unsaved changes. Save before exiting?");
        ButtonType SAVE   = new ButtonType("Save",       ButtonBar.ButtonData.YES);
        ButtonType NO     = new ButtonType("Don’t Save", ButtonBar.ButtonData.NO);
        ButtonType CANCEL = new ButtonType("Cancel",     ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(SAVE, NO, CANCEL);

        Optional<ButtonType> res = a.showAndWait();
        if (!res.isPresent() || res.get() == CANCEL) {
            return false;
        }
        if (res.get() == SAVE) {
            saveToDisk();
        }
        afterSaveOrSkip.run();
        return true;
    }

    private void saveToDisk() {
        try {
            store.saveTeams(league.getStandings());
            store.saveMatches(league.getMatches());
            setDirty(false);
            setStatus("Saved.");
        } catch (IOException e) {
            message("Save Error", "Error saving files: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void safeLoad() {
        try {
            Map<String, Team> loadedTeams = store.loadTeams();
            for (Team t : loadedTeams.values()) {
                league.addTeam(t.getName(), t.getOrigin(), t.getPlayers());
            }
            league.getMatches().addAll(store.loadMatches());
            league.rebuildTableFromMatches();
            setDirty(false);
            refreshTeamsTable();
            refreshStandingsTable();
            setStatus("Loaded existing data.");
        } catch (java.io.EOFException e) {
            setStatus("Warning: Save file appears truncated. Starting fresh.");
        } catch (java.io.StreamCorruptedException e) {
            setStatus("Warning: Save file is corrupted or incompatible. Starting fresh.");
        } catch (java.io.InvalidClassException e) {
            setStatus("Warning: Saved data version incompatible with current classes. Starting fresh.");
        } catch (ClassCastException e) {
            setStatus("Warning: Data format invalid. Starting fresh.");
        } catch (IOException e) {
            setStatus("No previous data found (or general load issue). Starting fresh.");
        }
    }

    // ----------------------- Helpers -----------------------

    private List<String> defaultPlayers() {
        return IntStream.rangeClosed(1, 16)
                .mapToObj(i -> "Player " + i)
                .collect(Collectors.toList());
    }

    /** Ensures:
     *  - First letter of the entire name is uppercase
     *  - Any word equal to "fc" becomes "FC"
     */
    private String formatTeamName(String name) {
        if (name == null || name.isEmpty()) return name;
        String[] tokens = name.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("fc")) tokens[i] = "FC";
        }
        String joined = String.join(" ", tokens);
        char first = Character.toUpperCase(joined.charAt(0));
        String rest = joined.length() > 1 ? joined.substring(1) : "";
        return first + rest;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private void message(String title, String content, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
