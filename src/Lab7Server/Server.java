package Lab7Server;

import Handlers.AcceptHandler;
import Handlers.CommandHandler;
import Handlers.RequestHandler;
import Handlers.ResponseHandler;
import Lab7Client.Command;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Server {
    private CommandManager commandManager;
    private ForkJoinPool requestProcessing = new ForkJoinPool(3);
    private ForkJoinPool readRequest = new ForkJoinPool(5);
    private static ExecutorService sendResponse = Executors.newFixedThreadPool(4);
    private static final Logger LOG = LogManager.getLogger();
    private static final int PORT = 13001;
    private static final String ADDRESS = "127.0.0.1";
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private ConcurrentHashMap<SelectionKey, Future<Command>> clientObj = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SelectionKey, Future<StringBuilder>> result = new ConcurrentHashMap<>();

    Server(CommandManager commandManager) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(ADDRESS, PORT));
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.commandManager = commandManager;
        consoleCommands();
    }

    void run() throws IOException {
        shutDownHook();
        LOG.info("Сервер запущен.");
        while (true) {
            selector.select();
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid())
                    continue;
                if (key.isAcceptable()) {
                    accept(key);
                }
                if (key.isReadable())
                    read(key);
                if (key.isWritable())
                    write(key);
            }
        }
    }

    private void accept(SelectionKey key) {
        System.out.println("новый клиент подключился");
        readRequest.invoke(new AcceptHandler(key, selector));
    }


    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        clientObj.put(key, readRequest.submit(new RequestHandler(socketChannel)));
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) {
        try {
            if (clientObj.containsKey(key) && clientObj.get(key).isDone()) {
                result.put(key, requestProcessing.submit(new CommandHandler(clientObj.get(key).get(), commandManager)));
                clientObj.remove(key);
            }
            if (result.containsKey(key) && result.get(key).isDone()) {
                sendResponse.execute(new ResponseHandler(key, result.get(key).get()));
                result.remove(key);
                key.interestOps(SelectionKey.OP_READ);
            }

        } catch (RuntimeException | InterruptedException | ExecutionException ignored) {
        }

    }

    private void shutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            readRequest.shutdown();
            requestProcessing.shutdown();
            sendResponse.shutdown();
        }));

    }

    private void consoleCommands() {
        new Thread(() -> {
            Scanner cin = new Scanner(System.in);
            while (cin.hasNext()) {
                String in = cin.nextLine();
                if (in.equals("exit")) {
                    LOG.info("Работа сервера завершена.");
                    System.exit(0);
                }
            }
        }).start();
    }
}
