package Commands;

import Lab7Server.ICommand;

public class HelpCommand implements ICommand {
    private CommandBase commandBase;
    public HelpCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.help();
    }
    @Override
    public String toString(){
        return "help";
    }
}
