package Lab7Server;

import Lab7Client.Command;


public class CommandManager {
    private Performer performer;
    private Command command;
    CommandManager(Performer performer) {
        this.performer = performer;
    }

    public StringBuilder executeCommand(Command command) {
        this.command = command;
        performer.execute(command);
        return performer.sendAnswer().poll();
    }
    public Command getCommandFromManager(){
        return command;
    }
}
