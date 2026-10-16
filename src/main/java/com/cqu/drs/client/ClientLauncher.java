package com.cqu.drs.client;

/**
 * Plain main class that starts {@link ClientApp}. Lets the client be started
 * with "Run File" in an IDE or with java -cp on the class path (where a class
 * extending Application cannot be the main class without the module path).
 */
public final class ClientLauncher {

    private ClientLauncher() {
    }

    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
