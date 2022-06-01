package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.TreeMap;

/**
 * @author Delete020
 * @since 5/30/22 9:14 PM
 */
public class Stage implements Serializable {
    TreeMap<String, String> additionMap;
    TreeMap<String, String> removalMap;

    public Stage() {
        additionMap = new TreeMap<>();
        removalMap = new TreeMap<>();
    }

    public TreeMap<String, String> getAdditionMap() {
        return additionMap;
    }

    public void setAdditionMap(TreeMap<String, String> additionMap) {
        this.additionMap = additionMap;
    }

    public TreeMap<String, String> getRemovalMap() {
        return removalMap;
    }

    public void setRemovalMap(TreeMap<String, String> removalMap) {
        this.removalMap = removalMap;
    }

    /**
     * Copy files from the working directory to the staging area
     * @param fileName File name
     * @param file Working directory file
     */
    public void addFile(String fileName, File file) throws IOException {
        String stageFileSha1 = Utils.sha1(Utils.readContents(file));
        File stageFile = GitletRepository.getObjectFile(stageFileSha1);

        removalMap.remove(fileName);

        // The file is identical to the parent commit file
        String parentVersion = GitletRepository.getHead().getBlobs().get(fileName);
        if (parentVersion != null &&  parentVersion.equals(stageFileSha1)) {
            additionMap.remove(fileName);
            return;
        }

        additionMap.put(fileName, stageFileSha1);

        // File already exists in the staging area, do nothing
        if (stageFile.exists()) {
            return;
        }

        // Copy file to staging area
        Files.copy(file.toPath(), stageFile.toPath());
    }


    /**
     * Deleting files from the staging area
     */
    private void deleteFile(String fileName) {
        Utils.join(GitletRepository.OBJECTS_DIR, fileName).delete();
    }
}
