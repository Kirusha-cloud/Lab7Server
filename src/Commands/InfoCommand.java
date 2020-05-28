package Commands;

import Lab7Server.ICommand;

public class InfoCommand implements ICommand {
    private CommandBase commandBase;
    public InfoCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.info();
    }
    @Override
    public String toString(){
        return "info";
    }
}
