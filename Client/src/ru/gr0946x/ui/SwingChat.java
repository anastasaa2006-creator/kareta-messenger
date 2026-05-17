package ru.gr0946x.ui;

import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SwingChat extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private Client client;

    public SwingChat() {
        setTitle("Карета Мессенджер");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Область чата
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(chatArea);
        add(scroll, BorderLayout.CENTER);

        // Нижняя панель
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Отправить");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Обработчики
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) return;

        client.sendData(text);
        inputField.setText("");
        inputField.requestFocus();
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

            client.addDataListener((data, type) -> {
                switch (type) {
                    case MESSAGE -> {
                        String[] parts = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
                        if (parts.length == 2) {
                            appendMessage(parts[0] + ": " + parts[1]);
                        }
                    }
                    case PRIVATE_MESSAGE -> {
                        String[] parts = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
                        if (parts.length == 2) {
                            appendMessage("[Лично от " + parts[0] + "]: " + parts[1]);
                        }
                    }
                    case INFO, HISTORY_RESPONSE, SEARCH_RESPONSE -> appendMessage("[INFO] " + data);
                    case ERROR -> appendMessage("[ОШИБКА] " + data);
                    case REQUEST -> appendMessage("[ЗАПРОС] " + data);
                    default -> appendMessage(data);
                }
            });

            client.start();
            appendMessage("=== КАРЕТА МЕССЕНДЖЕР ===");
            appendMessage("Подключено к серверу");
            appendMessage("Введите LOGIN:ник:пароль или REG:ник:пароль");

            inputField.requestFocus();

        } catch (IOException e) {
            appendMessage("Ошибка: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingChat window = new SwingChat();
        window.connectToServer();
    }
}