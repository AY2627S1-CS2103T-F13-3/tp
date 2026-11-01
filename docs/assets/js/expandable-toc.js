(function () {
    "use strict";

    const toc = document.getElementById("markdown-toc");
    if (!toc || toc.closest(".guide-toc")) {
        return;
    }

    const navigation = document.createElement("nav");
    navigation.className = "guide-toc";
    navigation.setAttribute("aria-label", "Table of contents");
    toc.before(navigation);

    const controls = document.createElement("div");
    controls.className = "toc-controls";
    const title = document.createElement("span");
    title.className = "toc-title";
    title.textContent = "Contents";
    controls.append(title);
    navigation.append(controls, toc);

    const branches = [];
    toc.querySelectorAll("li").forEach(function (item, index) {
        const children = item.querySelector(":scope > ul, :scope > ol");
        const link = item.querySelector(":scope > a");
        if (!children || !link) {
            return;
        }

        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "toc-toggle";
        children.id = "guide-toc-children-" + index;
        children.classList.add("toc-children");
        toggle.setAttribute("aria-controls", children.id);
        item.insertBefore(toggle, link);

        function setExpanded(expanded) {
            children.hidden = !expanded;
            toggle.setAttribute("aria-expanded", String(expanded));
            toggle.setAttribute("aria-label", (expanded ? "Collapse " : "Expand ") + link.textContent.trim());
            toggle.textContent = expanded ? "▾" : "▸";
        }

        toggle.addEventListener("click", function () {
            setExpanded(children.hidden);
        });
        setExpanded(false);
        branches.push({ item: item, setExpanded: setExpanded });
    });

    function addControl(label, expanded) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = label;
        button.addEventListener("click", function () {
            branches.forEach(function (branch) {
                branch.setExpanded(expanded);
            });
        });
        controls.append(button);
    }

    addControl("Expand all", true);
    addControl("Collapse all", false);

    function revealCurrentSection() {
        if (!window.location.hash) {
            return;
        }
        const link = Array.from(toc.querySelectorAll("a")).find(function (candidate) {
            return candidate.hash === window.location.hash;
        });
        if (link) {
            branches.forEach(function (branch) {
                if (branch.item.contains(link)) {
                    branch.setExpanded(true);
                }
            });
        }
    }

    revealCurrentSection();
    window.addEventListener("hashchange", revealCurrentSection);
}());
