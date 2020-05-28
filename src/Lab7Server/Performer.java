package Lab7Server;

import Commands.SendAnswerCommand;
import Lab7Client.Command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Класс, отвечающий за выполнение команды.
 * Содержит в себе экземпляр каждой команды.
 */
public class Performer {
    private ArrayList<ICommand> commands = new ArrayList<>();
    private SendAnswerCommand sendAnswerCommand;

    Performer(ICommand add, ICommand addIfMax, ICommand clear, ICommand executeScript,
              ICommand groupCountingByTransport, ICommand help, ICommand history, ICommand info,
              ICommand minByFurniture, ICommand printFieldNOF, ICommand removeById, ICommand removeGreater,
              ICommand show, ICommand updateId, SendAnswerCommand sendAnswerCommand, ICommand checkLogin, ICommand showMyObj) {
        Collections.addAll(commands, add, addIfMax, clear, executeScript, groupCountingByTransport, help, history, info,
                minByFurniture, printFieldNOF, removeById, removeGreater, show, updateId, checkLogin, showMyObj);
        this.sendAnswerCommand = sendAnswerCommand;

    }

    void execute(Command command) {
        try {
            for (ICommand next : commands) {
                if (next.toString().equals(command.getCommandName())) {
                    next.execute();
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    ConcurrentLinkedQueue<StringBuilder> sendAnswer() {
        return sendAnswerCommand.execute();
    }
}
