package gitlet;

import java.io.IOException;

import static gitlet.GitletRepository.exitWithError;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Delete020
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) throws IOException {
        // Handle failure case.
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        if (!("init".equals(args[0]) || GitletRepository.GITLET_DIR.exists())) {
            exitWithError("Not in an initialized Gitlet directory.");
        }

        // user inputs operands
        int operands = args.length - 1;
        switch (args[0]) {
            case ("init") -> {
                validateNumArgs(operands, 0);
                GitletRepository.init();
            }
            case ("add") -> GitletRepository.add();
            default -> exitWithError("No command with that name exists.");
        }
    }



    /**
     * Checks the number of arguments versus the expected number,
     * If a user inputs a command with the wrong number or format of operands,
     * print the message and exit.
     *
     * @param operands User input operands from command line
     * @param n    Number of expected arguments
     */
    public static void validateNumArgs(int operands, int n) {
        if (operands > n) {
            exitWithError("Incorrect operands.");
        }
    }

}
