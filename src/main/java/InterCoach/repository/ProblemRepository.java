package InterCoach.repository;

// JPA repository used to interact with the problems table.

import InterCoach.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import InterCoach.model.Difficulty;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
}