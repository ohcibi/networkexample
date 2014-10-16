package de.hhu.propra.netexample;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends JFrame {
    BufferedReader in;
    PrintWriter out;
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);

    public Client() {
        messageArea.setEditable(false);

        setTitle("Chat");
        getContentPane().add(textField, "North");
        getContentPane().add(new JScrollPane(messageArea), "Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();

        textField.addActionListener((e) -> {
            out.println(textField.getText());
            textField.setText("");
        });
    }

    private String showInputDialog(String message, String title) {
        return JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    private String getServerAddress() {
        return showInputDialog("IP Adresse eingeben", "Willkommen");
    }

    private String getUserName() {
        return showInputDialog("Name eingeben", "Name");
    }

    private void run() throws IOException {
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getUserName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.setVisible(true);
        client.run();
    }
}
