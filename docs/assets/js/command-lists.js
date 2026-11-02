(function () {
    "use strict";

    if (!("popover" in HTMLElement.prototype) || !("popoverTargetElement" in HTMLButtonElement.prototype)) {
        return;
    }

    const reference = document.querySelector(".command-list-reference");
    if (!reference) {
        return;
    }

    const groups = new Map();
    document.querySelectorAll("a[data-command-list]").forEach(function (link) {
        const panel = document.getElementById("command-list-" + link.dataset.commandList);
        if (!panel) {
            return;
        }
        if (!groups.has(panel.id)) {
            groups.set(panel.id, { panel: panel, buttons: [], invoker: null, printOpen: false });
        }
        const group = groups.get(panel.id);
        const button = document.createElement("button");
        button.type = "button";
        button.className = link.className;
        button.setAttribute("popovertarget", panel.id);
        button.setAttribute("aria-controls", panel.id);
        button.setAttribute("aria-haspopup", "dialog");
        button.setAttribute("aria-expanded", "false");
        while (link.firstChild) {
            button.append(link.firstChild);
        }
        button.addEventListener("click", function () {
            group.invoker = button;
        });
        link.replaceWith(button);
        group.buttons.push(button);
    });

    if (!groups.size) {
        return;
    }

    let printing = false;

    function position(group) {
        if (!group.panel.matches(":popover-open")) {
            return;
        }
        const invoker = group.invoker || group.buttons[0];
        const anchor = invoker.getBoundingClientRect();
        if (anchor.bottom < 0 || anchor.top > window.innerHeight) {
            group.panel.hidePopover();
            return;
        }
        const bounds = group.panel.getBoundingClientRect();
        const left = Math.max(16, Math.min(anchor.left, window.innerWidth - bounds.width - 16));
        let top = anchor.bottom + 8;
        if (top + bounds.height > window.innerHeight - 8) {
            top = Math.max(8, anchor.top - bounds.height - 8);
        }
        group.panel.style.left = left + "px";
        group.panel.style.top = top + "px";
    }

    groups.forEach(function (group) {
        group.panel.setAttribute("popover", "auto");
        group.panel.setAttribute("role", "dialog");
        group.panel.querySelector(".command-list-close").hidden = false;
        group.panel.addEventListener("toggle", function () {
            const open = group.panel.matches(":popover-open");
            group.buttons.forEach(function (button) {
                button.setAttribute("aria-expanded", String(open));
            });
            if (open) {
                position(group);
            } else if (!printing && group.panel.contains(document.activeElement) && group.invoker) {
                group.invoker.focus({ preventScroll: true });
            }
        });
    });
    reference.classList.add("command-list-reference-enhanced");

    function positionOpenGroups() {
        groups.forEach(position);
    }
    window.addEventListener("resize", positionOpenGroups);
    window.addEventListener("scroll", positionOpenGroups, { capture: true, passive: true });

    window.addEventListener("beforeprint", function () {
        printing = true;
        groups.forEach(function (group) {
            group.printOpen = group.panel.matches(":popover-open");
            if (group.printOpen) {
                group.panel.hidePopover();
            }
            group.panel.removeAttribute("popover");
        });
    });
    window.addEventListener("afterprint", function () {
        groups.forEach(function (group) {
            group.panel.setAttribute("popover", "auto");
        });
        printing = false;
        groups.forEach(function (group) {
            if (group.printOpen) {
                group.panel.showPopover();
            }
        });
    });
}());
