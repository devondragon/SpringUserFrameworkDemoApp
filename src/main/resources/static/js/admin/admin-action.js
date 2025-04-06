document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('adminActionForm');
    const usernameInput = document.getElementById('username');
    const actionSelect = document.getElementById('actionType');
    const globalMessage = document.getElementById('globalMessage');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = usernameInput.value.trim();
        const action = actionSelect.value;

        if (!email || !action) {
            showMessage('Please fill all fields', 'danger');
            return;
        }

        const endpoint = action === 'LOCK' ? '/admin/lockAccount' : '/admin/unlockAccount';

        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json",
                    [document.querySelector("meta[name='_csrf_header']").content]:
                    document.querySelector("meta[name='_csrf']").content,
                },
                body: JSON.stringify({ email }),
            });

            const result = await response.json();

            if (response.ok) {
                showMessage(result.messages[0] || 'Action successful!', 'success');
            } else {
                showMessage(result.messages[0] || 'Something went wrong.', 'danger');
            }
        } catch (error) {
            console.error(error);
            showMessage('Error occurred while performing action', 'danger');
        }
    });

    function showMessage(message, type) {
        globalMessage.className = `alert alert-${type} text-center`;
        globalMessage.textContent = message;
        globalMessage.classList.remove('d-none');
    }
});
