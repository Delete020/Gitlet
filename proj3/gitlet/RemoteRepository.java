package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static gitlet.GitletRepository.exitWithError;

/**
 * @author Delete020
 * @since 6/7/22 4:45 PM
 */
public class RemoteRepository {

    private final String CWD = System.getProperty("user.dir");
    private final String GITLET_NAME = ".gitlet";
    private final File GITLET_DIR = Utils.join(CWD, GITLET_NAME);
    private final File REMOTE_DIR = Utils.join(GITLET_DIR, "remote");
    private final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");
    private final File COMMIT_DIR = Utils.join(GITLET_DIR, "commit");
    private final File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");
    private final File HEAD = Utils.join(GITLET_DIR, "HEAD");


    /**
     * add a remote gitlet directory
     */
    public void addRemote(String removeName, String path) throws IOException {
        File removeFile = Utils.join(REMOTE_DIR, removeName);
        if (removeFile.exists()) {
            exitWithError("A remote with that name already exists.");
        }
        removeFile.createNewFile();
        Utils.writeContents(removeFile, path);
    }


    /**
     * remove remote gitlet
     */
    public void rmRemote(String removeName) {
        File removeFile = Utils.join(REMOTE_DIR, removeName);
        if (!removeFile.exists()) {
            exitWithError("A remote with that name does not exist.");
        }
        removeFile.delete();
    }

    public void push(String remoteName, String remoteBranchName) throws IOException {
        File remoteGitlet = getRemoteGitlet(remoteName);
        if (!remoteGitlet.getName().equals(GITLET_NAME)) {
            exitWithError("Remote directory not found.");
        }

        // get two gitletRepository
        GitletRepository currentGitletRepository = new GitletRepository();
        File remoteWorkingDir = remoteGitlet.getParentFile();
        GitletRepository remoteGitletRepository = new GitletRepository(remoteWorkingDir.getPath());

        // check the remote branch's head is in the history of the current local head
        String remoteCommitSha1 = getRemoteCommitSha1(remoteGitlet, remoteBranchName);
        String headSha1 = currentGitletRepository.getHeadSha1();
        Commit headCommit = currentGitletRepository.getHead();

        // check current commit history
        String sha1 = headSha1;
        while (sha1 != null) {
            Commit commit = currentGitletRepository.getCommit(sha1);
            if (commit.getParent() == null) {
                exitWithError("Please pull down remote changes before pushing.");
            }
            if (sha1.equals(remoteCommitSha1)) {
                break;
            }
            sha1 = commit.getParent();
        }

        // push current objects to remote objects directory
        pushFile(GITLET_DIR, remoteGitlet, headCommit);

        // create new commit
        Commit newCommit = new Commit(headCommit.getMessage(), remoteCommitSha1);
        newCommit.setBlobs(headCommit.getBlobs());
        // persistent commit to remote gitlet directory
        String newCommitSha1 = Utils.sha1(Utils.serialize(newCommit));
        remoteGitletRepository.persistentCommit(headSha1, newCommit);
        remoteGitletRepository.reset(headSha1);
    }


    /**
     * Brings down commits from the remote Gitlet repository into the local Gitlet repository.
     */
    public void fetch(String remoteName, String remoteBranchName) throws IOException {
        File remoteGitlet = getRemoteGitlet(remoteName);
        if (!remoteGitlet.getName().equals(GITLET_NAME) || !remoteGitlet.exists()) {
            exitWithError("Remote directory not found.");
        }

        File remoteBranch = Utils.join(remoteGitlet, "branches", remoteBranchName);
        if (!remoteBranch.exists()) {
            exitWithError("That remote does not have that branch.");
        }

        // create new branch in current gitlet
        String branchName = remoteName + "/" + remoteBranchName;
        File currentBranch = Utils.join(BRANCH_DIR, branchName);
        if (!currentBranch.exists()) {
            currentBranch.getParentFile().mkdir();
            currentBranch.createNewFile();
        }

        String remoteCommitSha1 = Utils.readContentsAsString(remoteBranch);
        Utils.writeContents(currentBranch, remoteCommitSha1);

        // copies all remote commits and blobs to current gitlet
        while (remoteCommitSha1 != null) {
            File remoteCommitFile = Utils.join(remoteGitlet, "commit", remoteCommitSha1);
            File currentCommitFile = Utils.join(COMMIT_DIR, remoteCommitSha1);
            Commit remoteCommit = getRemoteCommit(remoteGitlet, remoteCommitSha1);
            remoteCommitSha1 = remoteCommit.getParent();
            if (currentCommitFile.exists()) {
                break;
            }
            // copy commit
            Files.copy(remoteCommitFile.toPath(), currentCommitFile.toPath());
            // copy blobs
            pushFile(remoteGitlet, GITLET_DIR, remoteCommit);
        }
    }


