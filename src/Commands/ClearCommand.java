package Commands;

import Lab7Server.ICommand;


public class ClearCommand implements ICommand {
    private CommandBase commandBase;
    public ClearCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.clear();
    }
    @Override
    public String toString(){
        return "clear";
    }
}
