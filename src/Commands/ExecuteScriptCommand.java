package Commands;

import Lab7Server.ICommand;

public class ExecuteScriptCommand implements ICommand {
    private CommandBase commandBase;
    public ExecuteScriptCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.executeScript();
    }
    @Override
    public String toString(){
        return "execute_script";
    }
}
