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

    // Store structured AI feedback so later features can analyze weak areas.
    @Column(length = 3000)
    private String feedbackSummary;

    @Column(length = 3000)
    private String correctness;

    @Column(length = 5000)
    private String bugs;

    @Column(length = 5000)
    private String edgeCases;

    private String timeComplexity;
    private String spaceComplexity;

    @Column(length = 5000)
    private String hint;

    @Column(length = 5000)
    private String suggestedImprovement;

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
    public String getFeedbackSummary() { return feedbackSummary; }
    public String getCorrectness() { return correctness; }
    public String getBugs() { return bugs; }
    public String getEdgeCases() { return edgeCases; }
    public String getTimeComplexity() { return timeComplexity; }
    public String getSpaceComplexity() { return spaceComplexity; }
    public String getHint() { return hint; }
    public String getSuggestedImprovement() { return suggestedImprovement; }
    public Instant getCreatedAt() { return createdAt; }
    public Problem getProblem() { return problem; }

    public void setSubmittedCode(String submittedCode) { this.submittedCode = submittedCode; }
    public void setLanguage(String language) { this.language = language; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
    public void setFeedbackSummary(String feedbackSummary) { this.feedbackSummary = feedbackSummary; }
    public void setCorrectness(String correctness) { this.correctness = correctness; }
    public void setBugs(String bugs) { this.bugs = bugs; }
    public void setEdgeCases(String edgeCases) { this.edgeCases = edgeCases; }
    public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }
    public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }
    public void setHint(String hint) { this.hint = hint; }
    public void setSuggestedImprovement(String suggestedImprovement) { this.suggestedImprovement = suggestedImprovement; }
    public void setProblem(Problem problem) { this.problem = problem; }
}