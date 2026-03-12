import java.util.*;

public class League {

    private final Map<String, Team> teams = new HashMap<>();
    private final List<Match> matches = new ArrayList<>();

    // ------------- Add Team -------------
    public boolean addTeam(String name, String origin, List<String> players) {
        String key = name.toLowerCase();
        if (teams.containsKey(key)) return false;

        teams.put(key, new Team(name, origin, players));
        return true;
    }

    // ------------- Remove Team -------------
    public boolean removeTeam(String name) {
        String key = name.toLowerCase();
        return teams.remove(key) != null;
    }

    // ------------- Getters -------------
    public Team getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public Collection<Team> getTeams() {
        return teams.values();
    }

    public List<Match> getMatches() {
        return matches;
    }

    // ------------- Record Match -------------
    public boolean recordMatch(Match m) {
        Team home = teams.get(m.getHomeTeam().toLowerCase());
        Team away = teams.get(m.getAwayTeam().toLowerCase());

        if (home == null || away == null || home == away) return false;

        matches.add(m);
        home.recordMatch(m.getHomeGoals(), m.getAwayGoals());
        away.recordMatch(m.getAwayGoals(), m.getHomeGoals());
        return true;
    }

    // ------------- Rebuild from file load -------------
    public void rebuildTableFromMatches() {
        teams.values().forEach(Team::resetStats);

        for (Match m : matches) {
            Team home = teams.get(m.getHomeTeam().toLowerCase());
            Team away = teams.get(m.getAwayTeam().toLowerCase());

            if (home != null && away != null) {
                home.recordMatch(m.getHomeGoals(), m.getAwayGoals());
                away.recordMatch(m.getAwayGoals(), m.getHomeGoals());
            }
        }
    }

    // ------------- Standings -------------
    public List<Team> getStandings() {
        List<Team> list = new ArrayList<>(teams.values());
        list.sort(Team::compareTo);
        return list;
    }
}
