// File: /js/user/update-password.js
import { showMessage, showError, clearErrors } from "/js/shared.js";
import {
    initPasswordStrengthMeter,
    initPasswordRequirements,
} from "/js/utils/password-validation.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#updatePasswordForm");
    const globalMessage = document.querySelector("#globalMessage");
    const currentPasswordField = document.querySelector("#currentPassword");
    const newPasswordField = document.querySelector("#newPassword");
    const confirmPasswordField = document.querySelector("#confirmPassword");
    const confirmPasswordError = document.querySelector("#confirmPasswordError");

    // Initialize password strength meter for new password field
    const passwordStrength = document.getElementById("password-strength");
    const strengthLevel = document.getElementById("strengthLevel");
    const strengthLabel = document.getElementById("strengthLabel");
    initPasswordStrengthMeter(newPasswordField, passwordStrength, strengthLevel, strengthLabel);

    // Initialize password requirements visibility toggle
    const passwordRules = document.getElementById("password-requirements");
    const newPasswordError = document.querySelector("#newPasswordError");
    initPasswordRequirements(newPasswordField, passwordRules, newPasswordError);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        const currentPassword = currentPasswordField.value;
        const newPassword = newPasswordField.value;
        const confirmPassword = confirmPasswordField.value;

        // Validate passwords
        if (newPassword !== confirmPassword) {
            showError(confirmPasswordError, "Passwords do not match.");
            return;
        }

        // Prepare JSON payload
        const requestData = {
            oldPassword: currentPassword,
            newPassword: newPassword,
        };

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
                showMessage(globalMessage, data.messages.join(" "), "alert-success");
                form.reset(); // Reset the form on successful submission
            } else {
                const errorMessage = data.messages?.join(" ") || "Unable to update your password.";
                showMessage(globalMessage, errorMessage, "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalMessage, "An unexpected error occurred. Please try again later.", "alert-danger");
        }
    });

    // Real-time validation for password matching
    [newPasswordField, confirmPasswordField].forEach((field) => {
        field.addEventListener("input", () => {
            if (newPasswordField.value !== confirmPasswordField.value) {
                showError(confirmPasswordError, "Passwords do not match.");
            } else {
                confirmPasswordError.textContent = "";
                confirmPasswordError.classList.add("d-none");
            }
        });
    });
});
