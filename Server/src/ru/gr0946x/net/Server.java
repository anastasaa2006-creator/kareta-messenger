package ru.gr0946x.net;

import ru.gr0946x.server.db.service.UserService;
import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private boolean isActive;
    private UserService userService;

    public Server(int port, UserService userService) {
        this.userService = userService;
        isActive = true;
        new Thread(()->{
            try (var serverSocket = new ServerSocket(port)) {
                System.out.println("Сервер запущен");
                while (isActive) {
                    try{
                        var socket = serverSocket.accept();
                        System.out.println("Клиент подключен");
                        var connClient = new ConnectedClient(socket, userService);
                        connClient.start();
                    } catch (Exception e) {
                        System.out.println("Ошибка подключения клиентов...");
                        isActive = false;
                    }
                }
            } catch (IOException e) {
                System.out.println("Ошибка включения сервера");
            }
        }).start();
    }
}