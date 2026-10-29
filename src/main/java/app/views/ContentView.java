package app.views;

import com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport;
import java.util.Locale;
import static app.views.ViewStyles.*;

/** Scrollable output adapted from the reference app's history viewport. */
public final class ContentView {
    private final Viewport viewport = Viewport.create(78, 8);
    private final StringBuilder content = new StringBuilder();
    private String prefix = "";
    private int tableRow;

    /** Appends unchanged view output and follows the newest result. */
    void append(String text) {
        String normalized = text.replace("\r\n", "\n");
        for (String line : normalized.split("(?<=\n)", -1)) {
            if (line.isEmpty()) continue;
            boolean newline = line.endsWith("\n");
            String value = newline ? line.substring(0, line.length() - 1) : line;
            content.append(style(value));
            if (newline) content.append('\n');
        }
        refresh();
    }

    /** Records menu navigation and field answers using the sample's command transcript. */
    void recordInput(String prompt, String value, HeaderView header) {
        if (prompt.equals("Choice: ")) {
            tableRow = 0;
            if (!content.isEmpty()) content.append('\n');
            content.append(BLUE.render("❯ ")).append(WHITE.render(value)).append('\n');
            String action = header.action(value.trim());
            if (!action.isEmpty()) content.append(PURPLE.copy().bold(true).render(action)).append('\n');
        } else {
            content.append(YELLOW.render(prompt + value)).append('\n');
        }
        refresh();
    }

    /** Clears only visible history, leaving the pending form and application data intact. */
    void clear() { content.setLength(0); tableRow = 0; refresh(); }

    /** Selects presentation colors for unchanged result and validation messages. */
    private String style(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (line.startsWith("Time:")) return DIM.render(line);
        if (line.startsWith("=====")) {
            tableRow = 0;
            return PURPLE.copy().bold(true).render(line);
        }
        if (line.matches("-{3,}")) return DIM.render(line);
        if (line.startsWith("Bucket ")) {
            int separator = line.indexOf(": ");
            if (separator >= 0) return BLUE.render(line.substring(0, separator + 2))
                    + (line.endsWith(": null") ? DIM : CYAN).render(line.substring(separator + 2));
        }
        if (line.contains(" | ")) {
            String firstColumn = line.substring(0, line.indexOf(" | ")).trim();
            if (firstColumn.equals("Unit") || firstColumn.equals("Tenant ID")
                    || firstColumn.equals("Payment ID") || firstColumn.equals("Charge ID")) {
                tableRow = 0;
                return PURPLE.copy().bold(true).render(line);
            }
            return (tableRow++ % 2 == 0 ? CYAN : WHITE).render(line);
        }
        if (line.startsWith("Total ")) return CYAN.copy().bold(true).render(line);
        int separator = line.indexOf(": ");
        if (separator >= 0) return BLUE.render(line.substring(0, separator + 2))
                + CYAN.render(line.substring(separator + 2));
        if (lower.contains("successfully")) return GREEN.render(line);
        if (lower.contains("must ") || lower.contains("invalid") || lower.contains("cannot ")
                || lower.contains("not found") || lower.contains("is required") || lower.contains("already ")
                || lower.contains("more than the balance") || lower.startsWith("could not")) return RED.render(line);
        if (lower.contains("cancelled") || lower.startsWith("no ")) return DIM.render(line);
        return WHITE.render(line);
    }

    /** Refreshes the viewport after transcript changes and follows the newest message. */
    private void refresh() {
        viewport.setContent(prefix + content);
        viewport.gotoBottom();
    }

    /** Resizes the panel, retaining its scroll position unless it was following output. */
    void resize(int width, int height) {
        boolean follow = viewport.atBottom();
        viewport.setWidth(Math.max(1, width));
        viewport.setHeight(Math.max(1, height));
        if (follow) viewport.gotoBottom();
    }

    /** Scrolls output while leaving the header and input in place. */
    void scroll(String key) {
        switch (key) {
            case "up" -> viewport.scrollUp(1);
            case "down" -> viewport.scrollDown(1);
            case "pgup" -> viewport.pageUp();
            case "pgdown" -> viewport.pageDown();
            case "ctrl+left" -> viewport.scrollLeft(8);
            case "ctrl+right" -> viewport.scrollRight(8);
            default -> { }
        }
    }

    /** Returns only the visible output; wide tables can be panned horizontally. */
    String render(String prefix) {
        if (!this.prefix.equals(prefix)) {
            this.prefix = prefix;
            viewport.setContent(prefix + content);
            viewport.gotoBottom();
        }
        return viewport.view();
    }
}
