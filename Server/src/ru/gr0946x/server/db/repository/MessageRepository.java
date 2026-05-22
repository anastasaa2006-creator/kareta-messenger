package ru.gr0946x.server.db.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.server.db.entity.Message;
import ru.gr0946x.server.db.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import java.util.Optional;
import java.util.List;

/**
 * Репозиторий для работы с публикациями.
 * <p>
 * Наследует стандартные CRUD-операции от {@link JpaRepository}
 * и добавляет специфичные методы выборки для социальной сети.
 * <p>
 * Spring Data JPA автоматически реализует этот интерфейс во время выполнения,
 * генерируя необходимые SQL-запросы на основе имён методов и аннотаций.
 *
 * @author Маклецов С. В.
 * @see Post
 * @see ru.smak.db.service.PostService
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Возвращает все публикации конкретного автора,
     * отсортированные по дате создания (новые сверху).
     * <p>
     * Генерирует запрос:
     * {@code SELECT p FROM Post p WHERE p.author = ?1 ORDER BY p.createdAt DESC}
     *
     * @param author пользователь-автор публикаций
     * @return список постов, отсортированный по убыванию даты
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :u1 AND m.recipient = :u2) OR " +
            "(m.sender = :u2 AND m.recipient = :u1) " +
            "ORDER BY m.timestamp DESC")
    List<Message> findConversation(@Param("u1") User u1, @Param("u2") User u2);

    /**
     * Ищет публикации автора, содержащие заданный фрагмент текста.
     * <p>
     * Использует кастомный JPQL-запрос с оператором {@code LIKE}
     * для полнотекстового поиска без учёта регистра.
     * <p>
     * Параметры запроса передаются через аннотацию {@code @Param},
     * что обеспечивает безопасность от SQL-инъекций.
     *
     * @param fragment искомый фрагмент текста
     * @param author пользователь, чьи публикации ищем
     * @return список найденных публикаций (может быть пустым)
     */
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1)) " +
            "AND LOWER(m.text) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Message> searchMessagesBetweenUsers(@Param("user1") User user1,
                                             @Param("user2") User user2,
                                             @Param("keyword") String keyword);
    @Modifying
    @Query("UPDATE Message m SET m.status = :status WHERE m.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ' WHERE m.sender = :sender AND m.recipient = :recipient AND m.status = 'SENT'")
    void markMessagesAsRead(@Param("sender") User sender, @Param("recipient") User recipient);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.nick = :nick OR m.recipient.nick = :nick) " +
            "AND LOWER(m.text) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Message> searchAllMessages(@Param("nick") String nick, @Param("keyword") String keyword);

    @Query("SELECT m FROM Message m WHERE m.sender = :user OR m.recipient = :user OR m.recipient IS NULL")
    List<Message> findMessagesForUser(@Param("user") User user);

    @Query("SELECT m FROM Message m WHERE m.id = :id")
    Optional<Message> findMessageById(@Param("id") Long id);

    @Query("SELECT m FROM Message m WHERE m.recipient.nick = :nick AND m.status = 'SENT' ORDER BY m.timestamp ASC")
    List<Message> findUnreadMessagesForUser(@Param("nick") String nick);
}