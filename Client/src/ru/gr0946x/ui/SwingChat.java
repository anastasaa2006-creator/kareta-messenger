package ru.gr0946x.ui;

import ru.gr0946x.net.Client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SwingChat extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private Client client;

    public SwingChat() {
        setTitle("Карета Мессенджер");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Область чата
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Поле ввода
        inputField = new JTextField();
        add(inputField, BorderLayout.SOUTH);

        // Отправка по Enter
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Отправляем на сервер
        client.sendData(text);
        inputField.setText("");
    }

    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void connectToServer() {
        try {
            client = new Client("localhost", 9460);

            // Обработка входящих сообщений
            client.addDataListener((data, type) -> {
                switch (type) {
                    case MESSAGE -> {
                        String[] parts = data.split(":", 2);
                        appendMessage(parts[0] + ": " + parts[1]);
                    }
                    case INFO, HISTORY_RESPONSE, SEARCH_RESPONSE -> appendMessage("[INFO] " + data);
                    case ERROR -> appendMessage("[ERROR] " + data);
                    case REQUEST -> appendMessage("[ЗАПРОС] " + data);
                    default -> appendMessage(data);
                }
            });

            client.start();
            appendMessage("Подключено к серверу");

        } catch (IOException e) {
            appendMessage("Ошибка подключения: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingChat window = new SwingChat();
        window.connectToServer();
    }
}