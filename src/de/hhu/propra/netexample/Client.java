package de.hhu.propra.netexample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client extends Application {
    private TextArea messageArea = new TextArea();
    private TextField textField = new TextField();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    // Must be volatile (Set by client-thread, read by threadpool-thread)
    private volatile PrintWriter out;

    private String serverAddress;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Chat");

        textField.setOnAction((event) -> {
            sendToServer(textField.getText());
            textField.setText("");
        });

        messageArea.setEditable(false);
        messageArea.setPrefSize(800, 600);

        BorderPane root = new BorderPane();
        root.setCenter(messageArea);
        root.setBottom(textField);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        serverAddress = getServerAddress();

        executorService.execute(this::run);
    }

    private void run() {
        // Create resources using try-with-resources statement
        try (Socket socket = new Socket(serverAddress, Server.PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Set the writer reference (volatile-flush)
            this.out = out;

            chat(in);
        } catch (IOException e) {
            appendMessage("[Error] " + e.getMessage());
        } catch (NullPointerException e) {
            appendMessage("[Warning] Server quit");
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private void chat(BufferedReader in) throws IOException {
        String name = null;

        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                sendToServer(name = getUserName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                sendToServer(name + " hat den Raum betreten");
            } else if (line.startsWith("MESSAGE")) {
                appendMessage(line.substring(8));
            }
        }
    }

    private void sendToServer(String msg) {
        executorService.execute(() -> out.println(msg));
    }

    private String showInputDialog(String placeholder, String header, String text) {
        TextInputDialog dialog = new TextInputDialog(placeholder);
        dialog.setTitle("Chat-Example");
        dialog.setHeaderText(header);
        dialog.setContentText(text);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        }

        return null;
    }

    private String getServerAddress() {
        return showInputDialog("127.0.0.1", "IP-Adresse", "Bitte geben Sie die IP-Adresse des Servers an.");
    }

    private String getUserName() {
        return showInputDialog("", "Name", "Bitte geben Sie Ihren Namen ein.");
    }

    private CompletableFuture<String> getUserNameLater() {
        CompletableFuture<String> future = new CompletableFuture<>();
        Platform.runLater(() -> future.complete(getUserName()));
        return future;
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> messageArea.appendText(message + "\n"));
    }
}
