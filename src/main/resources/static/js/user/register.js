// File: /js/user/register.js
import {
    showMessage,
    showError,
    showHtmlError,
    hideError,
    clearErrors,
} from "/js/shared.js";
import {
    calculateStrength,
    updateStrengthBar,
    initPasswordStrengthMeter,
    initPasswordRequirements,
} from "/js/utils/password-validation.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#registerForm");
    const passwordField = document.querySelector("#password");
    const matchPasswordField = document.querySelector("#matchPassword");
    const signUpButton = document.querySelector("#signUpButton");
    const termsCheckbox = document.querySelector("#terms");
    const strengthLevel = document.querySelector("#strengthLevel");
    const strengthLabel = document.querySelector("#strengthLabel");

    form.addEventListener("submit", (event) => handleRegistration(event));

    // Real-time password matching validation
    [passwordField, matchPasswordField].forEach((field) => {
        field.addEventListener("input", () => {
            const errorContainer =
                matchPasswordField.parentElement.querySelector(".form-text");
            if (passwordField.value !== matchPasswordField.value) {
                showError(errorContainer, "Passwords do not match.");
            } else {
                hideError(errorContainer);
            }
        });
    });

    // Initialize password requirements visibility toggle
    const passwordRules = document.getElementById("password-requirements");
    const passwordError = document.querySelector("#passwordError");
    initPasswordRequirements(passwordField, passwordRules, passwordError);

    // Initialize password strength meter
    const passwordStrength = document.getElementById("password-strength");
    initPasswordStrengthMeter(passwordField, passwordStrength, strengthLevel, strengthLabel);
});

async function handleRegistration(event) {
    event.preventDefault();
    const form = document.querySelector("#registerForm");
    const signUpButton = document.querySelector("#signUpButton");
    const globalError = document.querySelector("#globalError");

    signUpButton.disabled = true;
    clearErrors();

    const password = document.querySelector("#password").value;
    const matchPassword = document.querySelector("#matchPassword").value;

    // Validate password matching
    if (password !== matchPassword) {
        const errorContainer = document
            .querySelector("#matchPassword")
            .parentElement.querySelector(".form-text");
        showError(errorContainer, "Passwords do not match.");
        signUpButton.disabled = false;
        return;
    }

    // Validate terms and conditions
    const termsCheckbox = document.querySelector("#terms");
    if (!termsCheckbox.checked) {
        alert("You must agree to the Terms and Conditions to register.");
        signUpButton.disabled = false;
        return;
    }

    // Prepare JSON payload
    const formData = Object.fromEntries(new FormData(form).entries());

    console.log("Submitting form data:", JSON.stringify(formData));

    try {
        const response = await fetch(form.action, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [document.querySelector("meta[name='_csrf_header']").content]:
                    document.querySelector("meta[name='_csrf']").content,
            },
            body: JSON.stringify(formData),
        });

        const data = await response.json();

        if (response.ok && data.success) {
            window.location.href = data.redirectUrl;
        } else if (data.errors && data.errors.password) {
            // Show password-specific errors from validation
            const passwordError = document.querySelector("#passwordError");
            console.log("Password validation error: ", data.errors.password);

            const errorMessage = data.errors.password;
            showHtmlError(passwordError, `<div>• ${errorMessage}</div>`);
        } else if (data.code === 1 && containsPasswordError(data)) {
            // Show password-specific errors from business logic (old format)
            const passwordError = document.querySelector("#passwordError");
            console.log("Password Error are: ", data.messages);

            const allMessages = data?.messages?.[0] ?? "";
            const formattedErrorMessages = allMessages
                .split(".") // split at each period
                .map((msg) => msg.trim()) // remove extra spaces
                .filter(Boolean) // remove empty strings
                .map((msg) => `<div>• ${msg}</div>`) // wrap each in a bullet div
                .join("");

            showHtmlError(passwordError, formattedErrorMessages);
        } else if (data.errors) {
            // Show field-specific errors in global error area
            console.log("Validation errors: ", data.errors);
            const errorMessages = Object.entries(data.errors)
                .map(([field, message]) => `${field}: ${message}`)
                .join("<br>");
            showMessage(globalError, errorMessages, "alert-danger");
        } else {
            console.log("General error: ", data.messages || data.message);
            const errorMessage =
                data.messages?.join(" ") || data.message || "Registration failed. Please try again.";
            showMessage(globalError, errorMessage, "alert-danger");
        }
    } catch (error) {
        console.error("Request failed:", error);
        showMessage(
            globalError,
            "An unexpected error occurred. Please try again later.",
            "alert-danger"
        );
    } finally {
        signUpButton.disabled = false;
    }

    function containsPasswordError(data) {
        const msg = data?.messages?.[0] ?? "";
        return msg.toLowerCase().includes("password");
    }
}
