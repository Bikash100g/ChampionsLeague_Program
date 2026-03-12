import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.*;

/**
 * Console UI for League Manager
 * - Uses binary persistence via FileStore
 * - Validates date inputs (yyyy-MM-dd)
 * - Formats team names:
 *      * First letter of the whole name must be capital
 *      * Any token equal to "fc" becomes "FC"
 * - After each modifying action: prompts "Save? (Y/N)" and returns to main menu
 */
public class MainMenu {

    private final Scanner sc = new Scanner(System.in);
    private final League league = new League();
    private final FileStore store = new FileStore();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        new MainMenu().run();
    }

    // --------------------------- Program Loop ---------------------------

    private void run() {
        load();

        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    addTeamUI();
                    break;
                case "2":
                    listTeamsUI();
                    break;
                case "3":
                    removeTeamUI();
                    break;
                case "4":
                    recordMatchUI();
                    break;
                case "5":
                    showTableUI();
                    break;
                case "6":
                    saveToDisk();
                    break; // note: no exit here, consistent with behavior
                case "0":
                    exitProgram();
                    return; // optional: explicit exit
                default:
                    System.out.println("Invalid option. Please choose 1-6 (or 0 to Exit).");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== League Manager ===");
        System.out.println("1) Add team");
        System.out.println("2) View Teams");
        System.out.println("3) Remove team");
        System.out.println("4) Record match result");
        System.out.println("5) Show league table");
        System.out.println("6) Save");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    // --------------------------- Add Team ---------------------------

    private void addTeamUI() {
        try {
            System.out.println("\n=== Add New Team ===");

            // 1) Team name (mandatory)
            String name;
            while (true) {
                System.out.print("Team name: ");
                name = safeReadLine().trim();
                if (!name.isEmpty()) break;
                System.out.println("Error: Team name cannot be empty.");
            }
            name = formatTeamName(name);

            // 2) Origin (mandatory)
            String origin;
            while (true) {
                System.out.print("Team origin / country : ");
                origin = sc.nextLine().trim();
                if (!origin.isEmpty()) break;
                System.out.println("Error: Origin cannot be empty.");
            }
            origin = capitalize(origin);

            // 3) Ask if the user wants to enter player names
            List<String> players;
            while (true) {
                System.out.print("Do you want to enter player names now? (Y/N): ");
                String ans = sc.nextLine().trim();
                if (ans.equalsIgnoreCase("y")) {
                    players = inputPlayersOrDefaults(); // prompts Player 1..16, Enter keeps default
                    break;
                } else if (ans.equalsIgnoreCase("n")) {
                    players = buildDefaultPlayers();     // Player 1..Player 16
                    break;
                } else {
                    System.out.println("Please enter Y or N.");
                }
            }

            // 4) Add to league
            if (league.addTeam(name, origin, players)) {
                System.out.println("Team added: " + name + " (" + origin + ")");
                postActionSavePrompt(); // Save? (Y/N) -> returns to main menu
            } else {
                System.out.println("Failed: A team with this name already exists.");
            }

        } catch (Exception e) {
            System.out.println("Error adding team: " + e.getMessage());
        }
    }


    // Build default list: ["Player 1", ..., "Player 16"]

    private List<String> buildDefaultPlayers() {
        return IntStream.rangeClosed(1, 16)
                .mapToObj(i -> "Player " + i)
                .collect(Collectors.toList());
    }


    // Prompt user for players; blank keeps default per player
    private List<String> inputPlayersOrDefaults() {
        System.out.println("\nEnter names for 16 players.");
        System.out.println("Press Enter to keep default name (Player 1 ... Player 16)\n");

        return IntStream.rangeClosed(1, 16)
                .mapToObj(i -> {
                    System.out.print("Player " + i + ": ");
                    String input = sc.nextLine().trim();
                    return input.isEmpty() ? ("Player " + i) : capitalize(input);
                })
                .collect(Collectors.toList());
    }


// --------------------------- List Teams + Submenu ---------------------------

    private void listTeamsUI() {
        Collection<Team> teams = league.getTeams();
        if (teams.isEmpty()) {
            System.out.println("\n(no teams yet)");
            return;
        }

        System.out.println("\n=== Teams List ===");
        league.getTeams().stream()
                .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(t -> System.out.println("- " + t.getName()));

        while (true) {
            System.out.println("\n=== Team Options ===");
            System.out.println("1) Team Performance");
            System.out.println("2) Team Origin");
            System.out.println("3) Team Players");
            System.out.println("4) Back");
            System.out.print("Choose: ");

            String choice = safeReadLine().trim();
            switch (choice) {
                case "1":
                    teamPerformanceUI();
                    break;
                case "2":
                    teamOriginUI();
                    break;
                case "3":
                    teamPlayersUI();
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Invalid choice.");
            }

            // After viewing any report (no data changes), we do NOT prompt to save.
            // If you want a save prompt here, call postActionSavePrompt();
        }
    }

    private void teamPerformanceUI() {
        Team t = promptTeamByName();
        if (t == null) return;

        System.out.println("\n=== Team Performance: " + t.getName() + " ===");
        System.out.println("Played:          " + t.getPlayed());
        System.out.println("Won:             " + t.getWon());
        System.out.println("Drawn:           " + t.getDrawn());
        System.out.println("Lost:            " + t.getLost());
        System.out.println("Goals For (GF):  " + t.getGoalsFor());
        System.out.println("Goals Against:   " + t.getGoalsAgainst());
        System.out.println("Goal Difference: " + t.getGoalDifference());
        System.out.println("Points:          " + t.getPoints());
    }

    private void teamOriginUI() {
        Team t = promptTeamByName();
        if (t == null) return;

        System.out.println("\nTeam: " + t.getName());
        System.out.println("Origin/Country: " + t.getOrigin());
    }

    private void teamPlayersUI() {
        Team t = promptTeamByName();
        if (t == null) return;

        System.out.println("\n=== Players of " + t.getName() + " ===");
        List<String> players = t.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            System.out.println((i + 1) + ". " + players.get(i));
        }
    }

    private Team promptTeamByName() {
        System.out.print("Enter team name: ");
        String name = sc.nextLine().trim();
        Team t = league.getTeam(name);
        if (t == null) System.out.println("Team not found.");
        return t;
    }

    // --------------------------- Remove Team ---------------------------

    private void removeTeamUI() {
        try {
            System.out.print("Enter exact team name to remove: ");
            String name = safeReadLine().trim();
            if (name.isEmpty()) {
                System.out.println("Team name cannot be empty.");
                return;
            }

            if (league.removeTeam(name)) {
                System.out.println("Removed: " + name);
                postActionSavePrompt();
            } else {
                System.out.println("Team not found.");
            }

        } catch (Exception e) {
            System.out.println("Error removing team: " + e.getMessage());
        }
    }

    // --------------------------- Record Match ---------------------------

    private void recordMatchUI() {
        try {
            System.out.print("Date (yyyy-MM-dd, blank for today): ");
            String sDate = safeReadLine().trim();
            LocalDate date;
            if (sDate.isEmpty()) {
                date = LocalDate.now();
            } else {
                try {
                    date = LocalDate.parse(sDate, DATE_FMT);
                } catch (DateTimeParseException ex) {
                    System.out.println("Invalid date. Please use format yyyy-MM-dd.");
                    return;
                }
            }
            String dateStr = DATE_FMT.format(date);

            System.out.print("Home team: ");
            String home = safeReadLine().trim();
            System.out.print("Away team: ");
            String away = safeReadLine().trim();

            if (home.equalsIgnoreCase(away)) {
                System.out.println("Home and away teams must be different.");
                return;
            }
            if (league.getTeam(home) == null) {
                System.out.println("Unknown home team: " + home);
                return;
            }
            if (league.getTeam(away) == null) {
                System.out.println("Unknown away team: " + away);
                return;
            }

            int hg = readNonNegativeInt("Home goals: ");
            int ag = readNonNegativeInt("Away goals: ");

            Match m = new Match(dateStr, home, away, hg, ag);
            if (league.recordMatch(m)) {
                System.out.println("Result recorded.");
                postActionSavePrompt();
            } else {
                System.out.println("Failed to record result (check teams).");
            }

        } catch (Exception e) {
            System.out.println("Error recording match: " + e.getMessage());
        }
    }

    private int readNonNegativeInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String s = sc.nextLine().trim();
                int v = Integer.parseInt(s);
                if (v < 0) {
                    System.out.println("Please enter a non-negative integer.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid non-negative integer.");
            }
        }
    }

    // --------------------------- Show Table ---------------------------

    private void showTableUI() {
        System.out.println("\nTeam             P  W  D  L  GF  GA  GD  Pts");
        league.getStandings().forEach(System.out::println);
        if (league.getTeams().isEmpty()) System.out.println("(no teams yet)");
    }

    // --------------------------- Save & Load ---------------------------

    /**
     * Prompt shown after any modifying action (add/remove/record).
     * Now: "Save? (Y/N)" — saving returns to the main menu (no exit).
     */
    private void postActionSavePrompt() {
        postActionSavePrompt(() -> {
            // For now, nothing special after saving/not saving.
            // But we could pass different actions from different places.
        });
    }

    private void postActionSavePrompt(Runnable afterDecision) {
        while (true) {
            System.out.print("Save? (Y/N): ");
            String ans = safeReadLine().trim();
            if (ans.equalsIgnoreCase("y")) {
                saveToDisk();
                afterDecision.run();  // <-- lambda executed here
                return; // back to main menu
            } else if (ans.equalsIgnoreCase("n")) {
                System.out.println("Changes kept in memory only. (Not saved to disk)");
                afterDecision.run();  // <-- lambda executed here
                return; // back to main menu
            } else {
                System.out.println("Please enter Y or N.");
            }
        }
    }


    private void saveToDisk() {
        try {
            store.saveTeams(league.getStandings());
            store.saveMatches(league.getMatches());
            System.out.println("Saved.");
        } catch (IOException e) {
            System.out.println("Error saving files: " + e.getMessage());
        }
    }

    private void exitProgram() {
        System.out.println("Thank you for using me. Come back soon!");
        System.exit(0);
    }

    private void load() {
        try {
            Map<String, Team> loadedTeams = store.loadTeams();
            // Re-add teams preserving origin & players
            for (Team t : loadedTeams.values()) {
                league.addTeam(t.getName(), t.getOrigin(), t.getPlayers());
            }
            // Load matches and rebuild to guarantee consistency
            league.getMatches().addAll(store.loadMatches());
            league.rebuildTableFromMatches();
        } catch (IOException e) {
            System.out.println("No previous data found (or error loading). Starting fresh.");
        }
    }

    // --------------------------- Helpers ---------------------------


    /**
     * Safe wrapper around sc.nextLine() that prevents crashes when the user
     * presses Ctrl+D/Ctrl+Z or when input stream closes unexpectedly.
     */
    private String safeReadLine() {
        try {
            return sc.nextLine();
        } catch (NoSuchElementException | IllegalStateException ex) {
            System.out.println("(Input error detected, treating as blank input)");
            return "";
        }
    }


    /**
     * Ensures:
     *  - First letter of the entire name is uppercase
     *  - Any word equal to "fc" (case-insensitive) is forced to "FC"
     * All other words keep their original user-provided casing.
     */
    private String formatTeamName(String name) {
        if (name == null || name.isEmpty()) return name;

        // Normalize whitespace and split into tokens
        String[] tokens = name.trim().split("\\s+");

        // Force "FC" tokens
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase("fc")) {
                tokens[i] = "FC";
            }
        }

        // Re-join using original (now possibly "FC") tokens
        String joined = String.join(" ", tokens);

        // Ensure first character of the entire string is uppercase
        char first = Character.toUpperCase(joined.charAt(0));
        String rest = joined.length() > 1 ? joined.substring(1) : "";

        return first + rest;
    }


    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}