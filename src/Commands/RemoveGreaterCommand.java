package Commands;

import Lab7Server.ICommand;

public class RemoveGreaterCommand implements ICommand {
    private CommandBase commandBase;
    public RemoveGreaterCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.removeGreater();
    }
    @Override
    public String toString(){
        return "remove_greater";
    }
}
