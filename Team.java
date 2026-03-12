import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Team implements Comparable<Team>, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String origin;
    private List<String> players;

    private int played, won, drawn, lost, goalsFor, goalsAgainst, points;

    // ---------- Constructor ----------
    public Team(String name, String origin, List<String> players) {
        this.name = name;
        this.origin = origin;
        this.players = new ArrayList<>(players);
    }

    // ---------- Getters ----------
    public String getName() { return name; }
    public String getOrigin() { return origin; }
    public List<String> getPlayers() { return players; }

    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }
    public int getPoints() { return points; }

    // ---------- Stats Management ----------
    public void resetStats() {
        played = won = drawn = lost = goalsFor = goalsAgainst = points = 0;
    }

    public void recordMatch(int gf, int ga) {
        goalsFor += gf;
        goalsAgainst += ga;
        played++;

        if (gf > ga) { won++; points += 3; }
        else if (gf == ga) { drawn++; points += 1; }
        else { lost++; }
    }

    // ---------- Sorting for standings ----------
    @Override
    public int compareTo(Team o) {
        int c = Integer.compare(o.points, this.points);
        if (c != 0) return c;

        c = Integer.compare(o.getGoalDifference(), this.getGoalDifference());
        if (c != 0) return c;

        c = Integer.compare(o.goalsFor, this.goalsFor);
        if (c != 0) return c;

        return this.name.compareToIgnoreCase(o.name);
    }

    // ---------- Display Format ----------
    @Override
    public String toString() {
        return String.format("%-15s %2d %2d %2d %2d %3d %3d %3d %3d",
                name, played, won, drawn, lost, goalsFor, goalsAgainst,
                getGoalDifference(), points);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team team = (Team) o;
        return name.equalsIgnoreCase(team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }
}