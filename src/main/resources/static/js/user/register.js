// File: /js/user/register.js
import { showMessage, showError, hideError, clearErrors } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#registerForm");
    const passwordField = document.querySelector("#password");
    const matchPasswordField = document.querySelector("#matchPassword");
    const signUpButton = document.querySelector("#signUpButton");
    const termsCheckbox = document.querySelector("#terms");

    form.addEventListener("submit", (event) => handleRegistration(event));

    // Real-time password matching validation
    [passwordField, matchPasswordField].forEach((field) => {
        field.addEventListener("input", () => {
            const errorContainer = matchPasswordField.parentElement.querySelector(".form-text");
            if (passwordField.value !== matchPasswordField.value) {
                showError(errorContainer, "Passwords do not match.");
            } else {
                hideError(errorContainer);
            }
        });
    });
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
        const errorContainer = document.querySelector("#matchPassword").parentElement.querySelector(".form-text");
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
        } else {
            const errorMessage = data.messages?.join(" ") || "Registration failed. Please try again.";
            showMessage(globalError, errorMessage, "alert-danger");
        }
    } catch (error) {
        console.error("Request failed:", error);
        showMessage(globalError, "An unexpected error occurred. Please try again later.", "alert-danger");
    } finally {
        signUpButton.disabled = false;
    }
}
