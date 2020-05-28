package Commands;

import Lab7Server.ICommand;

public class ExitCommand implements ICommand {
    private CommandBase commandBase;
    public ExitCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        //commandBase.exit();
    }
    @Override
    public String toString(){
        return "exit";
    }
}
