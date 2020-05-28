package Lab7Server;

import Commands.*;
import Commands.LoginCommand;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        CommandBase commandBase = new CommandBase();
        Performer performer = new Performer(new AddCommand(commandBase), new AddIfMaxCommand(commandBase),
                new ClearCommand(commandBase), new ExecuteScriptCommand(commandBase), new GroupCountingByTransportCommand(commandBase),
                new HelpCommand(commandBase), new HistoryCommand(commandBase), new InfoCommand(commandBase),
                new MinByFurnitureCommand(commandBase), new PrintFieldDescendingNumberOfRoomsCommand(commandBase),
                new RemoveByIdCommand(commandBase), new RemoveGreaterCommand(commandBase), new ShowCommand(commandBase),
                new UpdateIdCommand(commandBase), new SendAnswerCommand(commandBase), new LoginCommand(commandBase), new ShowMyObjCommand(commandBase));
        CommandManager commandManager = new CommandManager(performer);
        commandBase.initializeCommandManager(commandManager);
        Server server = new Server(commandManager);
        server.run();
    }
}
