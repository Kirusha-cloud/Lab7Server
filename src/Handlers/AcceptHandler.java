package Handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.RecursiveAction;

public class AcceptHandler extends RecursiveAction {
    private static final Logger LOG = LogManager.getLogger();
    private SelectionKey key;
    private Selector selector;
    public AcceptHandler(SelectionKey key, Selector selector) {
        this.key = key;
        this.selector = selector;
    }

    @Override
    protected void compute() {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            LOG.info("Соединение с клиентом установлено." + socketChannel.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
