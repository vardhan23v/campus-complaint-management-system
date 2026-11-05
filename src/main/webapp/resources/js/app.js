/*
 * Nitte Resolve - small progressive enhancements. Every page works without
 * this file; it only adds:
 *   1. the mobile navigation toggle
 *   2. a loading state on the clicked button and protection against double submits
 *   3. a preview and an early size/type check for the complaint image
 */
(function () {
    "use strict";

    // 1. Mobile navigation ---------------------------------------------------
    document.addEventListener("click", function (event) {
        var toggle = event.target.closest("[data-menu-toggle]");
        if (!toggle) {
            return;
        }
        var sidebar = document.getElementById(toggle.getAttribute("aria-controls"));
        if (sidebar) {
            var open = sidebar.classList.toggle("is-open");
            toggle.setAttribute("aria-expanded", open ? "true" : "false");
        }
    });

    // 2. Loading state ----------------------------------------------------------
    // The button is NOT disabled: a disabled submit button would not be sent
    // with the form, and JSF would not know which action to run.
    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (form.getAttribute("data-submitting") === "true") {
            event.preventDefault();          // second click while the first request runs
            return;
        }
        if (form.hasAttribute("data-no-loading")) {
            return;
        }
        form.setAttribute("data-submitting", "true");
        var button = event.submitter;
        if (button && button.classList.contains("btn")) {
            button.classList.add("is-loading");
            button.setAttribute("aria-busy", "true");
        }
    });

    // Reset when the page is shown again from the back/forward cache.
    window.addEventListener("pageshow", function () {
        document.querySelectorAll("form[data-submitting]").forEach(function (form) {
            form.removeAttribute("data-submitting");
        });
        document.querySelectorAll(".btn.is-loading").forEach(function (button) {
            button.classList.remove("is-loading");
            button.removeAttribute("aria-busy");
        });
    });

    // 3. Image preview ------------------------------------------------------------
    // The server validates again (type, signature, size); this only saves the
    // user an upload that would be refused.
    document.addEventListener("change", function (event) {
        var input = event.target;
        if (!input.matches || !input.matches("input[type=file][data-preview]")) {
            return;
        }
        var preview = document.getElementById(input.getAttribute("data-preview"));
        var message = document.getElementById(input.getAttribute("data-message"));
        var maxBytes = parseInt(input.getAttribute("data-max-bytes") || "5242880", 10);
        var file = input.files && input.files[0];

        if (message) {
            message.textContent = "";
        }
        if (preview) {
            preview.classList.remove("is-visible");
            preview.removeAttribute("src");
        }
        if (!file) {
            return;
        }
        if (!/^image\/(jpeg|png)$/.test(file.type) || !/\.(jpe?g|png)$/i.test(file.name)) {
            input.value = "";
            if (message) {
                message.textContent = "Only JPG, JPEG and PNG images can be attached.";
            }
            return;
        }
        if (file.size > maxBytes) {
            input.value = "";
            if (message) {
                message.textContent = "The image is too large. The maximum size is "
                        + Math.round(maxBytes / 1048576) + " MB.";
            }
            return;
        }
        if (preview && window.URL) {
            preview.src = URL.createObjectURL(file);
            preview.classList.add("is-visible");
        }
    });
})();
