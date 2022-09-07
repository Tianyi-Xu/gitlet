package gitlet;

import java.io.File;
import java.io.Reader;
import java.util.Date;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Tianyi Xu
 */
public class Main {
    static final File CWD = GTTree.CWD;
    static final File REPO_DIR = GTTree.REPO_DIR;
    static final File COMMITS_DIR = GTTree.COMMITS_DIR;
    static final File BLOBS_DIR = GTTree.BLOBS_DIR;
    public static GTTree gtTree;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        File gtFile = GTTree.GIT_TREE;
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        }

        if (args[0].equals("init")) {
            if (gtFile.exists()) {
                Utils.message("Gitlet version-control system already exists in the current directory.");
            } else {
                init();
            }
        } else {
            if (!gtFile.exists()) {
                Utils.message("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            /* Read gtTree from saved file */
            gtTree = GTTree.readTree();
            switch(args[0]) {
                case "add" :
                    add(args);
                    break;
                case "commit":
                    commit(args);
                    break;
                case "checkout":
                    checkout(args);
                    break;
                case "branch":
                    branch(args);
                    break;
                case "rm-branch":
                    rmBranch(args);
                    break;
                case "rm":
                    rm(args);
                    break;
                case "status":
                    status(args);
                    break;
                case "log":
                    log(args);
                    break;
                case "global-log":
                    globalLog(args);
                    break;
                case "find":
                    find(args);
                    break;
                case "merge":
                    merge(args);
                    break;
                case "reset":
                    reset(args);
                    break;
                default:
                    Utils.message("No command with that name exists.");
                    System.exit(0);
            }
            gtTree.saveTree();
        }



    }

    public static void init(){
        GTTree gtTree = new GTTree();
        gtTree.init();
        gtTree.saveTree();
    }

    public static void add(String... args) {
        for (int i = 1; i < args.length; i++) {
            gtTree.add(args[i]);
        }
    }

    public static void commit(String... args) {
        if (args.length == 1 || args[1].length() == 0) {
            Utils.message("Please enter a commit message.");
            System.exit(0);
        } else if (args.length > 2) {
            Utils.message("Please enter a commit message.");
        }
        gtTree.commit(args[1]);
    }

    public static void checkout(String... args) {
        if (args.length == 1) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        } else if (args.length == 2) {
            gtTree.checkOutBranch(args[1]);
        } else if (args.length == 3 && args[1].equals("--")) {
            gtTree.checkOut(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            gtTree.checkOut(args[3], args[1]);
        } else {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void find(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.find(args[1]);
    }

    public static void merge(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.merge(args[1]);
    }

    public static void branch(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.branch(args[1]);
    }

    public static void rmBranch(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.rmBranch(args[1]);
    }

    public static void rm(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.rmFile(args[1]);
    }

    public static void reset(String... args) {
        if (args.length == 1 || args.length > 2) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
        gtTree.reset(args[1]);
    }


    public static void log(String... args) {
        gtTree.log();
    }
    public static void globalLog(String... args) {
        gtTree.globalLog();
    }

    public static void status(String... args) {
        gtTree.status();
    }





}
