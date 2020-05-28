package Handlers;

import Lab7Client.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;

public class RequestHandler implements Callable<Command> {
    private static final Logger LOG = LogManager.getLogger();
    private SocketChannel sc;

    public RequestHandler(SocketChannel sc) {
        this.sc = sc;
    }

    @Override
    public Command call() throws Exception {
        System.out.println(Thread.currentThread().getName());
        ByteBuffer readBuffer = ByteBuffer.allocate(32 * 32);
        int numRead = sc.read(readBuffer);
        LOG.info("Прочитано байт: " + numRead);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(readBuffer.array()))) {
            Command command = (Command) ois.readObject();
            LOG.info("Принята команда: " + command.getCommandName());
            return command;
        }
    }
}
