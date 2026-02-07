package vaultWeb.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateGroupFromChatsRequest {
    @NotEmpty(message = "Chat IDS list can not be empty")
    List<Long> privateChatIds;
    @NotBlank(message = "Group name can not be blank")
    String groupName;
    String description;
}
