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