    /**
     * Simple fetch and merge remote branch
     */
    public void pull(String remoteName, String remoteBranchName) throws IOException {
        fetch(remoteName, remoteBranchName);
        GitletRepository gitletRepository = new GitletRepository();
        gitletRepository.merge(remoteName + "/" + remoteBranchName);
    }


    /**
     * Compares the contents of a commit with a working directory or compares two commits
     */
    public void diff(String... branches) {
        switch (branches.length) {
            case 0 -> diffHeadWithWorkingDirectory();
            case 1 -> diffBranchWithWorkingDirectory(branches[0]);
            case 2 -> diffTwoBranch(branches[0], branches[1]);
        }
    }

    /**
     * Compare the commit at the head of the current branch with the files in the working directory.
     */
    private void diffHeadWithWorkingDirectory() {
        Map<String, String> blobs = getHead().getBlobs();
        for (String filename : blobs.keySet()) {
            File commitFile = getCurrentObjectFile(blobs.get(filename));
            File workingFile = getWorkingDirectoryFile(filename);
            diffs(filename, commitFile, workingFile);
        }
    }


    /**
     * Compare commit in the specified branch header with files in the working directory.
     */
    private void diffBranchWithWorkingDirectory(String branch) {
        if (branchNotExist(branch)) {
            exitWithError("A branch with that name does not exist.");
        }
        Map<String, String> blobs = getBranch(branch).getBlobs();
        for (String filename : blobs.keySet()) {
            File commitFile = getCurrentObjectFile(blobs.get(filename));
            File workingFile = getWorkingDirectoryFile(filename);
            diffs(filename, commitFile, workingFile);
        }
    }


    /**
     * Compares the files in two specified branch header commits.
     */
    private void diffTwoBranch(String firstBranch, String secondBranch) {
        if (branchNotExist(firstBranch) && branchNotExist(secondBranch)) {
            exitWithError("At least one branch does not exist.");
        }
        Map<String, String> firstBlobs = getBranch(firstBranch).getBlobs();
        Map<String, String> secondBlobs = getBranch(secondBranch).getBlobs();
        Set<String> blobs = new TreeSet<>(firstBlobs.keySet());
        blobs.addAll(secondBlobs.keySet());

        for (String filename : blobs) {
            String firstSha1 = firstBlobs.get(filename);
            String secondSha1 = secondBlobs.get(filename);
            File firstFile = firstSha1 == null ? null : getCurrentObjectFile(firstBlobs.get(filename));
            File secondFile = secondSha1 == null ? null : getCurrentObjectFile(secondBlobs.get(filename));
            diffs(filename, firstFile, secondFile);
        }
    }


    /**
     * Check branch is not in the current gitlet
     */
    private boolean branchNotExist(String branch) {
        return !Utils.join(BRANCH_DIR, branch).exists();
    }


