const storageKey = "intercoach.session";
const apiBase = window.location.protocol === "file:"
    ? "http://localhost:8080"
    : "";

const state = {
    mode: "login",
    session: loadSession(),
    dashboard: null,
    analytics: null,
    recommendations: [],
    executionStatus: null,
    adminUsers: [],
    adminUsersAvailable: false,
    problems: [],
    mockInterviews: [],
    selectedProblemId: null,
    selectedProblem: null,
    selectedInterviewId: null,
    selectedInterview: null,
    authoringMode: "create",
    authoringProblemId: null,
    testCases: [],
    testCasesError: null,
    runResult: null,
    reviewResult: null,
    problemDrafts: {},
    runningCode: false,
    submittingReview: false,
    startingInterview: false,
    updatingInterview: false,
    savingProblem: false,
    deletingProblem: false,
    savingTestCase: false,
    creatingAdminUser: false
};

const elements = {
    authForm: document.querySelector("#auth-form"),
    authButtons: document.querySelectorAll("[data-auth-mode]"),
    authActionLabel: document.querySelector("#auth-action-label"),
    emailField: document.querySelector("#email-field"),
    username: document.querySelector("#username"),
    email: document.querySelector("#email"),
    password: document.querySelector("#password"),
    sessionDot: document.querySelector("#session-dot"),
    sessionLabel: document.querySelector("#session-label"),
    signOut: document.querySelector("#sign-out"),
    refreshData: document.querySelector("#refresh-data"),
    scrollProgress: document.querySelector("#scroll-progress"),
    topbar: document.querySelector(".topbar"),
    navLinks: document.querySelectorAll(".nav-list a"),
    connectionCopy: document.querySelector("#connection-copy"),
    welcomeHeading: document.querySelector("#welcome-heading"),
    metricSubmissions: document.querySelector("#metric-submissions"),
    metricReviewed: document.querySelector("#metric-reviewed"),
    metricWeakRate: document.querySelector("#metric-weak-rate"),
    metricInterviews: document.querySelector("#metric-interviews"),
    reviewRate: document.querySelector("#review-rate"),
    strongestCategory: document.querySelector("#strongest-category"),
    weakestCategory: document.querySelector("#weakest-category"),
    trendChart: document.querySelector("#trend-chart"),
    categoryBreakdown: document.querySelector("#category-breakdown"),
    recommendationList: document.querySelector("#recommendation-list"),
    problemCount: document.querySelector("#problem-count"),
    problemList: document.querySelector("#problem-list"),
    problemStatus: document.querySelector("#problem-status"),
    problemDetail: document.querySelector("#problem-detail"),
    authoringSection: document.querySelector("#authoring"),
    authoringNav: document.querySelector("#authoring-nav"),
    codeRunForm: document.querySelector("#code-run-form"),
    codeEditor: document.querySelector("#code-editor"),
    runCode: document.querySelector("#run-code"),
    submitReview: document.querySelector("#submit-review"),
    codeRunStatus: document.querySelector("#code-run-status"),
    runResult: document.querySelector("#run-result"),
    reviewResult: document.querySelector("#review-result"),
    problemAuthorForm: document.querySelector("#problem-author-form"),
    problemAuthorMode: document.querySelector("#problem-author-mode"),
    problemAuthorTitle: document.querySelector("#problem-author-title"),
    problemAuthorDifficulty: document.querySelector("#problem-author-difficulty"),
    problemAuthorCategory: document.querySelector("#problem-author-category"),
    problemAuthorTags: document.querySelector("#problem-author-tags"),
    problemAuthorDescription: document.querySelector("#problem-author-description"),
    problemAuthorExamples: document.querySelector("#problem-author-examples"),
    problemAuthorConstraints: document.querySelector("#problem-author-constraints"),
    problemAuthorStarterCode: document.querySelector("#problem-author-starter-code"),
    problemAuthorSolution: document.querySelector("#problem-author-solution"),
    saveProblem: document.querySelector("#save-problem"),
    editSelectedProblem: document.querySelector("#edit-selected-problem"),
    resetProblemAuthor: document.querySelector("#reset-problem-author"),
    deleteProblem: document.querySelector("#delete-problem"),
    problemAuthorMessage: document.querySelector("#problem-author-message"),
    testCaseForm: document.querySelector("#test-case-form"),
    testCaseInput: document.querySelector("#test-case-input"),
    testCaseExpected: document.querySelector("#test-case-expected"),
    testCaseHidden: document.querySelector("#test-case-hidden"),
    addTestCase: document.querySelector("#add-test-case"),
    testCaseMessage: document.querySelector("#test-case-message"),
    authorTestCount: document.querySelector("#author-test-count"),
    authorTestList: document.querySelector("#author-test-list"),
    interviewCount: document.querySelector("#interview-count"),
    interviewForm: document.querySelector("#interview-form"),
    interviewDifficulty: document.querySelector("#interview-difficulty"),
    interviewDuration: document.querySelector("#interview-duration"),
    startInterview: document.querySelector("#start-interview"),
    interviewMessage: document.querySelector("#interview-message"),
    interviewList: document.querySelector("#interview-list"),
    interviewStatus: document.querySelector("#interview-status"),
    interviewDetail: document.querySelector("#interview-detail"),
    assistantForm: document.querySelector("#assistant-form"),
    assistantQuestion: document.querySelector("#assistant-question"),
    assistantAnswer: document.querySelector("#assistant-answer"),
    profileForm: document.querySelector("#profile-form"),
    profileUsername: document.querySelector("#profile-username"),
    profileEmail: document.querySelector("#profile-email"),
    profileMessage: document.querySelector("#profile-message"),
    passwordForm: document.querySelector("#password-form"),
    currentPassword: document.querySelector("#current-password"),
    newPassword: document.querySelector("#new-password"),
    passwordMessage: document.querySelector("#password-message"),
    adminUsersSection: document.querySelector("#admin-users"),
    adminUsersNav: document.querySelector("#admin-users-nav"),
    adminUserCount: document.querySelector("#admin-user-count"),
    adminUserList: document.querySelector("#admin-user-list"),
    adminUserForm: document.querySelector("#admin-user-form"),
    adminUsername: document.querySelector("#admin-username"),
    adminEmail: document.querySelector("#admin-email"),
    adminPassword: document.querySelector("#admin-password"),
    createAdminUser: document.querySelector("#create-admin-user"),
    adminUserMessage: document.querySelector("#admin-user-message"),
    operationsSection: document.querySelector("#operations"),
    operationsNav: document.querySelector("#operations-nav"),
    operationsMode: document.querySelector("#operations-mode"),
    operationsGrid: document.querySelector("#operations-grid")
};

elements.authButtons.forEach((button) => {
    button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});
elements.authForm.addEventListener("submit", handleAuth);
elements.signOut.addEventListener("click", signOut);
elements.refreshData.addEventListener("click", loadWorkspace);
elements.problemList.addEventListener("click", handleProblemSelection);
elements.recommendationList.addEventListener("click", handleRecommendationOpen);
elements.codeRunForm.addEventListener("submit", handleCodeRun);
elements.submitReview.addEventListener("click", handleReviewSubmission);
elements.codeEditor.addEventListener("input", handleCodeDraft);
elements.problemAuthorForm.addEventListener("submit", handleProblemSave);
elements.editSelectedProblem.addEventListener("click", beginEditingSelectedProblem);
elements.resetProblemAuthor.addEventListener("click", () => resetProblemAuthoring());
elements.deleteProblem.addEventListener("click", handleProblemDelete);
elements.testCaseForm.addEventListener("submit", handleTestCaseSave);
elements.interviewForm.addEventListener("submit", handleInterviewStart);
elements.interviewList.addEventListener("click", handleInterviewSelection);
elements.interviewDetail.addEventListener("click", handleInterviewAction);
elements.assistantForm.addEventListener("submit", handleAssistantQuestion);
elements.profileForm.addEventListener("submit", handleProfileUpdate);
elements.passwordForm.addEventListener("submit", handlePasswordChange);
elements.adminUserForm.addEventListener("submit", handleAdminUserCreate);

setupScrollInteractions();
renderSession();
renderWorkspace();

if (state.session) {
    loadWorkspace();
}

function loadSession() {
    try {
        return JSON.parse(localStorage.getItem(storageKey));
    } catch (error) {
        return null;
    }
}

function isAdminSession() {
    return state.session?.user?.role?.toUpperCase() === "ADMIN";
}

