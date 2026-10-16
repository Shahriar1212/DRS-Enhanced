package com.cqu.drs.client;

import com.cqu.drs.protocol.Request;
import com.cqu.drs.protocol.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs server requests on a background thread (javafx.concurrent.Task) so the
 * GUI never freezes while waiting for the network or database, then delivers
 * the response back on the JavaFX Application Thread.
 */
public final class ServerCall {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "drs-client-io");
        t.setDaemon(true);
        return t;
    });

    private ServerCall() {
    }

    /**
     * Sends {@code request}; {@code onResponse} runs on the FX thread with the
     * server's response, or with an error response if the server is unreachable.
     */
    public static void send(Request request, Consumer<Response> onResponse) {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ClientSession.getConnection().send(request);
            }
        };
        task.setOnSucceeded(e -> onResponse.accept(task.getValue()));
        task.setOnFailed(e -> onResponse.accept(Response.error("Cannot reach the DRS server ("
                + task.getException().getMessage() + "). Is ServerMain running?")));
        EXECUTOR.execute(task);
    }

    /** Runs code on the FX thread (convenience for callers on other threads). */
    public static void ui(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
