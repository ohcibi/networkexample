package de.hhu.propra.netexample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    static final int PORT = 1337;

    private static final HashSet<String> names = new HashSet<>();
    private static final HashSet<PrintWriter> writers = new HashSet<>();

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        try (ServerSocket listener = new ServerSocket(PORT)) {
            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = listener.accept();
                executorService.execute(new Handler(socket));
            }
        }
    }

    private static class Handler implements Runnable {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (ClientConnection connection = new ClientConnection(socket)) {
                connection.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientConnection implements AutoCloseable {
        private final BufferedReader in;
        private final PrintWriter out;
        private final Socket socket;
        private String name;

        public ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void connect() throws IOException {
            getName();
            out.println("NAMEACCEPTED");
            synchronized (writers) {
                writers.add(out);
            }
            startBroadcasting();
        }

        private void startBroadcasting() throws IOException {
            while (true) {
                String input = in.readLine();
                if (input == null) {
                    return;
                }
                broadcast("MESSAGE " + name + ": " + input);
            }
        }

        private void broadcast(String message) {
            synchronized (writers) {
                for (PrintWriter writer : writers) {
                    writer.println(message);
                }
            }
        }

        private void getName() throws IOException {
            while (true) {
                out.println("SUBMITNAME");
                name = in.readLine();
                if (name == null) {
                    return;
                }
                synchronized (names) {
                    if (!names.contains(name)) {
                        names.add(name);
                        break;
                    }
                }
            }
        }

        @Override
        public void close() throws Exception {
            if (name != null) {
                synchronized (names) {
                    names.remove(name);
                }
            }
            synchronized (writers) {
                writers.remove(out);
            }
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }
}