function saveSession(session) {
    state.session = session;
    localStorage.setItem(storageKey, JSON.stringify(session));
    renderSession();
}

function setAuthMode(mode) {
    state.mode = mode;
    elements.authButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.authMode === mode);
    });
    elements.emailField.classList.toggle("hidden", mode !== "register");
    elements.email.required = mode === "register";
    elements.authActionLabel.textContent = mode === "register"
        ? "Register"
        : "Login";
}

async function handleAuth(event) {
    event.preventDefault();
    setConnection("Connecting");

    const isRegistering = state.mode === "register";
    const payload = isRegistering
        ? {
            username: elements.username.value.trim(),
            email: elements.email.value.trim(),
            password: elements.password.value
        }
        : {
            usernameOrEmail: elements.username.value.trim(),
            password: elements.password.value
        };

    try {
        const response = await api(
            isRegistering ? "/api/auth/register" : "/api/auth/login",
            {
                method: "POST",
                body: JSON.stringify(payload)
            }
        );
        saveSession({
            token: response.token,
            user: response.user
        });
        elements.password.value = "";
        setConnection("Connected");
        await loadWorkspace();
    } catch (error) {
        setConnection(error.message);
    }
}

function signOut() {
    localStorage.removeItem(storageKey);
    state.session = null;
    state.dashboard = null;
    state.analytics = null;
    state.recommendations = [];
    state.executionStatus = null;
    state.adminUsers = [];
    state.adminUsersAvailable = false;
    state.problems = [];
    state.mockInterviews = [];
    state.selectedProblemId = null;
    state.selectedProblem = null;
    state.selectedInterviewId = null;
    state.selectedInterview = null;
    state.authoringMode = "create";
    state.authoringProblemId = null;
    state.testCases = [];
    state.testCasesError = null;
    state.runResult = null;
    state.reviewResult = null;
    state.problemDrafts = {};
    state.runningCode = false;
    state.submittingReview = false;
    state.startingInterview = false;
    state.updatingInterview = false;
    state.savingProblem = false;
    state.deletingProblem = false;
    state.savingTestCase = false;
    state.creatingAdminUser = false;
    elements.assistantAnswer.innerHTML = "";
    fillProblemAuthorForm(emptyProblemAuthorFields());
    clearTestCaseForm();
    clearAdminUserForm();
    clearFormStatus(elements.problemAuthorMessage);
    clearFormStatus(elements.testCaseMessage);
    clearFormStatus(elements.interviewMessage);
    clearFormStatus(elements.adminUserMessage);
    clearFormStatus(elements.profileMessage);
    clearFormStatus(elements.passwordMessage);
    renderSession();
    renderWorkspace();
    setConnection("Signed out");
}

async function loadWorkspace() {
    if (!state.session?.user?.id) {
        renderWorkspace();
        return;
    }

    setConnection("Loading");
    const userId = state.session.user.id;

    try {
        const [
            dashboard,
            analytics,
            recommendations,
            executionStatus,
            adminUsers,
            problems,
            mockInterviews
        ] = await Promise.all([
            api(`/api/users/${userId}/dashboard`),
            api(`/api/users/${userId}/analytics`),
            api(`/api/users/${userId}/recommendations`),
            loadExecutionStatus(),
            loadAdminUsers(),
            api("/api/problems"),
            api(`/api/users/${userId}/mock-interviews`)
        ]);

        state.dashboard = dashboard;
        state.analytics = analytics;
        state.recommendations = recommendations;
        state.executionStatus = executionStatus;
        state.adminUsersAvailable = Array.isArray(adminUsers);
        state.adminUsers = state.adminUsersAvailable ? adminUsers : [];
        state.problems = Array.isArray(problems) ? problems : [];
        state.mockInterviews = Array.isArray(mockInterviews) ? mockInterviews : [];
        syncSelectedProblem();
        syncSelectedInterview();
        renderWorkspace();
        await loadSelectedProblemDetails();
        setConnection("Connected");
    } catch (error) {
        setConnection(error.message);
        renderError(error.message);
    }
}

async function refreshWorkspaceSummaries() {
    const userId = state.session?.user?.id;

    if (!userId) {
        return;
    }

    const [dashboard, analytics, recommendations, mockInterviews] = await Promise.all([
        api(`/api/users/${userId}/dashboard`),
        api(`/api/users/${userId}/analytics`),
        api(`/api/users/${userId}/recommendations`),
        api(`/api/users/${userId}/mock-interviews`)
    ]);

    state.dashboard = dashboard;
    state.analytics = analytics;
    state.recommendations = recommendations;
    state.mockInterviews = Array.isArray(mockInterviews) ? mockInterviews : [];
    syncSelectedInterview();
    renderWorkspace();
}

async function loadSelectedProblemDetails() {
    if (!state.session || !state.selectedProblem) {
        state.testCases = [];
        state.testCasesError = null;
        renderProblemWorkspace();
        renderAuthoringWorkspace();
        return;
    }

    state.testCasesError = null;

    try {
        const testCases = await api(
            `/api/problems/${state.selectedProblem.id}/test-cases`
        );
        state.testCases = Array.isArray(testCases) ? testCases : [];
    } catch (error) {
        state.testCases = [];
        state.testCasesError = error.message;
    }

    renderProblemWorkspace();
    renderAuthoringWorkspace();
}

async function loadExecutionStatus() {
    try {
        return await api("/api/admin/execution/status");
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            return null;
        }

        throw error;
    }
}

async function loadAdminUsers() {
    try {
        return await api("/api/users");
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            return null;
        }

        throw error;
    }
}

async function handleProblemSelection(event) {
    const button = event.target.closest("[data-problem-id]");

    if (!button) {
        return;
    }

    await selectProblem(button.dataset.problemId);
}

