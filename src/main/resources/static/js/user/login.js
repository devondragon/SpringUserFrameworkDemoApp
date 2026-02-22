import { showMessage } from "/js/shared.js";
import { isWebAuthnSupported } from "/js/user/webauthn-utils.js";
import { authenticateWithPasskey } from "/js/user/webauthn-authenticate.js";

document.addEventListener("DOMContentLoaded", () => {
	const form = document.querySelector("form");

	form.addEventListener("submit", (event) => {
		if (!validateForm(form)) {
			event.preventDefault();
		}
	});

	// Show passkey login button if WebAuthn is supported
	const passkeySection = document.getElementById("passkey-login-section");
	const passkeyBtn = document.getElementById("passkeyLoginBtn");

	if (passkeySection && passkeyBtn && isWebAuthnSupported()) {
		passkeySection.style.display = "block";
		const passkeyError = document.getElementById("passkeyError");

		passkeyBtn.addEventListener("click", async () => {
			passkeyBtn.disabled = true;
			passkeyBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Authenticating...';

			try {
				const redirectUrl = await authenticateWithPasskey();
				window.location.href = redirectUrl;
			} catch (error) {
				console.error("Passkey authentication failed:", error);
				showMessage(passkeyError, "Passkey authentication failed. Please try again.", "alert-danger");
				passkeyBtn.disabled = false;
				passkeyBtn.innerHTML = '<i class="bi bi-key me-2"></i> Sign in with Passkey';
			}
		});
	}
});

function validateForm(form) {
	const username = form.username;
	const password = form.password;

	if (!username.value && !password.value) {
		showMessage(null, "Both username and password are required.", "alert-danger");
		username.focus();
		return false;
	}
	if (!username.value) {
		showMessage(null, "Username is required.", "alert-danger");
		username.focus();
		return false;
	}
	if (!password.value) {
		showMessage(null, "Password is required.", "alert-danger");
		password.focus();
		return false;
	}
	return true;
}
