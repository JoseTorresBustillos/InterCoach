package InterCoach.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Store submitted code as text because solutions can be several lines long.
    @Column(length = 10000, nullable = false)
    private String submittedCode;

    private String language;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(length = 10000)
    private String aiFeedback;

    private String timeComplexity;
    private String spaceComplexity;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();

        // New submissions start pending until AI review/code execution is added.
        if (status == null) {
            status = SubmissionStatus.PENDING;
        }
    }

    public Long getId() { return id; }
    public String getSubmittedCode() { return submittedCode; }
    public String getLanguage() { return language; }
    public SubmissionStatus getStatus() { return status; }
    public String getAiFeedback() { return aiFeedback; }
    public String getTimeComplexity() { return timeComplexity; }
    public String getSpaceComplexity() { return spaceComplexity; }
    public Instant getCreatedAt() { return createdAt; }
    public Problem getProblem() { return problem; }

    public void setSubmittedCode(String submittedCode) { this.submittedCode = submittedCode; }
    public void setLanguage(String language) { this.language = language; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }
    public void setProblem(Problem problem) { this.problem = problem; }
}