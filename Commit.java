package gitlet;
import java.io.Serializable;
import java.util.*;


/** Represents a gitlet commit object, a snapshot of our CWD
 *  @author Morgan Sinnock & Conrad Ehlers
 */

public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;

    /** The commit's parent's sha1 id */
    private String parent; // changed to String

    /** timestamp of the commit */
    private Date timestamp;

    /** list of parent commits (useful for merge case) */
    private List<String> parents;

    /** the sha1 id of commit's merge parent (useful for merge case) */
    private String mergeParent;

    /** key = fileName, value = blob's sha1 id that has fileName's contents */
    private HashMap<String, String> blobs;

    private String id;

    /** Constructor */
    public Commit(String message, String parent, String mergeParent) {
        this.message = message;
        this.parent = parent;
        this.timestamp = new Date(); // NOTE: no longer using data as a parameter in Commit constructor
        this.blobs = new HashMap<>();
        this.mergeParent = mergeParent;
        this.id = Utils.sha1(Utils.serialize(this));

        // setting up initial commit
        if (message.equals("initial commit")) {
            this.timestamp = new Date(0);
            this.parent = null;
        }
    }

    /** get commit's parent as sha1 id */
    public String getParent() {
        return this.parent;
    }

    public void setBlobs(HashMap<String, String> blobs) {
        this.blobs = blobs;
    }

    /** get commit's parents as list of sha1 ids */
    public List<String> getParents() {
        return this.parents;
    }
    /** get commit's timestamp as a Date object */
    public Date getTimestamp() {
        return this.timestamp;
    }

    /** get commit's message */
    public String getMessage() {
        return this.message;
    }

    /** get commit's merge parent (if it has one) */
    public String getMergeParent() {
        return mergeParent;
    }

    /** get commit's blob references (see diagram in slides) */
    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public String getCommitId() {
        return id;
    }
}


