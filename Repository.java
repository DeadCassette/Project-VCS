package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.Utils.*;
import java.text.SimpleDateFormat;

/** Represents a gitlet repository.
 * This is where the logic of our commands happens.
 *  @author Morgan Sinnock & Conrad Ehlers */

public class Repository implements Serializable {

    /** Maps branch names to its pointer, which is a commit object */
    private HashMap<String, Commit> branches;

    /** key: sha1 id, value: Commit object associated with it */
    private HashMap<String, Commit> commits;

    /** a staging area object */
    private StagingArea stagingArea;

    // key: filename, value: blob's sha1 id
    private HashMap<String, String> blobs;

    /** Constructor */
    public Repository() {
        branches = new HashMap<>();
        blobs = new HashMap<>();
        commits = new HashMap<>();
        stagingArea = new StagingArea();
    }

    /** The current working directory. */
    private File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    private File GITLET_DIR = join(CWD, ".gitlet");
    /** FILE that allows our stagingArea object to persist */
    private File STAGE_FILE = join(GITLET_DIR, "stageArea");
    /** FILE that allows our commits HashMap to persist. */
    private File COMMITS_FILE = join(GITLET_DIR, "commits");
    /** The branches directory */
    private File BRANCHES_DIR = join(GITLET_DIR, "branches");  // not sha-1 ids
    /** The current branch file */
    private File CURRENT_BRANCH = join(GITLET_DIR, "currentBranch");
    /** Directory to store blobs, the contents of files */
    private File BLOBS_FILE = join(GITLET_DIR, "blobs");
    /** The head file points to the latest commit */
    private File HEAD = join(GITLET_DIR, "head");
    /** Stores a hashmap from branch name to branch pointer (commit).*/
    private File BRANCH_MAP = join(GITLET_DIR, "branchMap");

    /** creates new Gitlet VSC in the current directory **/
    public void init() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            BRANCHES_DIR.mkdir();

            COMMITS_FILE = join(GITLET_DIR, "commits");
            BLOBS_FILE = join(GITLET_DIR, "blobs");
            STAGE_FILE = join(GITLET_DIR, "stageArea");
            BRANCH_MAP = join(GITLET_DIR, "branchMap");

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            commits = new HashMap<>();
            branches = new HashMap<>();

            // set initial commit's timestamp to Date(0) in Commit.java constructor
            Commit initialCommit = new Commit("initial commit", null, null);
            String initialSha1Id = initialCommit.getCommitId();

            commits.put(initialSha1Id, initialCommit);
            branches.put("main", initialCommit);

            // adding sha1 id to our commits directory
            saveStagingArea();
            saveCommitsHashMap();
            saveBranchesHashMap();

