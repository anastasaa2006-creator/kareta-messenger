package ru.gr0946x.net;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import ru.gr0946x.server.db.entity.User;
import ru.gr0946x.server.db.service.UserService;
import ru.gr0946x.server.db.service.MessageService;


public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private User currentUser = null;
    private String name = null;
    private UserService userService;
    private MessageService messageService;

    public ConnectedClient(Socket socket, UserService userService, MessageService messageService) throws IOException {
        this.userService = userService;
        this.messageService = messageService;
        this.communicator = new Communicator(socket);
        this.communicator.addDataListener(this::parseData);
        synchronized (clients) {
            clients.add(this);
        }
    }
    public void start(){
        communicator.start();
        sendData(MessageType.REQUEST
                + ProtocolConstants.COMMAND_SEPARATOR
                + "Введите имя:");
    }

    public void sendData(String data){
        communicator.sendData(data);
    }

    private void parseData(String data){
        if (currentUser == null) {
            String[] parts = data.split(":", 3);
            if (parts.length < 3) {
                sendData(MessageType.ERROR + ":" + "Для регистрации: REG:ВашНик:Пароль");
                sendData(MessageType.REQUEST + ":" + "Для входа: LOGIN:ВашНик:Пароль");
                return;
            }

            String cmd = parts[0];
            String nick = parts[1];
            String pass = parts[2];

            if ("LOGIN".equalsIgnoreCase(cmd)) {
                try {
                    currentUser = userService.login(nick, pass);
                    name = currentUser.getNick();
                    sendData(MessageType.INFO + ":" + "Добро пожаловать, " + name);
                    sendForAll(MessageType.INFO, "Пользователь " + name + " вошел в чат");
                } catch (Exception e) {
                    sendData(MessageType.ERROR + ":" + e.getMessage());
                }
            } else if ("REG".equalsIgnoreCase(cmd)) {
                if (!nick.matches("^[A-Za-zА-Яа-я].*$")) {
                    sendData(MessageType.ERROR + ":" + "Имя должно начинаться с буквы");
                    return;
                }
                try {
                    currentUser = userService.register(nick, pass);
                    name = currentUser.getNick();
                    sendData(MessageType.INFO + ":" + "Регистрация успешна! Добро пожаловать, " + name);
                    sendForAll(MessageType.INFO, "Пользователь " + name + " присоединился к чату");
                } catch (Exception e) {
                    sendData(MessageType.ERROR + ":" + e.getMessage());
                }
            } else {
                sendData(MessageType.ERROR + ":" + "Неизвестная команда. Используйте LOGIN или REG");
            }
        } else {
            handleCommand(data);
        }
    }

    private void sendForAll(MessageType type, String data){
        var author = (type == MessageType.MESSAGE) ?
                name + ProtocolConstants.AUTHOR_SEPARATOR :
                "";
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.name != null)
                    .forEach(client -> {
                        client.sendData(type
                                + ProtocolConstants.COMMAND_SEPARATOR
                                + author
                                + data);
                    });
        }
    }
    private boolean isInUse(String name){
        synchronized (clients) {
            return clients.stream()
                    .anyMatch(c -> c.name != null && c.name.equalsIgnoreCase(name));
        }
    }

    public void stop(){
        communicator.stop();
    }

    private void handleCommand(String data) {
        String[] parts = data.split(":", 3);
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "MSG" -> {
                if (parts.length > 1) {
                    sendForAll(MessageType.MESSAGE, parts[1]);
                }
            }
            case "PRIVATE" -> {
                if (parts.length > 2) {
                    try {
                        messageService.saveMessage(currentUser.getNick(), parts[1], parts[2]);
                        findClientByNick(parts[1]).ifPresent(client ->
                                client.sendData(MessageType.PRIVATE_MESSAGE + ":" + currentUser.getNick() + ":" + parts[2])
                        );
                        sendData(MessageType.INFO + ":" + "Отправлено " + parts[1]);
                    } catch (Exception e) {
                        sendData(MessageType.ERROR + ":" + e.getMessage());
                    }
                }
            }
            case "HISTORY" -> {
                if (parts.length > 1) {
                    // TODO: добавить историю
                }
            }
            case "SEARCH" -> {
                if (parts.length > 2) {
                    try {
                        var results = messageService.searchMessagesWithUser(currentUser.getNick(), parts[1], parts[2]);
                        sendData(MessageType.INFO + ":" + "Найдено: " + results.size());
                        for (var msg : results) {
                            String from = msg.getSender().getNick().equals(currentUser.getNick()) ? "Я" : msg.getSender().getNick();
                            sendData(MessageType.SEARCH_RESPONSE + ":" + from + ": " + msg.getText());
                        }
                    } catch (Exception e) {
                        sendData(MessageType.ERROR + ":" + e.getMessage());
                    }
                }
            }
            case "USERS" -> sendUserList();
            default -> sendData(MessageType.ERROR + ":" + "Неизвестно. MSG, PRIVATE, HISTORY, SEARCH, USERS");
        }
    }

    private java.util.Optional<ConnectedClient> findClientByNick(String nick) {
        synchronized (clients) {
            return clients.stream()
                    .filter(c -> c.name != null && c.name.equalsIgnoreCase(nick))
                    .findFirst();
        }
    }

    private void sendUserList() {
        List<String> online;
        synchronized (clients) {
            online = clients.stream()
                    .filter(c -> c.name != null && c != this)
                    .map(c -> c.name)
                    .toList();
        }
        sendData(MessageType.INFO + ":" + "Онлайн: " + (online.isEmpty() ? "нет" : String.join(", ", online)));
    }
}
