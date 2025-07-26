package meety.dtos;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String content;
    private Long senderId;
    private Long groupId;
    private String timestamp;
}