package Commands;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SendAnswerCommand {
    private CommandBase commandBase;
    public SendAnswerCommand(CommandBase commandBase){
        this.commandBase = commandBase;
    }
    public ConcurrentLinkedQueue<StringBuilder> execute(){
            return commandBase.sendAnswer();
    }
}
