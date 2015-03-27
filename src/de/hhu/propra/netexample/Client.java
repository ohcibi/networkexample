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
import java.util.function.Consumer;

public class Client extends Application {
    private TextArea messageArea = new TextArea();
    private TextField textField = new TextField();

    private BufferedReader in;
    private PrintWriter out;

    private String serverAddress;
    private String name;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Chat");

        textField.setOnAction((event) -> {
            out.println(textField.getText());
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

        new Thread(() -> {
            try {
                run();
            } catch (IOException e) { }
        }).start();
    }

    private void run() throws IOException {
        Socket socket = new Socket(serverAddress, Server.PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                getUserName((name) -> {
                    this.name = name;
                    out.println(name);
                });
            } else if (line.startsWith("NAMEACCEPTED")) {
                out.println(name + " hat den Raum betreten");
            } else if (line.startsWith("MESSAGE")) {
                appendMessage(line.substring(8));
            }
        }
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

    private void getUserName(Consumer<String> callback) {
        Platform.runLater(() -> {
            callback.accept(showInputDialog("", "Name", "Bitte geben Sie Ihren Namen ein."));
        });
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            messageArea.appendText(message + "\n");
        });
    }
}
