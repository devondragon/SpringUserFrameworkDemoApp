import { showMessage, showError, clearErrors } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#resendVerificationForm");
    const globalError = document.querySelector("#globalError");
    const alreadyEnabledMessage = document.querySelector("#alreadyEnabledMessage");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        const email = document.querySelector("#email");
        if (!email.value) {
            showError(document.querySelector("#emailError"), "Email is required.");
            return;
        }

        // Prepare JSON payload
        const requestData = { email: email.value };

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
                window.location.href = data.redirectUrl;
            } else if (data.code === 1) {
                alreadyEnabledMessage.classList.remove("d-none");
            } else {
                const errorMessage = data.messages?.join(" ") || "Unable to resend verification email.";
                showMessage(globalError, errorMessage, "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalError, "An unexpected error occurred.", "alert-danger");
        }
    });
});
