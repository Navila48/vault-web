package meety.services;

import lombok.RequiredArgsConstructor;
import meety.dtos.ChatMessageDto;
import meety.models.ChatMessage;
import meety.models.Group;
import meety.models.User;
import meety.repositories.ChatMessageRepository;
import meety.repositories.GroupRepository;
import meety.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    /**
     * Saves a chat message to the database.
     *
     * @param dto DTO containing the content, sender ID, and group ID.
     * @return The persisted ChatMessage entity.
     */
    public ChatMessage saveMessage(ChatMessageDto dto) {
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        ChatMessage message = new ChatMessage();
        message.setContent(dto.getContent());
        message.setSender(sender);
        message.setGroup(group);

        if (dto.getTimestamp() != null) {
            message.setTimestamp(Instant.parse(dto.getTimestamp()));
        } else {
            message.setTimestamp(Instant.now());
        }

        return chatMessageRepository.save(message);
    }
}
