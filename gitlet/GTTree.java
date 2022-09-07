package gitlet;


import com.sun.source.util.Trees;
import jdk.jshell.execution.Util;

import java.io.File;
import java.io.Serializable;
import java.text.spi.BreakIteratorProvider;
import java.util.*;

/**
 * Gitlet commit tree
 *
 * @author Tianyi Xu
 */


public class GTTree implements Serializable {
    // Get the current working directory
    static final File CWD = new File(System.getProperty("user.dir"));
    static final File REPO_DIR = Utils.join(CWD, ".gitlet");
    static final File COMMITS_DIR = Utils.join(REPO_DIR, "commits");
    static final File BLOBS_DIR = Utils.join(REPO_DIR, "blobs");
    static final File GIT_TREE = Utils.join(REPO_DIR, "gitTree");

    /* Pointer point to the most recent commit */
    private Commit _HEAD;
    // Name of the branch
    private String currentBranch;

    /* Map the file name to its contents sha1 */
    Map<String, byte[]> stagedFiles;

    /* Staged files for removing, file are from the most recent commmit*/
    Set<String> stagedrmFiles;

    /* Map the branch name to the sha1 of its most recent commit */
    Map<String, String> branches;


    public GTTree() {
        // initialize with treemap to keep string in lexicographic orders
        stagedFiles = new TreeMap<>();
        stagedrmFiles = new TreeSet<>();
        branches = new TreeMap<>();
    }

    void init() {
        REPO_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        Commit initial = new Commit("initial commit", null, new HashMap<>());
        initial.saveCommit();
        branches.put("master", initial.getID());
        _HEAD = initial;
        currentBranch = "master";
    }


    void add(String fileName) {
        File file = Utils.join(CWD, fileName);
        if (stagedrmFiles.contains(fileName)) {
            stagedrmFiles.remove(fileName);
            return;
        }


        if (!file.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }

        byte[] content = Utils.readContents(file);
        String sha1 = Utils.sha1(content);

        // Compare the file with the file in the  previous commit,
        //don't add to stage if the file hasn't modified
        if (_HEAD.getBlobs().containsKey(fileName)) {
            String preSha1 = _HEAD.getBlobs().get(fileName);
            if (sha1.equals(preSha1)) {
                stagedFiles.remove(fileName);
                return;
            }
        }

        stagedFiles.put(fileName, content);
        stagedrmFiles.remove(fileName);
    }

    /**
     * create a new commit
     * */
    void commit(String message) {
//        if (!branches.get(currentBranch).equals(_HEAD.getID())) {
//            throw Utils.error("HEAD pointer departures from the current branch" +
//                    "can't commit", currentBranch);
//        }
        if (stagedFiles.isEmpty() && stagedrmFiles.isEmpty()) {
            Utils.message("No changes added to the commit.");
            System.exit(0);
        }

        Map<String, String> headBlobs = _HEAD.getBlobs();
        Map<String, String> toCommit = new HashMap<>();

        /* Copy the commit head pointed to */
//        Map<String, String> toCommit = Map.copyOf(headBlobs); // immutable copy
        for (String fileName : headBlobs.keySet()) {
                toCommit.put(fileName, headBlobs.get(fileName));
        }

        /* Use the staged file to modify the blobs, also clear the staging area */
        for (String fileName : stagedFiles.keySet()) {
            byte[] content = stagedFiles.get(fileName);
            String sha1 = Utils.sha1(content);
            toCommit.put(fileName, sha1);
            /* Write the blob to file */
            Utils.writeContents(Utils.join(BLOBS_DIR, sha1), content);
        }

        for (String fileName : stagedrmFiles) {
            toCommit.remove(fileName);
        }

        stagedFiles.clear();
        stagedrmFiles.clear();

        String parentSha1 = _HEAD.getID();
        Commit newCommit = new Commit(message, parentSha1, toCommit);
        newCommit.saveCommit();

        /* Move the head and the current branch to the new commit */
        _HEAD = newCommit;
        branches.put(currentBranch, newCommit.getID());
    }


    private String getCommit(String prefix) {
        for (String commit : Utils.plainFilenamesIn(COMMITS_DIR)) {
            if (commit.substring(0, prefix.length()).equals(prefix)) {
                return commit;
            }
        }
        return null;
    }


    void checkOut(String fileName) {
        checkOut(fileName, _HEAD.getID());
    }


