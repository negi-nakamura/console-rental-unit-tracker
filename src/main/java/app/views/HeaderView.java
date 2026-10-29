package app.views;

import static app.views.ViewStyles.*;
import java.io.PrintStream;

/** Adapts the reference app's fixed header to the tracker's existing menu content. */
public final class HeaderView {
    private String title = "RENT PAYMENT AND UNIT TRACKER";
    private String[] options = {};

    /** Sends an existing menu to the TUI header or prints it in a plain console. */
    public static void show(PrintStream output, String title, String... options) {
        if (output instanceof TuiSession.ViewOutput screen) {
            screen.showHeader(title, options);
            return;
        }
        output.println();
        output.println("===== " + title + " =====");
        for (String option : options) output.println(option);
    }

    /** Replaces only the visible menu, preserving scrollback and application data. */
    void set(String title, String[] options) {
        this.title = title;
        this.options = options.clone();
    }

    /** Renders the sample's title, subtitle, two-column options, and separated tip. */
    String render(int width) {
        StringBuilder header = new StringBuilder(TITLE.render(ScreenView.wrap(title, width))).append('\n');
        header.append(DIM.render(ScreenView.wrap("Command-driven console · powered by TUI4J", width))).append("\n\n");
        header.append(menu(width)).append('\n');
        header.append(DIM.render(ScreenView.wrap(HelpView.COMMANDS, width))).append('\n');
        return header.toString();
    }

    /** Renders bracketed choices in at most two columns; Back and Exit occupy their own row. */
    String menu(int width) {
        int column = 0;
        for (String option : options) if (!option.startsWith("0.")) column = Math.max(column, option.length() + 4);
        boolean twoColumns = width >= column * 2;
        StringBuilder result = new StringBuilder();
        String pending = null;
        for (String option : options) {
            if (option.startsWith("0.") || !twoColumns) {
                if (pending != null) { result.append(format(pending, width)).append('\n'); pending = null; }
                result.append(format(option, width)).append('\n');
            } else if (pending == null) {
                pending = option;
            } else {
                result.append(format(pending, width)).append(" ".repeat(Math.max(1, column - pending.length() - 1)))
                        .append(format(option, width)).append('\n');
                pending = null;
            }
        }
        if (pending != null) result.append(format(pending, width)).append('\n');
        return result.toString();
    }

    /** Colors option numbers like the sample while retaining the existing menu labels. */
    private String format(String option, int width) {
        int separator = option.indexOf(". ");
        if (separator < 0) return WHITE.render(ScreenView.wrap(option, width));
        String number = option.substring(0, separator);
        String label = option.substring(separator + 2);
        String bracket = "[" + number + "] ";
        return (number.equals("0") ? RED : BLUE).render(bracket)
                + WHITE.render(ScreenView.wrap(label, Math.max(1, width - bracket.length())));
    }

    /** Returns the selected option's existing label for the command transcript. */
    String action(String choice) {
        for (String option : options) if (option.startsWith(choice + ". ")) return option.substring(choice.length() + 2);
        return "";
    }

    /** Provides the fixed footer's concise menu hint without changing menu numbering. */
    String footer() {
        return switch (title) {
            case "UNIT MANAGEMENT" -> "1 add · 2 view · 3 update · 4 remove · 5 list · 6 count · 7 hash map";
            case "TENANT MANAGEMENT" -> "1 assign · 2 view · 3 update · 4 move out · 5 list · 6 count · 7 hash map";
            case "PAYMENTS" -> "1 record payment · 2 balance · 3 charge rent · 4 payment history · 5 charge history";
            case "REPORTS" -> "1 display table · 2 unpaid units · 3 vacant units · 4 total balance";
            case "TABLE SETTINGS" -> "1 table statistics · 2 rehash";
            default -> "Type a menu number and press Enter";
        };
    }
}
