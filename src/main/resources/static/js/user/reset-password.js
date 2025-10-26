// File: /js/user/reset-password.js
import { showMessage, showError, clearErrors } from "/js/shared.js";
import {
    initPasswordStrengthMeter,
    initPasswordRequirements,
} from "/js/utils/password-validation.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#resetPasswordForm");
    const globalMessage = document.querySelector("#globalMessage");
    const globalError = document.querySelector("#globalError");
    const passwordField = document.querySelector("#password");
    const matchPasswordField = document.querySelector("#matchPassword");
    const matchPasswordError = document.querySelector("#matchPasswordError");

    // Initialize password strength meter
    const passwordStrength = document.getElementById("password-strength");
    const strengthLevel = document.getElementById("strengthLevel");
    const strengthLabel = document.getElementById("strengthLabel");
    initPasswordStrengthMeter(passwordField, passwordStrength, strengthLevel, strengthLabel);

    // Initialize password requirements visibility toggle
    const passwordRules = document.getElementById("password-requirements");
    const passwordError = document.querySelector("#passwordError");
    initPasswordRequirements(passwordField, passwordRules, passwordError);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        const password = passwordField.value;
        const matchPassword = matchPasswordField.value;

        // Validate passwords match
        if (password !== matchPassword) {
            showError(matchPasswordError, "Passwords do not match.");
            return;
        }

        // Prepare JSON payload
        const requestData = Object.fromEntries(new FormData(form).entries());

        try {
            const response = await fetch(form.action, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    [document.querySelector("meta[name='_csrf_header']").content]:
                        document.querySelector("meta[name='_csrf']").content,
                },
                body: JSON.stringify(requestData),
            });

            const data = await response.json();
            if (response.ok && data.success) {
                form.classList.add("d-none");
                showMessage(globalMessage, data.messages.join(" "), "alert-success");
            } else {
                const errorMessage = data.messages?.join(" ") || "Unable to reset your password.";
                showMessage(globalError, errorMessage, "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalError, "An unexpected error occurred. Please try again later.", "alert-danger");
        }
    });

    // Real-time password matching validation
    [passwordField, matchPasswordField].forEach((field) => {
        field.addEventListener("input", () => {
            if (passwordField.value !== matchPasswordField.value) {
                showError(matchPasswordError, "Passwords do not match.");
            } else {
                matchPasswordError.textContent = "";
                matchPasswordError.classList.add("d-none");
            }
        });
    });
});
