import { showMessage, showError, clearErrors } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#forgotPasswordForm");
    const globalError = document.querySelector("#globalError");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        const email = document.querySelector("#email");
        if (!email.value) {
            showError(document.querySelector("#emailError"), "Email is required.");
            return;
        }

        // Prepare JSON body
        const formData = {
            email: email.value,
        };

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
                showMessage(globalError, data.messages?.join(" ") || "Unable to process your request.", "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalError, "An unexpected error occurred. Please try again later.", "alert-danger");
        }
    });
});
