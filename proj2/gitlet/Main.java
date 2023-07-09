package gitlet;

import java.util.Arrays;
import java.util.Objects;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        String command = args[0];
        String[] operands = Arrays.copyOfRange(args, 1, args.length);
        try {
            handleCommand(command, operands);
            exit();
        } catch (GitletException e) {
            exit(e.getMessage());
        }
    }

    static void handleCommand(String command, String[] operands) {
        switch (command) {
            case "init" -> {
                // handle the `init` command
                validateOperands(operands, 0, 0);
                Repository.initCmd();
            }
            case "add" -> {
                // handle the `add [filename]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.addCmd(operands[0]);
            }
            case "commit" -> {
                // handle the `commit [message]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.commitCmd(operands[0]);
            }
            case "rm" -> {
                // handle the `rm [file name]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.rmCmd(operands[0]);
            }
            case "log" -> {
                // handle the `log` command
                validateInitialized();
                validateOperands(operands, 0, 0);
                Repository.logCmd();
            }
            case "global-log" -> {
                // handle the `global-log` command
                validateInitialized();
                validateOperands(operands, 0, 0);
                Repository.globalLogCmd();
            }
            case "find" -> {
                // handle the `find [commit message]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.findCmd(operands[0]);
            }
            case "status" -> {
                // handle the `status` command
                validateInitialized();
                validateOperands(operands, 0, 0);
                Repository.statusCmd();
            }
            case "checkout" -> {
                // handle the `status` command, there are 3 possible use cases:
                // `checkout -- [file name]`
                // `checkout [commit id] -- [file name]`
                // `checkout [branch name]`
                validateInitialized();
                validateOperands(operands, 1, 3);
                if (operands.length == 2) {
                    if (!Objects.equals("--", operands[0])) {
                        exit("Incorrect operands.");
                    }
                    Repository.checkoutFileCmd(operands[1]);
                } else if (operands.length == 3) {
                    if (!Objects.equals("--", operands[1])) {
                        exit("Incorrect operands.");
                    }
                    Repository.checkoutFileCmd(operands[0], operands[2]);
                } else if (operands.length == 1) {
                    Repository.checkoutBranchCmd(operands[0]);
                }
            }
            case "branch" -> {
                // handle the `branch [branch name]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.branchCmd(operands[0]);
            }
            case "rm-branch" -> {
                // handle the `rm-branch [branch name]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.rmBranchCmd(operands[0]);
            }
            case "reset" -> {
                // handle the `reset [commit id]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.resetCmd(operands[0]);
            }
            case "merge" -> {
                // handle the `merge [branch name]` command
                validateInitialized();
                validateOperands(operands, 1, 1);
                Repository.mergeCmd(operands[0]);
            }
            default -> exit("No command with that name exists.");
        }
    }

    /**
     * Check if the repository has been initialized.
     */
    static void validateInitialized() {
        if (!Repository.isInitialized()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * Check if `operands` is a proper array whose length is at least `min` and
     * no more than `max`.
     */
    static void validateOperands(String[] operands, int min, int max) {
        if (operands.length < min || operands.length > max) {
            exit("Incorrect operands.");
        }
    }

    /**
     * Print the `message` then exit.
     */
    static void exit(String message) {
        System.out.println(message);
        exit();
    }

    static void exit() {
        System.exit(0);
    }
}
