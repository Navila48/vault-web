package meety.controllers;

import lombok.RequiredArgsConstructor;
import meety.dtos.ChatMessageDto;
import meety.models.ChatMessage;
import meety.services.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    /**
     * Handles incoming chat messages and broadcasts them to the appropriate group topic.
     *
     * @param messageDto DTO containing the message content, sender, and group.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto messageDto) {
        ChatMessage savedMessage = chatService.saveMessage(messageDto);

        messagingTemplate.convertAndSend(
                "/topic/group/" + savedMessage.getGroup().getId(),
                savedMessage
        );
    }
}
