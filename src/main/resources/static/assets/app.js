const storageKey = "intercoach.session";
const apiBase = window.location.protocol === "file:"
    ? "http://localhost:8080"
    : "";

const state = {
    mode: "login",
    session: loadSession(),
    dashboard: null,
    analytics: null,
    recommendations: []
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
    assistantForm: document.querySelector("#assistant-form"),
    assistantQuestion: document.querySelector("#assistant-question"),
    assistantAnswer: document.querySelector("#assistant-answer")
};

elements.authButtons.forEach((button) => {
    button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});
elements.authForm.addEventListener("submit", handleAuth);
elements.signOut.addEventListener("click", signOut);
elements.refreshData.addEventListener("click", loadWorkspace);
elements.assistantForm.addEventListener("submit", handleAssistantQuestion);

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
    elements.assistantAnswer.innerHTML = "";
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
        const [dashboard, analytics, recommendations] = await Promise.all([
            api(`/api/users/${userId}/dashboard`),
            api(`/api/users/${userId}/analytics`),
            api(`/api/users/${userId}/recommendations`)
        ]);

        state.dashboard = dashboard;
        state.analytics = analytics;
        state.recommendations = recommendations;
        renderWorkspace();
        setConnection("Connected");
    } catch (error) {
        setConnection(error.message);
        renderError(error.message);
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

    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.message || `Request failed with ${response.status}`);
    }

    return response.json();
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
    renderRecommendations(state.recommendations || []);
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
        row.className = "recommendation-row";
        row.innerHTML = `
            <strong>${escapeHtml(recommendation.title)}</strong>
            <div class="recommendation-meta">
                <span>${escapeHtml(recommendation.difficulty || "-")}</span>
                <span>${escapeHtml(recommendation.category || "-")}</span>
            </div>
            <p>${escapeHtml(recommendation.reason || "")}</p>
        `;
        elements.recommendationList.append(row);
    });
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
    renderEmpty(elements.recommendationList, message, true);
}

function renderEmpty(target, message, isError = false) {
    target.innerHTML = `<p class="${isError ? "error-copy" : "empty-copy"}">${escapeHtml(message)}</p>`;
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

    const currentSection = Array.from(document.querySelectorAll("main section"))
        .filter((section) => section.getBoundingClientRect().top <= 180)
        .at(-1);

    elements.navLinks.forEach((link) => {
        link.classList.toggle(
            "active",
            currentSection && link.getAttribute("href") === `#${currentSection.id}`
        );
    });
}
