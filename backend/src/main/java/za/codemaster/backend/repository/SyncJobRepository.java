package za.codemaster.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import za.codemaster.backend.domain.model.SyncJob;
import java.util.UUID;
public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {}
