package Commands;

import Lab7Server.ICommand;

public class UpdateIdCommand implements ICommand {
    private CommandBase commandBase;
    public UpdateIdCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.updateId();
    }
    @Override
    public String toString(){
        return "update_id";
    }
}