async function handleRecommendationOpen(event) {
    const button = event.target.closest("[data-open-problem-id]");

    if (!button) {
        return;
    }

    await selectProblem(button.dataset.openProblemId);
    document.querySelector("#problems")?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function handleCodeDraft() {
    if (!state.selectedProblem) {
        return;
    }

    state.problemDrafts[state.selectedProblem.id] = elements.codeEditor.value;
}

async function handleCodeRun(event) {
    event.preventDefault();

    if (!state.session) {
        renderRunError("Sign in before running code.");
        return;
    }

    if (!state.selectedProblem) {
        renderRunError("Choose a problem first.");
        return;
    }

    const submittedCode = elements.codeEditor.value;

    if (!submittedCode.trim()) {
        renderRunError("Add Java code before running visible tests.");
        return;
    }

    state.problemDrafts[state.selectedProblem.id] = submittedCode;
    state.runningCode = true;
    state.runResult = null;
    renderProblemWorkspace();
    setConnection("Running code");

    try {
        state.runResult = await api(
            `/api/problems/${state.selectedProblem.id}/run`,
            {
                method: "POST",
                body: JSON.stringify({
                    language: "Java",
                    submittedCode
                })
            }
        );
        setConnection(state.runResult.allPassed ? "All tests passed" : "Run completed");
    } catch (error) {
        state.runResult = {error: error.message};
        setConnection(error.message);
    } finally {
        state.runningCode = false;
        renderProblemWorkspace();
    }
}

async function handleReviewSubmission() {
    if (!state.session) {
        renderReviewError("Sign in before submitting for review.");
        return;
    }

    if (!state.selectedProblem) {
        renderReviewError("Choose a problem first.");
        return;
    }

    const submittedCode = elements.codeEditor.value;

    if (!submittedCode.trim()) {
        renderReviewError("Add Java code before asking for AI review.");
        return;
    }

    state.problemDrafts[state.selectedProblem.id] = submittedCode;
    state.submittingReview = true;
    state.reviewResult = null;
    renderProblemWorkspace();
    setConnection("Submitting review");

    try {
        state.reviewResult = await api(
            `/api/problems/${state.selectedProblem.id}/submissions`,
            {
                method: "POST",
                body: JSON.stringify({
                    userId: state.session.user.id,
                    language: "Java",
                    submittedCode
                })
            }
        );

        try {
            await refreshWorkspaceSummaries();
            setConnection(statusLabel(state.reviewResult.status));
        } catch (refreshError) {
            setConnection("Review saved; refresh failed");
        }
    } catch (error) {
        state.reviewResult = {error: error.message};
        setConnection(error.message);
    } finally {
        state.submittingReview = false;
        renderProblemWorkspace();
    }
}

async function handleProblemSave(event) {
    event.preventDefault();

    if (!isAdminSession()) {
        renderFormStatus(elements.problemAuthorMessage, "Administrator access required.", true);
        return;
    }

    if (state.savingProblem) {
        return;
    }

    const payload = problemPayloadFromForm();

    if (!payload) {
        return;
    }

    const isEditing = state.authoringMode === "edit" && state.authoringProblemId != null;
    state.savingProblem = true;
    clearFormStatus(elements.problemAuthorMessage);
    renderAuthoringWorkspace();
    setConnection(isEditing ? "Saving problem" : "Creating problem");

    try {
        const problemPath = isEditing
            ? `/api/problems/${state.authoringProblemId}`
            : "/api/problems";
        const problemMethod = isEditing ? "PUT" : "POST";
        const problem = await api(problemPath, {
            method: problemMethod,
            body: JSON.stringify(payload)
        });

        if (state.selectedProblem) {
            state.problemDrafts[state.selectedProblem.id] = elements.codeEditor.value;
        }

        state.problems = mergeProblem(problem, state.problems);
        state.selectedProblemId = problem.id;
        state.selectedProblem = problem;
        state.authoringMode = "edit";
        state.authoringProblemId = problem.id;
        fillProblemAuthorForm(problem);
        ensureProblemDraft(problem);
        renderFormStatus(
            elements.problemAuthorMessage,
            isEditing ? "Problem saved." : "Problem created."
        );
        renderWorkspace();
        await loadSelectedProblemDetails();
        setConnection(isEditing ? "Problem saved" : "Problem created");
    } catch (error) {
        renderFormStatus(elements.problemAuthorMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.savingProblem = false;
        renderAuthoringWorkspace();
    }
}

function beginEditingSelectedProblem() {
    if (!isAdminSession()) {
        renderFormStatus(elements.problemAuthorMessage, "Administrator access required.", true);
        return;
    }

    if (!state.selectedProblem) {
        renderFormStatus(elements.problemAuthorMessage, "Choose a problem first.", true);
        return;
    }

    state.authoringMode = "edit";
    state.authoringProblemId = state.selectedProblem.id;
    fillProblemAuthorForm(state.selectedProblem);
    clearFormStatus(elements.problemAuthorMessage);
    renderAuthoringWorkspace();
}

function resetProblemAuthoring(clearStatus = true) {
    state.authoringMode = "create";
    state.authoringProblemId = null;
    fillProblemAuthorForm(emptyProblemAuthorFields());

    if (clearStatus) {
        clearFormStatus(elements.problemAuthorMessage);
    }

    renderAuthoringWorkspace();
}

async function handleProblemDelete() {
    if (!isAdminSession()) {
        renderFormStatus(elements.problemAuthorMessage, "Administrator access required.", true);
        return;
    }

    if (state.deletingProblem) {
        return;
    }

    if (state.authoringMode !== "edit" || state.authoringProblemId == null) {
        renderFormStatus(elements.problemAuthorMessage, "Load a problem before deleting.", true);
        return;
    }

    const shouldDelete = window.confirm("Delete this problem?");

    if (!shouldDelete) {
        return;
    }

    const deletedProblemId = state.authoringProblemId;
    state.deletingProblem = true;
    clearFormStatus(elements.problemAuthorMessage);
    renderAuthoringWorkspace();
    setConnection("Deleting problem");

    try {
        await api(`/api/problems/${deletedProblemId}`, {method: "DELETE"});
        state.problems = state.problems.filter((problem) => {
            return String(problem.id) !== String(deletedProblemId);
        });
        delete state.problemDrafts[deletedProblemId];

        if (String(state.selectedProblemId) === String(deletedProblemId)) {
            state.selectedProblemId = null;
        }

        syncSelectedProblem();
        resetProblemAuthoring(false);
        renderFormStatus(elements.problemAuthorMessage, "Problem deleted.");
        renderWorkspace();
        await loadSelectedProblemDetails();
        setConnection("Problem deleted");
    } catch (error) {
        renderFormStatus(elements.problemAuthorMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.deletingProblem = false;
        renderAuthoringWorkspace();
    }
}

async function handleTestCaseSave(event) {
    event.preventDefault();

    if (!isAdminSession()) {
        renderFormStatus(elements.testCaseMessage, "Administrator access required.", true);
        return;
    }

    if (!state.selectedProblem) {
        renderFormStatus(elements.testCaseMessage, "Choose a problem first.", true);
        return;
    }

    if (state.savingTestCase) {
        return;
    }

    const input = elements.testCaseInput.value;
    const expectedOutput = elements.testCaseExpected.value;

    if (!input.trim() || !expectedOutput.trim()) {
        renderFormStatus(elements.testCaseMessage, "Input and expected output are required.", true);
        return;
    }

    state.savingTestCase = true;
    clearFormStatus(elements.testCaseMessage);
    renderAuthoringWorkspace();
    setConnection("Adding test case");

    try {
        await api(`/api/problems/${state.selectedProblem.id}/test-cases`, {
            method: "POST",
            body: JSON.stringify({
                input,
                expectedOutput,
                hidden: elements.testCaseHidden.checked
            })
        });
        clearTestCaseForm();
        await loadSelectedProblemDetails();
        renderFormStatus(elements.testCaseMessage, "Test case added.");
        setConnection("Test case added");
    } catch (error) {
        renderFormStatus(elements.testCaseMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.savingTestCase = false;
        renderAuthoringWorkspace();
    }
}

async function handleInterviewStart(event) {
    event.preventDefault();

    if (!state.session) {
        renderFormStatus(elements.interviewMessage, "Sign in first.", true);
        return;
    }

    if (state.startingInterview) {
        return;
    }

    const durationMinutes = Number(elements.interviewDuration.value);

    if (!Number.isFinite(durationMinutes) || durationMinutes < 10 || durationMinutes > 180) {
        renderFormStatus(
            elements.interviewMessage,
            "Choose a duration from 10 to 180 minutes.",
            true
        );
        return;
    }

    state.startingInterview = true;
    clearFormStatus(elements.interviewMessage);
    renderInterviewWorkspace();
    setConnection("Starting interview");

    try {
        const interview = await api(
            `/api/users/${state.session.user.id}/mock-interviews`,
            {
                method: "POST",
                body: JSON.stringify({
                    difficulty: elements.interviewDifficulty.value,
                    durationMinutes
                })
            }
        );
        state.mockInterviews = mergeInterview(interview, state.mockInterviews);
        state.selectedInterviewId = interview.sessionId;
        state.selectedInterview = interview;
        renderFormStatus(elements.interviewMessage, "Interview started.");

        try {
            await refreshWorkspaceSummaries();
            setConnection("Interview started");
        } catch (refreshError) {
            setConnection("Interview started; refresh failed");
        }
    } catch (error) {
        renderFormStatus(elements.interviewMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.startingInterview = false;
        renderInterviewWorkspace();
    }
}

function handleInterviewSelection(event) {
    const button = event.target.closest("[data-interview-id]");

    if (!button) {
        return;
    }

    selectInterview(button.dataset.interviewId);
}

async function handleInterviewAction(event) {
    const openProblemButton = event.target.closest("[data-interview-problem-id]");
    const actionButton = event.target.closest("[data-interview-action]");

    if (openProblemButton) {
        await selectProblem(openProblemButton.dataset.interviewProblemId);
        document.querySelector("#problems")?.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
        return;
    }

    if (!actionButton || !state.selectedInterview) {
        return;
    }

    await updateInterviewStatus(actionButton.dataset.interviewAction);
}

function selectInterview(interviewId) {
    const interview = state.mockInterviews.find((candidate) => {
        return String(candidate.sessionId) === String(interviewId);
    });

    if (!interview) {
        return;
    }

    state.selectedInterviewId = interview.sessionId;
    state.selectedInterview = interview;
    renderInterviewWorkspace();
}

function mergeInterview(interview, interviews) {
    if (!interview) {
        return interviews || [];
    }

    const remainingInterviews = (interviews || []).filter((candidate) => {
        return String(candidate.sessionId) !== String(interview.sessionId);
    });

    return [interview, ...remainingInterviews];
}

async function updateInterviewStatus(action) {
    if (!["complete", "abandon"].includes(action)) {
        return;
    }

    if (state.updatingInterview || !state.selectedInterview) {
        return;
    }

    state.updatingInterview = true;
    renderInterviewWorkspace();
    setConnection(`${statusLabel(action)} interview`);

    try {
        const interview = await api(
            `/api/mock-interviews/${state.selectedInterview.sessionId}/${action}`,
            {method: "PATCH"}
        );
        state.mockInterviews = mergeInterview(interview, state.mockInterviews);
        state.selectedInterviewId = interview.sessionId;
        state.selectedInterview = interview;

        try {
            await refreshWorkspaceSummaries();
            setConnection(statusLabel(interview.status));
        } catch (refreshError) {
            setConnection("Interview updated; refresh failed");
        }
    } catch (error) {
        renderFormStatus(elements.interviewMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.updatingInterview = false;
        renderInterviewWorkspace();
    }
}

async function handleAssistantQuestion(event) {
    event.preventDefault();

    if (!state.session) {
        renderAssistantError("Sign in before asking the assistant.");
        return;
    }

    const question = elements.assistantQuestion.value.trim();

    if (!question) {
        renderAssistantError("Add a question first.");
        return;
    }

    elements.assistantAnswer.innerHTML = "<p class=\"empty-copy\">Thinking...</p>";

    try {
        const response = await api("/api/study-assistant/ask", {
            method: "POST",
            body: JSON.stringify({question})
        });
        renderAssistantAnswer(response);
    } catch (error) {
        renderAssistantError(error.message);
    }
}

async function handleProfileUpdate(event) {
    event.preventDefault();

    if (!state.session) {
        renderFormStatus(elements.profileMessage, "Sign in first.", true);
        return;
    }

    try {
        const response = await api("/api/users/me", {
            method: "PATCH",
            body: JSON.stringify({
                username: elements.profileUsername.value.trim(),
                email: elements.profileEmail.value.trim()
            })
        });

        saveSession({
            token: response.token,
            user: response.user
        });
        renderFormStatus(elements.profileMessage, "Profile saved.");
        setConnection("Profile saved");
        await loadWorkspace();
    } catch (error) {
        renderFormStatus(elements.profileMessage, error.message, true);
        setConnection(error.message);
    }
}

async function handlePasswordChange(event) {
    event.preventDefault();

    if (!state.session) {
        renderFormStatus(elements.passwordMessage, "Sign in first.", true);
        return;
    }

    try {
        await api("/api/users/me/password", {
            method: "PATCH",
            body: JSON.stringify({
                currentPassword: elements.currentPassword.value,
                newPassword: elements.newPassword.value
            })
        });

        elements.currentPassword.value = "";
        elements.newPassword.value = "";
        renderFormStatus(elements.passwordMessage, "Password updated.");
        setConnection("Password updated");
    } catch (error) {
        renderFormStatus(elements.passwordMessage, error.message, true);
        setConnection(error.message);
    }
}

async function handleAdminUserCreate(event) {
    event.preventDefault();

    if (!state.adminUsersAvailable) {
        renderFormStatus(elements.adminUserMessage, "Admin access required.", true);
        return;
    }

    if (state.creatingAdminUser) {
        return;
    }

    const username = elements.adminUsername.value.trim();
    const email = elements.adminEmail.value.trim();
    const password = elements.adminPassword.value;

    if (!username || !email || password.length < 8) {
        renderFormStatus(
            elements.adminUserMessage,
            "Username, email, and an 8+ character password are required.",
            true
        );
        return;
    }

    state.creatingAdminUser = true;
    clearFormStatus(elements.adminUserMessage);
    renderAdminUsers();
    setConnection("Creating user");

    try {
        const user = await api("/api/users", {
            method: "POST",
            body: JSON.stringify({
                username,
                email,
                password
            })
        });
        state.adminUsers = mergeAdminUser(user, state.adminUsers);
        clearAdminUserForm();
        renderFormStatus(elements.adminUserMessage, "User created.");
        setConnection("User created");
    } catch (error) {
        renderFormStatus(elements.adminUserMessage, error.message, true);
        setConnection(error.message);
    } finally {
        state.creatingAdminUser = false;
        renderAdminUsers();
    }
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    if (state.session?.token) {
        headers.set("Authorization", `Bearer ${state.session.token}`);
    }

    const response = await fetch(`${apiBase}${path}`, {
        ...options,
        headers
    });

    const text = await response.text();

    if (!response.ok) {
        const errorBody = parseJson(text) || {};
        const error = new Error(
            errorBody.message || `Request failed with ${response.status}`
        );
        error.status = response.status;
        throw error;
    }

    if (!text) {
        return null;
    }

    const body = parseJson(text);

    if (!body) {
        throw new Error("Response was not valid JSON.");
    }

    return body;
}

function parseJson(text) {
    try {
        return JSON.parse(text);
    } catch (error) {
        return null;
    }
}

function renderSession() {
    const username = state.session?.user?.username;
    elements.sessionDot.classList.toggle("active", Boolean(username));
    elements.sessionLabel.textContent = username
        ? `Signed in as ${username}`
        : "Signed out";
    elements.signOut.classList.toggle("hidden", !username);
    elements.welcomeHeading.textContent = username
        ? `${username}'s interview workspace`
        : "Coding interview dashboard";
    renderAccount();
}

function renderWorkspace() {
    const dashboard = state.dashboard || {};
    const analytics = state.analytics || {};

    elements.metricSubmissions.textContent = number(dashboard.totalSubmissions);
    elements.metricReviewed.textContent = number(dashboard.reviewedSubmissions);
    elements.metricWeakRate.textContent = percent(analytics.weakSubmissionRate);
    elements.metricInterviews.textContent = number(dashboard.mockInterviewsStarted);
    elements.reviewRate.textContent = `${percent(analytics.reviewRate)} reviewed`;
    elements.strongestCategory.textContent = analytics.strongestCategory || "-";
    elements.weakestCategory.textContent = analytics.weakestCategory || "-";

    renderTrend(analytics.activityTrend || []);
    renderCategoryBreakdown(analytics.categoryBreakdown || []);
    renderProblemWorkspace();
    renderAuthoringWorkspace();
    renderInterviewWorkspace();
    renderRecommendations(state.recommendations || []);
    renderAdminUsers();
    renderOperations();
}

function renderAccount() {
    const user = state.session?.user;
    const disabled = !user;

    elements.profileUsername.disabled = disabled;
    elements.profileEmail.disabled = disabled;
    elements.currentPassword.disabled = disabled;
    elements.newPassword.disabled = disabled;
    elements.profileForm.querySelector("button").disabled = disabled;
    elements.passwordForm.querySelector("button").disabled = disabled;

    if (!user) {
        elements.profileUsername.value = "";
        elements.profileEmail.value = "";
        elements.currentPassword.value = "";
        elements.newPassword.value = "";
        renderFormStatus(elements.profileMessage, "Signed out.");
        clearFormStatus(elements.passwordMessage);
        return;
    }

    elements.profileUsername.value = user.username || "";
    elements.profileEmail.value = user.email || "";

    if (elements.profileMessage.textContent === "Signed out.") {
        clearFormStatus(elements.profileMessage);
    }
}

function renderTrend(days) {
    elements.trendChart.innerHTML = "";

    if (!state.session) {
        renderEmpty(elements.trendChart, "Sign in to load activity.");
        return;
    }

    if (!days.length) {
        renderEmpty(elements.trendChart, "No activity yet.");
        return;
    }

    const maxTotal = Math.max(...days.map((day) => {
        return (day.submissions || 0) + (day.mockInterviews || 0);
    }));

    days.slice(-7).forEach((day) => {
        const total = (day.submissions || 0) + (day.mockInterviews || 0);
        const width = maxTotal ? Math.max((total / maxTotal) * 100, 6) : 0;
        const row = document.createElement("div");
        row.className = "trend-row";
        row.innerHTML = `
            <div class="trend-meta">
                <strong>${escapeHtml(formatDate(day.date))}</strong>
                <span>${number(day.submissions)} submissions / ${number(day.mockInterviews)} interviews</span>
            </div>
            <div class="trend-track">
                <span class="trend-fill" style="width: ${width}%"></span>
            </div>
        `;
        elements.trendChart.append(row);
    });
}

function renderCategoryBreakdown(buckets) {
    elements.categoryBreakdown.innerHTML = "";

    if (!state.session) {
        renderEmpty(elements.categoryBreakdown, "Sign in to load categories.");
        return;
    }

    if (!buckets.length) {
        renderEmpty(elements.categoryBreakdown, "No category data yet.");
        return;
    }

    buckets.forEach((bucket) => {
        const row = document.createElement("div");
        row.className = "breakdown-row";
        row.innerHTML = `
            <div class="breakdown-meta">
                <strong>${escapeHtml(bucket.name)}</strong>
                <span>${percent(bucket.weakSubmissionRate)} weak</span>
            </div>
            <div class="breakdown-track">
                <span class="breakdown-fill" style="width: ${Math.min(bucket.weakSubmissionRate || 0, 100)}%"></span>
            </div>
            <div class="breakdown-meta">
                <span>${number(bucket.totalSubmissions)} total</span>
                <span>${number(bucket.reviewedSubmissions)} reviewed</span>
            </div>
        `;
        elements.categoryBreakdown.append(row);
    });
}

function syncSelectedProblem() {
    if (!state.session || !state.problems.length) {
        state.selectedProblemId = null;
        state.selectedProblem = null;
        state.testCases = [];
        state.testCasesError = null;
        state.runResult = null;
        state.reviewResult = null;
        return;
    }

    const previousProblemId = state.selectedProblemId;
    const selectedProblem = state.problems.find((problem) => {
        return String(problem.id) === String(state.selectedProblemId);
    });

    state.selectedProblem = selectedProblem || state.problems[0];
    state.selectedProblemId = state.selectedProblem.id;
    ensureProblemDraft(state.selectedProblem);

    if (String(previousProblemId) !== String(state.selectedProblemId)) {
        state.testCases = [];
        state.testCasesError = null;
        state.runResult = null;
        state.reviewResult = null;
    }
}

function syncSelectedInterview() {
    if (!state.session || !state.mockInterviews.length) {
        state.selectedInterviewId = null;
        state.selectedInterview = null;
        return;
    }

    const selectedInterview = state.mockInterviews.find((interview) => {
        return String(interview.sessionId) === String(state.selectedInterviewId);
    });
    const activeInterview = state.mockInterviews.find((interview) => {
        return interview.status === "IN_PROGRESS";
    });

    state.selectedInterview = selectedInterview || activeInterview || state.mockInterviews[0];
    state.selectedInterviewId = state.selectedInterview.sessionId;
}

async function selectProblem(problemId) {
    const problem = state.problems.find((candidate) => {
        return String(candidate.id) === String(problemId);
    });

    if (!problem) {
        return;
    }

    if (state.selectedProblem) {
        state.problemDrafts[state.selectedProblem.id] = elements.codeEditor.value;
    }

    state.selectedProblemId = problem.id;
    state.selectedProblem = problem;
    state.testCases = [];
    state.testCasesError = null;
    state.runResult = null;
    state.reviewResult = null;
    ensureProblemDraft(problem);
    renderProblemWorkspace();
    setConnection("Loading problem");
    await loadSelectedProblemDetails();
    setConnection(state.testCasesError || "Problem loaded");
}

function mergeProblem(problem, problems) {
    if (!problem) {
        return problems || [];
    }

    const remainingProblems = (problems || []).filter((candidate) => {
        return String(candidate.id) !== String(problem.id);
    });

    return [problem, ...remainingProblems];
}

function problemPayloadFromForm() {
    const title = elements.problemAuthorTitle.value.trim();
    const description = elements.problemAuthorDescription.value.trim();

    if (!title || !description) {
        renderFormStatus(
            elements.problemAuthorMessage,
            "Title and description are required.",
            true
        );
        return null;
    }

    return {
        title,
        description,
        difficulty: elements.problemAuthorDifficulty.value,
        category: nullableText(elements.problemAuthorCategory.value),
        tags: nullableText(elements.problemAuthorTags.value),
        examples: nullableText(elements.problemAuthorExamples.value),
        constraints: nullableText(elements.problemAuthorConstraints.value),
        starterCode: nullableText(elements.problemAuthorStarterCode.value),
        solutionExplanation: nullableText(elements.problemAuthorSolution.value)
    };
}

function nullableText(value) {
    const text = String(value || "").trim();
    return text ? text : null;
}

function fillProblemAuthorForm(problem) {
    elements.problemAuthorTitle.value = problem.title || "";
    elements.problemAuthorDifficulty.value = problem.difficulty || "EASY";
    elements.problemAuthorCategory.value = problem.category || "";
    elements.problemAuthorTags.value = problem.tags || "";
    elements.problemAuthorDescription.value = problem.description || "";
    elements.problemAuthorExamples.value = problem.examples || "";
    elements.problemAuthorConstraints.value = problem.constraints || "";
    elements.problemAuthorStarterCode.value = problem.starterCode || "";
    elements.problemAuthorSolution.value = problem.solutionExplanation || "";
}

function emptyProblemAuthorFields() {
    return {
        title: "",
        difficulty: "EASY",
        category: "",
        tags: "",
        description: "",
        examples: "",
        constraints: "",
        starterCode: "",
        solutionExplanation: ""
    };
}

function clearTestCaseForm() {
    elements.testCaseInput.value = "";
    elements.testCaseExpected.value = "";
    elements.testCaseHidden.checked = false;
}

function clearAdminUserForm() {
    elements.adminUsername.value = "";
    elements.adminEmail.value = "";
    elements.adminPassword.value = "";
}

function mergeAdminUser(user, users) {
    if (!user) {
        return users || [];
    }

    const remainingUsers = (users || []).filter((candidate) => {
        return String(candidate.id) !== String(user.id);
    });

    return [user, ...remainingUsers];
}

function renderProblemWorkspace() {
    renderProblemList();
    renderProblemDetail();
    renderRunResult();
    renderReviewResult();
}

function renderAuthoringWorkspace() {
    const visible = isAdminSession();
    elements.authoringSection.classList.toggle("hidden", !visible);
    elements.authoringNav.classList.toggle("hidden", !visible);

    renderProblemAuthorForm();
    renderAuthorTestCaseForm();
    renderAuthorTestCases();
}

function renderProblemAuthorForm() {
    const isEditing = state.authoringMode === "edit" && state.authoringProblemId != null;
    const disabled = !isAdminSession() || state.savingProblem || state.deletingProblem;
    const controls = elements.problemAuthorForm.querySelectorAll("input, select, textarea");

    elements.problemAuthorMode.textContent = isEditing
        ? `Editing #${escapeHtml(state.authoringProblemId)}`
        : "New problem";
    controls.forEach((control) => {
        control.disabled = disabled;
    });
    elements.saveProblem.disabled = disabled;
    elements.saveProblem.textContent = state.savingProblem
        ? "Saving..."
        : isEditing
            ? "Save problem"
            : "Create problem";
    elements.editSelectedProblem.disabled = !isAdminSession()
        || !state.selectedProblem
        || state.savingProblem
        || state.deletingProblem;
    elements.resetProblemAuthor.disabled = !isAdminSession()
        || state.savingProblem
        || state.deletingProblem;
    elements.deleteProblem.disabled = !isAdminSession()
        || !isEditing
        || state.savingProblem
        || state.deletingProblem;
    elements.deleteProblem.textContent = state.deletingProblem ? "Deleting..." : "Delete";
}

function renderAuthorTestCaseForm() {
    const disabled = !isAdminSession() || !state.selectedProblem || state.savingTestCase;
    const testCount = state.selectedProblem ? state.testCases.length : 0;
    const controls = elements.testCaseForm.querySelectorAll("input, textarea");

    elements.authorTestCount.textContent = state.selectedProblem
        ? `${number(testCount)} ${plural(testCount, "test", "tests")}`
        : "No problem";
    controls.forEach((control) => {
        control.disabled = disabled;
    });
    elements.addTestCase.disabled = disabled;
    elements.addTestCase.textContent = state.savingTestCase
        ? "Adding..."
        : "Add test case";
}

function renderAuthorTestCases() {
    elements.authorTestList.innerHTML = "";

    if (!isAdminSession()) {
        renderEmpty(elements.authorTestList, "Administrator access required.");
        return;
    }

    if (!state.selectedProblem) {
        renderEmpty(elements.authorTestList, "Choose a problem before adding tests.");
        return;
    }

    if (state.testCasesError) {
        renderEmpty(elements.authorTestList, state.testCasesError, true);
        return;
    }

    if (!state.testCases.length) {
        renderEmpty(elements.authorTestList, "No test cases have been added yet.");
        return;
    }

    state.testCases.forEach((testCase, index) => {
        const row = document.createElement("div");
        row.className = "test-case-row";
        row.innerHTML = `
            <div class="test-case-heading">
                <strong>Case ${number(index + 1)}</strong>
                <span>${testCase.hidden ? "Hidden" : "Visible"}</span>
            </div>
            <div class="test-case-grid">
                ${renderCodeSample("Input", testCase.input)}
                ${renderCodeSample("Expected", testCase.expectedOutput)}
            </div>
        `;
        elements.authorTestList.append(row);
    });
}

function renderInterviewWorkspace() {
    renderInterviewForm();
    renderInterviewList();
    renderInterviewDetail();
}

function renderInterviewForm() {
    const interviews = state.mockInterviews || [];
    const disabled = !state.session || state.startingInterview;

    elements.interviewCount.textContent = `${number(interviews.length)} ${plural(
            interviews.length,
            "session",
            "sessions"
    )}`;
    elements.interviewDifficulty.disabled = disabled;
    elements.interviewDuration.disabled = disabled;
    elements.startInterview.disabled = disabled;
    elements.startInterview.textContent = state.startingInterview
        ? "Starting..."
        : "Start mock interview";
}

function renderInterviewList() {
    const interviews = state.mockInterviews || [];
    elements.interviewList.innerHTML = "";

    if (!state.session) {
        renderEmpty(elements.interviewList, "Sign in to load interviews.");
        return;
    }

    if (!interviews.length) {
        renderEmpty(elements.interviewList, "No mock interviews yet.");
        return;
    }

    interviews.forEach((interview) => {
        const row = document.createElement("button");
        const isActive = String(interview.sessionId) === String(state.selectedInterviewId);
        row.className = `interview-row${isActive ? " active" : ""}`;
        row.type = "button";
        row.dataset.interviewId = interview.sessionId;
        row.innerHTML = `
            <span class="interview-row-main">
                <strong>${escapeHtml(interview.problemTitle || "Untitled prompt")}</strong>
                <span>${escapeHtml(formatDateTime(interview.startedAt))}</span>
            </span>
            <span class="interview-row-meta">
                <span>${escapeHtml(interview.difficulty || "-")}</span>
                <span>${escapeHtml(interview.category || "General")}</span>
                <span>${number(interview.durationMinutes)} min</span>
                <span>${escapeHtml(statusLabel(interview.status))}</span>
            </span>
        `;
        elements.interviewList.append(row);
    });
}

function renderInterviewDetail() {
    const interview = state.selectedInterview;

    if (!state.session) {
        elements.interviewStatus.textContent = "Signed out";
        renderEmpty(elements.interviewDetail, "Sign in to inspect interview prompts.");
        return;
    }

    if (!interview) {
        elements.interviewStatus.textContent = "No session";
        renderEmpty(elements.interviewDetail, "Start a mock interview when ready.");
        return;
    }

    const isActive = interview.status === "IN_PROGRESS";
    const actionDisabled = state.updatingInterview ? "disabled" : "";
    const activeActions = isActive
        ? `
            <button class="primary-action" type="button" data-interview-action="complete" ${actionDisabled}>
                Complete
            </button>
            <button class="secondary-action" type="button" data-interview-action="abandon" ${actionDisabled}>
                Abandon
            </button>
        `
        : "";
    const openProblemButton = interview.problemId == null
        ? ""
        : `
            <button class="inline-action" type="button" data-interview-problem-id="${escapeHtml(interview.problemId)}">
                Open problem
            </button>
        `;

    elements.interviewStatus.textContent = state.updatingInterview
        ? "Updating"
        : statusLabel(interview.status);
    elements.interviewDetail.innerHTML = `
        <div class="interview-summary">
            <div>
                <strong>${escapeHtml(interview.problemTitle || "Untitled prompt")}</strong>
                <span>${escapeHtml(interview.difficulty || "-")} / ${escapeHtml(interview.category || "General")}</span>
            </div>
            <div>
                <span>${number(interview.durationMinutes)} min</span>
                <span>${escapeHtml(formatDateTime(interview.startedAt))}</span>
            </div>
        </div>
        <p class="problem-description">${multiline(interview.description || "No prompt description yet.")}</p>
        ${renderProblemBlock("Examples", interview.examples)}
        ${renderProblemBlock("Constraints", interview.constraints)}
        ${renderProblemBlock("Starter Code", interview.starterCode)}
        <div class="interview-actions">
            ${activeActions}
            ${openProblemButton}
        </div>
    `;
}

function renderProblemList() {
    const problems = state.problems || [];
    elements.problemCount.textContent = `${number(problems.length)} ${plural(
            problems.length,
            "problem",
            "problems"
    )}`;
    elements.problemList.innerHTML = "";

    if (!state.session) {
        renderEmpty(elements.problemList, "Sign in to load problems.");
        return;
    }

    if (!problems.length) {
        renderEmpty(elements.problemList, "No problems are available yet.");
        return;
    }

    problems.forEach((problem) => {
        const row = document.createElement("button");
        const isActive = String(problem.id) === String(state.selectedProblemId);
        row.className = `problem-row${isActive ? " active" : ""}`;
        row.type = "button";
        row.dataset.problemId = problem.id;
        row.innerHTML = `
            <span class="problem-row-main">
                <strong>${escapeHtml(problem.title || "Untitled problem")}</strong>
                <span>${escapeHtml(shortText(problem.description, "No description yet.", 108))}</span>
            </span>
            <span class="problem-row-meta">
                <span>${escapeHtml(problem.difficulty || "-")}</span>
                <span>${escapeHtml(problem.category || "General")}</span>
            </span>
        `;
        elements.problemList.append(row);
    });
}

function renderProblemDetail() {
    const problem = state.selectedProblem;
    const canUseProblem = Boolean(state.session && problem);
    const canEdit = canUseProblem && !state.runningCode && !state.submittingReview;
    elements.codeRunForm.classList.toggle("hidden", !canUseProblem);
    elements.codeEditor.disabled = !canEdit;
    elements.runCode.disabled = !canEdit;
    elements.submitReview.disabled = !canEdit;

    if (!state.session) {
        elements.problemStatus.textContent = "Signed out";
        renderEmpty(elements.problemDetail, "Sign in to inspect and run problems.");
        return;
    }

    if (!problem) {
        elements.problemStatus.textContent = "No problem";
        renderEmpty(elements.problemDetail, "Choose a problem when one is available.");
        return;
    }

    const visibleCases = visibleTestCases();
    const hiddenCount = Math.max(state.testCases.length - visibleCases.length, 0);
    const draft = ensureProblemDraft(problem);
    const problemId = String(problem.id);

    if (
        elements.codeEditor.dataset.problemId !== problemId
            || document.activeElement !== elements.codeEditor
    ) {
        elements.codeEditor.value = draft;
    }

    elements.codeEditor.dataset.problemId = problemId;
    elements.problemStatus.textContent = state.runningCode
        ? "Running"
        : state.submittingReview
            ? "Reviewing"
        : `${number(visibleCases.length)} visible ${plural(
                visibleCases.length,
                "test",
                "tests"
        )}`;
    elements.codeRunStatus.textContent = workbenchStatus();

    elements.problemDetail.innerHTML = `
        <div class="problem-title-block">
            <strong>${escapeHtml(problem.title || "Untitled problem")}</strong>
            <div class="problem-meta">
                <span>${escapeHtml(problem.difficulty || "-")}</span>
                <span>${escapeHtml(problem.category || "General")}</span>
            </div>
            ${renderTagList(problem.tags)}
        </div>
        <p class="problem-description">${multiline(problem.description || "No description yet.")}</p>
        ${renderProblemBlock("Examples", problem.examples)}
        ${renderProblemBlock("Constraints", problem.constraints)}
        ${renderVisibleTestCaseList(visibleCases, hiddenCount)}
    `;
}

function renderRunResult() {
    const result = state.runResult;

    if (!state.session || !state.selectedProblem) {
        elements.runResult.innerHTML = "";
        return;
    }

    if (state.runningCode) {
        renderEmpty(elements.runResult, "Running visible tests...");
        return;
    }

    if (!result) {
        renderEmpty(elements.runResult, "Run your Java solution to see test output.");
        return;
    }

    if (result.error) {
        renderEmpty(elements.runResult, result.error, true);
        return;
    }

    const testCases = result.testCases || [];
    const resultClass = result.allPassed ? "passed" : "failed";
    const testCaseHtml = testCases.map((testCase) => {
        const caseClass = testCase.passed ? "passed" : "failed";
        return `
            <div class="run-case ${caseClass}">
                <div class="run-case-heading">
                    <strong>${escapeHtml(statusLabel(testCase.status))}</strong>
                    <span>${number(testCase.durationMs)} ms</span>
                </div>
                <div class="run-case-grid">
                    ${renderCodeSample("Input", testCase.input)}
                    ${renderCodeSample("Expected", testCase.expectedOutput)}
                    ${renderCodeSample("Actual", testCase.actualOutput)}
                    ${testCase.errorOutput ? renderCodeSample("Error", testCase.errorOutput) : ""}
                </div>
            </div>
        `;
    }).join("");

    elements.runResult.innerHTML = `
        <div class="run-summary ${resultClass}">
            <div>
                <strong>${escapeHtml(statusLabel(result.status))}</strong>
                <span>${number(result.passedTests)} of ${number(result.totalTests)} tests passed</span>
            </div>
            <span>${number(result.durationMs)} ms</span>
        </div>
        ${result.compileOutput ? renderCodeSample("Compile output", result.compileOutput) : ""}
        <div class="run-case-list">${testCaseHtml}</div>
    `;
}

function renderRunError(message) {
    renderEmpty(elements.runResult, message, true);
}

function renderReviewResult() {
    const result = state.reviewResult;

    if (!state.session || !state.selectedProblem) {
        elements.reviewResult.innerHTML = "";
        return;
    }

    if (state.submittingReview) {
        renderEmpty(elements.reviewResult, "Waiting for AI review...");
        return;
    }

    if (!result) {
        renderEmpty(
            elements.reviewResult,
            "Submit the current draft when you want structured AI feedback."
        );
        return;
    }

    if (result.error) {
        renderEmpty(elements.reviewResult, result.error, true);
        return;
    }

    const failed = result.status === "FAILED";
    const summary = result.feedbackSummary || result.aiFeedback || "";
    const details = [
        feedbackItem("Correctness", result.correctness),
        feedbackItem("Bugs", result.bugs),
        feedbackItem("Edge Cases", result.edgeCases),
        feedbackItem("Time Complexity", result.timeComplexity),
        feedbackItem("Space Complexity", result.spaceComplexity),
        feedbackItem("Hint", result.hint),
        feedbackItem("Suggested Improvement", result.suggestedImprovement)
    ].filter(Boolean).join("");

    elements.reviewResult.innerHTML = `
        <div class="review-summary ${failed ? "failed" : "reviewed"}">
            <div>
                <strong>${escapeHtml(statusLabel(result.status))}</strong>
                <span>${escapeHtml(formatDateTime(result.createdAt))}</span>
            </div>
            <span>#${escapeHtml(result.id || "-")}</span>
        </div>
        ${summary ? `<p class="review-copy">${multiline(summary)}</p>` : ""}
        ${details ? `<div class="feedback-grid">${details}</div>` : ""}
    `;
}

function renderReviewError(message) {
    renderEmpty(elements.reviewResult, message, true);
}

function workbenchStatus() {
    if (state.runningCode) {
        return "Running visible tests...";
    }

    if (state.submittingReview) {
        return "Submitting for AI review...";
    }

    return "Java only";
}

function renderRecommendations(recommendations) {
    elements.recommendationList.innerHTML = "";

    if (!state.session) {
        renderEmpty(elements.recommendationList, "Sign in to load practice.");
        return;
    }

    if (!recommendations.length) {
        renderEmpty(elements.recommendationList, "No recommendations yet.");
        return;
    }

    recommendations.forEach((recommendation) => {
        const row = document.createElement("div");
        const problemId = recommendation.problemId;
        const openDisabled = problemId == null ? "disabled" : "";
        row.className = "recommendation-row";
        row.innerHTML = `
            <strong>${escapeHtml(recommendation.title)}</strong>
            <div class="recommendation-meta">
                <span>${escapeHtml(recommendation.difficulty || "-")}</span>
                <span>${escapeHtml(recommendation.category || "-")}</span>
            </div>
            <div class="recommendation-footer">
                <p>${escapeHtml(recommendation.reason || "")}</p>
                <button class="inline-action" type="button" data-open-problem-id="${escapeHtml(problemId)}" ${openDisabled}>Open</button>
            </div>
        `;
        elements.recommendationList.append(row);
    });
}

function renderAdminUsers() {
    const visible = state.adminUsersAvailable;
    const users = state.adminUsers || [];
    const disabled = !visible || state.creatingAdminUser;

    elements.adminUsersSection.classList.toggle("hidden", !visible);
    elements.adminUsersNav.classList.toggle("hidden", !visible);

    if (!visible) {
        elements.adminUserList.innerHTML = "";
        elements.adminUserCount.textContent = "0 users";
        elements.createAdminUser.disabled = true;
        return;
    }

    elements.adminUserCount.textContent = `${number(users.length)} ${plural(
            users.length,
            "user",
            "users"
    )}`;
    elements.adminUsername.disabled = disabled;
    elements.adminEmail.disabled = disabled;
    elements.adminPassword.disabled = disabled;
    elements.createAdminUser.disabled = disabled;
    elements.createAdminUser.textContent = state.creatingAdminUser
        ? "Creating..."
        : "Create user";
    renderAdminUserList(users);
}

function renderAdminUserList(users) {
    elements.adminUserList.innerHTML = "";

    if (!users.length) {
        renderEmpty(elements.adminUserList, "No users found.");
        return;
    }

    users.forEach((user) => {
        const row = document.createElement("div");
        row.className = "admin-user-row";
        row.innerHTML = `
            <div class="admin-user-heading">
                <strong>${escapeHtml(user.username || "Unknown user")}</strong>
                <span>${escapeHtml(user.role || "USER")}</span>
            </div>
            <div class="admin-user-meta">
                <span>${escapeHtml(user.email || "-")}</span>
                <span>#${escapeHtml(user.id || "-")}</span>
                <span>${escapeHtml(formatDateTime(user.createdAt))}</span>
            </div>
        `;
        elements.adminUserList.append(row);
    });
}

function ensureProblemDraft(problem) {
    if (!problem) {
        return "";
    }

    if (state.problemDrafts[problem.id] === undefined) {
        state.problemDrafts[problem.id] = problem.starterCode?.trim()
            ? problem.starterCode
            : defaultJavaSolution(problem);
    }

    return state.problemDrafts[problem.id];
}

function visibleTestCases() {
    return (state.testCases || []).filter((testCase) => !testCase.hidden);
}

function renderVisibleTestCaseList(testCases, hiddenCount) {
    if (state.testCasesError) {
        return `
            <div class="problem-block">
                <span>Visible Tests</span>
                <p class="error-copy">${escapeHtml(state.testCasesError)}</p>
            </div>
        `;
    }

    if (!testCases.length) {
        const message = hiddenCount
            ? `${number(hiddenCount)} hidden ${plural(hiddenCount, "case", "cases")} reserved outside this runner.`
            : "No visible tests have been added yet.";

        return `
            <div class="problem-block">
                <span>Visible Tests</span>
                <p class="empty-copy">${escapeHtml(message)}</p>
            </div>
        `;
    }

    const cases = testCases.map((testCase, index) => {
        return `
            <div class="test-case-row">
                <div class="test-case-heading">
                    <strong>Case ${number(index + 1)}</strong>
                    <span>Visible</span>
                </div>
                <div class="test-case-grid">
                    ${renderCodeSample("Input", testCase.input)}
                    ${renderCodeSample("Expected", testCase.expectedOutput)}
                </div>
            </div>
        `;
    }).join("");

    const hiddenCopy = hiddenCount
        ? `<p class="form-note">${number(hiddenCount)} hidden ${plural(hiddenCount, "case", "cases")} reserved outside this runner.</p>`
        : "";

    return `
        <div class="problem-block">
            <span>Visible Tests</span>
            <div class="test-case-list">${cases}</div>
            ${hiddenCopy}
        </div>
    `;
}

function renderTagList(value) {
    const tags = String(value || "")
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean);

    if (!tags.length) {
        return "";
    }

    return `
        <div class="tag-list">
            ${tags.map((tag) => `<span>${escapeHtml(tag)}</span>`).join("")}
        </div>
    `;
}

function renderProblemBlock(title, value) {
    if (!value) {
        return "";
    }

    return `
        <div class="problem-block">
            <span>${escapeHtml(title)}</span>
            <pre>${escapeHtml(value)}</pre>
        </div>
    `;
}

function renderCodeSample(label, value) {
    return `
        <div class="code-sample">
            <span>${escapeHtml(label)}</span>
            <pre>${escapeHtml(value || "")}</pre>
        </div>
    `;
}

function feedbackItem(label, value) {
    if (!value) {
        return "";
    }

    return `
        <div class="feedback-item">
            <span>${escapeHtml(label)}</span>
            <p>${multiline(value)}</p>
        </div>
    `;
}

function defaultJavaSolution(problem) {
    const title = String(problem?.title || "Selected Problem")
        .replaceAll("*/", "")
        .replaceAll("\n", " ");

    return `public class Main {
    public static void main(String[] args) throws Exception {
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // Start solving ${title} here.
        while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
        }
    }
}`;
}

function shortText(value, fallback, limit) {
    const text = String(value || fallback || "").replaceAll(/\s+/g, " ").trim();

    if (text.length <= limit) {
        return text;
    }

    return `${text.slice(0, limit - 1)}...`;
}

function multiline(value) {
    return escapeHtml(value).replaceAll("\n", "<br>");
}

function statusLabel(value) {
    const label = String(value || "-").toLowerCase().replaceAll("_", " ");
    return label.charAt(0).toUpperCase() + label.slice(1);
}

function plural(value, singular, pluralValue) {
    return Number(value) === 1 ? singular : pluralValue;
}

function renderOperations() {
    const status = state.executionStatus;
    const visible = Boolean(status);

    elements.operationsSection.classList.toggle("hidden", !visible);
    elements.operationsNav.classList.toggle("hidden", !visible);

    if (!visible) {
        elements.operationsGrid.innerHTML = "";
        elements.operationsMode.textContent = "-";
        return;
    }

    elements.operationsMode.textContent = status.mode || "-";
    elements.operationsGrid.innerHTML = [
        operationRow("Language", status.supportedLanguage),
        operationRow("Compile timeout", seconds(status.compileTimeoutSeconds)),
        operationRow("Test timeout", seconds(status.testTimeoutSeconds)),
        operationRow("Source limit", characters(status.maxSourceCharacters)),
        operationRow("Output limit", characters(status.outputLimitCharacters)),
        operationRow("JVM heap", megabytes(status.maxHeapMegabytes)),
        operationRow("Active processors", number(status.activeProcessorCount)),
        operationRow("Visible tests", status.visibleTestCasesOnly ? "Only" : "All"),
        operationRow("Workspace", status.temporaryWorkspacePerRun ? "Temporary" : "Shared"),
        operationRow("Environment", status.childEnvironmentSanitized ? "Sanitized" : "Inherited"),
        operationRow("Isolation", status.hostPolicy?.isolation),
        operationRow("Local execution", status.hostPolicy?.localExecutionEnabled ? "Enabled" : "Disabled"),
        operationRow("OS isolation", status.hostPolicy?.osLevelIsolation ? "Enabled" : "Not enabled"),
        operationRow("Workspace policy", status.hostPolicy?.workspacePolicy),
        operationRow("Runs", number(status.runtime?.totalRuns)),
        operationRow("Successful runs", number(status.runtime?.successfulRuns)),
        operationRow("Failed runs", number(status.runtime?.failedRuns)),
        operationRow("Compile errors", number(status.runtime?.compileErrorRuns)),
        operationRow("Runtime errors", number(status.runtime?.runtimeErrorRuns)),
        operationRow("Timeouts", number(status.runtime?.timeoutRuns)),
        operationRow("Wrong answers", number(status.runtime?.wrongAnswerRuns)),
        operationRow("Last status", statusLabel(status.runtime?.lastStatus)),
        operationRow("Last run", status.runtime?.lastRunAt
            ? formatDateTime(status.runtime.lastRunAt)
            : "Never"),
        operationRow("Avg duration", milliseconds(status.runtime?.averageDurationMs)),
        operationRow("Docker image", status.docker?.image),
        operationRow("Docker CPU", number(status.docker?.cpuCount)),
        operationRow("Docker memory", megabytes(status.docker?.memoryMegabytes)),
        operationRow("Docker tmpfs", megabytes(status.docker?.tmpfsMegabytes)),
        operationRow("Docker PIDs", number(status.docker?.pidsLimit)),
        operationRow("Docker network", status.docker?.networkDisabled ? "Disabled" : "Enabled"),
        operationRow("Docker root", status.docker?.readOnlyRootFilesystem ? "Read-only" : "Writable")
    ].join("");
}

function operationRow(label, value) {
    return `
        <div class="operation-row">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value || "-")}</strong>
        </div>
    `;
}

function renderAssistantAnswer(response) {
    const citations = response.citations || [];
    const citationHtml = citations.map((citation) => {
        const title = citation.title || citation.submissionStatus || citation.type;
        return `<span>${escapeHtml(citation.label)} ${escapeHtml(title)}</span>`;
    }).join("");

    elements.assistantAnswer.innerHTML = `
        <p>${escapeHtml(response.answer || "")}</p>
        <div class="citation-list">${citationHtml}</div>
    `;
}

function renderAssistantError(message) {
    elements.assistantAnswer.innerHTML = `<p class="error-copy">${escapeHtml(message)}</p>`;
}

function renderError(message) {
    renderEmpty(elements.trendChart, message, true);
    renderEmpty(elements.categoryBreakdown, message, true);
    renderEmpty(elements.problemList, message, true);
    renderEmpty(elements.problemDetail, message, true);
    elements.codeRunForm.classList.add("hidden");
    elements.runResult.innerHTML = "";
    elements.reviewResult.innerHTML = "";
    renderEmpty(elements.authorTestList, message, true);
    elements.problemAuthorMode.textContent = "Error";
    elements.authorTestCount.textContent = "Error";
    renderEmpty(elements.interviewList, message, true);
    renderEmpty(elements.interviewDetail, message, true);
    elements.interviewStatus.textContent = "Error";
    elements.adminUsersSection.classList.add("hidden");
    elements.adminUsersNav.classList.add("hidden");
    elements.adminUserList.innerHTML = "";
    renderEmpty(elements.recommendationList, message, true);
}

function renderEmpty(target, message, isError = false) {
    target.innerHTML = `<p class="${isError ? "error-copy" : "empty-copy"}">${escapeHtml(message)}</p>`;
}

function renderFormStatus(target, message, isError = false) {
    target.classList.toggle("error", isError);
    target.textContent = message;
}

function clearFormStatus(target) {
    target.classList.remove("error");
    target.textContent = "";
}

function setConnection(message) {
    elements.connectionCopy.textContent = message;
}

function percent(value) {
    return `${Number(value || 0).toFixed(1).replace(".0", "")}%`;
}

function number(value) {
    return Number(value || 0).toLocaleString();
}

function seconds(value) {
    return `${number(value)}s`;
}

function milliseconds(value) {
    return `${number(value)} ms`;
}

function characters(value) {
    return `${number(value)} chars`;
}

function megabytes(value) {
    return `${number(value)} MB`;
}

function formatDate(value) {
    if (!value) {
        return "-";
    }

    return new Intl.DateTimeFormat(undefined, {
        month: "short",
        day: "numeric"
    }).format(new Date(`${value}T00:00:00Z`));
}

function formatDateTime(value) {
    if (!value) {
        return "Just now";
    }

    return new Intl.DateTimeFormat(undefined, {
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit"
    }).format(new Date(value));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

function setupScrollInteractions() {
    document.body.classList.add("animations-ready");

    const revealElements = document.querySelectorAll(".reveal");

    if ("IntersectionObserver" in window) {
        const revealObserver = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                }
            });
        }, {
            threshold: 0.16
        });

        revealElements.forEach((element) => revealObserver.observe(element));
    } else {
        revealElements.forEach((element) => element.classList.add("is-visible"));
    }

    window.addEventListener("scroll", updateScrollState, {passive: true});
    window.addEventListener("resize", updateScrollState);
    updateScrollState();
}

function updateScrollState() {
    const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
    const scrollDepth = maxScroll > 0 ? window.scrollY / maxScroll : 0;

    elements.scrollProgress.style.transform = `scaleX(${Math.min(scrollDepth, 1)})`;
    elements.topbar.classList.toggle("scrolled", window.scrollY > 12);

    const visibleSections = Array.from(document.querySelectorAll("main section:not(.hidden)"));
    let currentSection = visibleSections
        .filter((section) => section.getBoundingClientRect().top <= 180)
        .at(-1);

    if (scrollDepth > 0.95) {
        currentSection = visibleSections.at(-1) || currentSection;
    }

    elements.navLinks.forEach((link) => {
        link.classList.toggle(
            "active",
            currentSection && link.getAttribute("href") === `#${currentSection.id}`
        );
    });
}