    /**
     * Use diff to compare content of two file
     */
    private void diffs(String filename, File firstVersion, File secondVersion) {
        Diff diff = new Diff();
        // set two file to Diff
        diff.setSequences(firstVersion, secondVersion);
        // If two files are the same, skip
        if (diff.sequencesEqual()) {
            return;
        }

        // output compare information
        diffHeaderInfo(filename, firstVersion, secondVersion);
        // diff line array
        int[] diffs = diff.diffs();
        for (int i = 0; i < diffs.length; i += 4) {
            int n1 = diffs[i + 1];
            int n2 = diffs[i + 3];
            int l1 = n1 == 0 ? diffs[i] : diffs[i] + 1;
            int l2 = n2 == 0 ? diffs[i + 2] : diffs[i + 2] + 1;

            // sequence of edits
            String edits1 = n1 == 1 ? l1 + "" : l1 + "," + n1;
            String edits2 = n2 == 1 ? l2 + "" : l2 + "," + n2;
            System.out.println("@@ -" + edits1 + " +" + edits2 + " @@");

            l1 = diffs[i];
            for (int j = 0; j < n1; j++) {
                System.out.println("-" + diff.get1(l1 + j));
            }

            l2 = diffs[i + 2];
            for (int j = 0; j < n2; j++) {
                System.out.println("+" + diff.get2(l2 + j));
            }
        }
    }


    /**
     * The start of the differences for one of the files in the two versions
     */
    private void diffHeaderInfo(String filename, File firstVersionFile, File secondVersionFile) {
        String firstFilename = firstVersionFile != null && firstVersionFile.exists() ? "a/" + filename : "/dev/null";
        String secondFilename = secondVersionFile != null && secondVersionFile.exists() ? "b/" + filename : "/dev/null";
        System.out.println("diff --git " + firstFilename + " " + secondFilename);
        System.out.println("--- " + firstFilename);
        System.out.println("+++ " + secondFilename);
    }


    /**
     * Get file object of remove gitlet directory
     */
    private File getRemoteGitlet(String remoteName) {
        File remote = Utils.join(REMOTE_DIR, remoteName);
        if (!remote.exists()) {
            exitWithError("A remote with that name does not exist.");
        }
        return Utils.join(Utils.readContentsAsString(remote));
    }


    /**
     * Get the sha1 of the specified branch header in the specified gitlet directory
     */
    private String getRemoteCommitSha1(File gitlet, String remoteBranchName) {
        File remoteBranch = Utils.join(gitlet, "branches", remoteBranchName);
        if (!remoteBranch.exists()) {
            exitWithError("Remote directory not found.");
        }
        return Utils.readContentsAsString(remoteBranch);
    }


    /**
     * Get commit object using sha1
     */
    private Commit getCurrentCommit(String sha1) {
        return getRemoteCommit(GITLET_DIR, sha1);
    }

    /**
     * Get remove commit object
     */
    private Commit getRemoteCommit(File gitlet, String sha1) {
        File commit = Utils.join(gitlet, "commit", sha1);
        return Utils.readObject(commit, Commit.class);
    }


    /**
     * Push current files of head commit to the objects directory of remote gitlet
     */
    private void pushFile(File currentGitlet, File remoteGitlet, Commit head) throws IOException {
        Map<String, String> blobs = head.getBlobs();
        for (String filename : blobs.keySet()) {
            String fileSha1 = blobs.get(filename);
            File currentFile = getObjectFile(currentGitlet, fileSha1);
            File remoteFile = getObjectFile(remoteGitlet, fileSha1);
            Files.copy(currentFile.toPath(), remoteFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }


    /**
     * Get the object file under the current gitlet
     */
    private File getCurrentObjectFile(String sha1) {
        return getObjectFile(GITLET_DIR, sha1);
    }


    /**
     * Get the object file under the specified gitlet
     */
    private File getObjectFile(File gitletDir, String sha1) {
        File dir = Utils.join(gitletDir, "objects", sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return Utils.join(dir, sha1.substring(2));
    }


    /**
     * Get the files in the current working directory
     */
    private File getWorkingDirectoryFile(String filename) {
        return Utils.join(CWD, filename);
    }


    /**
     * Get head commit object
     */
    private Commit getHead() {
        String headContext = Utils.readContentsAsString(HEAD);
        return getBranch(headContext);
    }


    /**
     * Get the head commit of the specified branch
     */
    private Commit getBranch(String branch) {
        String commitSha1 = Utils.readContentsAsString(Utils.join(BRANCH_DIR, branch));
        return getCurrentCommit(commitSha1);
    }

}
