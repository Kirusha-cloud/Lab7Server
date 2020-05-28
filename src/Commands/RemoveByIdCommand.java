package Commands;

import Lab7Server.ICommand;

public class RemoveByIdCommand implements ICommand {
    private CommandBase commandBase;
    public RemoveByIdCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.removeById();
    }
    @Override
    public String toString(){
        return "remove_by_id";
    }
}
