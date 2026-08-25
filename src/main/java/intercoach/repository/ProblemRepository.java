package intercoach.repository;

import intercoach.model.Difficulty;
import intercoach.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository used to interact with the problems table.
 */
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
}
