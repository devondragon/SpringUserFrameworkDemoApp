// File: /js/utils/password-validation.js
/**
 * Shared password strength calculation and UI update utilities.
 * Used across registration, password reset, and password update forms.
 */

/**
 * Calculate password strength score based on policy requirements.
 * @param {string} password - The password to evaluate
 * @returns {number} Score from 0-5 based on criteria met
 */
export function calculateStrength(password) {
    let score = 0;
    if (password.length >= 8) score++; // Rule 1: Length (min 8 chars)
    if (/[A-Z]/.test(password)) score++; // Rule 2: Uppercase letter
    if (/[a-z]/.test(password)) score++; // Rule 3: Lowercase letter
    if (/[0-9]/.test(password)) score++; // Rule 4: Digit
    if (/[^A-Za-z0-9]/.test(password)) score++; // Rule 5: Special character
    return score;
}

/**
 * Update the password strength progress bar and label.
 * @param {number} score - Strength score (0-5)
 * @param {HTMLElement} strengthLevel - Progress bar element
 * @param {HTMLElement} strengthLabel - Label element for text
 */
export function updateStrengthBar(score, strengthLevel, strengthLabel) {
    const levels = [
        { width: "0%", color: "", label: "" },
        { width: "20%", color: "bg-danger", label: "Very Weak" },
        { width: "40%", color: "bg-warning", label: "Weak" },
        { width: "60%", color: "bg-info", label: "Fair" },
        { width: "80%", color: "bg-primary", label: "Strong" },
        { width: "100%", color: "bg-success", label: "Very Strong" },
    ];

    const level = levels[score] || levels[0];
    strengthLevel.style.width = level.width;
    strengthLevel.className = `progress-bar ${level.color}`;
    strengthLabel.textContent = `Password strength: ${level.label}`;
}

/**
 * Initialize password strength meter on a password field.
 * @param {HTMLElement} passwordField - The password input field
 * @param {HTMLElement} strengthContainer - Container for the strength meter
 * @param {HTMLElement} strengthLevel - Progress bar element
 * @param {HTMLElement} strengthLabel - Label element
 */
export function initPasswordStrengthMeter(
    passwordField,
    strengthContainer,
    strengthLevel,
    strengthLabel
) {
    passwordField.addEventListener("input", () => {
        const password = passwordField.value;
        if (password) {
            strengthContainer.classList.remove("d-none");
            const score = calculateStrength(password);
            updateStrengthBar(score, strengthLevel, strengthLabel);
        } else {
            strengthContainer.classList.add("d-none");
        }
    });
}

/**
 * Initialize password requirements visibility toggle.
 * Shows requirements on focus, hides on blur.
 * @param {HTMLElement} passwordField - The password input field
 * @param {HTMLElement} requirementsContainer - Requirements list container
 * @param {HTMLElement} errorContainer - Error message container (optional)
 */
export function initPasswordRequirements(
    passwordField,
    requirementsContainer,
    errorContainer = null
) {
    if (!passwordField || !requirementsContainer) return;

    passwordField.addEventListener("focus", () => {
        requirementsContainer.classList.remove("d-none");
        if (errorContainer) {
            errorContainer.classList.add("d-none");
        }
    });

    passwordField.addEventListener("blur", () => {
        requirementsContainer.classList.add("d-none");
    });
}
