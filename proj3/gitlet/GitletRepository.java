package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Delete020
 * @since 5/25/22 9:57 PM
 * <p>
 * The structure of a Gitlet Repository is as follows:
 * <p>
 * .gitlet/ -- top level folder for all persistent data
 * - objects/ -- folder containing all of the persistent data for commits and blobs
 * - branches/ -- folder containing all of the persistent data for branch
 * - HEAD/ -- file containing the current HEAD point
 */
public class GitletRepository {

    private final String CWD;
    private final File GITLET_DIR;
    private final File OBJECTS_DIR;
    private final File COMMIT_DIR;
    private final File BRANCH_DIR;
    private final File HEAD;
    private final File STAGE;
    private final File REMOTE_DIR;
    private static final DateTimeFormatter ZONE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy Z");


    public GitletRepository() {
        this(System.getProperty("user.dir"));
    }

    public GitletRepository(String CWD) {
        this.CWD = CWD;
        String GITLET_NAME = ".gitlet";
        GITLET_DIR = Utils.join(CWD, GITLET_NAME);
        OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");
        COMMIT_DIR = Utils.join(GITLET_DIR, "commit");
        BRANCH_DIR = Utils.join(GITLET_DIR, "branches");
        HEAD = Utils.join(GITLET_DIR, "HEAD");
        STAGE = Utils.join(GITLET_DIR, "stage");
        REMOTE_DIR = Utils.join(GITLET_DIR, "remote");
    }

