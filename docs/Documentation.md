---
layout: page
title: Documentation guide
---

This guide is for contributors updating PonHub's guides and maintainers checking the project website.
The [User Guide](UserGuide.html) explains supported user workflows; the [Developer Guide](DeveloperGuide.html)
explains design, maintenance and testing. Keep contribution-bearing content in `docs/UserGuide.md` and
`docs/DeveloperGuide.md`, preserving useful anchors and authorship/reuse attribution. Coordinate shared
sections with their owners instead of maintaining parallel copies of the same instructions.

## Build and preview locally

The site uses Jekyll with the dependencies in `docs/Gemfile` and `docs/Gemfile.lock`.
`docs/_config.yml` selects the Minima theme, Kramdown Markdown processor, navigation pages and `jemoji` plugin.
Install Ruby and the lockfile's Bundler version (`2.5.11`), following the
[GitHub Pages local setup guide](https://docs.github.com/en/pages/setting-up-a-github-pages-site-with-jekyll/testing-your-github-pages-site-locally-with-jekyll)
for platform prerequisites. From the repository root, use a macOS/Linux shell:

```sh
cd docs
gem install bundler -v 2.5.11
BUNDLE_FROZEN=true bundle _2.5.11_ install
bundle _2.5.11_ exec jekyll build
bundle _2.5.11_ exec jekyll serve --host 127.0.0.1 --baseurl ""
```

Open `http://127.0.0.1:4000/Documentation.html` and the pages you changed. Stop the server with Ctrl+C.
`build` writes `docs/_site/`; `serve` rebuilds the preview when content changes. Generated `_site` output
is ignored by Git and must not replace the Markdown sources. The local empty base URL is a preview option;
do not change the deployment configuration to match it. For native Windows site builds, the Gemfile also
requests `wdm`; resolve any platform/dependency mismatch explicitly rather than silently rewriting the
shared lockfile. A macOS/Linux preview avoids that Windows-specific dependency.

The frozen install fails if the Gemfile and lockfile disagree or required gems cannot be installed.
Report the actual error; dependency updates belong in a deliberate, separately reviewed change.
A Markdown editor preview can help while writing, but it does not verify Jekyll layouts, plugins or links.
After a build, run `python3 tools/check_site_links.py` from the repository root to check rendered local
targets/fragments, table column counts and current source-file links. If the build uses a different output
directory or base URL, pass `--site PATH` and `--baseurl VALUE`. The checker writes JSON to standard output;
redirect it outside the checkout when needed. It does not check external HTTP destinations or visual layout.

## Write and review an update

Apply the [Week 9 developer-documentation guidance](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week9/topics.html#w9-8-writing-developer-documents):
identify the reader's task, explain the overall behavior before component details, and use simple prose.
Put useful examples and diagrams beside the explanation they support. Describe rationale and contracts
that help a maintainer understand the code, and link to shared explanations instead of repeating them.
Describe current behavior and distinguish it from proposals. Keep development, testing and audit
records in issue descriptions/comments rather than repository files or published pages.

Before requesting review:

1. Source-check command examples, paths, supported behavior and failure/recovery claims against the intended build.
2. Preview each affected page at desktop and narrow widths. Check headings, tables, code blocks and navigation.
3. Follow affected internal links and fragments, including incoming links if an anchor changed. Check source links
   against the current team repository; update them when classes move.
4. Check diagram readability and agreement with the implementation. Store editable PlantUML in `docs/diagrams/`
   with the corresponding rendered asset in `docs/images/`, and update both when a diagram changes.
5. Keep detailed source/build/preview records in the corresponding issue description/comments. Include a concise
   validation summary and any outstanding checks in the PR.

For more detail, use the [Google developer documentation style guide](https://developers.google.com/style),
[SE-EDU Markdown conventions](https://se-education.org/guides/conventions/markdown.html),
[PlantUML guide](https://se-education.org/guides/tutorials/plantUml.html) and
[IntelliJ soft-wrapping guidance](https://se-education.org/guides/tutorials/intellijUsefulSettings.html#enabling-soft-wrapping).

## Maintain and publish the website

The team uses the existing GitHub Pages deployment from `master` and `/docs`; merged source updates are
published through that setup. The repository defines no automatic PR website-preview workflow. Build locally before
merge, then check the deployment result and the affected [public pages](https://AY2627S1-CS2103T-F13-3.github.io/tp/).
A successful Java CI run does not prove that the website built or that the rendered content is correct.
See the [SE-EDU Jekyll guide](https://se-education.org/guides/tutorials/jekyll.html) for Pages settings.

Site-wide changes belong in `docs/_config.yml`, `docs/_includes/`, `docs/_layouts/` or `docs/_sass/`, as appropriate.
The print stylesheet `docs/_sass/minima/_base.scss` already labels the product **PonHub**. Preserve correct
branding and upstream credits; inspect print preview if styling changes. To export a guide, follow the
[web-document PDF guide](https://se-education.org/guides/tutorials/savingPdf.html) and check page breaks,
tables, links and diagram legibility in the resulting PDF.
