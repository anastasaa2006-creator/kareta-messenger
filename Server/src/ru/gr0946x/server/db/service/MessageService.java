package ru.gr0946x.server.db.service;

import org.springframework.stereotype.Service;
import ru.gr0946x.server.db.entity.Message;
import ru.gr0946x.server.db.entity.User;
import ru.gr0946x.server.db.repository.MessageRepository;
import ru.gr0946x.server.db.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;


    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }



    @Transactional
    public Message saveMessage(String senderNick, String recipientNick, String text) {
        User sender = userRepository.findByNickIgnoreCase(senderNick)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        User recipient = userRepository.findByNickIgnoreCase(recipientNick)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        Message message = new Message(sender, recipient, text);
        return messageRepository.save(message);
    }





//    @Transactional(readOnly = true)
//    public List<Post> getUserFeedWithInitializedAuthors(User author) {
//        var posts = postRepository.findByAuthorOrderByCreatedAtDesc(author);
//        // Инициализация прокси: обращение к полю внутри транзакции
//        posts.forEach(post -> post.getAuthor().getNick());
//        return posts;
//    }
//
//    @Transactional(readOnly = true)
//    public List<PostDto> getUserFeedAsDto(User author) {
//        return postRepository.findByAuthorOrderByCreatedAtDesc(author)
//                .stream()
//                .map(p -> new PostDto(
//                        p.getId(),
//                        p.getAuthor().getNick(), // безопасно: внутри транзакции
//                        p.getContent(),
//                        p.getCreatedAt()
//                ))
//                .toList();
//    }



    @Transactional(readOnly = true)
    public List<Message> searchMessagesWithUser(String currentNick, String otherNick, String keyword) {
        User current = userRepository.findByNickIgnoreCase(currentNick)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        User other = userRepository.findByNickIgnoreCase(otherNick)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return messageRepository.searchMessagesBetweenUsers(current, other, keyword);
    }
}