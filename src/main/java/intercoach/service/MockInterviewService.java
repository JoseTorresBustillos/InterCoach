package intercoach.service;

import intercoach.dto.MockInterviewRequest;
import intercoach.dto.MockInterviewResponse;
import intercoach.exception.ResourceNotFoundException;
import intercoach.model.AppUser;
import intercoach.model.InterviewStatus;
import intercoach.model.MockInterviewSession;
import intercoach.model.Problem;
import intercoach.repository.AppUserRepository;
import intercoach.repository.MockInterviewRepository;
import intercoach.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MockInterviewService {

    private final MockInterviewRepository mockInterviewRepository;
    private final AppUserRepository appUserRepository;
    private final ProblemRepository problemRepository;

    public MockInterviewService(
            MockInterviewRepository mockInterviewRepository,
            AppUserRepository appUserRepository,
            ProblemRepository problemRepository
    ) {
        this.mockInterviewRepository = mockInterviewRepository;
        this.appUserRepository = appUserRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional
    public MockInterviewResponse startInterview(
            Long userId,
            MockInterviewRequest request
    ) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        List<Problem> matchingProblems =
                problemRepository.findByDifficulty(request.getDifficulty());

        if (matchingProblems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No problems found for difficulty: "
                            + request.getDifficulty()
            );
        }

        // Random selection makes repeated interview sessions less predictable.
        int selectedIndex = ThreadLocalRandom.current()
                .nextInt(matchingProblems.size());

        Problem selectedProblem = matchingProblems.get(selectedIndex);

        MockInterviewSession session = new MockInterviewSession();
        session.setUser(user);
        session.setProblem(selectedProblem);
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setDurationMinutes(request.getDurationMinutes());

        return toResponse(mockInterviewRepository.save(session));
    }

    @Transactional(readOnly = true)
    public MockInterviewResponse getInterview(Long sessionId) {
        MockInterviewSession session = findSession(sessionId);

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<MockInterviewResponse> getInterviewsForUser(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        return mockInterviewRepository
                .findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MockInterviewResponse completeInterview(Long sessionId) {
        MockInterviewSession session = findSession(sessionId);

        ensureInterviewIsActive(session);

        session.setStatus(InterviewStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        return toResponse(mockInterviewRepository.save(session));
    }

    @Transactional
    public MockInterviewResponse abandonInterview(Long sessionId) {
        MockInterviewSession session = findSession(sessionId);

        ensureInterviewIsActive(session);

        session.setStatus(InterviewStatus.ABANDONED);
        session.setCompletedAt(Instant.now());

        return toResponse(mockInterviewRepository.save(session));
    }

    private MockInterviewSession findSession(Long sessionId) {
        return mockInterviewRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mock interview not found with id: "
                                        + sessionId
                        )
                );
    }

    private void ensureInterviewIsActive(MockInterviewSession session) {
        if (session.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only an active interview can be updated."
            );
        }
    }

    private MockInterviewResponse toResponse(
            MockInterviewSession session
    ) {
        Problem problem = session.getProblem();

        return new MockInterviewResponse(
                session.getId(),
                session.getUser().getId(),
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getCategory(),
                problem.getDescription(),
                problem.getConstraints(),
                problem.getExamples(),
                problem.getStarterCode(),
                session.getStatus(),
                session.getDurationMinutes(),
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }
}
