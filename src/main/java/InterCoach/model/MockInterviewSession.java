package InterCoach.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "mock_interview_session")
public class MockInterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each interview belongs to the user who started it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // The selected problem remains attached to the interview session.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewStatus status;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant completedAt;

    @PrePersist
    void onCreate() {
        startedAt = Instant.now();

        if (status == null) {
            status = InterviewStatus.IN_PROGRESS;
        }
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Problem getProblem() {
        return problem;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}