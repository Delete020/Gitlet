package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

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

    public void fetch(String remoteName, String remoteBranchName) throws IOException {
        File remoteGitlet = getRemoteGitlet(remoteName);
        if (!remoteGitlet.getName().equals(GITLET_NAME)) {
            exitWithError("Remote directory not found.");
        }

        File remoteBranch = Utils.join(remoteGitlet, "branches", remoteBranchName);
        if (!remoteBranch.exists()) {
            System.out.println(remoteBranch.toPath());
            exitWithError("That remote does not have that branch.");
        }

        String branchName = remoteName + "/" + remoteBranchName;
        File currentBranch = Utils.join(BRANCH_DIR, branchName);
        if (!currentBranch.exists()) {
            currentBranch.getParentFile().mkdir();
            currentBranch.createNewFile();
        }

        String remoteCommitSha1 = Utils.readContentsAsString(remoteBranch);
        String currentCommitSha1 = Utils.readContentsAsString(currentBranch);
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
     * Given remote branch name, get commit object
     */
    private Commit getRemoteBranch(File gitlet, String remoteBranchName) {
        return getRemoteCommit(gitlet, getRemoteCommitSha1(gitlet, remoteBranchName));
    }

    private String getRemoteCommitSha1(File gitlet, String remoteBranchName) {
        File remoteBranch = Utils.join(gitlet, "branches", remoteBranchName);
        if (!remoteBranch.exists()) {
            exitWithError("Remote directory not found.");
        }
        return Utils.readContentsAsString(remoteBranch);
    }

    /**
     * Get remove commit object
     */
    private Commit getRemoteCommit(File gitlet, String sha1) {
        File commit = Utils.join(gitlet, "commit", sha1);
        return Utils.readObject(commit, Commit.class);
    }

    /**
     * delete files of remote working directory
     */
    private void clearRemoteWorkingDirectory(File workingDir, Commit remoteCommit) {
        remoteCommit.getBlobs().keySet().forEach(filename -> Utils.join(workingDir, filename).delete());
    }

    private void pushFile(File currentGitlet, File remoteGitlet, Commit head) throws IOException {
        Map<String, String> blobs = head.getBlobs();
        for (String filename : blobs.keySet()) {

            String fileSha1 = blobs.get(filename);
            File currentFile = getObjectFile(currentGitlet, fileSha1);
            File remoteFile = getObjectFile(remoteGitlet, fileSha1);
            Files.copy(currentFile.toPath(), remoteFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

    }

    private File getObjectFile(File gitletDir, String sha1) {
        File dir = Utils.join(gitletDir, "objects", sha1.substring(0, 2));
        if (!dir.exists()) {
            dir.mkdir();
        }
        return Utils.join(dir, sha1.substring(2));
    }

    private void remoteAdd(GitletRepository remote, Commit head) throws IOException {
        for (String filename : head.getBlobs().keySet()) {
            remote.add(filename);
        }
    }
}
