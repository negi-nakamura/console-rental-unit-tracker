package app.views;

import com.williamcallahan.tui4j.compat.lipgloss.Size;
import static app.views.ViewStyles.*;

/** Composes a fixed header and input around the scrolling content panel. */
public final class ScreenView {
    private static final String TIPS = HelpView.TIPS;

    /** Renders the screen using the actual wrapped header and prompt heights. */
    String render(HeaderView header, ContentView content, String prompt, String input, int width, int height) {
        int inner = Math.max(1, width - 2);
        String rule = " " + DIM.render("─".repeat(inner));
        String top = rule + '\n' + indent(header.render(inner)) + rule + "\n\n";
        String footer = prompt.equals("Choice: ") ? header.footer() : prompt;
        String bottom = '\n' + rule + '\n' + indent(DIM.render(wrap(footer, inner))) + '\n' + ' ' + input + '\n'
                + rule + '\n' + indent(DIM.render(wrap(TIPS, inner)));
        int fixed = top.split("\n", -1).length - 1 + bottom.split("\n", -1).length;
        String prefix = "";
        // A short terminal keeps menu choices accessible inside the scrolling panel.
        if (height <= fixed) {
            prefix = header.render(inner) + '\n';
            top = rule + '\n';
            bottom = rule + '\n' + indent(DIM.render(wrap(footer, inner))) + '\n' + ' ' + input;
            fixed = 1 + bottom.split("\n", -1).length;
        }
        if (height <= fixed) return wrap("Enlarge the terminal to continue.", inner);
        content.resize(inner, Math.max(1, height - fixed));
        return top + indent(content.render(prefix)) + '\n' + bottom;
    }

    /** Applies the sample's one-column left margin consistently to multiline components. */
    private static String indent(String text) {
        return " " + text.replace("\n", "\n ").replaceAll("\n $", "\n");
    }

    /** Wraps labels at word boundaries, splitting only words wider than the terminal. */
    static String wrap(String text, int width) {
        StringBuilder result = new StringBuilder();
        int used = 0;
        for (String word : text.split(" +")) {
            if (word.isEmpty()) continue;
            if (used > 0) {
                if (used + 1 + Size.width(word) > width) { result.append('\n'); used = 0; }
                else { result.append(' '); used++; }
            }
            for (int offset = 0; offset < word.length();) {
                int codePoint = word.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                int cells = Size.width(character);
                if (used > 0 && used + cells > width) { result.append('\n'); used = 0; }
                result.append(character);
                used += cells;
                offset += Character.charCount(codePoint);
            }
        }
        return result.toString();
    }
}