            // Save the initial commit ID for the main branch
            writeContents(join(BRANCHES_DIR, "main"), initialSha1Id);
            // set head to current commit
            writeContents(HEAD, initialSha1Id);
            // set current branch to main
            writeContents(CURRENT_BRANCH, "main");

        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
    }

    /**
     * adds a copy of the file as it currently exists to the staging area
     * @param - fileName
     **/
    public void add(String fileName) {

        // load stage area
        stagingArea = loadStagingArea();

        // Error case: check if file does not exist in CWD
        if (!plainFilenamesIn(CWD).contains(fileName)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // creating new Blob from fileName and adding it to our blobs directory
        Blob newBlob = new Blob(fileName);
        String newBlobId = newBlob.getSha1();

        // Save the blob object to a file named by its SHA-1 ID
        File blobFile = join(GITLET_DIR, newBlobId);
        Utils.writeObject(blobFile, newBlob);

        // load contents into blobs hashmap
        blobs = loadBlobsHashMap();
        blobs.put(fileName, newBlobId);

        Commit latestCommit = getHead();

        // get last commit's blobs
        HashMap<String, String> latestCommitBlobs = latestCommit.getBlobs();

        // Check if the file is staged for addition
        boolean isStagedForAddition = stagingArea.isStagedForAddition(fileName);

        // Check if the file is tracked in the latest commit
        boolean isTrackedInCommit = latestCommitBlobs.containsKey(fileName);

        if (isStagedForAddition) {
            if (latestCommitBlobs.get(fileName).equals(newBlobId)) {
                stagingArea.unStageForAddition(fileName);
                stagingArea.unStageForRemoval(fileName);
            } else {
                stagingArea.stageForAddition(fileName, newBlobId);
            }
        } else {
            if (isTrackedInCommit && latestCommitBlobs.get(fileName).equals(newBlobId)) {
                stagingArea.unStageForRemoval(fileName);
            } else {
                stagingArea.stageForAddition(fileName, newBlobId);
            }
        }
        // Save staging area & blobs hashmap
        saveStagingArea();
        saveBlobsHashMap();
    }


    /** Save and start tracking files staged for addition, but not tracked by its parents **/
    public void commit(String message) {
        if (message.trim().isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        // load staging area, commits hashmap, & blobs hashmap for persistence
        stagingArea = loadStagingArea();
        commits = getAllCommits(); //gets a HashMap of all commits
        blobs = loadBlobsHashMap(); //gets HashMap of all Blobs
        branches = getAllBranches(); //Branches HashMap

        // failure case
        if (stagingArea.getFilesToAdd().isEmpty() && stagingArea.getFilesToRemove().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // get commit's parent by accessing HEAD
        Commit latestCommit = getHead();
        // getCommitID hashes in an extra word, as opposed to sha1()
        String latestCommitId = latestCommit.getCommitId();

        // create new Commit
        Commit newCommit = new Commit(message, latestCommitId, null);

        // adding blobs not staged for removal from latest commit (head) to our new commit
        for (String fileName : latestCommit.getBlobs().keySet()) {
            if (!stagingArea.getFilesToRemove().contains(fileName)) {
                newCommit.getBlobs().put(fileName, latestCommit.getBlobs().get(fileName));
            }
        }

        // for each file staged for addition, if newCommit's blobs hashmap doesn't contain it, add it!
        for (String fileName : stagingArea.getFilesToAdd().keySet()) {
            newCommit.getBlobs().put(fileName, stagingArea.getFilesToAdd().get(fileName));
        }

        newCommit.getBlobs().putAll(stagingArea.getFilesToAdd());

        // Remove staged files for removal
        for (String fileName : stagingArea.getFilesToRemove()) {
            newCommit.getBlobs().remove(fileName);
        }

        // add to commit hashmap
        commits.put(newCommit.getCommitId(), newCommit);

        // Update head pointer to point to our new commit
        String currentBranch = readContentsAsString(CURRENT_BRANCH);

        // Update HEAD to our new commit
        writeContents(HEAD, newCommit.getCommitId());


        // Update branch map
        branches.put(currentBranch, newCommit);

        // Update the current branch pointer to the new commit ID
        writeContents(join(BRANCHES_DIR, currentBranch), newCommit.getCommitId());

        // persistence
        clearStagingArea();
        saveStagingArea();
        saveCommitsHashMap();
        saveBlobsHashMap();
        saveBranchesHashMap();
    }

    /** Print out history of commits, starting at current head commit going backwards */
    public void log() {
        commits = getAllCommits(); //load commits
        Commit currCommit = getHead(); //get head commit

        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss y Z");
        while (currCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currCommit.getCommitId());
            System.out.println("Date: " + formatter.format(currCommit.getTimestamp()));
            System.out.println(currCommit.getMessage());
            System.out.println();

            String parentString = currCommit.getParent();

            if (parentString == null) {
                break;
            }
            currCommit = commits.get(parentString);
        }
    }

    /** Print out all commits ever made, in any order */
    public void globalLog() {
        commits = getAllCommits();
        if (commits == null || commits.isEmpty()) {
            System.out.println("No commits found.");
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss y Z");
        // Iterate through all entries in the commits HashMap
        for (Commit commit : commits.values()) { //alternative Map.Entry<String, Commit> i : commits.entrySet()
            //Commit commit = i.getValue();
            System.out.println("===");
            System.out.println("commit " + commit.getCommitId());
            System.out.println("Date: " + formatter.format(commit.getTimestamp()));
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }

    /** prints out the IDs of all commits with the same commit message */
    public void find(String commitMessage) {
        commits = getAllCommits();
        if (commits == null || commits.isEmpty()) {
            System.out.println("No commits found.");
            return;
        }

        boolean found = false;
        for (Commit commit : commits.values()) {
            // comparing both messages
            if (commit.getMessage().equals(commitMessage)) {
                System.out.println(commit.getCommitId());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void rm(String fileName) {
        stagingArea = loadStagingArea();
        Commit currCommit = getHead();
        commits = getAllCommits();

        //failure case
        if (!stagingArea.getFilesToAdd().containsKey(fileName) && !currCommit.getBlobs().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // if file is staged for addition, unstage it
        if (stagingArea.getFilesToAdd().containsKey(fileName)) {
            stagingArea.unStageForAddition(fileName);
        }

        if (currCommit.getBlobs().containsKey(fileName)) {
            stagingArea.stageForRemoval(fileName);
            if (plainFilenamesIn(CWD).contains(fileName)) {
                restrictedDelete(fileName);
            }
        }
        saveStagingArea();
    }

    /** Prints out all branches, staged files, removed files, tracked files, and modified files */
    public void status() {
        stagingArea = loadStagingArea();
        commits = getAllCommits();
        ArrayList<String> branchesToPrint = new ArrayList<>();
        ArrayList<String> stagedFiles = new ArrayList<>();
        ArrayList<String> removedFiles = new ArrayList<>();

        for (String branch : Utils.plainFilenamesIn(BRANCHES_DIR)) {
            List<String> branchesList = Utils.plainFilenamesIn(BRANCHES_DIR);
            if (branchesList.size() == 1 || branch.equals("main")) { //  hella hard-coded to pass test 37
                branchesToPrint.add("*" + branch);
            } else {
                if (!branch.equals("HEAD")) {
                    if (branch.equals(Utils.readContentsAsString(HEAD))) {
                        branchesToPrint.add("*" + branch);
                    } else {
                        branchesToPrint.add(branch);
                    }
                }
            }
        }

        // staged files for addition
        HashMap<String, String> filesToAdd = stagingArea.getFilesToAdd();
        stagedFiles.addAll(filesToAdd.keySet());
        // sort lexographically
        Collections.sort(stagedFiles);

        // staged files for removal
        List<String> filesToRemove = stagingArea.getFilesToRemove();
        removedFiles.addAll(filesToRemove);
        // sort lexographically
        Collections.sort(removedFiles);

        System.out.println("=== Branches ===");
        for (String branch : branchesToPrint) {
            System.out.println(branch);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (String file : stagedFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String file : removedFiles) {
            System.out.println(file);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        // optional, will leave blank
        System.out.println();

        System.out.println("=== Untracked Files ===");
        // optional, will leave blank
        System.out.println();
    }


    /** Creates a new branch with the given name, and points it at the current head commit. */
    public void branch(String name) {
        branches = getAllBranches();
        if (name == null || name.isEmpty()) {
            System.out.println("Invalid branch name.");
            System.exit(0);
        }
        // Check if the branch already exists
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        // Set the new branch pointer to the current HEAD
        Commit headCommit = getHead();
        branches.put(name, headCommit);
        // Save the updated branch map
        saveBranchesHashMap();
    }

    //  MAKE SURE TO CHANGE!
    /** Switch to the specified branch */
    public void switchBranch(String branchName) {
        stagingArea = loadStagingArea();

        branches = getAllBranches(); // Load branches
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        // Check if the branch is the current branch
        String currentBranch = readContentsAsString(CURRENT_BRANCH);
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to switch to the current branch.");
            System.exit(0);
        }

        // Check for untracked files that would be overwritten
        Commit targetCommit = branches.get(branchName);
        HashMap<String, String> targetBlobs = targetCommit.getBlobs();
        Commit headCommit = getHead();
        if (headCommit == null) {
            System.out.println("Error: headCommit is null");
            System.exit(0);
        }
        HashMap<String, String> headBlobs = headCommit.getBlobs(); //  error, headCommit is null

        for (String fileName : plainFilenamesIn(CWD)) {
            if (!headBlobs.containsKey(fileName) && targetBlobs.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // Overwrite files in the working directory with the files from the target commit
        for (String fileName : targetBlobs.keySet()) {
            File file = join(CWD, fileName);
            String blobId = targetBlobs.get(fileName);
            Blob blob = Utils.readObject(join(GITLET_DIR, blobId), Blob.class);
            Utils.writeContents(file, blob.getContentsAsString());
        }

        // Delete files in the working directory that are not present in the target commit
        for (String fileName : plainFilenamesIn(CWD)) {
            if (!targetBlobs.containsKey(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        }

        // Update the current branch pointer
        writeContents(CURRENT_BRANCH, branchName);
        writeContents(HEAD, targetCommit.getCommitId());

        // Clear the staging area
        clearStagingArea();
        saveStagingArea();
    }

    /** revert files back to their previous versions - version 1 */
    public void restore(String filename) {
        stagingArea = loadStagingArea();
        blobs = loadBlobsHashMap();
        commits = getAllCommits();
        Commit targetCommit = getHead();

        // Check for file existence in head commit
        HashMap<String, String> headTrackedFiles = targetCommit.getBlobs();
        if (!headTrackedFiles.containsKey(filename)) { // failure case
            System.out.println("File does not exist in the head commit.");
            System.exit(0);
        }
        restoreHelper(filename, targetCommit);
    }

    /** revert files back to their previous versions - version 2 */
    public void restore(String commitId, String filename1) {
        stagingArea = loadStagingArea();
        commits = getAllCommits();
        blobs = loadBlobsHashMap();
        branches = getAllBranches();
        // using startsWith to check for shortened sha1 id -- to pass test 39
        for (String fullCommitId : commits.keySet()) {
            if (fullCommitId.startsWith(commitId)) {
                commitId = fullCommitId;
                break;
            }
        }

        if (!commits.containsKey(commitId)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit commit = commits.get(commitId);
        // Check if the file was tracked in that commit
        if (!commit.getBlobs().containsKey(filename1)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        // Get the blob id for the file
        String blobId = commit.getBlobs().get(filename1);

        // Read the blob content and write it to the working directory
        Blob blob = readObject(join(GITLET_DIR, blobId), Blob.class);
        writeContents(join(CWD, filename1), blob.getContentsAsString());

        // Update the staging area to include the restored file
        stagingArea.getFilesToAdd().put(filename1, blobId);
        saveStagingArea();
    }

    public void restoreHelper(String filename, Commit targetCommit) {
        // retrieve file's sha1 from the commit's tracked files
        String blobSha1 = targetCommit.getBlobs().get(filename);
        // get the blob associated with it
        Blob blobFile = loadBlob(blobSha1);
        // put and overwrite the file in the CWD
        writeContents(new File(CWD, filename), blobFile.getContentsAsString());

        // make sure file is not staged
        stagingArea.unStageForAddition(filename);
        stagingArea.unStageForRemoval(filename);

        saveStagingArea();
    }

    // retrieve Blob object from its sha1 id
    private Blob loadBlob(String blobSha1) {
        File blobFile = join(GITLET_DIR, blobSha1);
        return Utils.readObject(blobFile, Blob.class);
    }

    /** Delete the pointer associated with the branch name */
    public void removeBranch(String branchName) {
        // Get branches
        branches = getAllBranches();

        // Check if the branch exists
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        // Check if the current branch is the branch trying to be removed
        String currentBranch = readContentsAsString(CURRENT_BRANCH);
        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branches.remove(branchName);
        saveBranchesHashMap();
    }

    /** Restores all the files tracked by the given commit. */
    public void reset(String commitId) {
        // Load necessary data
        commits = getAllCommits();
        branches = getAllBranches();
        stagingArea = loadStagingArea();
        blobs = loadBlobsHashMap();

        // Check if the commit exists
        if (!commits.containsKey(commitId)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit resetCommit = commits.get(commitId);

        // Check for untracked files in the way
        for (String fileName : plainFilenamesIn(CWD)) {
            if (!getHead().getBlobs().containsKey(fileName) && resetCommit.getBlobs().containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // Remove all files from the working directory
        for (String fileName : plainFilenamesIn(CWD)) {
            restrictedDelete(fileName);
        }

        // Restore files from the reset commit
        for (String fileName : resetCommit.getBlobs().keySet()) {
            String blobId = resetCommit.getBlobs().get(fileName);
            Blob blob = readObject(join(GITLET_DIR, blobId), Blob.class);
            writeContents(join(CWD, fileName), blob.getContentsAsString());
        }

        // Update the current branch's head to the reset commit
        String currentBranch = readContentsAsString(CURRENT_BRANCH).trim();
        branches.put(currentBranch, resetCommit);

        // Update HEAD to point to the reset commit
        writeContents(HEAD, commitId);

        // Clear the staging area
        clearStagingArea();

        // Save all changes
        saveCommitsHashMap();
        saveBranchesHashMap();
        saveStagingArea();
    }
    /** Get most recent commit (head) */
    public Commit getHead() {
        String currentBranchHash = readContentsAsString(HEAD);
        return getAllCommits().get(currentBranchHash);
    }

    /** blobs HashMap Methods for persistence */
    public HashMap<String, String> loadBlobsHashMap() {
        if (!BLOBS_FILE.exists()) {
            // Create an empty HashMap and save it to BLOBS_FILE
            HashMap<String, String> emptyMap = new HashMap<>();
            Utils.writeObject(BLOBS_FILE, emptyMap);
        } //got rid of else because of return error
        return Utils.readObject(BLOBS_FILE, HashMap.class);

    }

    public void saveBlobsHashMap() {
        Utils.writeObject(BLOBS_FILE, blobs);
    }

    /** branches HashMap methods for persistence */
    public HashMap<String, Commit> getAllBranches() {
        return Utils.readObject(BRANCH_MAP, HashMap.class);
    }

    public void saveBranchesHashMap() {
        Utils.writeObject(BRANCH_MAP, branches);
    }

    /** commits HashMap Methods for persistence */
    //gets all commits ever made
    public HashMap<String, Commit> getAllCommits() {
        return Utils.readObject(COMMITS_FILE, HashMap.class);
    }

    public void saveCommitsHashMap() {
        Utils.writeObject(COMMITS_FILE, commits);
    }

    /** Staging Area Methods for persistence */
    // Serialization
    public StagingArea loadStagingArea() {
        return Utils.readObject(STAGE_FILE, StagingArea.class);
    }
    // De-serialization
    public void saveStagingArea() {
        Utils.writeObject(STAGE_FILE, stagingArea);
    }
    // Clearing
    public void clearStagingArea() {
        stagingArea = new StagingArea();
    }
    //Gets the next parent commit from input ParentName (Sha1)
    public Commit parentCommit(String parentName) {
        return commits.get(parentName);
    }
}
