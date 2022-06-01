package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * @author Delete020
 * @since 5/25/22 9:57 PM
 *
 * The structure of a Capers Repository is as follows:
 *
 * .gitlet/ -- top level folder for all persistent data
 *    - objects/ -- folder containing all of the persistent data for commits and blobs
 *    - branches/ -- folder containing all of the persistent data for branch
 *    - HEAD/ -- file containing the current HEAD point
 */
public class GitletRepository {

    static final String CWD = System.getProperty("user.dir");
    static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    static final File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");
    static final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");
    static final File HEAD = Utils.join(GITLET_DIR, "HEAD");
    static final File STAGE = Utils.join(GITLET_DIR, "stage");


    /**
     * Creates a new Gitlet version-control system in the current directory.
     */
    public static void init() throws IOException {
        // If there is already a Gitlet version-control system in the current directory
        if (GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system already exists in the current directory.");
        }

        // Initialize folders
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdirs();
        BRANCH_DIR.mkdirs();

        // Initialize files, head, master branches, stage
        HEAD.createNewFile();
        File master = Utils.join(BRANCH_DIR, "master");
        master.createNewFile();
        saveStage(new Stage());

        // first commit
        Commit initialCommit = new Commit("initial commit", null);
        initialCommit.setTimestamp(ZonedDateTime.of(1970, 1, 1, 0, 0, 0,0, ZoneOffset.UTC));

        // persistent commit
        String commitSha1 = getSha1(initialCommit);
        persistentCommit(commitSha1, initialCommit);

        // branches point to the fist commit
        Utils.writeContents(HEAD, commitSha1);
        Utils.writeContents(master, commitSha1);
    }


    /**
     * Adds a copy of the file as it currently exists to the staging area
     */
    public static void add(String fileName) throws IOException {
        File file = Utils.join(CWD, fileName);
        // Check the file exists in the working directory
        if (!file.exists()) {
            exitWithError("File does not exist.");
        }

        Stage stage = Utils.readObject(STAGE, Stage.class);
        stage.addFile(fileName, file);
        saveStage(stage);
    }


    public static void branch() {

    }


    /**
     * Prints out MESSAGE and exits with error code 0.
     *
     * @param message message to print
     */
    public static void exitWithError(String message) {
        if (message != null && !"".equals(message)) {
            System.out.println(message);
        }
        System.exit(0);
    }


    public static Commit getHead() {
        String currentBranch = Utils.readContentsAsString(HEAD);
        return Utils.readObject(getObjectFile(currentBranch), Commit.class);
    }

    /**
     * Returns the SHA-1 hash of the concatenation of object
     */
    private static String getSha1(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }


    /**
     * Persistent commit
     */
    private static void persistentCommit(String sha1, Serializable obj) {
        Utils.writeObject(getObjectFile(sha1), obj);
    }


    public static File getObjectFile(String sha1) {
        File dir = Utils.join(GitletRepository.OBJECTS_DIR, sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return Utils.join(dir, sha1.substring(2));
    }


    private static void saveStage(Stage stage) {
        Utils.writeObject(STAGE, stage);
    }

}
