package vaultWeb.dtos.dashboard;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated payload returned to the frontend so a dashboard can render all user-centric data with
 * a single request.
 */
public record UserDashboardDto(
    ProfileSummary profile,
    List<GroupSummary> groups,
    List<PrivateChatSummary> privateChats,
    List<PollSummary> polls,
    List<MessagePreview> recentMessages) {

  public record ProfileSummary(
      Long id, String username, int groupCount, int privateChatCount, long messagesSent) {}

  public record GroupSummary(
      Long id,
      String name,
      String description,
      String role,
      boolean isPublic,
      int memberCount,
      Instant createdAt,
      int pollCount) {}

  public record PrivateChatSummary(
      Long id, String participant, String lastMessagePreview, Instant lastMessageAt) {}

  public record PollSummary(
      Long id,
      String question,
      Long groupId,
      String groupName,
      boolean anonymous,
      Instant deadline,
      int optionCount,
      int totalVotes) {}

  public record MessagePreview(
      Long id, String content, Instant timestamp, Long groupId, Long privateChatId) {}
}
