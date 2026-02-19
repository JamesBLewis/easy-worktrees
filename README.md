<p align="center">
  <img src="icon.png" width="120" alt="Easy Worktrees">
</p>

# Easy Worktrees

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/30206-easy-worktrees.svg?label=JetBrains%20Marketplace)](https://plugins.jetbrains.com/plugin/30206-easy-worktrees)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30206-easy-worktrees.svg)](https://plugins.jetbrains.com/plugin/30206-easy-worktrees)

An IntelliJ IDEA plugin for managing Git worktrees. Adds a **Worktrees** tab directly inside the existing Git tool window — no extra sidebar icons, no clutter.

Built for developers who use `git worktree` heavily (especially with tools like [1code](https://github.com/21st-dev/1code) for running multiple Claude Code sessions).

## Features

- **Switch to main/master** — One click to jump to whichever worktree has your primary branch checked out. Don't care which directory it's in, just get there.
- **Search worktrees** — Real-time filtering by branch name or path as you type.
- **Worktrees tab in Git tool window** — Lives alongside Log, Console, etc. No separate tool window.
- **Double-click to switch** — Double-click any worktree row to switch the current window to it.
- **Open in new window** — Right-click context menu option to open a worktree in a separate IDE window instead.
- **Current worktree highlighting** — Your current worktree is bolded in the table so you always know where you are.

## Installation

### From JetBrains Marketplace

1. In IntelliJ: **Settings > Plugins > Marketplace**
2. Search for **Easy Worktrees**
3. Click **Install** and restart IDE

### From disk

1. Build the plugin (see below) or grab a zip from [GitHub Releases](https://github.com/JamesBLewis/easy-worktrees/releases)
2. In IntelliJ: **Settings > Plugins > Gear icon > Install Plugin from Disk**
3. Select the zip file
4. Restart IDE

## Usage

1. Open a project that uses git worktrees
2. Open the **Git** tool window (bottom bar)
3. Click the **Worktrees** tab
4. From there:
   - **Search**: Type in the search field to filter worktrees
   - **Switch**: Double-click a row to switch the current window to that worktree
   - **Switch to Main**: Click the toolbar button to jump to master/main/trunk
   - **Right-click** for more options: Switch to Worktree, Open in New Window, Copy Path

## Building

Requires JDK 21. If you have Homebrew:

```bash
brew install openjdk@21
```

Then build:

```bash
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ./gradlew buildPlugin
```

The plugin zip will be at `build/distributions/easy-worktrees-1.0.0.zip`.

### Run a sandboxed IDE for testing

```bash
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ./gradlew runIde
```

## Compatibility

- IntelliJ IDEA 2024.2+
- Requires Git plugin (bundled with IntelliJ)
- Git 2.15+ (for `git worktree` support)

## License

MIT
