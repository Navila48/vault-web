package vaultWeb.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vaultWeb.models.PrivateChat;
import vaultWeb.models.User;

public interface PrivateChatRepository extends JpaRepository<PrivateChat, Long> {
  Optional<PrivateChat> findByUser1AndUser2(User user1, User user2);

  Optional<PrivateChat> findByUser2AndUser1(User user1, User user2);

  List<PrivateChat> findByUser1OrUser2(User user1, User user2);
}
