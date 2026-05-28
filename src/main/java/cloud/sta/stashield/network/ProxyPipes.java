package cloud.sta.stashield.network;

import cloud.sta.stashield.log.StaLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public final class ProxyPipes {
    private ProxyPipes() {
    }

    public static void bridge(Socket client, Socket backend, ExecutorService executor, StaLogger logger) throws IOException {
        CountDownLatch done = new CountDownLatch(2);
        executor.execute(new Pump(client, backend, done, logger));
        executor.execute(new Pump(backend, client, done, logger));
        try {
            done.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Bridge interrupted", ex);
        }
    }

    private static final class Pump implements Runnable {
        private final Socket source;
        private final Socket target;
        private final CountDownLatch done;
        private final StaLogger logger;

        private Pump(Socket source, Socket target, CountDownLatch done, StaLogger logger) {
            this.source = source;
            this.target = target;
            this.done = done;
            this.logger = logger;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try {
                InputStream input = source.getInputStream();
                OutputStream output = target.getOutputStream();
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } catch (SocketException ignored) {
                // Normal close path when either side disconnects.
            } catch (IOException ex) {
                logger.warn("Proxy pipe closed: " + ex.getMessage());
            } finally {
                closeQuietly(source);
                closeQuietly(target);
                done.countDown();
            }
        }
    }

    static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already closed.
        }
    }
}
