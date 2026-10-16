package com.cqu.drs.client;

import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.protocol.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Client side of the socket protocol. One persistent TCP connection per
 * client; {@link #send(Request)} is synchronized so background tasks can share
 * it safely, and it transparently reconnects once if the link dropped.
 */
public class ServerConnection {

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String sessionToken;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Opens the connection (5 second timeout). */
    public synchronized void connect() throws IOException {
        close();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /** Sends a request (adding the session token) and waits for the response. */
    public synchronized Response send(Request request) throws IOException {
        request.setSessionToken(sessionToken);
        try {
            return exchange(request);
        } catch (IOException first) {
            // connection lost (e.g. server restarted) - reconnect once and retry
            connect();
            return exchange(request);
        }
    }

    private Response exchange(Request request) throws IOException {
        if (!isConnected()) {
            connect();
        }
        out.writeObject(request);
        out.flush();
        out.reset();
        try {
            Object reply = in.readObject();
            if (reply instanceof Response) {
                return (Response) reply;
            }
            throw new IOException("Unexpected reply from server");
        } catch (ClassNotFoundException e) {
            throw new IOException("Incompatible server version", e);
        }
    }

    public synchronized void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    /** Logs out (best effort) and closes the socket. */
    public synchronized void logoutAndClose() {
        if (sessionToken != null && isConnected()) {
            try {
                send(new Request(Command.LOGOUT));
            } catch (IOException ignored) {
                // closing anyway
            }
        }
        sessionToken = null;
        close();
    }

    public synchronized void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // already closed
        }
        socket = null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
