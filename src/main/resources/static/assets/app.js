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
    problems: [],
    selectedProblemId: null,
    selectedProblem: null,
    testCases: [],
    testCasesError: null,
    runResult: null,
    problemDrafts: {},
    runningCode: false
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
    codeRunForm: document.querySelector("#code-run-form"),
    codeEditor: document.querySelector("#code-editor"),
    runCode: document.querySelector("#run-code"),
    codeRunStatus: document.querySelector("#code-run-status"),
    runResult: document.querySelector("#run-result"),
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
elements.codeEditor.addEventListener("input", handleCodeDraft);
elements.assistantForm.addEventListener("submit", handleAssistantQuestion);
elements.profileForm.addEventListener("submit", handleProfileUpdate);
elements.passwordForm.addEventListener("submit", handlePasswordChange);

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
    state.problems = [];
    state.selectedProblemId = null;
    state.selectedProblem = null;
    state.testCases = [];
    state.testCasesError = null;
    state.runResult = null;
    state.problemDrafts = {};
    state.runningCode = false;
    elements.assistantAnswer.innerHTML = "";
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
            problems
        ] = await Promise.all([
            api(`/api/users/${userId}/dashboard`),
            api(`/api/users/${userId}/analytics`),
            api(`/api/users/${userId}/recommendations`),
            loadExecutionStatus(),
            api("/api/problems")
        ]);

        state.dashboard = dashboard;
        state.analytics = analytics;
        state.recommendations = recommendations;
        state.executionStatus = executionStatus;
        state.problems = Array.isArray(problems) ? problems : [];
        syncSelectedProblem();
        renderWorkspace();
        await loadSelectedProblemDetails();
        setConnection("Connected");
    } catch (error) {
        setConnection(error.message);
        renderError(error.message);
    }
}

async function loadSelectedProblemDetails() {
    if (!state.session || !state.selectedProblem) {
        state.testCases = [];
        state.testCasesError = null;
        renderProblemWorkspace();
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
    renderRecommendations(state.recommendations || []);
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
    }
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
    ensureProblemDraft(problem);
    renderProblemWorkspace();
    setConnection("Loading problem");
    await loadSelectedProblemDetails();
    setConnection(state.testCasesError || "Problem loaded");
}

function renderProblemWorkspace() {
    renderProblemList();
    renderProblemDetail();
    renderRunResult();
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
    const canRun = canUseProblem && !state.runningCode;
    elements.codeRunForm.classList.toggle("hidden", !canUseProblem);
    elements.codeEditor.disabled = !canRun;
    elements.runCode.disabled = !canRun;

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
        : `${number(visibleCases.length)} visible ${plural(
                visibleCases.length,
                "test",
                "tests"
        )}`;
    elements.codeRunStatus.textContent = state.runningCode
        ? "Running visible tests..."
        : "Java only";

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
