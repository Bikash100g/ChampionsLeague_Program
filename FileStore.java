import java.io.*;
import java.util.*;


public class FileStore {

    private final File teamsFile = new File("teams.bin");
    private final File matchesFile = new File("matches.bin");

    // ---- Save Teams ----
    public void saveTeams(Collection<Team> teams) throws IOException {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(teamsFile))) {
            oos.writeObject(new ArrayList<>(teams));
        }
    }

    // ---- Save Matches ----
    public void saveMatches(List<Match> matches) throws IOException {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(matchesFile))) {
            oos.writeObject(matches);
        }
    }

    // ---- Load Teams ----
    public Map<String, Team> loadTeams() throws IOException {
        Map<String, Team> map = new HashMap<>();

        if (!teamsFile.exists()) return map;

        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(teamsFile))) {

            List<Team> list = (List<Team>) ois.readObject();
            for (Team t : list) map.put(t.getName().toLowerCase(), t);

        } catch (ClassNotFoundException e) {
            throw new IOException("Class mismatch while loading teams");
        }

        return map;
    }

    // ---- Load Matches ----
    public List<Match> loadMatches() throws IOException {
        if (!matchesFile.exists()) return new ArrayList<>();

        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(matchesFile))) {

            return (List<Match>) ois.readObject();

        } catch (ClassNotFoundException e) {
            throw new IOException("Class mismatch while loading matches");
        }
    }
}