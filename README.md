# Rent Payment and Unit Tracker

Java 21 console application with the existing MVC structure and a TUI4J 0.3.5 view.

Run in Windows Terminal or IntelliJ's Terminal:

```powershell
.\run.cmd
```

Use Java 21 for Maven (`JAVA_HOME`). Import/reload `pom.xml` in IntelliJ after the dependency change. The ordinary IntelliJ Run console may not support a full terminal screen; use its Terminal tab for the TUI. `Main` uses the TUI automatically when attached to an interactive terminal. `--tui` selects it explicitly; `--console` selects the original line-by-line interface for redirection or debugging:

```powershell
.\run.cmd --console
```

The fixed header matches the supplied menu-app layout: a blue title, dim TUI4J subtitle, bracketed choices in two columns, a separate Back/Exit row, and a tip above the content divider. It retains all existing menu numbers and labels. Results, validation messages, input history, lists, payment history, charge history, and hash-map output appear in the scrolling content view. The current question or menu hint, the editable input row, and a keyboard-help footer remain below it. Command choices appear in the transcript; action names and table headings are purple. Entered field values, including confirmation answers, are yellow in both the input row and transcript. Table rows alternate cyan and white; detail labels are blue, values and totals cyan, separators and complexity messages dim, validation errors red, and successful results green.

- Enter submits a value. Existing update fields still accept Enter to keep their current values.
- Left/Right, Home/End, and Backspace edit the current input.
- Up/Down and Page Up/Page Down scroll output.
- Ctrl+Left/Right pans wide output such as history tables and hash-map chains.
- Help lists `/back`, `/cancel`, `/exit`, and `/clear`.
- `/back` follows the current menu's `0` option (Back in a section, Exit at the main menu). `/cancel` abandons pending input and returns to the current section's menu without saving. At a menu, `/cancel` keeps you there. `/exit` closes the application. `/clear` clears visible history while preserving the pending field and application data.
- Menu option 0 keeps its existing Back/Exit behavior; Ctrl+C closes the terminal session.

On short terminals the menu moves into scrollback to preserve the current input. Enlarge the terminal for the full fixed header.

`HeaderView`, `ContentView`, `InputRowView`, `ScreenView`, `TuiView`, and `TuiSession` live under `app.views`. Their layout and widgets are adapted from the provided `menu-app`. `InputReader` retains all existing validation and accepts either Scanner lines or TUI input. Existing controllers and domain models are unchanged. TUI4J calls its event adapter a `Model`; `TuiView` implements that library interface within the view layer and is separate from the application's domain models.

Run the automated view and MVC integration tests:

```powershell
mvn test
```

Tests cover existing menu content, input validation, Enter-to-skip, carriage-return and line-feed Enter keys, duplicate submissions, Unicode output, resizing, scrolling, wide-table panning, the actual controller payment flow, and closing during a pending payment. Business data and transaction history remain in memory for the current run.
