package Commands;

import Lab7Server.ICommand;

public class MinByFurnitureCommand implements ICommand {
    private CommandBase commandBase;
    public MinByFurnitureCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.minByFurniture();
    }
    @Override
    public String toString(){
        return "min_by_furniture";
    }
}
