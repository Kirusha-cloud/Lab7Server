package Commands;

import Lab7Server.ICommand;


public class ShowMyObjCommand implements ICommand {
    private CommandBase commandBase;

    public ShowMyObjCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }

    @Override
    public void execute() {
        commandBase.showMyObj();
    }
    @Override
    public String toString(){
        return "show_my_obj";
    }
}
