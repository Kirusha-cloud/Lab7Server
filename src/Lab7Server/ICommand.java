package Lab7Server;

import java.sql.SQLException;

public interface ICommand {
    void execute() throws SQLException;
    String toString();
}
