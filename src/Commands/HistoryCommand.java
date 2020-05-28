package Commands;

import Lab7Server.ICommand;

public class HistoryCommand implements ICommand {
    private CommandBase commandBase;
    public HistoryCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.history();
    }
    @Override
    public String toString(){
        return "history";
    }
}
