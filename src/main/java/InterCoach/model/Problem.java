package InterCoach.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private String category;

    @Column(length = 2000)
    private String tags;

    @Column(length = 5000)
    private String examples;

    @Column(length = 3000)
    private String constraints;

    @Column(length = 5000)
    private String starterCode;

    @Column(length = 5000)
    private String solutionExplanation;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getCategory() { return category; }
    public String getTags() { return tags; }
    public String getExamples() { return examples; }
    public String getConstraints() { return constraints; }
    public String getStarterCode() { return starterCode; }
    public String getSolutionExplanation() { return solutionExplanation; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setCategory(String category) { this.category = category; }
    public void setTags(String tags) { this.tags = tags; }
    public void setExamples(String examples) { this.examples = examples; }
    public void setConstraints(String constraints) { this.constraints = constraints; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }
    public void setSolutionExplanation(String solutionExplanation) { this.solutionExplanation = solutionExplanation; }
}