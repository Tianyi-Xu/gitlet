package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;

/** Commit class for gitlet
 *  @author Tianyi Xu
 */

public class Commit implements Serializable {
    private String message;
    private Date timestamp;
    private String parent;
    private String parent2;
    /* length from the root commit */
    private int len;
    /* File name -> content sha1*/
    private Map<String, String> blobs;

    public Commit(String message, String parentSha1, Map<String, String> blobs) {
        this.message = message;
        if (parentSha1 == null) {
            timestamp = new Date(0);
            len = 0;
        } else {
            timestamp = new Date();
            len = readCommit(parentSha1).len + 1;
        }
        this.parent = parentSha1;
        this.blobs = blobs;

    }

    public String getMessage() {
        return this.message;
    }

    public String getParent() {
        return this.parent;
    }

    public String getParent2() {
        return this.parent2;
    }

    public int getLen() {
        return this.len;
    }

    public void setParent2(String parentID) {
        this.parent2 = parentID;
    }

    public Map<String, String> getBlobs() {
        return this.blobs;
    }

    public String getID() {
        byte[] commitSerialized = Utils.serialize(this);
        return Utils.sha1(commitSerialized);
    }

    void saveCommit() {
        Utils.writeObject(Utils.join(GTTree.COMMITS_DIR, getID()), this);
    }

   static Commit readCommit(String commitId) {
        return Utils.readObject(Utils.join(GTTree.COMMITS_DIR, commitId), Commit.class);
    }

    /** Get the kth parent commit from the current commmit
     * Assume the k <= len */
    Commit getParentCommit(int k) {
        Commit c = this;
        for (int i = 0; i < k; i++) {
            c = readCommit(c.parent);
        }
        return c;
    }

    boolean isAncestor(String commitID) {
        Commit c = this;
        while (c!= null) {
            if (c.getParent().equals(commitID)) {
                return true;
            } else {
                c = c.getParentCommit(1);
            }
        }
        return false;
    }


    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===\n");
        out.format("commit " + getID() + "\n");
        if (parent2 != null) {
            out.format("Merge: " + parent.substring(0, 7) + " " + parent2.substring(0, 7) + "\n");
        }
        //Sat Nov 11 12:30:00 2017 -0800
        String pattern = "EEE MMM dd HH:mm:ss yyyy Z";
        SimpleDateFormat fmt = new SimpleDateFormat(pattern);
        String dateFormatted = fmt.format(timestamp);
        out.format("Date: " + dateFormatted + "\n");
        out.format(message + "\n");
        return out.toString();
    }

}
