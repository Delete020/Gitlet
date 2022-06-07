package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.GitletRepository.exitWithError;

/**
 * @author Delete020
 * @since 5/30/22 9:14 PM
 */
public class Stage implements Serializable {
    private Map<String, String> additionMap;
    private Map<String, String> removalMap;

    public Stage() {
        additionMap = new TreeMap<>();
        removalMap = new TreeMap<>();
    }

    public Map<String, String> getAdditionMap() {
        return additionMap;
    }

    public void setAdditionMap(Map<String, String> additionMap) {
        this.additionMap = additionMap;
    }

    public Map<String, String> getRemovalMap() {
        return removalMap;
    }

    public void setRemovalMap(Map<String, String> removalMap) {
        this.removalMap = removalMap;
    }
}
