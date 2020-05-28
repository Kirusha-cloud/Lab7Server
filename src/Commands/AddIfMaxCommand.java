package Commands;

import Lab7Server.ICommand;

public class AddIfMaxCommand implements ICommand {
    private CommandBase commandBase;
    public AddIfMaxCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.addIfMax();
    }
    @Override
    public String toString(){
        return "add_if_max";
    }
}