    /**
     * Creates a new Gitlet version-control system in the current directory.
     */
    public void init() throws IOException {
        // If there is already a Gitlet version-control system in the current directory
        if (GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system already exists in the current directory.");
        }

        // Initialize folders
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        BRANCH_DIR.mkdir();
        COMMIT_DIR.mkdir();
        REMOTE_DIR.mkdir();

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
    public void add(String filename) throws IOException {
        // Get stage object
        Stage stage = Utils.readObject(STAGE, Stage.class);
        Map<String, String> additionMap = stage.getAdditionMap();
        Map<String, String> removalMap = stage.getRemovalMap();

        // Get working directory file
        File file = Utils.join(CWD, filename);
        // Check the file exists in the working directory
        if (!file.exists()) {
            exitWithError("File does not exist.");
        }

        String addFileSha1 = Utils.sha1(filename, Utils.readContents(file));
        File stageFile = getObjectFile(addFileSha1);

        removalMap.remove(filename);

        // The file is identical to the parent commit file or not
        String parentVersion = getHead().getBlobs().get(filename);
        if (parentVersion != null && parentVersion.equals(addFileSha1)) {
            // Remove it from the staging area， if a file changed, added, and then changed back
            additionMap.remove(filename);
        } else {
            additionMap.put(filename, addFileSha1);
        }

        // File already exists in the staging area, do nothing
        if (!stageFile.exists()) {
            // Copy file to staging area
            Files.copy(file.toPath(), stageFile.toPath());
        }

        // persistent stage object
        saveStage(stage);
    }


    /**
     * Saves a snapshot of tracked files
     */
    public void commit(String message) {
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
        for (String filename : stage.getRemovalMap().keySet()) {
            blobs.remove(filename);
        }

        // clear staging area
        saveStage(new Stage());

        // Remove the files that removed by the system rm command
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
    public void rm(String filename) {
        // get stage object
        Stage stage = getStage();
        Map<String, String> additionMap = stage.getAdditionMap();
        Map<String, String> removalMap = stage.getRemovalMap();

        // get current head blobs
        Map<String, String> blobs = getHead().getBlobs();

        // Two cases, file currently staged or in the current commit, otherwise error
        if (additionMap.containsKey(filename)) {
            additionMap.remove(filename);
        } else if (blobs.containsKey(filename)) {
            removalMap.put(filename, blobs.get(filename));
            Utils.restrictedDelete(filename);
        } else {
            exitWithError("No reason to remove the file.");
        }

        // persistent stage object
        saveStage(stage);
    }


    /**
     * Display information about each commit backwards along the commit tree until the initial commit.
     */
    public void log() {
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
    public void globalLog() {
        for (String commitSha1 : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
            Commit commit = getCommit(commitSha1);
            displayCommitInfo(commit, commitSha1);
        }
    }

    /**
     * Print all the information of a commit object
     */
    private void displayCommitInfo(Commit commit, String commitSha1) {
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
    public void find(String message) {
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
    public void status() {
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
        Map<String, String> stageAdditionList = stage.getAdditionMap();
        List<String> modifyList = new ArrayList<>();
        // staged file
        System.out.println("=== Staged Files ===");
        for (String filename : stageAdditionList.keySet()) {
            System.out.println(filename);
            blobs.remove(filename);
            differentFile(filename, stageAdditionList, modifyList);
        }
        System.out.println();

        // removed file
        System.out.println("=== Removed Files ===");
        for (String filename : stage.getRemovalMap().keySet()) {
            System.out.println(filename);
            blobs.remove(filename);
        }
        System.out.println();

        // modify not staged file
        System.out.println("=== Modifications Not Staged For Commit ===");
        blobs.keySet().forEach(filename -> differentFile(filename, blobs, modifyList));
        Collections.sort(modifyList);
        modifyList.forEach(System.out::println);
        System.out.println();

        // untracked files
        System.out.println("=== Untracked Files ===");
        blobs.putAll(stageAdditionList);
        for (String filename : Utils.plainFilenamesIn(CWD)) {
            if (!blobs.containsKey(filename)) {
                System.out.println(filename);
            }
        }
    }


    /**
     * Check the sha1 of file in working directory same as given map
     */
    private void differentFile(String filename, Map<String, String> compare, List<String> modifyList) {
        File file = Utils.join(CWD, filename);
        if (!file.exists()) {
            modifyList.add(filename + " (deleted)");
            return;
        }
        String fileSha1 = Utils.sha1(filename, Utils.readContentsAsString(file));
        if (!compare.get(filename).equals(fileSha1)) {
            modifyList.add(filename + " (modified)");
        }
    }


    /**
     * Checkout of branch or file
     * Updates the files in the working directory to match the version stored in given commit
     * Restore the entire working directory to the version of the specified branch
     */
    public void checkout(String... args) throws IOException {
        Map<String, String> currentBlobs = getHead().getBlobs();
        // if checkout a branch
        if (args.length == 1) {
            // failure if branch not exists
            if (args[0].equals(Utils.readContentsAsString(HEAD))) {
                exitWithError("No need to checkout the current branch.");
            }

            // Recovery version by sha1 for the given branch
            restoreVersion(currentBlobs, getBranchSha1(args[0]));
            // change current branch to the given branch
            Utils.writeContents(HEAD, args[0]);
        } else if ("--".equals(args[0]) && args.length == 2) {
            // takes the version of the file as it exists in the head commit
            if (!currentBlobs.containsKey(args[1])) {
                exitWithError("File does not exist in that commit.");
            }

            // delete file and copy
            Utils.restrictedDelete(args[1]);
            Files.copy(getObjectFile(currentBlobs.get(args[1])).toPath(), Utils.join(CWD, args[1]).toPath());
        } else if ("--".equals(args[1]) && args.length == 3) {
            // takes the version of the file as it exists in the commit with the given id
            String commitSha1 = getCommitSha1(args[0]);
            currentBlobs = getCommit(commitSha1).getBlobs();
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


    /**
     * Remove current working directory files,
     * restore previous version and moves the current branch's head to that commit node
     */
    public void reset(String commitId) throws IOException {
        if (Utils.join(BRANCH_DIR, commitId).exists()) {
            exitWithError("No commit with that id exists.");
        }

        // restore
        Map<String, String> headBlobs = getHead().getBlobs();
        String commit = getCommitSha1(commitId);
        restoreVersion(headBlobs, commit);

        // move branch
        File branchFile = Utils.join(BRANCH_DIR, Utils.readContentsAsString(HEAD));
        if (branchFile.exists()) {
            Utils.writeContents(branchFile, commit);
        } else {
            Utils.writeContents(HEAD, commit);
        }
    }


    /**
     * Restore working directory to the given commit version
     */
    private void restoreVersion(Map<String, String> headBlobs, String commitSha1) throws IOException {
        // failure if working directory had modified file
        for (Map.Entry<String, String> entry : headBlobs.entrySet()) {
            File file = Utils.join(CWD, entry.getKey());
            if (!file.exists() || !entry.getValue().equals(getCwdFileSha1(file))) {
                exitWithError("here is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        // restore files to working directory
        Map<String, String> commitBlobs = getCommit(commitSha1).getBlobs();
        restoreWorkingDirectory(commitBlobs, headBlobs);
    }


    /**
     * Clear working directory file, then copy given blobs files to working directory
     */
    private void restoreWorkingDirectory(Map<String, String> restoreBlobs, Map<String, String> currentBlobs) throws IOException {
        //in order to not overwrite untracked files, check working file is untracked, but have same filename of restore commit
        Map<String, String> differBlobs = new TreeMap<>(restoreBlobs);
        currentBlobs.keySet().forEach(differBlobs::remove);
        for (String filename : differBlobs.keySet()) {
            File file = Utils.join(CWD, filename);
            if (file.exists()) {
                exitWithError("here is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        // delete all files in the working directory
        currentBlobs.keySet().forEach(Utils::restrictedDelete);

        for (Map.Entry<String, String> entry : restoreBlobs.entrySet()) {
            File file = Utils.join(CWD, entry.getKey());
            Files.copy(getObjectFile(entry.getValue()).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // clear staging area
        saveStage(new Stage());
    }


    /**
     * Create new branch
     */
    public void branch(String branchName) throws IOException {
        File branchFile = Utils.join(BRANCH_DIR, branchName);
        if (branchFile.exists()) {
            exitWithError("A branch with that name already exists.");
        }
        branchFile.createNewFile();
        Utils.writeContents(branchFile, getHeadSha1());
    }


    /**
     * Remove a branch
     */
    public void rmBranch(String branchName) {
        if (branchName.equals(Utils.readContentsAsString(HEAD))) {
            exitWithError("Cannot remove the current branch.");
        }

        File file = Utils.join(BRANCH_DIR, branchName);
        if (!file.exists()) {
            exitWithError("A branch with that name does not exist.");
        }
        file.delete();
    }


    /**
     * Merges files from the given branch into the current branch.
     */
    public void merge(String branchName) throws IOException {
        // error, staging area is not empty
        Stage stage = getStage();
        if (!(stage.getAdditionMap().isEmpty() && stage.getRemovalMap().isEmpty())) {
            exitWithError("You have uncommitted changes.");
        }
        // error, branch not exists
        if (!Utils.join(BRANCH_DIR, branchName).exists()) {
            exitWithError("A branch with that name does not exist.");
        }

        // get current head blob, ancestor blobs, merge branch blobs
        String branchCommitSha1 = getBranchSha1(branchName);
        Map<String, String> spilt = Objects.requireNonNull(commonAncestor(branchCommitSha1, branchName)).getBlobs();
        Map<String, String> head = getHead().getBlobs();
        Map<String, String> branch = getCommit(branchCommitSha1).getBlobs();
        // use to save result blobs
        Map<String, String> blobs = new TreeMap<>(spilt);
        blobs.putAll(head);
        blobs.putAll(branch);

        // choose which version should save to merge version
        for (String filename : blobs.keySet()) {
            // get file sha1
            String spiltSha1 = spilt.get(filename);
            String headSha1 = head.get(filename);
            String branchSha1 = branch.get(filename);

            // check file is different with ancestor
            boolean isHeadModify = !Objects.equals(headSha1, spiltSha1);
            boolean isBranchModify = !Objects.equals(branchSha1, spiltSha1);
            // save correct version file
            if ((isHeadModify && !isBranchModify) || Objects.equals(headSha1, branchSha1)) {
                if (headSha1 == null) {
                    spilt.remove(filename);
                } else {
                    spilt.put(filename, headSha1);
                }
            } else if (!isHeadModify && isBranchModify) {
                if (branchSha1 == null) {
                    spilt.remove(filename);
                } else {
                    spilt.put(filename, branchSha1);
                }
            } else {
                // replace the contents of the conflicted file
                // create file content
                String currentContent = headSha1 == null ? "" : Utils.readContentsAsString(getObjectFile(headSha1));
                String branchContent = branchSha1 == null ? "" : Utils.readContentsAsString(getObjectFile(branchSha1));
                String fileContent = "<<<<<<< HEAD\n" + currentContent + "=======\n" + branchContent + ">>>>>>>\n";
                String fileSha1 = Utils.sha1(filename, fileContent);
                // save file to object directory
                Utils.writeContents(getObjectFile(fileSha1), fileContent);
                spilt.put(filename, fileSha1);
                System.out.println("Encountered a merge conflict.");
            }
        }

        // copy merge version files to working directory
        restoreWorkingDirectory(spilt, head);

        // create merge commit
        String commitMessage = "Merged " + branchName + " into " + Utils.readContentsAsString(HEAD) + ".";
        Commit mergeCommit = new Commit(commitMessage, getHeadSha1(), branchCommitSha1);
        mergeCommit.setBlobs(spilt);
        String mergeCommitSha1 = getObjectSha1(mergeCommit);
        persistentCommit(mergeCommitSha1, mergeCommit);

        // update branch
        File headBranch = Utils.join(BRANCH_DIR, Utils.readContentsAsString(HEAD));
        if (headBranch.exists()) {
            Utils.writeContents(headBranch, mergeCommitSha1);
        } else {
            Utils.writeContents(HEAD, mergeCommitSha1);
        }
    }


    /**
     * Given a sha1 of commit, find the latest common commit with current head commit
     */
    private Commit commonAncestor(String mergeSha1, String branchName) throws IOException {
        String currentSha1 = getHeadSha1();
        if (currentSha1.equals(mergeSha1)) {
            exitWithError("Cannot merge a branch with itself.");
        }

        // get current commit tree
        Map<String, Integer> currentCommitMap = new HashMap<>();
        dfs(currentSha1, currentCommitMap, 0);
        if (currentCommitMap.containsKey(mergeSha1)) {
            exitWithError("Given branch is an ancestor of the current branch.");
        }

        // get merge commit tree
        Queue<String> queue = new LinkedList<>();
        queue.add(mergeSha1);
        // record latest
        int minDistance = Integer.MAX_VALUE;
        Commit latestAncestor = null;

        // BFS, search the common ancestor
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String sha1 = queue.poll();
                if (Objects.equals(sha1, currentSha1)) {
                    checkout(branchName);
                    exitWithError("Current branch fast-forwarded.");
                }
                Commit commit = getCommit(sha1);
                // select the closest
                if (currentCommitMap.containsKey(sha1)) {
                    int distance = currentCommitMap.get(sha1);
                    if (distance < minDistance) {
                        latestAncestor = commit;
                        minDistance = distance;
                    }
                }
                if (commit.getParent() != null) {
                    queue.add(commit.getParent());
                }
                if (commit.getMergeFrom() != null) {
                    queue.add(commit.getMergeFrom());
                }
            }
        }

        return latestAncestor;
    }


    /**
     * Deep first search, for traverse commit tree
     */
    private void dfs(String sha1, Map<String, Integer> commitMap, int depth) {
        if (sha1 == null) {
            return;
        }
        Commit commit = getCommit(sha1);
        commitMap.put(sha1, depth);
        depth++;
        dfs(commit.getParent(), commitMap, depth);
        dfs(commit.getMergeFrom(), commitMap, depth);
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
    public String getHeadSha1() {
        String headContent = Utils.readContentsAsString(HEAD);
        if (Utils.join(BRANCH_DIR, headContent).exists()) {
            return getBranchSha1(headContent);
        }
        return headContent;
    }


    /**
     * Returns the sha1 string of the commit pointed to by the specified branch
     */
    public String getBranchSha1(String branch) {
        File branchFile = Utils.join(BRANCH_DIR, branch);
        if (!branchFile.exists()) {
            exitWithError("No such branch exists.");
        }
        return Utils.readContentsAsString(branchFile);
    }


    public String getCommitSha1(String shortSha1) {
        int length = shortSha1.length();
        for (String sha1 : Objects.requireNonNull(Utils.plainFilenamesIn(COMMIT_DIR))) {
            if (sha1.substring(0, length).equals(shortSha1)) {
                return sha1;
            }
        }
        exitWithError("No commit with that id exists.");
        return shortSha1;
    }


    /**
     * Get the object of the branch or commit pointed to by head
     */
    public Commit getHead() {
        String headCommitSha1 = getHeadSha1();
        return getCommit(headCommitSha1);
    }


    /**
     * Get the commit object that the branch points to
     */
    public Commit getBranch(String branch) {
        String branchCommitSha1 = getBranchSha1(branch);
        return getCommit(branchCommitSha1);
    }


    public Commit getCommit(String sha1) {
        File commitFile = Utils.join(COMMIT_DIR, sha1);
        if (!commitFile.exists() || !commitFile.isFile()) {
            exitWithError("No commit with that id exists.");
        }
        return Utils.readObject(commitFile, Commit.class);
    }


    /**
     * Get the staging area object
     */
    public Stage getStage() {
        return Utils.readObject(STAGE, Stage.class);
    }


    /**
     * Returns the SHA-1 hash of the concatenation of object
     */
    private String getObjectSha1(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }


    /**
     * Return sah1 string of files in working directory
     */
    private String getCwdFileSha1(String filename) {
        return getCwdFileSha1(Utils.join(CWD, filename));
    }


    private String getCwdFileSha1(File file) {
        return Utils.sha1(file.getName(), Utils.readContents(file));
    }


    /**
     * Persistent commit
     */
    public void persistentCommit(String sha1, Serializable obj) {
        Utils.writeObject(Utils.join(COMMIT_DIR, sha1), obj);
    }


    /**
     * Get the file directory of the commit or blob
     */
    public File getObjectFile(String sha1) {
        if (sha1 == null || sha1.isEmpty()) {
            return null;
        }
        File dir = Utils.join(OBJECTS_DIR, sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return Utils.join(dir, sha1.substring(2));
    }


    /**
     * Persistent stage
     */
    private void saveStage(Stage stage) {
        Utils.writeObject(STAGE, stage);
    }

}
