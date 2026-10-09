package intercoach.problem.repository;

import intercoach.problem.model.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository used to interact with the test cases table.
 */
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProblemId(Long problemId);
}
