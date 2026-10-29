package app.views;

import com.williamcallahan.tui4j.compat.bubbles.textinput.TextInput;
import com.williamcallahan.tui4j.compat.lipgloss.Size;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

/** Bounded editable input row adapted from the reference app's input renderer. */
public final class InputRowView {
    private static final Style BLUE = Style.newStyle().foreground(Color.color("#75B6FF"));
    private static final Style WHITE = Style.newStyle().foreground(Color.color("#F1F4F8"));

    /** Draws the input and cursor without allowing a long value to wrap the row. */
    String render(TextInput input, int width, boolean ready) {
        return render(input, width, ready, "");
    }

    /** Adds the sample's command placeholder only while choosing a menu option. */
    String render(TextInput input, int width, boolean ready, String placeholder) {
        if (!ready) return BLUE.render("❯ ") + "Please wait…";
        String value = input.value();
        int pos = Math.max(0, Math.min(input.position(), value.length()));
        String before = value.substring(0, pos);
        String after = value.substring(pos);
        String cursor = after.isEmpty() ? " " : after.substring(0, 1);
        String tail = after.isEmpty() ? "" : after.substring(1);
        if (value.isEmpty() && !placeholder.isEmpty()) {
            cursor = placeholder.substring(0, 1);
            tail = placeholder.substring(1);
        }
        int available = Math.max(1, width - Size.width("❯ "));
        while (Size.width(before + cursor + tail) > available && !before.isEmpty()) {
            before = before.substring(before.offsetByCodePoints(0, 1));
        }
        while (Size.width(before + cursor + tail) > available && !tail.isEmpty()) {
            tail = tail.substring(0, tail.offsetByCodePoints(tail.length(), -1));
        }
        Style textStyle = value.isEmpty() ? ViewStyles.DIM
                : placeholder.isEmpty() ? ViewStyles.YELLOW : WHITE;
        Style cursorStyle = textStyle.copy().inline(true);
        if (!input.cursor().isBlink()) cursorStyle.reverse(true);
        return BLUE.render("❯ ") + textStyle.render(before) + cursorStyle.render(cursor) + textStyle.render(tail);
    }
}
