import assert from "node:assert/strict";
import { copyFile, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";

const sourceRoot = path.resolve("src/main/resources/static/js");
const moduleRoot = path.join(tmpdir(), `landing-tarjetas-static-ui-${process.pid}`);

await rm(moduleRoot, { force: true, recursive: true });
await mkdir(moduleRoot, { recursive: true });
await writeFile(path.join(moduleRoot, "package.json"), JSON.stringify({ type: "module" }));

for (const fileName of ["api.js", "app.js", "categories.js", "dashboard.js", "statements.js", "transactions.js", "utils.js"]) {
    await copyFile(path.join(sourceRoot, fileName), path.join(moduleRoot, fileName));
}

try {
    const { setButtonBusy } = await import(pathToFileURL(path.join(moduleRoot, "utils.js")));
    const { monthTabLabel, visibleMonthTabs } = await import(pathToFileURL(path.join(moduleRoot, "dashboard.js")));

    const successButton = fakeButton("Save");
    setButtonBusy(successButton, true, "Saving...");
    assert.equal(successButton.textContent, "Saving...");
    assert.equal(successButton.disabled, true);
    assert.equal(successButton.attributes.get("aria-busy"), "true");
    setButtonBusy(successButton, false);
    assert.equal(successButton.textContent, "Save");
    assert.equal(successButton.disabled, false);
    assert.equal(successButton.attributes.has("aria-busy"), false);
    assert.equal("idleLabel" in successButton.dataset, false);

    const errorButton = fakeButton("Delete");
    setButtonBusy(errorButton, true, "Deleting...");
    setButtonBusy(errorButton, false);
    assert.equal(errorButton.textContent, "Delete");
    assert.equal(errorButton.disabled, false);
    assert.equal(errorButton.attributes.has("aria-busy"), false);

    const months = Array.from({ length: 10 }, (_, index) => ({
        yearMonth: `2026-${String(index + 1).padStart(2, "0")}`,
        currentReal: true,
        projectionOnly: false
    }));
    const visible = visibleMonthTabs("2026-10", months, 8);
    assert.equal(visible.length, 8);
    assert.equal(visible.some((month) => month.yearMonth === "2026-10"), true);

    assert.equal(monthTabLabel({ currentReal: true, hasProjectedData: true, projectionOnly: false }), ' <span class="tab-label">Actual</span>');
    assert.equal(monthTabLabel({ currentReal: false, hasProjectedData: true, projectionOnly: true }), ' <span class="tab-label">Projection</span>');
    assert.equal(monthTabLabel({ currentReal: false, hasProjectedData: true, projectionOnly: false }), "");
} finally {
    await rm(moduleRoot, { force: true, recursive: true });
}

function fakeButton(textContent) {
    const attributes = new Map();
    return {
        attributes,
        dataset: {},
        disabled: false,
        textContent,
        removeAttribute(name) {
            attributes.delete(name);
        },
        setAttribute(name, value) {
            attributes.set(name, value);
        }
    };
}
