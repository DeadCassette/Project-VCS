package gitlet;
import java.io.File;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Conrad Ehlers & Morgan Sinnock
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Please enter a command.");
                return;
            }
            Repository repo = new Repository();
            mainHelper(args, repo);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** Handle the command based on the first argument */
    private static void mainHelper(String[] args, Repository repo) {
        switch (args[0]) {
            case "init":
                repo.init();
                break;
            case "add":
                validateArgs(args, 2);
                repo.add(args[1]);
                break;
            case "commit":
                validateArgs(args, 2);
                repo.commit(args[1]);
                break;
            case "rm":
                validateArgs(args, 2);
                repo.rm(args[1]);
                break;
            case "reset":
                validateArgs(args, 2);
                repo.reset(args[1]);
                break;
            case "branch":
                validateArgs(args, 2);
                repo.branch(args[1]);
                break;
            case "find":
                validateArgs(args, 2);
                repo.find(args[1]);
                break;
            case "status":
                checkGitletDir();
                validateArgs(args, 1);
                repo.status();
                break;
            case "log":
                validateArgs(args, 1);
                repo.log();
                break;
            case "rm-branch":
                validateArgs(args, 2);
                repo.removeBranch(args[1]);
                break;
            case "restore":
                if (args.length == 3) {
                    repo.restore(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        throw new GitletException("Incorrect operands.");
                    }
                    repo.restore(args[1], args[3]);
                } else {
                    throw new GitletException("Incorrect operands.");
                }
                break;
            case "switch":
                validateArgs(args, 2);
                repo.switchBranch(args[1]);
                break;
            case "global-log":
                validateArgs(args, 1);
                repo.globalLog();
                break;
            default:
                throw new GitletException("No command with that name exists.");
        }
    }

    /** Validate arguments for the command */
    private static void validateArgs(String[] args, int expectedLength) {
        if (args.length != expectedLength) {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Check if the Gitlet directory exists */
    private static void checkGitletDir() {
        File gitletDir = new File(".gitlet");
        if (!gitletDir.exists() || !gitletDir.isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
