package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

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
    static final File HEAD_POINT = Utils.join(GITLET_DIR, "HEAD");

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

        // Initialize head and master branches
        HEAD_POINT.createNewFile();
        File master = Utils.join(BRANCH_DIR, "master");
        master.createNewFile();

        // first commit
        Commit initialCommit = new Commit("initial commit", null);
        initialCommit.setTimestamp(ZonedDateTime.of(1970, 1, 1, 0, 0, 0,0, ZoneOffset.UTC));

        // persistent commit
        String commitSha1 = getSha1(initialCommit);
        persistentObject(commitSha1, initialCommit);

        // branches point to the fist commit
        Utils.writeContents(HEAD_POINT, commitSha1);
        Utils.writeContents(master, commitSha1);
    }


    public static void add() {

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


    /**
     * Returns the SHA-1 hash of the concatenation of object
     */
    private static String getSha1(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }

    /**
     * Persistent commit and blobs
     */
    private static void persistentObject(String sha1, Serializable obj) {
        File dir = Utils.join(GitletRepository.OBJECTS_DIR, sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = Utils.join(dir, sha1.substring(2));
        Utils.writeObject(file, obj);
    }
}
