package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static gitlet.GitletRepository.exitWithError;

/**
 * @author Delete020
 * @since 6/7/22 4:45 PM
 */
public class RemoteRepository {

    private String CWD = System.getProperty("user.dir");
    private final String GITLET_NAME = ".gitlet";
    private final File GITLET_DIR = Utils.join(CWD, GITLET_NAME);
    private final File REMOTE_DIR = Utils.join(GITLET_DIR, "remote");

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

        GitletRepository currentGitletRepository = new GitletRepository();

        String remoteBranchCommitSha1 = getRemoteCommitSha1(remoteGitlet, remoteBranchName);
        Commit headCommit = currentGitletRepository.getHead();
        // check the remote branch's head is in the history of the current local head
        String headSha1 = currentGitletRepository.getHeadSha1();
        while (headSha1 != null) {
            Commit commit = currentGitletRepository.getCommit(headSha1);
            if (commit.getParent() == null) {
                exitWithError("Please pull down remote changes before pushing.");
            }
            if (headSha1.equals(remoteBranchCommitSha1)) {
                break;
            }
            headSha1 = commit.getParent();
        }

        Commit remoteCommit = getRemoteCommit(remoteGitlet, remoteBranchCommitSha1);
        File remoteWorkingDir = remoteGitlet.getParentFile();
        clearRemoteWorkingDirectory(remoteWorkingDir, remoteCommit);
        pushFile(remoteWorkingDir, headCommit.getBlobs());


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
        File remoteBranch = Utils.join(gitlet, "branch", remoteBranchName);
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

    private void pushFile(File remoteWorkingDir, Map<String, String> blobs) throws IOException {
        for (String filename : blobs.keySet()) {
            //File currentFile = GitletRepository.getObjectFile(blobs.get(filename));
            File remoteFile = Utils.join(remoteWorkingDir, filename);
            //Files.copy(currentFile.toPath(), remoteFile.toPath());
        }
    }

    private String createRemoteCommit(File remoteGitlet) {
        return null;
    }
}
