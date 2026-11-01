# Guide command-list popover checks

Build the site with the frozen `docs/Gemfile.lock` workflow, then serve the generated directory. Run this
browser check against its actual base URL. It verifies command contents, all five trigger locations,
keyboard/touch activation, dismissal/focus, viewport bounds, print CSS/events, and both no-JavaScript and
simulated unsupported-API fallbacks. It does not claim acceptance in other browser engines.

The application does not acquire a JavaScript dependency. For an isolated POSIX test installation:

```sh
browser_check_root="$(mktemp -d)"
npm install --prefix "$browser_check_root" playwright@1.62.1
"$browser_check_root/node_modules/.bin/playwright" install chromium
NODE_PATH="$browser_check_root/node_modules" node tools/manual-checks/command-list-popovers/check.cjs http://127.0.0.1:4000/tp/
```

Optionally set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to an installed Chromium/Chrome executable, and
`GUIDE_BROWSER_EVIDENCE_DIR` to a directory for screenshots. This suite is separate from the Java tests.
For release review, also inspect an actual print/PDF export and confirm every command group is present.
