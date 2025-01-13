import { showMessage, clearErrors } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#deleteAccountForm");
    const globalMessage = document.querySelector("#globalMessage");
    const globalError = document.querySelector("#globalError");
    const deleteConfirmationInput = document.querySelector("#deleteConfirmationInput");
    const confirmDeletionButton = document.querySelector("#confirmDeletionButton");
    const deleteConfirmationModal = new bootstrap.Modal(document.querySelector("#deleteConfirmationModal"));

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        deleteConfirmationModal.show();
    });

    confirmDeletionButton.addEventListener("click", async () => {
        clearErrors();

        // Validate deletion confirmation
        if (deleteConfirmationInput.value !== "DELETE") {
            showMessage(globalError, "You must type 'DELETE' to confirm.", "alert-danger");
            return;
        }

        confirmDeletionButton.disabled = true;

        // Prepare JSON body
        const requestData = { confirmation: deleteConfirmationInput.value };

        try {
            const response = await fetch(form.action, {
                method: "DELETE",
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
                form.classList.add("d-none");
            } else {
                showMessage(globalError, data.messages.join(" "), "alert-danger");
            }
        } catch (error) {
            console.error("Request failed:", error);
            showMessage(globalError, "An unexpected error occurred.", "alert-danger");
        } finally {
            // Reset input and modal
            deleteConfirmationInput.value = "";
            deleteConfirmationModal.hide();
            confirmDeletionButton.disabled = false;
        }
    });
});
