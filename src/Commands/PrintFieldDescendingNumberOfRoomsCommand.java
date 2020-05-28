package Commands;

import Lab7Server.ICommand;

public class PrintFieldDescendingNumberOfRoomsCommand implements ICommand {
    private CommandBase commandBase;
    public PrintFieldDescendingNumberOfRoomsCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.printFieldDescendingNumberOfRooms();
    }
    @Override
    public String toString(){
        return "print_field_descending_number_of_rooms";
    }
}
