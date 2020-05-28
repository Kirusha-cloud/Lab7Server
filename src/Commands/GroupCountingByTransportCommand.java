package Commands;

import Lab7Server.ICommand;

public class GroupCountingByTransportCommand implements ICommand {
    private CommandBase commandBase;
    public GroupCountingByTransportCommand(CommandBase commandBase) {
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.groupCountingByTransport();
    }
    @Override
    public String toString(){
        return "group_counting_by_transport";
    }
}