    void checkOut(String fileName, String commitPrefix) {
        String commitSha1 = getCommit(commitPrefix);
        if (commitSha1 == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit commit = Commit.readCommit(commitSha1);
        String blobSha1 = commit.getBlobs().get(fileName);
        if (blobSha1 == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        File blobFile = Utils.join(BLOBS_DIR, blobSha1);
        byte[] oldContent = Utils.readContents(blobFile);
        File curFile = new File(fileName);
        Utils.writeContents(curFile, oldContent);
    }



    void checkOutBranch(String branchName) {
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        String commitId = branches.get(branchName);
        if (commitId == null) {
            System.out.println("No such branch exists.");
            return;
        }

        checkOutCommit(commitId);
        // the given branch will now be considered the current branch (HEAD).
        currentBranch = branchName;
    }

    private void checkOutCommit(String commitPrefix) {
        String commitId = getCommit(commitPrefix);

        if (commitId == null) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit commit = Commit.readCommit(commitId);


        /* if the untracked file will be overwritten by the checkout */
        Set<String> untracked = findUntrackedFiles();
        Map<String, String> givenBlobs = commit.getBlobs();
        Map<String, String> curBlobs = _HEAD.getBlobs();

        /* as a file could be tracked in the provided commit, but untracked in the current commit. */
        for (String fileName : untracked) {
            if (givenBlobs.containsKey(fileName) &&
                    (!curBlobs.containsKey(fileName) || !stagedFiles.containsKey(fileName))) {
                System.out.println("There is an untracked file in the way; " +
                        "delete it, or add and commit it first");
                System.exit(0);
            }
        }

        /* Write or overwrite the files in the given commit to cwd */
        for (String fileName : givenBlobs.keySet()) {
            String blobSha1 = givenBlobs.get(fileName);
            byte[] contents = Utils.readContents(Utils.join(BLOBS_DIR, blobSha1));
            File curFile = Utils.join(CWD, fileName);
            Utils.writeContents(curFile, contents);
        }

        /* Any files that are tracked in the current branch
        but are not present in the checked-out branch are deleted. */
        for (String fileName : curBlobs.keySet()) {
            if (!givenBlobs.containsKey(fileName)) {
                Utils.restrictedDelete(Utils.join(CWD, fileName));
            }
        }
        stagedFiles.clear();
        stagedrmFiles.clear();
        _HEAD = commit;
    }

    //https://docs.google.com/document/d/1r6hVlEd9X7aoECsgpA5px9rA8a7kEjECvy98IL-1NO0/edit
    // untracked files for merge and check out
    private Set<String> findUntrackedForCommit(String commitID) {
        Map<String, String> curBlobs = _HEAD.getBlobs();

        String commitId = getCommit(commitID);
        Commit branchCommit = Commit.readCommit(commitId);
        Map<String, String> commitBlobs = branchCommit.getBlobs();

        // find files not tracked in the current commit
        Set<String> untrackedFiles = findUntrackedFiles();

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            // file in the given commit but has different context as the untracked file
            if(commitBlobs.containsKey(fileName)) {
                byte[] content = Utils.readContents(Utils.join(CWD, fileName));
                String cwdsha1 = Utils.sha1(content);
                if (!commitBlobs.get(fileName).equals(cwdsha1)) {
                    untrackedFiles.add(fileName);
                }
            } else {
                // file not in the given commit?
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }



    void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        branches.put(branchName, _HEAD.getID());
    }

    void rmBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("branch with that name does not exist.");
            return;
        }

        if (branchName.equals(currentBranch)) {
            Utils.message("Cannot remove the current branch.");
            return;
        }
        branches.remove(branchName);
    }

    /**Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from the working directory
     * (do not remove it unless it is tracked in the current commit).
     * */
    void rmFile(String fileName) {
        // If the file is neither staged nor tracked by the head commit,
        // print the error message No reason to remove the file.
        Map<String, String> curBlobs = _HEAD.getBlobs();
        if (!stagedFiles.containsKey(fileName) && !curBlobs.containsKey(fileName)) {
            Utils.message("No reason to remove the file.");
            return;
        }


        if (stagedFiles.containsKey(fileName)) {
            stagedFiles.remove(fileName);
        }

        if (curBlobs.containsKey(fileName)) {
            stagedrmFiles.add(fileName);
            stagedFiles.remove(fileName);
            if (Utils.join(CWD, fileName).exists()) {
                Utils.restrictedDelete(fileName);
            }
        }
    }


    /**The command is essentially checkout of an arbitrary commit
     * that also changes the current branch head.
     */
    void reset(String commitPrefix) {
        checkOutCommit(commitPrefix);
        String commitID = getCommit(commitPrefix);
        branches.put(currentBranch, commitID);
    }

