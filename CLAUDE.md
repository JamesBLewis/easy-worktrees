# CLAUDE.md — Easy Worktrees IntelliJ Plugin

## Project Overview

An IntelliJ IDEA plugin for managing Git worktrees. Adds a **Worktrees** tab to the existing Git tool window (not a separate tool window). Published on the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30206-easy-worktrees).

## Tech Stack

- **Language:** Kotlin 1.9.25
- **Build:** IntelliJ Platform Gradle Plugin 2.11.0 (`org.jetbrains.intellij.platform`)
- **Gradle:** 8.13
- **JVM:** 21
- **Target:** IntelliJ Community 2024.2+ (sinceBuild=242, no untilBuild)
- **Dependency:** Bundled `Git4Idea` plugin

## Build & Run

```bash
# Build the plugin (produces build/distributions/*.zip)
./gradlew buildPlugin

# Run in a sandboxed IDE for testing
./gradlew runIde

# Publish to JetBrains Marketplace (requires JETBRAINS_TOKEN env var)
./gradlew publishPlugin
```

If no global JDK is installed, set `JAVA_HOME` explicitly:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew buildPlugin
```

## Project Structure

```
src/main/kotlin/com/github/jameslewis/easyworktrees/
├── model/WorktreeInfo.kt              # Data class for worktree entries
├── services/WorktreeService.kt        # Git worktree operations (project-level service)
├── ui/
│   ├── WorktreeToolWindowFactory.kt   # WorktreeContentProvider (changesViewContent EP)
│   ├── WorktreePanel.kt               # Main UI: search + table + toolbar + context menu
│   ├── WorktreeTableModel.kt          # Filterable/sortable table model
│   └── SortableHeaderRenderer.kt      # Native IntelliJ sort indicators on column headers
└── actions/
    ├── SwitchToMainAction.kt          # Find & switch to main/master/trunk worktree
    └── RefreshWorktreesAction.kt      # Refresh worktree list
```

Key resource: `src/main/resources/META-INF/plugin.xml` — plugin descriptor.

## Architecture Decisions

### Git Integration
- Uses `changesViewContent` extension point (`com.intellij.changesViewContent`) to inject a tab into the existing Git tool window — NOT a standalone `toolWindow`.
- Git commands run via `GeneralCommandLine` + `CapturingProcessHandler`, NOT the Git4Idea `GitLineHandler`/`GitCommand` API (which has private constructors for worktree operations).
- Git executable path obtained via `GitExecutableManager.getInstance().getPathToGit(project)`.
- Parses `git worktree list --porcelain` output.

### Worktree Switching
- Default: replaces current window (`ProjectUtil.openOrImport(path, project, false)`).
- Right-click "Open in New Window" uses `forceNewFrame = true`.
- If the target worktree is already open, brings that window to focus instead.

## IntelliJ Platform Development Rules

### Threading Model
| Operation | Thread | API |
|-----------|--------|-----|
| Git commands / data fetching | Background | `ApplicationManager.getApplication().executeOnPooledThread {}` |
| UI updates (table, notifications) | EDT | `ApplicationManager.getApplication().invokeLater {}` |
| Action `update()` method | BGT | Return `ActionUpdateThread.BGT` from `getActionUpdateThread()` |

**Critical rules:**
- Never do file I/O, git operations, or PSI access on EDT — it freezes the UI.
- Never touch Swing components from background threads.
- `update()` is called frequently and must be fast — no real work, just check state.
- Read actions from BGT require `ReadAction.compute {}` or `runReadAction {}`.
- Write actions are EDT-only via `WriteAction.run {}`.

### Services
- Use `@Service(Service.Level.PROJECT)` for project-scoped light services.
- Service classes must be `final` (default in Kotlin).
- Never cache service instances in fields — always call `project.getService()` or the companion `getInstance()` at the call site.
- Avoid heavy work in constructors — initialize lazily.
- For coroutine support, accept `CoroutineScope` as a constructor parameter.

### Actions
- All actions extend `AnAction` (or `DumbAwareAction` to work during indexing).
- **AnAction subclasses must not have mutable class fields** — they leak memory.
- `getActionUpdateThread()` must return `ActionUpdateThread.BGT` (preferred) or `ActionUpdateThread.EDT`.
- BGT update: has read access to PSI/VFS/project model, but no Swing access.
- EDT update: has Swing access, but no PSI/VFS/project model access.
- Register actions in plugin.xml `<actions>` with unique `id` attributes.
- Extract reusable logic into services, not static methods on actions.

### Extension Points
- Declare extensions in plugin.xml under `<extensions defaultExtensionNs="com.intellij">`.
- This plugin uses `changesViewContent` (tab in Git tool window) and `notificationGroup` (balloon notifications).

### Dynamic Plugin Compatibility
- No components — use services, extensions, and listeners only.
- All `<group>` elements need unique IDs.
- Services implementing `Disposable` must clean up in `dispose()`.

### UI Guidelines
- Follow IntelliJ UI conventions — use `JBTable`, `SearchTextField`, `SimpleToolWindowPanel`.
- Use `UIManager.getIcon("Table.ascendingSortIcon")` for native sort indicators, not Unicode.
- Plugin icon in `META-INF/pluginIcon.png` must be exactly 40x40px.
- Use `AllIcons.*` constants for standard icons.

### Testing
- IntelliJ uses model-level functional tests, not isolated unit tests.
- Tests run headless with real components (minus UI).
- Don't mock IntelliJ platform components — use real ones.
- Test framework: `TestFrameworkType.Platform` (already in dependencies).

## Versioning & Release

- Version is set in `gradle.properties` as `pluginVersion` (default for local dev).
- CI overrides it via `-PpluginVersion=X.Y.Z` from the git tag.
- Tag format: `vX.Y.Z` (e.g., `v1.1.2`). RC tags are excluded from releases.
- Release workflow (`.github/workflows/release.yml`): builds, signs, publishes to Marketplace, creates GitHub Release.
- Plugin signing uses `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` secrets.
- Marketplace publishing uses `JETBRAINS_TOKEN` secret.

## Common Pitfalls

- `SimpleToolWindowPanel` is in `com.intellij.openapi.ui`, NOT `com.intellij.ui`.
- `GitCommand.read("worktree")` has a private constructor — use `GeneralCommandLine` instead.
- `pluginIcon.png` at any size other than 40x40 won't display in the IDE plugin manager.
- Gradle wrapper must match the IntelliJ Platform Gradle Plugin's minimum requirement (currently 8.13 for plugin 2.11.0).
- `settings.gradle.kts` needs `pluginManagement { repositories { mavenCentral(); gradlePluginPortal() } }` or the Kotlin plugin won't resolve.
