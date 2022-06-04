package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Delete020
 * @since 5/25/22 9:57 PM
 * <p>
 * The structure of a Capers Repository is as follows:
 * <p>
 * .gitlet/ -- top level folder for all persistent data
 * - objects/ -- folder containing all of the persistent data for commits and blobs
 * - branches/ -- folder containing all of the persistent data for branch
 * - HEAD/ -- file containing the current HEAD point
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
        initialCommit.setTimestamp(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        // persistent commit
        String commitSha1 = getSha1(initialCommit);
        persistentCommit(commitSha1, initialCommit);

        // branches point to the fist commit
        Utils.writeContents(HEAD, "master");
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


    /**
     * Saves a snapshot of tracked files
     */
    public static void commit(String message) {
        // Commit must have a non-blank message.
        if (message.isEmpty()) {
            exitWithError("Please enter a commit message.");
        }

        // Get staging area, if empty means no files have been staged, abort.
        Stage stage = getStage();
        if (stage.getAdditionMap().isEmpty() && stage.getRemovalMap().isEmpty()) {
            exitWithError("No changes added to the commit.");
        }

        // Get the contents of head
        String headContent = Utils.readContentsAsString(HEAD);
        // Get parent commit sha1 string
        String parentSha1 = headContent;
        // Two cases, head points to a branch or head points to a commit object
        boolean isHeadPointBranch = false;
        if (Utils.join(BRANCH_DIR, headContent).exists()) {
            isHeadPointBranch = true;
            parentSha1 = getBranchSha1(headContent);
        }

        // Create a new commit object, get the parent commit blobs
        Commit commit = new Commit(message, parentSha1);
        Map<String, String> blobs = getHead().getBlobs();
        commit.setBlobs(blobs);

        // Add files saved in the staging area and remove files deleted from the staging area
        blobs.putAll(stage.getAdditionMap());
        for (String fileName : stage.getRemovalMap().keySet()) {
            blobs.remove(fileName);
        }

        // clear staging area, then persistent stage
        stage.getAdditionMap().clear();
        stage.getRemovalMap().clear();
        saveStage(stage);

        // Remove the files removed by the system rm command
        for (String blobsFile : blobs.keySet()) {
            if (!Utils.join(CWD, blobsFile).exists()) {
                blobs.remove(blobsFile);
            }
        }

        // Persistent new commit
        String commitSha1 = getSha1(commit);
        persistentCommit(commitSha1, commit);

        // Update branch or head point to new commit
        if (isHeadPointBranch) {
            File branch = Utils.join(BRANCH_DIR, headContent);
            Utils.writeContents(branch, commitSha1);
        } else {
            Utils.writeContents(HEAD, commitSha1);
        }
    }


    public static void rm(String fileName) {
        Stage stage = getStage();
        Commit head  = getHead();
        Map<String, String> additionMap = stage.getAdditionMap();
        Map<String, String> blobs = head.getBlobs();

        if (additionMap.containsKey(fileName)) {
            additionMap.remove(fileName);
            saveStage(stage);
        } else if (blobs.containsKey(fileName)) {
            stage.getRemovalMap().put(fileName, blobs.get(fileName));
            Utils.restrictedDelete(fileName);
            saveStage(stage);
        } else {
            exitWithError("No reason to remove the file.");
        }
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


    public static String getBranchSha1(String branch) {
        File branchFile = Utils.join(BRANCH_DIR, branch);
        return Utils.readContentsAsString(branchFile);
    }


    /**
     * Get the object of the branch or commit pointed to by head
     */
    public static Commit getHead() {
        String headContent = Utils.readContentsAsString(HEAD);
        if (Utils.join(BRANCH_DIR, headContent).exists()) {
            return getBranch(headContent);
        }
        return getCommit(headContent);
    }


    /**
     * Get the commit object that the branch points to
     */
    public static Commit getBranch(String branch) {
        String branchCommitSha1 = getBranchSha1(branch);
        return getCommit(branchCommitSha1);
    }


    public static Commit getCommit(String sha1) {
        File commit = getObjectFile(sha1);
        if (!commit.exists() || !commit.isFile()) {
            throw Utils.error("No commit with that id exists.");
        }
        return Utils.readObject(commit, Commit.class);
    }


    /**
     * Get the staging area object
     */
    public static Stage getStage() {
        return Utils.readObject(STAGE, Stage.class);
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


    /**
     * Get the file directory of the commit or blob
     */
    public static File getObjectFile(String sha1) {
        File dir = Utils.join(GitletRepository.OBJECTS_DIR, sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return Utils.join(dir, sha1.substring(2));
    }


    /**
     * Persistent stage
     */
    private static void saveStage(Stage stage) {
        Utils.writeObject(STAGE, stage);
    }

}
