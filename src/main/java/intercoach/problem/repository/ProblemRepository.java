package intercoach.problem.repository;

import intercoach.problem.model.Difficulty;
import intercoach.problem.model.Problem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository used to interact with the problems table.
 */
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByDifficulty(Difficulty difficulty);
}
