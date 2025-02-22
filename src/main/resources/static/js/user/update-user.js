import { showMessage, clearErrors } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#updateUserForm");
    const globalMessage = document.querySelector("#globalMessage");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors();

        // Prepare JSON payload
        const formData = new FormData(form);
        const requestData = {};
        formData.forEach((value, key) => {
            requestData[key] = value;
        });

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
            } else {
                const errorMessage = data.messages?.join(" ") || "Unable to update your profile.";
                showMessage(globalMessage, errorMessage, "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalMessage, "An unexpected error occurred. Please try again later.", "alert-danger");
        }
    });
});
