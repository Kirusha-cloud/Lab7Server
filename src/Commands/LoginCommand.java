package Commands;

import Lab7Server.ICommand;


public class LoginCommand implements ICommand {
    private CommandBase commandBase;
    public LoginCommand(CommandBase commandBase){
        this.commandBase = commandBase;
    }
    @Override
    public void execute() {
        commandBase.checkLogin();
    }
    @Override
    public String toString(){
        return "login";
    }
}
