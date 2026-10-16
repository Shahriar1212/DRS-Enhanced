package com.cqu.drs.server;

import com.cqu.drs.protocol.Request;
import com.cqu.drs.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Serves one connected client on a worker thread from the server's pool:
 * reads {@link Request} objects, passes them to the {@link RequestDispatcher}
 * and writes back {@link Response} objects until the client disconnects.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final int clientNumber;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher, int clientNumber) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.clientNumber = clientNumber;
    }

    @Override
    public void run() {
        String who = "client-" + clientNumber + " " + socket.getRemoteSocketAddress();
        DRSServer.log("Connected " + who);
        try (Socket s = socket;
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                while (!s.isClosed()) {
                    Object obj = in.readObject();
                    if (!(obj instanceof Request)) {
                        out.writeObject(Response.error("Unknown message type"));
                        continue;
                    }
                    Request req = (Request) obj;
                    long start = System.nanoTime();
                    Response resp = dispatcher.handle(req);
                    out.writeObject(resp);
                    out.flush();
                    out.reset();   // do not let the stream cache stale copies of objects
                    DRSServer.log(who + " " + req.getCommand() + " -> " + (resp.isSuccess() ? "OK" : "ERROR: " + resp.getMessage())
                            + " (" + (System.nanoTime() - start) / 1_000_000 + " ms)");
                }
            }
        } catch (EOFException | SocketException e) {
            // client closed the connection
        } catch (IOException | ClassNotFoundException e) {
            DRSServer.log(who + " error: " + e.getMessage());
        } finally {
            DRSServer.log("Disconnected " + who);
        }
    }
}
