package Handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public class ResponseHandler implements Runnable {
    private SelectionKey key;
    private StringBuilder sb;
    private static final Logger LOG = LogManager.getLogger();
    public ResponseHandler(SelectionKey key, StringBuilder sb) {
        this.key = key;
        this.sb = sb;
    }

    @Override
    public void run() {
        try {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(Objects.requireNonNull(sb.toString().getBytes()));
            LOG.info("Отправлен ответ: " + sb.toString());
            baos.flush();
            socketChannel.write(ByteBuffer.wrap(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
