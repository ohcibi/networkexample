package de.hhu.propra.netexample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class Server {
    static final int PORT = 1337;

    private static HashSet<String> names = new HashSet<String>();
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        try (ServerSocket listener = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = listener.accept();
                new Thread(new Handler(socket)).start();
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        public void connect() throws IOException {
            getName();
            out.println("NAMEACCEPTED");
            synchronized (writers) {
                writers.add(out);
            }
            broadcast();
        }

        private void broadcast() throws IOException {
            while (true) {
                String input = in.readLine();
                if (input == null) {
                    return;
                }
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + ": " + input);
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
            if (out != null) {
                synchronized (writers) {
                    writers.remove(out);
                }
            }
            try {
                socket.close();
            } catch (IOException e) { }
        }
    }
}