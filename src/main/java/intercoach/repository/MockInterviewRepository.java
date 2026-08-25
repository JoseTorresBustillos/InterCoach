package intercoach.repository;

import intercoach.model.MockInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MockInterviewRepository
        extends JpaRepository<MockInterviewSession, Long> {

    List<MockInterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);

    @Query("select interview.user.id from MockInterviewSession interview where interview.id = :sessionId")
    Optional<Long> findUserIdById(@Param("sessionId") Long sessionId);
}
