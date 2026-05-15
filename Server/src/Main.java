package ru.gr0946x.net;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0946x.server.db.config.DatabaseConfig;
import ru.gr0946x.server.db.service.UserService;

public class Main {
    public static void main(String[] args) {
        // Запускаем Spring
        var context = new AnnotationConfigApplicationContext(DatabaseConfig.class);
        var userService = context.getBean(UserService.class);

        // Запускаем сервер с userService
        new Server(9460, userService);

        System.out.println("Сервер запущен с БД");
    }
}