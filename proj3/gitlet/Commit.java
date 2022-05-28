package gitlet;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Delete020
 * @since 5/28/22 3:45 PM
 */
public class Commit implements Serializable {
    private String message;
    private ZonedDateTime timestamp;
    private final String parent;
    Map<String, String> blobs;

    public Commit(String message, String parent) {
        this.message = message;
        this.timestamp = ZonedDateTime.now();
        this.parent = parent;
        this.blobs = new TreeMap<>();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getParent() {
        return parent;
    }

    public Map<String, String> getBlobs() {
        return blobs;
    }

    public void setBlobs(Map<String, String> blobs) {
        this.blobs = blobs;
    }
}
