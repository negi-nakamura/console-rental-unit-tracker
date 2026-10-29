package app.views;

import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

/** Shared terminal palette adapted from menu-app's presentation styles. */
final class ViewStyles {
    static final Style BLUE = Style.newStyle().foreground(Color.color("#75B6FF"));
    static final Style DIM = Style.newStyle().foreground(Color.color("#9099A7"));
    static final Style WHITE = Style.newStyle().foreground(Color.color("#F1F4F8"));
    static final Style GREEN = Style.newStyle().foreground(Color.color("#9EDB9E"));
    static final Style RED = Style.newStyle().foreground(Color.color("#FF8795"));
    static final Style YELLOW = Style.newStyle().foreground(Color.color("#F6D365"));
    static final Style PURPLE = Style.newStyle().foreground(Color.color("#C9A7FF"));
    static final Style CYAN = Style.newStyle().foreground(Color.color("#8EDCE6"));
    static final Style TITLE = BLUE.copy().bold(true);

    /** Prevents creating instances of the shared style palette. */
    private ViewStyles() { }
}
