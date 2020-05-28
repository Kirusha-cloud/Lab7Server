package Handlers;

import Lab7Client.Command;
import Lab7Server.CommandManager;

import java.util.concurrent.Callable;

public class CommandHandler implements Callable<StringBuilder> {
    private CommandManager commandManager;
    private Command command;

    public CommandHandler(Command command, CommandManager commandManager) {
        this.command = command;
        this.commandManager = commandManager;
    }

    @Override
    public StringBuilder call() {
        return commandManager.executeCommand(command);
    }
}
