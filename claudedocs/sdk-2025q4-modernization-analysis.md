# Handy Export Jar — Modernization Analysis (IDEA 2025.3 / Q4 2025)

**Plugin:** `org.yanhuang.plugins.intellij.exportjar` ("Handy Export Jar"), version 2.5.5
**Target platform:** IntelliJ IDEA 2025.3 (build branch `253`, Java 21 / JBR 21)
**Report date:** 2026-05-31
**Scope:** Six modernization dimensions — D1 reflection removal, D2 UI-workaround replacement, D3 SDK upgrade, D4 build-SDK upgrade, D5 Gradle/plugin upgrades, D6 test expansion.

---

## 1. Executive Summary

### Current state

The plugin exports selected project files or VCS local-changes into a JAR. It works today against IDEA 2025.1 (`platformVersion=2025.1`, `platformType=IC`, `pluginSinceBuild=202`), but it leans heavily on reflection and platform-internal APIs to do so. There are **9 reflection sites** across 4 files and **12 distinct UI workarounds**, most of which are symptoms of two architectural choices: subclassing platform dialogs and tearing Swing panels out of them, and re-implementing the VCS changes tree by reaching into internal node/builder classes.

The build toolchain is one to several minor versions behind across the board (Gradle 8.13, IntelliJ Platform Gradle Plugin 2.5.0, Kotlin 2.1.20). Test coverage is effectively limited to one class (`CommonUtilsTest`, 5 methods); two test stubs are empty.

### Target

Reach IntelliJ IDEA 2025.3 (build `253`), eliminate reflection, confine the irreducible internal-API surface to a small number of owned adapter classes, modernize the build, and establish a real test suite for the deterministic core.

### Top risks (full register in §6)

1. **`intellijIdeaCommunity()` / `IC-2025.3` no longer resolves.** Starting 2025.3, JetBrains ships a unified distribution; the build must switch to the `intellijIdea("2025.3")` helper. This is a BLOCKING build-config change, not a version-string bump (JetBrains 2025.3 platform blog).
2. **VCS classes moved to separately-classloaded modules.** `intellij.platform.vcs.impl` and `intellij.platform.vcs.dvcs` now require explicit `bundledModule(...)` declarations or the plugin will not compile (JetBrains 2025.3 platform blog; api-changes-list-2025).
3. **`SingleChangeListCommitWorkflow` is `@ApiStatus.Internal`** — its constructor signature has changed twice and is not covered by the official breaking-change pages, so it can shift silently between releases. The reflective 3-signature fallback in `WorkflowHelper.kt` cannot be verified against 2025.3 from source alone; it needs a runtime check (intellij-community master).
4. **2025.1/2025.3 threading changes** (no auto write-intent lock on `invokeLater`; some write actions moved off-EDT) make the borrowed-commit-dialog lifecycle reflection in `LocalChangesDialogProvider` the highest-risk runtime site (api-notable-list-2025).

### Rough effort

| Dimension | Effort | Note |
|---|---|---|
| D5 Gradle + plugins | S–M | Mechanical version bumps, two cleanups |
| D4 build SDK (intellij.platform) | S | Coupled to Gradle ceiling (see §3) |
| D3 SDK 2025.3 | M | Build-config rewrite + verify/runtime gates |
| D1 reflection removal | M–L | One L-effort site (FileListDialog); rest collapse into one refactor |
| D2 UI extensions | L | The bulk; ~5 new owned classes, several deletions |
| D6 tests | M | Phase 1 pure-logic tests are high value/low cost |

D1 and D2 overlap substantially — the same refactor that removes reflection sites 2–9 also removes UI workarounds W5/W6/W12. Treating them as one body of work is more accurate than summing them.

---

## 2. Per-Dimension Analysis

### D5 — Gradle and Gradle-plugin upgrades

The decisive constraint is a version-coupling cliff: **IntelliJ Platform Gradle Plugin 2.12+ requires Gradle 9.0+**, and Gradle 9.0 requires the daemon JVM to be Java 17+ (satisfied by the existing `jvmToolchain(21)`). There is no intermediate path — either stay on Gradle 8.x with intellij.platform ≤ 2.11.x, or jump to Gradle 9 + intellij.platform 2.12–2.16 together.

