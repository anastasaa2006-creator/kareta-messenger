package ru.gr0946x.server.db.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0946x.server.db.entity.User;
import ru.gr0946x.server.db.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервисный слой бизнес-логики для управления пользователями.
 * <p>
 * Выступает посредником между уровнем данных
 * ({@link ru.smak.db.repository.UserRepository})
 * и внешним миром (контроллерами или консолью).
 * <p>
 * <b>Основные обязанности:</b>
 * <ul>
 *   <li><b>Регистрация:</b> создание нового пользователя с
 *       предварительной проверкой уникальности ника и хэшированием
 *       пароля через {@link BCrypt}.</li>
 *   <li><b>Авторизация:</b> проверка корректности введённого
 *       пароля по сравнению с сохранённым хэшем.</li>
 *   <li><b>Безопасность:</b> обеспечивает целостность данных с
 *       помощью аннотации {@link Transactional}.</li>
 * </ul>
 * <p>
 * В случае нарушения бизнес-правил (занятый ник или неверный пароль) методы
 * выбрасывают {@link IllegalArgumentException}.
 *
 * @author Маклецов С. В.
 */
@Service
public class UserService {
    private final UserRepository userRepository;

    /**
     * Создаёт экземпляр сервиса с внедрённым репозиторием пользователей.
     * <p>
     * Использует конструкторную инъекцию зависимостей,
     * что является рекомендуемой практикой в Spring для обязательных
     * компонентов.
     * @param userRepository репозиторий для работы с сущностью {@link User}
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Проверяет, занят ли указанный псевдоним (никнейм).
     * <p>
     * Выполняет быструю проверку без загрузки полной сущности из базы данных.
     * Аннотация {@code @Transactional(readOnly = true)} оптимизирует транзакцию
     * только для операций чтения.
     * @param nick псевдоним для проверки (регистронезависимо)
     * @return {@code true}, если ник уже существует в базе, иначе {@code false}
     */
    @Transactional(readOnly = true)
    public boolean isNickTaken(String nick) {
        return userRepository.existsByNickIgnoreCase(nick);
    }

    /**
     * Регистрирует нового пользователя в системе.
     * <p>
     * Метод проверяет уникальность ника, генерирует криптографический
     * хэш пароля с помощью {@link BCrypt} и сохраняет запись в базу данных.
     * Вся операция выполняется в одной транзакции.
     *
     * @param nick уникальный псевдоним пользователя
     * @param password пароль в открытом виде
     *                 (будет захэширован перед сохранением)
     * @return сохранённая сущность {@link User} с присвоенным ID
     * @throws IllegalArgumentException если указанный ник уже зарегистрирован
     */
    @Transactional
    public User register(String nick, String password) {
        var existing = userRepository.existsByNickIgnoreCase(nick);
        if (existing) {
            throw new IllegalArgumentException("Ник "+nick+" уже занят");
        }
        var passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(nick, passwordHash);
        return userRepository.save(newUser);
    }

    /**
     * Выполняет авторизацию пользователя по нику и паролю.
     * <p>
     * Находит пользователя в базе и сравнивает введённый пароль с
     * сохранённым хэшем через {@link BCrypt#checkpw}.
     * При успешной проверке возвращает сущность пользователя.
     *
     * @param nick псевдоним пользователя
     * @param password пароль для проверки
     * @return сущность {@link User}, если учётные данные верны
     * @throws IllegalArgumentException если пользователь не найден
     * или пароль не совпадает
     */
    @Transactional
    public User login(String nick, String password) {
        Optional<User> existing = userRepository.findByNickIgnoreCase(nick);
        if (existing.isPresent()) {
            var user = existing.get();
            if (BCrypt.checkpw(password, user.getPasswordHash()))
                return existing.get();
        }
        throw new IllegalArgumentException("Неверный ник или пароль");
    }

    /**
     * Возвращает список всех зарегистрированных пользователей.
     * <p>
     * Делегирует вызов репозиторию. Рекомендуется использовать с осторожностью
     * при большом количестве записей в базе
     * (в продакшене лучше применять пагинацию).
     * @return список сущностей {@link User}
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}