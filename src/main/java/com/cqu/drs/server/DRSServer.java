package com.cqu.drs.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-threaded TCP server. The accept loop runs on its own thread and hands
 * each client socket to a {@link ClientHandler} executed by a fixed-size
 * thread pool, so many JavaFX clients are served concurrently.
 */
public class DRSServer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int port;
    private final RequestDispatcher dispatcher;
    private final ExecutorService pool;
    private final AtomicInteger clientCounter = new AtomicInteger();
    private volatile ServerSocket serverSocket;
    private volatile boolean running;
    private Thread acceptThread;

    public DRSServer(int port, int threads, RequestDispatcher dispatcher) {
        this.port = port;
        this.dispatcher = dispatcher;
        AtomicInteger n = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "drs-worker-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /** Binds the port and starts accepting clients in the background. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "drs-acceptor");
        acceptThread.start();
        log("DRS-Enhanced server listening on port " + getPort());
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.execute(new ClientHandler(client, dispatcher, clientCounter.incrementAndGet()));
            } catch (SocketException e) {
                if (running) {
                    log("Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                log("Accept failed: " + e.getMessage());
            }
        }
    }

    /** Stops accepting clients and shuts the worker pool down. */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // closing anyway
        }
        pool.shutdownNow();
        try {
            pool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log("Server stopped");
    }

    /** Actual bound port (useful when started on port 0 in tests). */
    public int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    public boolean isRunning() {
        return running;
    }

    static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(TIME) + "][" + Thread.currentThread().getName() + "] " + msg);
    }
}
