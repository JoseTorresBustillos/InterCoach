package intercoach.repository;

import intercoach.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA repository used to interact with the test cases table.
 */
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProblemId(Long problemId);
}
