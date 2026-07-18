package InterCoach.repository;

import InterCoach.model.MockInterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockInterviewRepository
        extends JpaRepository<MockInterviewSession, Long> {

    List<MockInterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);
}