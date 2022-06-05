package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    static final File COMMIT_DIR = Utils.join(GITLET_DIR, "commit");
    static final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");
    static final File HEAD = Utils.join(GITLET_DIR, "HEAD");
    static final File STAGE = Utils.join(GITLET_DIR, "stage");

    static final DateTimeFormatter ZONE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");

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
        OBJECTS_DIR.mkdir();
        BRANCH_DIR.mkdir();
        COMMIT_DIR.mkdir();

        // Initialize files, head, master branches, stage
        HEAD.createNewFile();
        File master = Utils.join(BRANCH_DIR, "master");
        master.createNewFile();
        saveStage(new Stage());

        // first commit
        Commit initialCommit = new Commit("initial commit", null);
        initialCommit.setTimestamp(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        // persistent commit
        String commitSha1 = getObjectSha1(initialCommit);
        persistentCommit(commitSha1, initialCommit);

        // branches point to the fist commit
        Utils.writeContents(HEAD, "master");
        Utils.writeContents(master, commitSha1);
    }


    /**
     * Adds a copy of the file as it currently exists to the staging area
     */
    public static void add(String fileName) throws IOException {
        Stage stage = Utils.readObject(STAGE, Stage.class);
        stage.addFile(fileName);
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
        String commitSha1 = getObjectSha1(commit);
        persistentCommit(commitSha1, commit);

        // Update branch or head point to new commit
        if (isHeadPointBranch) {
            File branch = Utils.join(BRANCH_DIR, headContent);
            Utils.writeContents(branch, commitSha1);
        } else {
            Utils.writeContents(HEAD, commitSha1);
        }
    }


    /**
     * Remove file from staging area or current working directory
     */
    public static void rm(String fileName) {
        Stage stage = getStage();
        stage.removeFile(fileName);
        saveStage(stage);
    }


    /**
     * Display information about each commit backwards along the commit tree until the initial commit.
     */
    public static void log() {
        String sha1 = getHeadSha1();
        Commit commit;

        while (sha1 != null) {
            commit = getCommit(sha1);
            displayCommitInfo(commit, sha1);
            sha1 = commit.getParent();
        }
    }


    /**
     * Display information about all commits ever made
     */
    public static void globalLog() {
        for (String commitSha1 : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
            Commit commit = getCommit(commitSha1);
            displayCommitInfo(commit, commitSha1);
        }
    }

    /**
     * Print all the information of a commit object
     */
    private static void displayCommitInfo(Commit commit, String commitSha1) {
        System.out.println("===");
        System.out.println("commit " + commitSha1);
        if (commit.getMergeFrom() != null) {
            System.out.println("Merge: " + commit.getParent().substring(0, 7) + " " + commit.getMergeFrom().substring(0, 7));
        }
        System.out.println("Date: " + commit.getTimestamp().withZoneSameInstant(ZoneOffset.systemDefault()).format(ZONE_DATE_TIME_FORMATTER));
        System.out.println(commit.getMessage());
        System.out.println();
    }


    /**
     * Prints out the ids of all commits that have the given commit message
     */
    public static void find(String message) {
        boolean exists = false;
        for (String commitSha1 : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
            Commit commit = getCommit(commitSha1);
            if (commit.getMessage().contains(message)) {
                System.out.println(commitSha1);
                exists = true;
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
        }
    }


    /**
     * Display current branch information
     */
    public static void status() {
        // branch status
        String headContent = Utils.readContentsAsString(HEAD);
        System.out.println("=== Branches ===");
        List<String> branchList = Objects.requireNonNull(Utils.plainFilenamesIn(BRANCH_DIR));
        Collections.sort(branchList);
        for (String branch : branchList) {
            if (branch.equals(headContent)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();

        // get staging area and head commit blobs
        Map<String, String> blobs = getHead().getBlobs();
        Stage stage = getStage();
        TreeMap<String, String> stageAdditionList = stage.getAdditionMap();
        List<String> modifyList = new ArrayList<>();
        // staged file
        System.out.println("=== Staged Files ===");
        for (String fileName : stageAdditionList.keySet()) {
            System.out.println(fileName);
            blobs.remove(fileName);
            differentFile(fileName, stageAdditionList, modifyList);
        }
        System.out.println();

        // removed file
        System.out.println("=== Removed Files ===");
        for (String fileName : stage.getRemovalMap().keySet()) {
            System.out.println(fileName);
            blobs.remove(fileName);
        }
        System.out.println();

        // modify not staged file
        System.out.println("=== Modifications Not Staged For Commit ===");
        blobs.keySet().forEach(fileName -> differentFile(fileName, blobs, modifyList));
        Collections.sort(modifyList);
        modifyList.forEach(System.out::println);
        System.out.println();

        // untracked files
        System.out.println("=== Untracked Files ===");
        blobs.putAll(stageAdditionList);
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!blobs.containsKey(fileName)) {
                System.out.println(fileName);
            }
        }
    }


    /**
     * Check the sha1 of file in working directory same as given map
     */
    private static void differentFile(String fileName, Map<String, String> compare, List<String> modifyList) {
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            modifyList.add(fileName + " (deleted)");
            return;
        }
        String fileSha1 = Utils.sha1(fileName, Utils.readContentsAsString(file));
        if (!compare.get(fileName).equals(fileSha1)) {
            modifyList.add(fileName + " (modified)");
        }
    }


    /**
     * Checkout of branch or file
     * Updates the files in the working directory to match the version stored in given commit
     * Restore the entire working directory to the version of the specified branch
     */
    public static void checkout(String... args) throws IOException {
        Map<String, String> currentBlobs = getHead().getBlobs();
        // if checkout a branch
        if (args.length == 1) {
            // failure if branch not exists
            if (args[0].equals(Utils.readContentsAsString(HEAD))) {
                exitWithError("No need to checkout the current branch.");
            }

            // failure if working directory had untracked file
            List<String> workingDir = Utils.plainFilenamesIn(CWD);
            if (workingDir.size() != currentBlobs.size()) {
                exitWithError("here is an untracked file in the way; delete it, or add and commit it first.");
            }

            // failure if working directory had modified file
            for (Map.Entry<String, String> entry : currentBlobs.entrySet()) {
                File file = Utils.join(CWD, entry.getKey());
                if (!file.exists() || entry.getValue().equals(getCwdFileSha1(file))) {
                    exitWithError("here is an untracked file in the way; delete it, or add and commit it first.");
                }
            }

            // delete all files in the working directory
            workingDir.forEach(Utils::restrictedDelete);

            // restore files to working directory
            Map<String, String> blobs = getCommit(getBranchSha1(args[0])).getBlobs();
            for (Map.Entry<String, String> entry : blobs.entrySet()) {
                Files.copy(getObjectFile(entry.getValue()).toPath(), Utils.join(CWD, entry.getKey()).toPath());
            }
            // change current branch to the given branch
            Utils.writeContents(HEAD, args[0]);
            // clear staging area
            saveStage(new Stage());
        } else if("--".equals(args[0]) && args.length == 2) {
            // takes the version of the file as it exists in the head commit
            if (!currentBlobs.containsKey(args[1])) {
                exitWithError("File does not exist in that commit.");
            }

            // delete file and copy
            Utils.restrictedDelete(args[1]);
            Files.copy(getObjectFile(currentBlobs.get(args[1])).toPath(), Utils.join(CWD, args[1]).toPath());
        } else if ("--".equals(args[1]) && args.length == 3) {
            // takes the version of the file as it exists in the commit with the given id
            currentBlobs = getCommit(args[0]).getBlobs();
            if (!currentBlobs.containsKey(args[2])) {
                exitWithError("File does not exist in that commit.");
            }

            // delete file and copy
            Utils.restrictedDelete(args[2]);
            Files.copy(getObjectFile(currentBlobs.get(args[2])).toPath(), Utils.join(CWD, args[2]).toPath());
        } else {
            // command not correct
            exitWithError("Incorrect operands.");
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

    /**
     * Returns the sha1 string of the commit pointed to by HEAD
     */
    public static String getHeadSha1() {
        String headContent = Utils.readContentsAsString(HEAD);
        if (Utils.join(BRANCH_DIR, headContent).exists()) {
            return getBranchSha1(headContent);
        }
        return headContent;
    }

    /**
     * Returns the sha1 string of the commit pointed to by the specified branch
     */
    public static String getBranchSha1(String branch) {
        File branchFile = Utils.join(BRANCH_DIR, branch);
        if (!branchFile.exists()) {
            exitWithError("No such branch exists.");
        }
        return Utils.readContentsAsString(branchFile);
    }


    /**
     * Get the object of the branch or commit pointed to by head
     */
    public static Commit getHead() {
        String headCommitSha1 = getHeadSha1();
        return getCommit(headCommitSha1);
    }


    /**
     * Get the commit object that the branch points to
     */
    public static Commit getBranch(String branch) {
        String branchCommitSha1 = getBranchSha1(branch);
        return getCommit(branchCommitSha1);
    }


    public static Commit getCommit(String sha1) {
        String commitFileName = sha1;
        // find the specified commit by a few prefixes sha1
        if (sha1.length() != Utils.UID_LENGTH) {
            int length = sha1.length();
            for (String fileName : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
                if (fileName.substring(0, length).equals(sha1)) {
                    commitFileName = fileName;
                }
            }
            if (commitFileName.length() == length) {
                throw Utils.error("No commit with that id exists.");
            }
        }

        File commitFile = Utils.join(COMMIT_DIR, commitFileName);
        if (!commitFile.exists() || !commitFile.isFile()) {
            throw Utils.error("No commit with that id exists.");
        }
        return Utils.readObject(commitFile, Commit.class);
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
    private static String getObjectSha1(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }

    private static String getCwdFileSha1(String fileName) {
        return getCwdFileSha1(Utils.join(CWD, fileName));
    }

    private static String getCwdFileSha1(File file) {
        return Utils.sha1(file.getName(), Utils.readContents(file));
    }

    /**
     * Persistent commit
     */
    private static void persistentCommit(String sha1, Serializable obj) {
        Utils.writeObject(Utils.join(COMMIT_DIR, sha1), obj);
    }


    /**
     * Get the file directory of the commit or blob
     */
    public static File getObjectFile(String sha1) {
        if (sha1 == null || sha1.isEmpty()) {
            throw Utils.error("sha1 is null, can't get object");
        }
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
