package gitlet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/** A stagingArea object contains the files to be staged for addition or removal */

public class StagingArea implements Serializable {

    /** filesToAdd is a Hashmap with key = fileName, value = blob's sha1 id */
    private HashMap<String, String> filesToAdd;
    /** filesToRemove is an ArrayList with key = fileName */
    private ArrayList<String> filesToRemove;

    /** Constructor */
    public StagingArea() {
        this.filesToAdd = new HashMap<>();
        this.filesToRemove = new ArrayList<>();
    }

    /** Methods */
    public void stageForAddition(String fileName, String blobId) {
        filesToAdd.put(fileName, blobId);
        filesToRemove.remove(fileName); // make sure not staged for removal
    }

    public void unStageForAddition(String fileName) {
        filesToAdd.remove(fileName);
    }

    public void stageForRemoval(String fileName) {
        filesToRemove.add(fileName);
        filesToAdd.remove(fileName); // make sure not staged for addition
    }

    public void unStageForRemoval(String fileName) {
        filesToRemove.remove(fileName);
    }

    public void clearStagingArea() {
        filesToAdd.clear();
        filesToRemove.clear();
    }

    public boolean isStagedForAddition(String fileName) {
        return filesToAdd.containsKey(fileName);
    }

    public boolean isStagedForRemoval(String fileName) {
        return filesToRemove.contains(fileName);
    }

    // getters
    public HashMap<String, String> getFilesToAdd() {
        if (filesToAdd.isEmpty()) {
            return new HashMap<>();
        } else {
            return filesToAdd;
        }
    }

    public List<String> getFilesToRemove() {
        if (filesToRemove.isEmpty()) {
            return Collections.emptyList(); // better than returning null
        } else {
            return filesToRemove;
        }
    }
}
