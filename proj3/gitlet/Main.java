package gitlet;

import java.io.File;
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
        // .getlet file
        File gitletDirectory = Utils.join(System.getProperty("user.dir"), ".gitlet");
        if (!("init".equals(args[0]) || gitletDirectory.exists())) {
            exitWithError("Not in an initialized Gitlet directory.");
        }

        // create GitletRepository object 
        GitletRepository gitletRepository = new GitletRepository();
        // create RemoteRepository Object
        RemoteRepository remoteRepository = new RemoteRepository();

        // user inputs operands
        int operands = args.length - 1;
        switch (args[0]) {
            case ("init") -> {
                validateNumArgs(operands, 0);
                gitletRepository.init();
            }
            case ("add") -> {
                validateNumArgs(operands, 1);
                gitletRepository.add(args[1]);
            }
            case ("commit") -> {
                validateNumArgs(operands, 1);
                gitletRepository.commit(args[1]);
            }
            case ("rm") -> {
                validateNumArgs(operands, 1);
                gitletRepository.rm(args[1]);
            }
            case ("log") -> {
                validateNumArgs(operands, 0);
                gitletRepository.log();
            }
            case ("global-log") -> {
                validateNumArgs(operands, 0);
                gitletRepository.globalLog();
            }
            case ("find") -> {
                validateNumArgs(operands, 1);
                gitletRepository.find(args[1]);
            }
            case ("status") -> {
                validateNumArgs(operands, 0);
                gitletRepository.status();
            }
            case ("checkout") -> {
                validateNumArgs(operands, 1, 3);
                // remove first arg
                gitletRepository.checkout(Arrays.stream(args).skip(1).toArray(String[]::new));
            }
            case ("branch") -> {
                validateNumArgs(operands, 1);
                gitletRepository.branch(args[1]);
            }
            case ("rm-branch") -> {
                validateNumArgs(operands, 1);
                gitletRepository.rmBranch(args[1]);
            }
            case ("reset") -> {
                validateNumArgs(operands, 1);
                gitletRepository.reset(args[1]);
            }
            case ("merge") -> {
                validateNumArgs(operands, 1);
                gitletRepository.merge(args[1]);
            }
            case ("add-remote") -> {
                validateNumArgs(operands, 2);
                remoteRepository.addRemote(args[1], args[2]);
            }
            case ("rm-remote") -> {
                validateNumArgs(operands, 1);
                remoteRepository.rmRemote(args[1]);
            }
            case ("push") -> {
                validateNumArgs(operands, 2);
                remoteRepository.push(args[1], args[2]);
            }
            case ("fetch") -> {
                validateNumArgs(operands, 2);
                remoteRepository.fetch(args[1], args[2]);
            }
            case ("pull") -> {
                validateNumArgs(operands, 2);
                remoteRepository.pull(args[1], args[2]);
            }
            case ("diff") -> {
                validateNumArgs(operands, 0, 2);
                // remove first arg
                remoteRepository.diff(Arrays.stream(args).skip(1).toArray(String[]::new));
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
     * @param number   Number of expected arguments
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
