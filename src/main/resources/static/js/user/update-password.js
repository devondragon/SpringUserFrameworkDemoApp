// File: /js/user/update-password.js
import { showMessage, showError, clearErrors } from "/js/shared.js";
import {
    initPasswordStrengthMeter,
    initPasswordRequirements,
} from "/js/utils/password-validation.js";
import { getAuthMethods } from "/js/user/auth-methods.js";

let isSetPasswordMode = false;

document.addEventListener("DOMContentLoaded", async () => {
    const form = document.querySelector("#updatePasswordForm");
    const globalMessage = document.querySelector("#globalMessage");
    const currentPasswordField = document.querySelector("#currentPassword");
    const newPasswordField = document.querySelector("#newPassword");
    const confirmPasswordField = document.querySelector("#confirmPassword");
    const confirmPasswordError = document.querySelector("#confirmPasswordError");

    // Check if user has a password; if not, switch to "set password" mode
    try {
        const auth = await getAuthMethods();
        if (!auth.hasPassword) {
            const currentPasswordSection = document.querySelector("#currentPasswordSection");
            if (currentPasswordSection) {
                currentPasswordSection.classList.add("d-none");
            }
            if (currentPasswordField) {
                currentPasswordField.removeAttribute("required");
            }
            const setPasswordInfo = document.querySelector("#setPasswordInfo");
            if (setPasswordInfo) {
                setPasswordInfo.classList.remove("d-none");
            }
            const pageTitle = document.querySelector("#pageTitle");
            if (pageTitle) {
                pageTitle.textContent = "Set a Password";
            }
            isSetPasswordMode = true;
        }
    } catch (error) {
        console.error("Failed to check auth methods:", error);
    }

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

        const newPassword = newPasswordField.value;
        const confirmPassword = confirmPasswordField.value;

        // Validate passwords
        if (newPassword !== confirmPassword) {
            showError(confirmPasswordError, "Passwords do not match.");
            return;
        }

        if (isSetPasswordMode) {
            // Set password mode - no old password needed
            const requestData = {
                newPassword: newPassword,
                confirmPassword: confirmPassword,
            };

            try {
                const response = await fetch("/user/setPassword", {
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
                    form.reset();
                } else {
                    const errorMessage = data.messages?.join(" ") || "Unable to set your password.";
                    showMessage(globalMessage, errorMessage, "alert-danger");
                }
            } catch (error) {
                console.error("Request failed:", error);
                showMessage(globalMessage, "An unexpected error occurred. Please try again later.", "alert-danger");
            }
            return;
        }

        // Standard update password mode
        const currentPassword = currentPasswordField.value;

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
