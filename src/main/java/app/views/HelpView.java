package app.views;

/** Shares the supported navigation commands across the help display. */
final class HelpView {
    static final String COMMANDS = "/back · /cancel · /exit · /clear";
    static final String TIPS = "Enter submit · ↑/↓ scroll";
    static final String DETAILS = "===== MENU HELP =====\n"
            + "/back - Follow this menu's 0 option.\n"
            + "/cancel - Discard pending input and return to this section's menu.\n"
            + "/exit - Close the application.\n"
            + "/clear - Clear visible history; keep the current input.\n";

    /** Prevents creating instances of this shared help text. */
    private HelpView() { }
}
