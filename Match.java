import java.io.Serializable;

public class Match implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date; // yyyy-MM-dd
    private String homeTeam;
    private String awayTeam;
    private int homeGoals;
    private int awayGoals;

    public Match(String date, String homeTeam, String awayTeam, int homeGoals, int awayGoals) {
        this.date = date;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public String getDate() { return date; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public int getHomeGoals() { return homeGoals; }
    public int getAwayGoals() { return awayGoals; }
}
