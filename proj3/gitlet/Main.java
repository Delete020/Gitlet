package gitlet;

import java.io.IOException;
import java.util.Arrays;

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
            case ("add") -> {
                validateNumArgs(operands, 1);
                GitletRepository.add(args[1]);
            }
            case ("commit") -> {
                validateNumArgs(operands, 1);
                GitletRepository.commit(args[1]);
            }
            case ("rm") -> {
                validateNumArgs(operands, 1);
                GitletRepository.rm(args[1]);
            }
            case ("log") -> {
                validateNumArgs(operands, 0);
                GitletRepository.log();
            }
            case ("global-log") -> {
                validateNumArgs(operands, 0);
                GitletRepository.globalLog();
            }
            case ("find") -> {
                validateNumArgs(operands, 1);
                GitletRepository.find(args[1]);
            }
            case ("status") -> {
                validateNumArgs(operands, 0);
                GitletRepository.status();
            }
            case ("checkout") -> {
                validateNumArgs(operands, 1, 3);
                // remove first arg
                GitletRepository.checkout(Arrays.stream(args).skip(1).toArray(String[]::new));
            }
            case ("branch") -> {
                validateNumArgs(operands, 1);
                GitletRepository.branch(args[1]);
            }
            case ("rm-branch") -> {
                validateNumArgs(operands, 1);
                GitletRepository.rmBranch(args[1]);
            }
            default -> exitWithError("No command with that name exists.");
        }
    }



    /**
     * Checks the number of arguments versus the expected number,
     * If a user inputs a command with the wrong number or format of operands,
     * print the message and exit.
     *
     * @param operands User input operands from command line
     * @param number    Number of expected arguments
     */
    public static void validateNumArgs(int operands, int number) {
        validateNumArgs(operands, number, number);
    }

    public static void validateNumArgs(int operands, int min, int max) {
        if (operands > max || operands < min) {
            exitWithError("Incorrect operands.");
        }
    }
}
