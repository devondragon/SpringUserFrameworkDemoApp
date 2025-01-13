import { showMessage } from "/js/shared.js";

document.addEventListener("DOMContentLoaded", () => {
	const form = document.querySelector("form");

	form.addEventListener("submit", (event) => {
		if (!validateForm(form)) {
			event.preventDefault();
		}
	});
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
