package Commands;

import Lab7Server.ICommand;


public class AddCommand implements ICommand {
    private CommandBase commandBase;
    public AddCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }

    @Override
    public void execute() {
        commandBase.add();
    }
    @Override
    public String toString(){
        return "add";
    }
}