    void merge(String branch) {
        if (!stagedFiles.isEmpty() || !stagedrmFiles.isEmpty()) {
            Utils.message("You have uncommitted changes.");
            System.exit(0);
        }

        if (!branches.containsKey(branch)) {
            Utils.message("A branch with that name does not exist."); return;
        }

        if (branch.equals(currentBranch)) {
            Utils.message("Cannot merge a branch with itself."); return;
        }

        Commit givenCommit = Commit.readCommit(branches.get(branch));
        Commit splitCommit = findSplit(branch);


        /* If the split point is the same commit as the given branch, then we do nothing; */
        if (splitCommit.getID().equals(givenCommit.getID())) {
            Utils.message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        /* If the split point is the current branch, then the effect is to check out the given branch */
        if (splitCommit.getID().equals(_HEAD.getID())) {
            checkOutBranch(branch);
            Utils.message("Current branch fast-forwarded.");
            System.exit(0);
        }


        Map<String, String> headBlobs = _HEAD.getBlobs();
        Map<String, String> givenBlobs = givenCommit.getBlobs();
        Map<String, String> splitBlobs = splitCommit.getBlobs();



        // all file names at least occurs once in the three sets
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(headBlobs.keySet());
        allFiles.addAll(givenBlobs.keySet());
        allFiles.addAll(splitBlobs.keySet());


        /* if the untracked file will be overwritten by the checkout */
        Set<String> untracked = findUntrackedFiles();
        /*  file could be tracked in the provided commit, but untracked in the current commit. */
        for (String fileName : untracked) {
            if (givenBlobs.containsKey(fileName) &&
                    (!headBlobs.containsKey(fileName) || !stagedFiles.containsKey(fileName))) {
                System.out.println("There is an untracked file in the way; " +
                        "delete it, or add and commit it first");
                System.exit(0);
            }
        }

        Map<String, String> toMerge = new HashMap<>();

        for (String fileName : allFiles) {
            String givenID = givenBlobs.getOrDefault(fileName, "");
            String curID = headBlobs.getOrDefault(fileName, "");
            String splitID = splitBlobs.getOrDefault(fileName, "");


            if (splitID.equals("")) {
                // case 4, remain as the head
                if (givenID.equals("") && !curID.equals("")) {
                    continue;
                }
                // case 5, change to other
                if (!givenID.equals("") && curID.equals("")) {
                    mergeAndAddFile(fileName, givenID);
                    continue;
                }

                if (!givenID.equals("") && !curID.equals("")) {
                    changeToConflict(fileName, curID, givenID);

                    continue;
                }
            } else {
                // case 6, remove file
                if (splitID.equals(curID) && givenID.equals("")) {
                    rmFile(fileName);
                    continue;
                    }
                }

                // case 7, remain removed
                if (splitID.equals(givenID) && curID.equals("")) {
                    continue;
                }
                // case 1
                if (splitID.equals(curID) && !splitID.equals(givenID)) {
                    mergeAndAddFile(fileName, givenID);
                    continue;
                }
                // case 2
                if (splitID.equals(givenID) && !splitID.equals(curID)) {
                    continue;
                }

                // case 3
                if (!splitID.equals(curID) && !splitID.equals(givenID)) {
                    // current and given file either all empty or same
                    if (curID.equals(splitID)) {
                        continue;
                    } else {
                        changeToConflict(fileName, curID, givenID);
                        continue;
                    }
                }
        }
        String message = String.format("Merged %s into %s.", branch, currentBranch);
        commit(message);
        _HEAD.setParent2(givenCommit.getID());
    }


    private void mergeAndAddFile(String fileName, String fileID) {
        File blob = Utils.join(BLOBS_DIR, fileID);
        File curFile = Utils.join(CWD, fileName);
        byte[] contents = Utils.readContents(blob);
        Utils.writeContents(curFile, contents);
        stagedFiles.put(fileName, contents);
    }

    private void changeToConflict(String fileName, String curID, String givenID) {
        File file = Utils.join(CWD, fileName);
        File curBlob = Utils.join(BLOBS_DIR, curID);
        File givenBlob = Utils.join(BLOBS_DIR, givenID);
        String givenContent = "";
        String curContent = "";
        if (!curID.equals("")) {
            curContent = Utils.readContentsAsString(curBlob);
        }
        if (!givenID.equals("")) {
            givenContent = Utils.readContentsAsString(givenBlob);
        }
        String conflictContent = "<<<<<<< HEAD\n" + curContent +
                "=======\n" + givenContent + ">>>>>>>";
        Utils.writeContents(file, conflictContent);
        stagedFiles.put(fileName, Utils.readContents(file));
        System.out.println("Encountered a merge conflict.");
    }







    /** find the split commit of two branch
     * Assume branch names exist */
    private Commit findSplit(String branch) {
        Commit current = _HEAD;
        Commit given = Commit.readCommit(branches.get(branch));

        /* Move two pointer to the same length from the root */
        if (current.getLen() > given.getLen()) {
            current = current.getParentCommit(current.getLen() - given.getLen());
        } else {
            given = given.getParentCommit(given.getLen() - current.getLen());
        }
        /* Move the two pointers forward 1 step as same time */
        while(!current.getID().equals(given.getID())) {
            current = current.getParentCommit(1);
            given = given.getParentCommit(1);
        }
         return current;

    }

    void log() {
        Commit c = _HEAD;
        while (c != null) {
            System.out.println(c.toString());
            if (c.getParent() == null) {
                break;
            }
            c = Commit.readCommit(c.getParent());
        }
    }

    void globalLog() {
        for (String commitID : Utils.plainFilenamesIn(COMMITS_DIR)) {
            Commit c = Commit.readCommit(commitID);
            System.out.println(c.toString());
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message
     * */
    void find(String message) {
        boolean ifFind = false;
        for (String commitID : Utils.plainFilenamesIn(COMMITS_DIR)) {
            Commit commit = Commit.readCommit(commitID);
            if (commit.getMessage().equals(message)) {
                System.out.println(commitID);
                ifFind = true;
            }
        }

        if (!ifFind) {
            Utils.message("Found no commit with that message.");
        }

    }

    void status() {
        headStatus("Branches");
        for (String branchName : branches.keySet()) {
            if (branchName.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();

        //print Staged Files
        headStatus("Staged Files");
        for (String fileName : stagedFiles.keySet()) {
            System.out.println(fileName);
        }
        System.out.println();

        //print Removed Files
        headStatus("Removed Files");
        for (String fileName : stagedrmFiles) {
            System.out.println(fileName);
        }
        System.out.println();

        headStatus("Modifications Not Staged For Commit");
        Map<String, String> modified = findModifiedFiles();
        for(String fileName : modified.keySet()) {
            System.out.println(fileName + " " +modified.get(fileName));
        }
        System.out.println();

        headStatus("Untracked Files");
        Set<String> untracked = findUntrackedFiles();
        for (String fileName : untracked) {
            System.out.println(fileName);
        }
        System.out.println();

    }

    private void headStatus(String head) {
        System.out.printf("=== %s ===", head);
        System.out.println();
    }


    private Set<String> findUntrackedFiles() {
        Set<String> untrackedFiles = new TreeSet<>();
        Map<String, String> curBlobs = _HEAD.getBlobs();

        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            // files that have been staged for removal, but then re-created without Gitlet's knowledge(not staged?)
            if(stagedrmFiles.contains(fileName) && !stagedFiles.containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
            // Files present in the working directory but neither staged for addition nor tracked.
            if(!curBlobs.containsKey(fileName) && !stagedFiles.containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    private Map<String, String> findModifiedFiles() {
        // filename -> type
        Map<String, String> untrackedFiles = new TreeMap<>();

        List<String> fileInCWD = Utils.plainFilenamesIn(CWD);
        // Tracked in the current commit, changed in the working directory, but not staged
        Map<String, String> currentBlobs = _HEAD.getBlobs();
        for (String fileName : currentBlobs.keySet()) {
            if (fileInCWD.contains(fileName)) {
                byte[] content = Utils.readContents(Utils.join(CWD, fileName));
                String sha1 = Utils.sha1(content);
                if (!currentBlobs.get(fileName).equals(sha1) && !stagedFiles.containsKey(fileName)) {
                    untrackedFiles.put(fileName, "(modified)");
                }
            } // tracked in the current commit and deleted from the working directory, Not staged for removal,
            else {
                if (!stagedrmFiles.contains(fileName))  {
                    untrackedFiles.put(fileName, "(deleted)");
                }
            }
        }

       // Staged for addition, but with different contents than in the working directory
        for (String fileName : stagedFiles.keySet()) {
                if (fileInCWD.contains(fileName)) {
                    String stagedSha1 = Utils.sha1(stagedFiles.get(fileName));
                    byte[] content = Utils.readContents(Utils.join(CWD, fileName));
                    String sha1 = Utils.sha1(content);
                    if (!stagedSha1.equals(sha1)) {
                        untrackedFiles.put(fileName, "(modified)");
                    }
                }
                // Staged for addition, but deleted in the working directory;
                else {
                    untrackedFiles.put(fileName, "(deleted)");
                }
        }
        return untrackedFiles;
    }





    public void saveTree() {
        Utils.writeObject(GTTree.GIT_TREE, this);
    }

    public static GTTree readTree() {
        return Utils.readObject(GIT_TREE, GTTree.class);
    }


}



