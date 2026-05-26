package InterCoach.repository;

// JPA repository used to interact with the problems table.

import InterCoach.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}