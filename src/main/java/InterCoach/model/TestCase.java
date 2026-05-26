package InterCoach.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 3000)
    private String input;

    @Column(length = 3000)
    private String expectedOutput;

    private boolean hidden;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public boolean isHidden() { return hidden; }
    public Instant getCreatedAt() { return createdAt; }
    public Problem getProblem() { return problem; }

    public void setInput(String input) { this.input = input; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public void setProblem(Problem problem) { this.problem = problem; }
}