**Recommendation: stay below the Gradle 9 boundary for this milestone.** Target Gradle **8.14.2** (last 8.x) with intellij.platform **2.11.0** (last release supporting Gradle 8.x — *UNVERIFIED, confirm against the plugin's GitHub releases before applying*). The Gradle 9 + intellij.platform 2.16.0 jump is a separate, higher-risk follow-on task.

| Tool | Current | Target | Source / note |
|---|---|---|---|
| Gradle wrapper | 8.13 | **8.14.2** | docs.gradle.org 8.14.x release notes; no breaking changes for plugin builds |
| org.jetbrains.intellij.platform | 2.5.0 | **2.11.0** (this milestone); 2.10.4 is the floor required by 2025.3 | plugins.gradle.org; JetBrains 2025.3 blog (min 2.10.4) |
| org.jetbrains.kotlin.jvm | 2.1.20 | **2.3.20** | kotlinlang.org; K2 default since 2.2.0, no DSL change |
| org.jetbrains.changelog | 2.2.1 | **2.5.0** | plugins.gradle.org; drop-in |
| org.jetbrains.kotlinx.kover | 0.9.1 | **0.9.8** | plugins.gradle.org; drop-in within 0.9.x |
| org.jetbrains.qodana | 2024.3.4 | **2025.3.2** | plugins.gradle.org; version scheme now matches IDE train |
| foojay-resolver-convention | 0.9.0 | **1.0.0** | plugins.gradle.org; 1.0.0 needs Java 17+ to run (satisfied) |
| junit | 4.13.2 | 4.13.2 (no change) | JUnit 4 EOL; engine decision in D6 |
| opentest4j | 1.3.0 | 1.3.0 (no change) | already latest |

**Cleanups folded in:** remove the deprecated `org.gradle.unsafe.configuration-cache` alias (`gradle.properties:53`, redundant with the stable key on line 68) and the `systemProp.org.gradle.unsafe.kotlin.assignment` line (`:74`, a no-op in Gradle 8.14+). Delete the dead commented `testIdeUi` block (`build.gradle.kts:242–249`).

**DSL impact:** none within intellij.platform 2.5→2.11; the `intellijPlatform { }`, `bundledModules()`, `testFramework()`, `pluginVerifier()`, `zipSigner()`, `kover { }`, and `changelog { }` blocks are all stable across this range. K2 may emit deprecation warnings on first build (warnings only).

### D4 — Build SDK note

D4 is effectively absorbed into D5 above: the IntelliJ Platform Gradle Plugin version is the one D4 lever, and its ceiling is dictated by the Gradle-9 coupling. The minimum acceptable version for targeting 2025.3 is **2.10.4** (JetBrains 2025.3 blog); the recommended value for this milestone is **2.11.0** to stay on Gradle 8.x. Reaching the absolute latest **2.16.0** is deferred with the Gradle 9 jump.

---

### D3 — SDK upgrade to IDEA 2025.3

This is the pivot dimension: it forces concrete decisions in D1 (which reflection branches survive), D2 (which UI paths must change), and D6 (test ordering and the VCS canary). It is verified against current (May 2026) JetBrains sources.

**Verified facts:**

| Fact | Value | Source |
|---|---|---|
| 2025.3 build prefix | `253` | build-number-ranges |
| Java / JBR for 2025.3 | Java 21 (JBR 21) | build-number-ranges |
| Java floor by branch | 222→11, 233/241→17, 242/251/253→21 | build-number-ranges |
| `IC` product code for 2025.3 | **dropped** — `IC-2025.3`, `intellijIdeaCommunity()` error | JetBrains 2025.3 blog |
| Correct dependency helper | `intellijIdea("2025.3")` (unified) | JetBrains 2025.3 blog |
| Min intellij.platform plugin | 2.10.4 | JetBrains 2025.3 blog |
| VCS modules need explicit `bundledModule(...)` | yes (own classloaders) | JetBrains 2025.3 blog; api-changes-list-2025 |
| `@Storage` top-level ban (2025.3) | **no impact** — zero `@Storage`/`@State`/`PersistentStateComponent` usages in this plugin | local grep |

**The key correction:** this is not an "IC→IU" swap. `platformType=IC` with `create("IC","2025.3")` fails to resolve, and `create("IU",...)` maps to Ultimate, which also fails for the unified build. The only correct path is the dedicated `intellijIdea(version)` helper. Note the side effect: `intellijIdea("<old version>")` does not resolve for pre-2025.3, so local builds against an older SDK would require reverting the helper. That lock-in is acceptable since the goal is to target 2025.3.

**Required `build.gradle.kts` changes:**
- Replace `create(properties("platformType"), properties("platformVersion"))` with `intellijIdea(properties("platformVersion"))`.
- Remove the `is20242OrLater` conditional (lines 93–112) — with `platformVersion` pinned to 2025.3 it is always `true`. Replace with unconditional `bundledModules(properties("platformBundledModules").map { it.split(',') })`.
- `sinceBuild`/`untilBuild` already read from properties; no code change, but `pluginUntilBuild` must be defined (currently absent).

**Required `gradle.properties` changes:**
- `platformVersion = 2025.3`; `platformType = IU` (now informational only).
- `platformBundledModules = intellij.platform.vcs.impl,intellij.platform.vcs.dvcs` — **note the naming change** from the current `...vcs.dvcs.impl`. The blog lists `intellij.platform.vcs.dvcs`. *UNVERIFIED — confirm the exact jar names in `<sdk>/lib/modules/` at the first compile checkpoint.*
- Add `pluginUntilBuild = 253.*` (referenced by `build.gradle.kts:156` but currently undefined; Marketplace ignores `until-build` for 243+ but it is valid local documentation).
- `verifyPluginUseIdes = IC-2023.3,IC-2024.2,IC-2025.2,IC-2025.3` — the open-source verifier still uses the `IC` coordinate even though the compile dependency uses `intellijIdea()`. *Validate `IC-2025.3` resolvability at the verify gate; cap at `IC-2025.2` if the feed lags.*

**Java level decision (resolves a cross-dimension question).** Two independent settings:
- **Toolchain** `jvmToolchain(21)`: must be 21 (compiling against the 2025.3 Java-21 classpath). Already set, no decision.
- **Bytecode target** `javaVersion`: must be ≤ the Java runtime of the oldest supported IDE, i.e. the `pluginSinceBuild` floor.

| Floor | Min IDE Java | Max `javaVersion` | Effect |
|---|---|---|---|
| 202 (status quo) | 11 | 11 | keeps all pre-2022 reflection branches |
| **233 (recommended)** | 17 | **17** | retires `reflectBeforeVer2022` + `reflectAfterVer2022` in WorkflowHelper — a concrete D1 win |
| 242 | 21 | 21 | most modern; also drops the 2022-era branch |

**Recommendation: floor = `233`, `javaVersion = 17`.** It follows JetBrains' "current − few" support guidance, raises bytecode to 17, and immediately deletes two of the three reflective constructor branches. The conservative fallback (floor `202`, `javaVersion 11`) is fully viable — the 2025.3 SDK compiles to Java 11 bytecode — and drops no source, but it preserves the pre-2022 reflection. Given the project goal explicitly permits dropping unsupported features, `233`/`17` is the better target.

**Source-level risks the 2025.3 classpath introduces** (these feed D1/D2; none are hard compile breaks beyond the build-config items, because the SDK still ships the classes):

| Site | 2025.x change | Forces |
|---|---|---|
| `SingleChangeListCommitWorkflow` ctor (WorkflowHelper.kt) | `@Internal`; 2025+ 6-arg branch must match real 253 signature, not in breaking-change pages | D1 — verify at runtime |
| `CommitChangeListDialog` lifecycle reflection (LocalChangesDialogProvider) | class present but in extracted classloader; off-EDT/write-intent threading changes | D1+D2 — highest runtime risk |
| `SelectFilesDialog` private `tree` field injection | `@Internal`, in `vcs.impl` module | D2 |
| `ChangeListManagerImpl` cast (VcsHelper:37) | dead once floor ≥ 203 | D1 — use public `getUnversionedFilesPaths()` |
| `BackgroundableProcessIndicator`, `EDT.isCurrentThreadEdt()`, `LangDataKeys`, `CompilerPaths.getModuleOutputPath`, `disposeIfNeeded()` | impl/deprecated | D1 — public replacements exist (see D1) |
| bundled ASM `Opcodes.API_VERSION` reflection | field name stable; low risk | D1 — pure deletion |

**Ordered upgrade sequence (compile/verify gate after each), on a `feature/sdk-2025.3` branch:**
1. Bump intellij.platform to 2.10.4+. Gate: `./gradlew help` resolves the plugin.
2. Switch to `intellijIdea("2025.3")` + set properties. Gate: `./gradlew dependencies --configuration intellijPlatformDependency` resolves `idea:idea:2025.3` (the single most likely failure point).
3. Declare VCS bundled modules + remove `is20242OrLater`. Gate: inspect `lib/modules/` for exact jar names, then `compileJava compileKotlin`.
4. Set floor + bytecode (`233`/`17`) + `pluginUntilBuild`. Gate: `clean buildPlugin`, confirm `<idea-version since-build="233" until-build="253.*">` in the built `plugin.xml`.
5. (If floor ≥ 233) prune WorkflowHelper's two older branches. Gate: `compileKotlin buildPlugin`.
6. Update verifier matrix; run `verifyPlugin -i`. Expect many internal-API warnings; `failureLevel=NONE` keeps them non-blocking — triage as the D1/D2 backlog.
7. **Runtime smoke test** via `runIde`: exercise both export paths. This is where the `@Internal` reflection sites surface if 253 signatures shifted — the verifier cannot catch these.
8. CI confirm (already JDK 21).

Risk-ranked, steps 2 (artifact resolution) and 7 (runtime reflection against internal VCS API) are the genuine blockers; steps 3–6 are mechanical.

---

### D1 — Replace reflection with platform API

There are 9 reflection sites across 4 files. The central structural finding: **sites 2–5 (LocalChangesDialogProvider lifecycle) and sites 7–9 (WorkflowHelper constructors) are one problem.** WorkflowHelper exists only to feed `SingleChangeListCommitWorkflow` into `new DefaultCommitChangeListDialog(workflow)`, whose browser is then borrowed and hand-initialized via reflection. Constructing the browser directly eliminates all six sites and deletes `WorkflowHelper.kt`.

| # | Site (file:line) | Target | Replacement | Effort | Risk after |
|---|---|---|---|---|---|
| 1 | CommonUtils.java:53–59 | ASM `Opcodes.API_VERSION` | Delete probe (dead code — fallback is direct access); inline `Opcodes.API_VERSION` (or `ASM9`) at the call site | S | Minimal |
| 2 | LocalChangesDialogProvider.java:70–73 | `CommitChangeListDialog.beforeInit()` | Removed — `MultipleLocalChangeListsBrowser` self-inits in its constructor | M | Medium |
| 3 | LocalChangesDialogProvider.java:77–80 | `DialogWrapper.init()` | Removed (primary); or a subclass calls the `protected init()` directly (fallback) | M | Medium |
| 4 | LocalChangesDialogProvider.java:84–87 | `CommitChangeListDialog.afterInit()` | Removed (primary); subclass-call-or-omit (fallback) | M | Medium |
| 5 | LocalChangesDialogProvider.java:97–100 | `getBrowserBottomPanel()` | Drop the status strip, or rebuild as a public `JBLabel` + inclusion listener | S | Low |
| 6 | FileListDialog.java:65–90 | `SelectFilesDialog` private tree field | Extend `DialogWrapper` directly, host `FileListTree` (already public `VirtualFileList` ctor) | L | Medium |
| 7 | WorkflowHelper.kt:42–52 | `SingleChangeListCommitWorkflow` ctor (2025) | Delete WorkflowHelper (primary); single direct ctor call (fallback) | M | Medium |
| 8 | WorkflowHelper.kt:61–74 | ctor (2022) | Delete (drop pre-2025) | — | — |
| 9 | WorkflowHelper.kt:83–97 | ctor (pre-2022) | Delete (drop pre-2025) | — | — |

**Site 1** is the clearest win: the `catch` fallback already does `versionOpcodes = Opcodes.API_VERSION` — direct access to the same `public static final int`. The probe never had a reachable failure mode on any 2020.2+ platform. Pure deletion.

**Sites 2–5 + 7–9 (the cluster).** `getBrowser()` already returns a `MultipleLocalChangeListsBrowser` (the code's own comment names it). That class is directly constructible:

```java
var browser = new MultipleLocalChangeListsBrowser(project,
        /*showChangelistChooser*/ false, /*enableUnversioned*/ true, /*enablePartialCommit*/ false);
browser.getViewer().rebuildTree();
browser.getViewer().getInclusionModel().clearInclusion();
browser.getViewer().getInclusionModel().addInclusion(includeChanges);
return JBUI.Panels.simplePanel().addToCenter(browser);
```

Self-initialization removes sites 2–4; `DefaultCommitChangeListDialog`/`SingleChangeListCommitWorkflow` are no longer referenced, so `WorkflowHelper.kt` (7–9) is deleted; `dispose()` becomes `Disposer.dispose(browser)`. We trade *reflection on internal* for *compile-visible use of internal* — strictly better (fails at build, not silently at runtime). It requires the `bundledModule("intellij.platform.vcs.impl")` declaration from D3. *UNVERIFIED: exact `MultipleLocalChangeListsBrowser` ctor arity for 253; `getDisplayedUnversionedFiles()`/`getIncludedUnversionedFiles()` names.* The fallback (keep the dialog, subclass it to call `protected init()`, single direct workflow ctor) still removes 100% of reflection.

**Site 6 (highest brittleness, the L-effort piece).** `SelectFilesDialog` exposes no constructor parameter, setter, or factory for a custom tree — hence the field injection. The recommended fix is to **stop subclassing `SelectFilesDialog`** and instead host the already-custom `FileListTree` (which extends `VirtualFileList` via its public ctor) inside the plugin's own `DialogWrapper`, building the toolbar via the public `ActionManager.createActionToolbar`. The plugin already overrides `createCenterPanel`, `createToolbarActions`, and `getFileList`, so most of `SelectFilesDialog`'s thin surface is re-implemented anyway. This converts a runtime identity-match-and-overwrite into a compile-checked field. *UNVERIFIED: `getSelectedFiles()` inclusion-vs-selection semantics must be diffed against `SelectFilesDialog` in 253; `VirtualFileList` package location.* Contained fallback: attempt the public path, retain field injection in a logged `catch` as degraded mode.

**Net outcome:** all 9 sites removed. Sites 1 and 5 are pure deletions; 2–4 + 7–9 collapse into one M-effort refactor that deletes `WorkflowHelper.kt`; site 6 is the one substantial L piece and the highest-value fix.

Other deprecated/internal symbols flagged in D3 that D1 should also fix while in these files (public replacements all exist): `BackgroundableProcessIndicator` → `ProgressManager.run(task)`; `EDT.isCurrentThreadEdt()` → `ApplicationManager.getApplication().isDispatchThread()`; `LangDataKeys.MODULE[_CONTEXT_ARRAY]` → `PlatformCoreDataKeys.*`; `CompilerPaths.getModuleOutputPath` → `CompilerModuleExtension.getCompilerOutputPath()`; `ChangeListManagerImpl` cast → public `ChangeListManager.getUnversionedFilesPaths()` (once floor ≥ 203); `disposeIfNeeded()` → `Disposer.dispose(...)`.

---

### D2 — Replace UI workarounds with platform extensions

The 12 workarounds are symptoms of two architectural decisions: **subclass-and-extract** (subclass a platform dialog, then tear a Swing panel out of it) and **re-implement the changes tree** (override platform internals instead of owning a tree). The target architecture is **composition**: own one `ChangesTree`, feed it from two model sources, and assemble plain `JComponent`s into a standard `DialogWrapper`.

**Target component model:**

```
ExportSettingsPanel       plain JComponent (Kotlin UI DSL) — jar combo, option checkboxes, template row;
                          reused by both the dialog and the commit session
ExportChangesTree         OWNED subclass of ChangesTree (not injected); one rebuildTree() signature
  └─ ChangesTreeModelSource (sealed): FlatFiles(files) | LocalChanges(ChangeListManager)
IncludeExcludeController  was FileListTreeHandler; node-subtype access funnelled through one NodeFileAdapter
ExportNodeRenderer        was FileListTreeCellRender; owns a ChangesBrowserNodeRenderer delegate
ExportSettingDialog       extends DialogWrapper; createCenterPanel() -> JBSplitter; createActions() -> standard
ExportCommitExecutor      LocalCommitExecutor (kept — real EP)
  └─ ExportCommitSession.getAdditionalConfigurationUI() -> ExportSettingsPanel only
```

**Delete:** `WorkflowHelper.kt`, `LocalChangesDialogProvider.java`, `LocalChangesSettingDialog.java`, `FileListTreeGroupPolicyFactory.java`, `VcsHelper.treeModelFromLocalChanges` (W12 — already dead, unreferenced), `SettingDialog.form`.

| ID | Workarounds | Target | Effort | Risk after | Depends on |
|---|---|---|---|---|---|
| D2-1 | W1 (`.form` + `getPeer().setContentPane()`) | Kotlin UI DSL `panel { }` + `createCenterPanel()`/`createActions()` | M | Low-Med | — |
| D2-2 | W2, W3 (`SelectFilesDialog` injection + `buildTreeModel` overloads) | Owned `ChangesTree` subclass, single `rebuildTree()` | M | Med | — |
| D2-3 | W4 (dead grouping factory) | Delete | S | Low | D2-2 |
| D2-4 | W5, W6, W12 (commit-dialog borrow + workflow reflection) | Own tree from `ChangeListManager`; delete WorkflowHelper | M-L | Med (was Very High) | D2-2 |
| D2-5 | W7 (`ChangeListManagerImpl` cast) | Public `getUnversionedFilesPaths()` | S | Low | D3 floor ≥ 203 |
| D2-6 | W8 (panel extraction in CommitSession) | Settings-only DSL panel; files from the passed `Change` collection | M | Low | D2-1 |
| D2-7 | W9 (LocalChangesSettingDialog subclass hook) | Single parameterized dialog (model source) | S-M | Low | D2-1, D2-4 |
| D2-8 | W10 (renderer child-scan) | Owned `ChangesBrowserNodeRenderer` delegate via `setCellRenderer` | M | Med | D2-2 |
| D2-9 | W11 (missing `getActionUpdateThread()`) | Override returning EDT or BGT (classified per action) | S | Low | — |
| D2-10 | FileListTreeHandler node access | `NodeFileAdapter.virtualFileOf(node)` isolation | M | Med | D2-2 |
| D2-11 | ExportCommitExecutor / non-modal commit UI | Keep EP; verify on running 2025.3 | S(+M) | Med (verify) | D2-4 |

**Notable points:**
- **D2-4 is the highest-value de-risk** (Very High → Medium) and the key non-modal-commit enabler. The non-modal commit tool window (default since ~2021) has **no `CommitChangeListDialog` to borrow**; the current code only works because it builds a throwaway modal dialog regardless of the user's commit-UI setting. Building the tree from `ChangeListManager` directly makes the export independent of commit-UI mode. `ExportLocalChangesAction.simpleVcsExport()` (currently unused) already demonstrates this cleaner path.
- **D2-9 must classify each action**, not blanket-apply BGT. Registered actions (`ExportJarAction`, `ExportLocalChangesAction`, etc.) read only the data context → **BGT**. Toolbar tree actions in `FileListActions` read live Swing tree state → **EDT**. The removed `OLD_EDT` default makes the override mandatory.
- **Fate of `SettingDialog.form`: migrate to Kotlin UI DSL, do not keep the `.form`.** The form is the direct cause of W1 — it generates its own OK/Cancel row, forcing the internal `getPeer().setContentPane()` and a `null` `createCenterPanel()`. The form already fights the runtime code (`updateFileListSettingSplitPanel()` re-parents form cells into a splitter at runtime), so its WYSIWYG benefit is already lost. DSL is M-effort pure churn with no user-visible feature gain, justified by removing the peer hack and regaining the standard lifecycle (button bar, validation, error text). Minimal fallback: strip the button row from the `.form`, return `contentPane` from `createCenterPanel()`, use `createActions()` (effort S-M) — removes the violation but defers the rewrite.

**Honest accounting of residual internal API.** Composition removes all reflection, field injection, dialog/workflow borrowing, and platform-dialog subclassing. It cannot remove the dependency on `ChangesTree` / `TreeModelBuilder` / `ChangesBrowserNode` / `ChangesBrowserNodeRenderer` / `ChangesGroupingSupport` themselves — these are `@ApiStatus.Internal` with no public replacement for a rich grouped checkbox file tree. The achievable goal is to **confine that surface to ~5 owned classes via stable builder/setter APIs**, so a future SDK break is localized and diagnosable instead of silently swallowed by `catch (Throwable)`.

**Recommended internal sub-sequence:** Phase A (D2-9, D2-5, D2-3 — low-risk groundwork) → Phase B (D2-2, then D2-8/D2-10 on top) → Phase C (D2-4 — delete WorkflowHelper + commit reflection) → Phase D (D2-1 → D2-7 → D2-6 → D2-11 verification on a running 2025.3 IDE).

---

### D6 — Test expansion

Today only `CommonUtilsTest` (5 methods, `BasePlatformTestCase`) is real; `JVcsPadTest.java` and `KVcsPadTest.kt` are empty stubs (delete them). The build uses `testFramework(TestFrameworkType.Platform)` (JUnit4) with `junit:4.13.2` + `opentest4j:1.3.0` already on the test classpath.

**Two corrections verified by reading the code:**
- `CommonUtils.createNewJar` only touches `project` inside `if (vf != null)`; passing an empty `filePathVfMap` makes `vf` always null, so it is **plain-JUnit4 testable with `project = null`** — no platform fixture. This is the core JAR-writing path.
- `CommonUtils.findOffspringClassName` uses only classpath ASM classes (no running `Application`), so it is **plain-JUnit4 testable** if real `.class` files exist; generate them at test time via `javax.tools.ToolProvider.getSystemJavaCompiler()` rather than committing binaries.

**Path-injection blocker:** `Constants.cachePath`/`historyFilePath2023` are `static final` interface fields derived from `user.home`. `HistoryDao` and `UpgradeManager` read them directly and so are **not testable without a small refactor**: add `HistoryDao(Path cacheRoot)` (default ctor delegates with `Constants.cachePath`) and a path-parameterized overload of `UpgradeManager.migrateHistoryToV2023`. Both are low-risk and independently useful.

**Engine decision (resolves the cross-dimension JUnit question): stay on JUnit4 / `TestFrameworkType.Platform`.** `BasePlatformTestCase` is JUnit3/4-based; the plugin's JUnit5 path does not provide it and would require rewriting every fixture test around `@TestApplication` extensions — real cost, no payoff for this plugin. Plain-logic tests run fine as POJO JUnit4 `@Test` classes on the same engine. The earlier research note that "JUnit 4 is EOL, migrate to 5" is acknowledged but **declined for this codebase** because of the `BasePlatformTestCase` dependency. If a project-wide JUnit5 mandate ever lands, the fallback is a hybrid (fixture tests on Platform/JUnit4 + `junit-jupiter` and `junit-vintage-engine` for POJO tests) — more expensive, only if mandated.

**Phase 1 — pure JUnit4 (fast, highest value/effort, run first):** `SimpleFileLockTest`, `ExportJarInfoTest`, `SettingTemplateTest`, `SettingSelectFileTest` (the non-trivial include/exclude priority rules — highest-value pure logic), `HistoryDataTest`, `CommonUtilsJsonTest` (Gson round-trip + transient-field absence), `CommonUtilsCreateJarTest` (reopen with `JarFile`, assert entries/bytes/dir-entries/manifest/timestamps), `JarEntryAssemblerTest` (depends on extracting `ExportPacker`'s private entry-assembly methods into a package-private helper), `CommonUtilsOffspringClassTest` (runtime-compiled `.class` fixtures). Delete the empty stubs here.

**Phase 2 — `BasePlatformTestCase`:** `CommonUtilsExportFilesTest` (test-source-root gating, compilable-resource exclusion, nested `webapp`), `FindClassNameDefineInTest` (PSI), `FileListTreeHandler` static utilities (`collectVirtualFilesInTree`, `updateUIFileListTreeGrouping`).

**Phase 3 — persistence (after the injection refactor):** `HistoryDaoTest` (readOrDefault, merge/sort/truncate, removeTemplate, v2024 round-trip, v2023 migration fallback, UI sizes), `UpgradeManagerTest` (`BasePlatformTestCase`, since it calls `MessagesUtils.info`).

**Phase 4 — VCS canary + manual checklist:** `WorkflowHelperTest` is a high-value canary — it asserts a non-null workflow on the current SDK and reveals which reflective branch fires, catching a 2025.3 break across all three branches. (If D1/D2 deletes WorkflowHelper, this canary instead targets whatever replaces it.) The reflective/dialog UI sites (LocalChangesDialogProvider, FileListDialog injection, SettingDialog, ExportCommitSession) are **manual `runIde` only** — a short smoke checklist (export selected files; export with `add_directory`; export local changes; save/load template; migrate old `select_history.json`), run after the D3 bump where regressions actually surface.

**Coverage targets (Kover, line):** `model/**` + `utils` logic + `HistoryData` + `HistoryDao` **≥ 80%**; extracted `ExportPacker` helper **≥ 70%**; `ui/**` and `changes/**` no target (dialog/VCS glue). Overall plugin coverage will realistically land **~45–55%** — expected given how much is UI glue. Keep Kover **report-only** initially; add a per-package gate for `model`/`utils` only once green, to avoid blocking D1–D5.

**Prerequisite refactors D6 requests (small, low-risk, also help D1/D2):** (a) extract `ExportPacker` private entry-assembly methods into a package-private helper; (b) add an injectable cache-root to `HistoryDao` + a path-parameterized `UpgradeManager` overload.

---

## 3. Cross-Dimension Dependency Graph and Execution Order

The natural order — **toolchain → SDK → reflection → UI → tests** — holds, with two adjustments justified below:
1. **D6 Phase 1 (pure-logic tests) is pulled forward** to run immediately after the toolchain bump, before the SDK change. These tests have no platform dependency, are cheap, and give a regression baseline for the core logic *before* the risky SDK and refactor work begins.
2. **The D6 prerequisite refactors** (ExportPacker helper extraction; HistoryDao path injection) are sequenced with Phase 1, since they are pure and unblock it.

```
M0 Toolchain (D5/D4)  ─┬─►  M1 Pure-logic tests + refactors (D6 Phase 1)
                       │
                       └─►  M2 SDK 2025.3 (D3) ──►  M3 Reflection removal (D1) ──►  M4 UI composition (D2) ──►  M5 Fixture/persistence/canary tests (D6 Phase 2-4)
```

D2 depends on D1 (the LocalChangesDialogProvider/WorkflowHelper deletions are shared). D2-5 depends on D3 raising the floor to ≥ 203. D1's branch pruning depends on D3's floor decision (233). D6 Phase 4's canary depends on whether D1/D2 has deleted WorkflowHelper.

### Milestones with verification gates

| # | Milestone | Key actions | Verification gate |
|---|---|---|---|
| **M0** | Toolchain upgrade (D5/D4) | Gradle 8.14.2; intellij.platform 2.11.0; Kotlin 2.3.20; changelog/kover/qodana/foojay bumps; gradle.properties cleanups | `./gradlew buildPlugin` green on the **existing** SDK (2025.1); `./gradlew verifyPlugin` runs |
| **M1** | Pure-logic tests + refactors (D6 P1) | Extract ExportPacker helper; HistoryDao path injection; add Phase 1 JUnit4 tests; delete empty stubs | `./gradlew test` green; Kover report shows model/utils coverage rising |
| **M2** | SDK 2025.3 (D3) | `intellijIdea("2025.3")`; bundledModules; floor 233 / bytecode 17; `pluginUntilBuild=253.*`; verifier matrix; prune WorkflowHelper old branches | Each D3 sub-step gate (§D3 1–8); critically `dependencies` resolves `idea:idea:2025.3`, `buildPlugin` green, `runIde` smoke passes both export paths |
| **M3** | Reflection removal (D1) | Site 1 deletion; cluster 2–9 refactor (direct browser, delete WorkflowHelper); site 6 (own DialogWrapper); deprecated-symbol replacements | `compileJava compileKotlin buildPlugin`; `runIde` both export paths; `verifyPlugin` internal-API warnings reduced |
| **M4** | UI composition (D2) | Phase A→D: action threads, drop impl cast, delete dead code; owned ChangesTree + renderer + adapter; commit-session settings panel; DSL dialog; non-modal verify | `buildPlugin`; `runIde` full smoke checklist; `verifyPlugin`; manual non-modal commit-UI check on 2025.3 |
| **M5** | Remaining tests (D6 P2-4) | Fixture tests; persistence tests; WorkflowHelper/replacement canary; refresh manual checklist | `./gradlew test` green; canary confirms VCS path on 253 |

Each milestone is independently revertible (commit after each green gate). M0 and M1 are safe to land before any 2025.3 work and de-risk everything after.

---

## 4. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `intellijIdea("2025.3")` artifact fails to resolve (unified-distribution change) | Medium | Blocking | Dedicated gate at D3 step 2 (`dependencies --configuration intellijPlatformDependency`); this is the single most likely failure point (JetBrains 2025.3 blog) |
| Wrong `bundledModule` names (`vcs.dvcs` vs `vcs.dvcs.impl`) → "VCS classes not found" | Medium | High | Inspect `<sdk>/lib/modules/` jar names at D3 step 3 before trusting the property |
| `SingleChangeListCommitWorkflow` ctor changed again in 253 (`@Internal`, undocumented) | Medium | High | D1 deletes the class usage entirely (primary). If kept: runtime smoke test at D3 step 7 + WorkflowHelper canary test (M5). Verifier cannot catch this |
| Borrowed commit-dialog lifecycle breaks under 2025.x off-EDT/write-intent threading | Medium-High | High | D2-4 removes the borrow (own tree from `ChangeListManager`); this is the primary motivation for prioritizing D2-4 |
| `SelectFilesDialog` private `tree` field renamed/restructured; `setAccessible` blocked by JBR 21 strong encapsulation | Medium | High | D1 site 6 / D2-2 stop subclassing `SelectFilesDialog`; host `FileListTree` in own `DialogWrapper` |
| `MultipleLocalChangeListsBrowser` ctor arity differs in 253 | Medium | Medium | Compile-check at M3; fallback to subclassing the dialog and calling `protected init()` |
| Non-modal commit UI does not surface `getAdditionalConfigurationUI()` | Medium | Medium | D2-11 verify on running 2025.3; fallback: open standalone `ExportSettingDialog` from session `execute()` |
| Internal `ChangesTree`/`TreeModelBuilder`/`ChangesBrowserNode` APIs break in a future release | Low (this release); Medium (future) | Medium | D2 confines them to ~5 owned classes (NodeFileAdapter, ExportChangesTree, ExportNodeRenderer) so breaks are localized, not silent |
| Gradle 9 coupling: accidental intellij.platform 2.12+ bump forces Gradle 9 | Low | Medium | Pin intellij.platform at 2.11.0; document the coupling (§D5); defer Gradle 9 |
| K2 compiler surfaces new deprecation errors | Low | Low | Warnings only at first; address incrementally |
| Coverage gate blocks unrelated work | Low | Low | Kover report-only initially; gate model/utils only after green |

### Features that may be dropped (replacement preferred)

| Feature | Trigger to drop | Preferred replacement |
|---|---|---|
| Pre-2022 / 2022-era commit-workflow reflection branches | floor raised to 233 (D3) | Keep only the 2025+ path; if D1 lands, no workflow construction at all. Pure code deletion, no user-facing loss |
| Commit-browser bottom status strip (`getBrowserBottomPanel`) | D1 site 5 | Rebuild as a public `JBLabel` + inclusion listener (~10 lines), or drop — `MultipleLocalChangeListsBrowser` already shows selection state |
| In-commit duplicate file tree (W8) | D2-6 | Use the commit dialog's own tree; the additional UI shows export settings only — a UX improvement |
| `changesGroupingPolicy` custom factory + `DirectoryNoCollapse` action | D2-3 | Already dead/commented; behavior delivered by the owned tree's expand/collapse logic |
| "Export from Local Changes" embedded-browser UX (last resort only) | only if D2-4 threading proves unfixable | Fall back to the plain file-list dialog populated from `ChangeListManager` |

---

## 5. UNVERIFIED / Needs Compile-Check, and Open Questions

### UNVERIFIED — must be confirmed against the 253 SDK or a live 2025.3 IDE

These are claims the analysis could not confirm from documentation alone (internal APIs are not covered by the official incompatible-changes pages):

1. **Exact `bundledModule` names** for VCS in 2025.3: `intellij.platform.vcs.dvcs` vs the current `...vcs.dvcs.impl`. Inspect `<sdk>/lib/modules/` (D3 step 3).
2. **`MultipleLocalChangeListsBrowser` constructor** arity/param order in 253, and whether a `Disposable`-parented variant exists (D1 cluster).
3. **`getDisplayedUnversionedFiles()` / `getIncludedUnversionedFiles()`** method names unchanged in 253.
4. **`SingleChangeListCommitWorkflow` 2025+ 6-arg signature** — the "2025+" reflective branch is unconfirmed for 2025.3 specifically (`@Internal`). Runtime smoke test required.
5. **`SelectFilesDialog.getSelectedFiles()` semantics** — included-vs-checked-and-selected — must be diffed against the 253 source so downstream export logic is unaffected.
6. **`VirtualFileList` package location** in 253 (top-level `com.intellij.openapi.vcs.changes.ui.VirtualFileList` vs nested).
7. **`ChangesBrowserNodeRenderer` constructor** signature in 253 (historically `(Project, Supplier<Boolean>, boolean)`).
8. **`ChangesTree` protected method names** `updateTreeModel` / `rebuildTree` in 253.
9. **intellij.platform 2.11.0 is the last Gradle-8.x-compatible release** — confirm against the plugin's GitHub releases before pinning.
10. **`IC-2025.3` verifier coordinate resolvability** — cap at `IC-2025.2` if the feed lags.
11. **Non-modal commit UI surfaces `getAdditionalConfigurationUI()`** (D2-11) — runtime check only.

### Open questions for the maintainer

1. **Support floor:** accept floor `233` / bytecode `17` (drops pre-2022 reflection — recommended), or keep `202` / `11` (zero source deletion, more legacy reflection retained)? This decision cascades into D1 branch pruning and D2-5.
2. **`SettingDialog.form`:** approve full Kotlin UI DSL migration (M effort, removes the peer hack cleanly), or the minimal fallback (strip the button row, keep the form)?
3. **Embedded local-changes browser:** is keeping the embedded commit-browser UX a hard requirement, or is "select local changes in a plain file tree" acceptable if the borrow proves fragile on 2025.3?
4. **Gradle 9 / intellij.platform 2.16.0:** schedule the follow-on jump now, or defer indefinitely while staying on the 8.14.2 / 2.11.0 pair?
5. **Coverage gating:** acceptable to keep Kover report-only, or is a hard CI coverage gate desired (and at what threshold)?
6. **Signing/publishing:** CI currently only runs `buildPlugin`; is a signing/publishing workflow in scope for this effort, or out of scope?

---

## Appendix — Source citations for version/API claims

- IntelliJ Platform 2025.3 plugin-developer changes (unified distribution, `intellijIdea()` helper, min plugin 2.10.4, VCS module extraction, `@Storage` ban): JetBrains Platform blog, 2025-11.
- Build prefix `253`, Java 21, Java-floor-by-branch table: JetBrains "Build Number Ranges" docs.
- `CommitChangeListDialog` / `SingleChangeListCommitWorkflow` still present in 2025.3: intellij-community `master` (vcs-impl).
- 2025.1 write-intent-lock change; 2025.3 off-EDT write actions: JetBrains "Notable API changes 2025" docs.
- Gradle 8.14.x release notes; Gradle 9.0 Java-17-daemon requirement: docs.gradle.org.
- Plugin/library latest versions (intellij.platform 2.16.0, Kotlin 2.3.20, changelog 2.5.0, kover 0.9.8, qodana 2025.3.2, foojay 1.0.0): Gradle Plugin Portal / kotlinlang.org, verified 2026-05-31.

*This report is an analysis and plan, not an implementation. All API replacements labeled with concrete signatures are MVP proposals pending the compile-checks in §5. No code has been changed.*

