// Exercise actual guide pages with Playwright; see README.md for isolated setup.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

const base = process.argv[2];
if (!base) {
    throw new Error("Usage: node check.cjs http://127.0.0.1:4000/tp/");
}
const expected = {
    "operational-mutations": ["add", "edit", "delete confirm", "addlesson", "editlesson",
        "deletelesson", "enrol", "unenrol", "mark", "unmark"],
    "student-position-examples": ["enrol", "mark", "lessons", "history"],
    "lesson-retrieval": ["lessons", "lessons STUDENT_INDEX", "history STUDENT_INDEX",
        "showlesson", "search c/lesson"],
    "record-validation": ["add", "edit", "addlesson", "editlesson", "mark", "unmark"]
};
const pages = [
    { file: "DeveloperGuide.html", groups: ["operational-mutations", "student-position-examples"], triggers: 3 },
    { file: "UserGuide.html", groups: ["lesson-retrieval", "record-validation"], triggers: 2 }
];
const evidence = process.env.GUIDE_BROWSER_EVIDENCE_DIR;
if (evidence) fs.mkdirSync(evidence, { recursive: true });

async function checkContents(page, config) {
    for (const key of config.groups) {
        const commands = await page.locator("#command-list-" + key + " code").allTextContents();
        assert.deepEqual(commands.sort(), [...expected[key]].sort(), key + " command contents");
    }
}

async function waitForState(page, id, open) {
    await page.waitForFunction(({ id, open }) => {
        const panel = document.getElementById(id);
        const visible = panel.matches(":popover-open");
        const controls = Array.from(document.querySelectorAll("button[popovertarget]"))
            .filter(button => button.getAttribute("aria-controls") === id);
        return visible === open && controls.every(button => button.getAttribute("aria-expanded") === String(open));
    }, { id, open });
}

(async function () {
    const browser = await chromium.launch({
        executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined,
        headless: true
    });
    const report = [];
    try {
        for (const config of pages) {
            const url = new URL(config.file, base.endsWith("/") ? base : base + "/").href;
            for (const width of [1440, 390, 320]) {
                const context = await browser.newContext({
                    viewport: { width, height: 1000 },
                    hasTouch: width === 390,
                    isMobile: width === 390
                });
                const page = await context.newPage();
                const errors = [];
                page.on("pageerror", error => errors.push(error.message));
                await page.goto(url);
                await checkContents(page, config);
                const triggers = page.locator("button.command-list-trigger");
                assert.equal(await triggers.count(), config.triggers);
                assert.equal(await page.locator(".command-list-panel[popover]:visible").count(), 0);
                for (let index = 0; index < config.triggers; index++) {
                    const trigger = triggers.nth(index);
                    const id = await trigger.getAttribute("aria-controls");
                    await trigger.scrollIntoViewIfNeeded();
                    if (width === 390) await trigger.tap();
                    else await trigger.click();
                    await waitForState(page, id, true);
                    const panel = page.locator("#" + id);
                    const bounds = await panel.evaluate(element => {
                        const r = element.getBoundingClientRect();
                        return { left: r.left, top: r.top, right: r.right, bottom: r.bottom,
                            width: innerWidth, height: innerHeight,
                            pageWidth: document.documentElement.scrollWidth };
                    });
                    assert(bounds.left >= 0 && bounds.top >= 0 && bounds.right <= bounds.width + 1
                        && bounds.bottom <= bounds.height + 1, JSON.stringify(bounds));
                    assert(bounds.pageWidth <= bounds.width, "Page-wide horizontal overflow");
                    if (evidence && index === 0) {
                        await page.screenshot({ path: path.join(evidence, config.file + "-" + width + ".png") });
                    }
                    await page.keyboard.press("Escape");
                    await waitForState(page, id, false);
                    assert(await trigger.evaluate(element => element === document.activeElement));
                    await trigger.focus();
                    await page.keyboard.press("Enter");
                    await waitForState(page, id, true);
                    await page.keyboard.press("Tab");
                    assert(await panel.locator(".command-list-close")
                        .evaluate(element => element === document.activeElement), "Tab must reach Close");
                    await panel.locator(".command-list-close").click();
                    await waitForState(page, id, false);
                    assert(await trigger.evaluate(element => element === document.activeElement));
                    await page.keyboard.press("Space");
                    await waitForState(page, id, true);
                    await page.mouse.click(4, 4);
                    await waitForState(page, id, false);
                }
                await page.emulateMedia({ media: "print" });
                for (const key of config.groups) {
                    const panel = page.locator("#command-list-" + key);
                    assert(await panel.isVisible(), "Closed group must be available in print CSS");
                    assert.equal(await panel.evaluate(element => getComputedStyle(element).position), "static");
                    assert.equal(await panel.locator(".command-list-close").isVisible(), false);
                }
                await page.emulateMedia({ media: "screen" });
                const first = triggers.first();
                const firstId = await first.getAttribute("aria-controls");
                await first.click();
                await waitForState(page, firstId, true);
                await page.evaluate(() => window.dispatchEvent(new Event("beforeprint")));
                assert.equal(await page.locator(".command-list-panel[popover]").count(), 0);
                await checkContents(page, config);
                await page.evaluate(() => window.dispatchEvent(new Event("afterprint")));
                await waitForState(page, firstId, true);
                await page.keyboard.press("Escape");
                assert.deepEqual(errors, []);
                report.push({ page: config.file, width, enhanced: true, commands: true,
                    keyboard: true, dismissal: true, print: true, errors: 0 });
                await context.close();
            }
            for (const fallback of ["no-javascript", "unsupported-api", "unsupported-controls"]) {
                const context = await browser.newContext({
                    viewport: { width: 390, height: 844 }, javaScriptEnabled: fallback !== "no-javascript"
                });
                if (fallback === "unsupported-api") {
                    await context.addInitScript(() => { delete HTMLElement.prototype.popover; });
                }
                if (fallback === "unsupported-controls") {
                    await context.addInitScript(() => { delete HTMLButtonElement.prototype.popoverTargetElement; });
                }
                const page = await context.newPage();
                await page.goto(url);
                await checkContents(page, config);
                assert.equal(await page.locator("a.command-list-trigger").count(), config.triggers);
                assert.equal(await page.locator("button.command-list-trigger").count(), 0);
                const link = page.locator("a.command-list-trigger").first();
                const target = await link.getAttribute("href");
                await link.click();
                assert(new URL(page.url()).hash === target, "Fallback link target");
                assert(await page.locator(target).isVisible(), "Fallback group visible");
                report.push({ page: config.file, fallback, commands: true, anchor: true });
                await context.close();
            }
        }
        console.log(JSON.stringify({ browser: browser.version(), results: report }, null, 2));
    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
