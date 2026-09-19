package za.codemaster.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    @Query("select s.user from Session s where s.id = :id and s.expiresAt > :now")
    Optional<User> findActiveUser(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
