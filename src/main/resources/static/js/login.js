import { appendCsrfField } from "./api.js?v=20260712-security-hardening";

if (typeof document !== "undefined") {
    document.addEventListener("DOMContentLoaded", () => {
        const form = document.querySelector("#login-form");
        appendCsrfField(form);
        renderLoginFeedback();
    });
}

export function renderLoginFeedback() {
    const feedback = document.querySelector("#login-feedback");
    const params = new URLSearchParams(window.location.search);
    if (params.has("error")) {
        feedback.textContent = "Usuario o contraseña inválidos.";
        feedback.classList.add("error");
        return;
    }
    if (params.has("logout")) {
        feedback.textContent = "Sesión cerrada correctamente.";
        feedback.classList.remove("error");
        return;
    }
    feedback.textContent = "";
    feedback.classList.remove("error");
}
