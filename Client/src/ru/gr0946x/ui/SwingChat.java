package ru.gr0946x.ui;

import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class SwingChat extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private Client client;
    private String currentNick;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JTextField searchField;
    private JButton searchButton;
    private JTextArea searchResultArea;

    public SwingChat() {
        setTitle("Карета Мессенджер");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Левая панель - Онлайн
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(180, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("Онлайн"));
        add(userScroll, BorderLayout.WEST);

        // Центр
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Чат"));
        centerPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Новое сообщение"));
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton sendButton = new JButton("Отправить");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Верхняя панель - Поиск
        JPanel searchPanel = new JPanel(new BorderLayout(10, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Поиск сообщений"));

        JPanel searchInputPanel = new JPanel(new BorderLayout(10, 5));
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchButton = new JButton("Найти");
        searchInputPanel.add(searchField, BorderLayout.CENTER);
        searchInputPanel.add(searchButton, BorderLayout.EAST);

        searchResultArea = new JTextArea(5, 0);
        searchResultArea.setEditable(false);
        searchResultArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane resultScroll = new JScrollPane(searchResultArea);
        resultScroll.setPreferredSize(new Dimension(0, 100));

        searchPanel.add(searchInputPanel, BorderLayout.NORTH);
        searchPanel.add(resultScroll, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);

        // Обработчики
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        searchButton.addActionListener(e -> searchMessages());

        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) return;

        client.sendData(text);
        inputField.setText("");
        inputField.requestFocus();
    }

    private void searchMessages() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            searchResultArea.setText("Введите слово для поиска");
            return;
        }
        searchResultArea.setText("Ищу \"" + keyword + "\"...\n");
        client.sendData("SEARCHALL:" + keyword);
    }

    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            String onlinePart = users.replace("Онлайн:", "").trim();
            if (!onlinePart.equals("нет") && !onlinePart.isEmpty()) {
                String[] online = onlinePart.split(", ");
                for (String user : online) {
                    if (!user.equals(currentNick) && !user.isEmpty()) {
                        userListModel.addElement(user);
                    }
                }
            }
        });
    }

    public void connectToServer(String nick) {
        try {
            this.currentNick = nick;
            client = new Client("localhost", 9460);

            client.addDataListener((data, type) -> {
                SwingUtilities.invokeLater(() -> {
                    switch (type) {
                        case MESSAGE -> {
                            int idx = data.indexOf(":");
                            if (idx > 0) {
                                String author = data.substring(0, idx);
                                String msg = data.substring(idx + 1);
                                appendMessage(author + " (всем): " + msg);
                            } else {
                                appendMessage(data);
                            }
                        }
                        case PRIVATE_MESSAGE -> {
                            // Формат: автор:текст:messageId
                            String[] msgParts = data.split(":");
                            if (msgParts.length >= 2) {
                                String author = msgParts[0];
                                String msgText = msgParts[1];
                                String msgId = msgParts.length > 2 ? msgParts[2] : null;

                                appendMessage("[Лично от " + author + "]: " + msgText + " ✓✓");

                                if (msgId != null) {
                                    client.sendData("READ:" + msgId);
                                }
                            }
                        }
                        case INFO -> {
                            if (data.startsWith("Онлайн:")) {
                                updateUserList(data);
                            }
                            if (data.contains("✓✓")) {
                                updateLastMessageStatusToRead();
                            }
                            appendMessage(data);
                        }
                        case SEARCH_RESPONSE -> {
                            searchResultArea.append(data + "\n");
                        }
                        case HISTORY_RESPONSE -> {
                            appendMessage("[ИСТОРИЯ] " + data);
                        }
                        case ERROR -> {
                            appendMessage("[ОШИБКА] " + data);
                        }
                        default -> {
                            appendMessage(data);
                        }
                    }
                });
            });

            client.start();
            appendMessage("=== КАРЕТА МЕССЕНДЖЕР ===");
            appendMessage("Подключено к серверу");
            appendMessage("Введите REG:ник:пароль или LOGIN:ник:пароль");
            inputField.requestFocus();

        } catch (IOException e) {
            appendMessage("Ошибка: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingChat window = new SwingChat();
        window.connectToServer("");
    }

    private void updateLastMessageStatusToRead() {
        SwingUtilities.invokeLater(() -> {
            String text = chatArea.getText();
            if (text.isEmpty()) return;

            String[] lines = text.split("\n");
            if (lines.length == 0) return;

            String lastLine = lines[lines.length - 1];
            if (lastLine.contains(" ✓") && !lastLine.contains("✓✓")) {
                lastLine = lastLine.replace(" ✓", " ✓✓");
                lines[lines.length - 1] = lastLine;
                chatArea.setText(String.join("\n", lines));
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
}