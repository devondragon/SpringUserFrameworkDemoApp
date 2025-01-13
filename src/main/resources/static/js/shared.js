export function showError(container, message) {
    if (!container) return;
    container.textContent = message;
    container.classList.remove("d-none");
}

export function hideError(container) {
    if (!container) return;
    container.textContent = "";
    container.classList.add("d-none");
}

export function clearErrors() {
    const errorMessages = document.querySelectorAll(".form-text.text-danger");
    errorMessages.forEach((error) => hideError(error));

    const globalMessage = document.querySelector("#globalMessage");
    if (globalMessage) hideError(globalMessage);
}

export function showMessage(container, message, alertClass) {
    if (!container) return;
    container.textContent = message;
    container.className = `alert ${alertClass} text-center`;
    container.classList.remove("d-none");
}
