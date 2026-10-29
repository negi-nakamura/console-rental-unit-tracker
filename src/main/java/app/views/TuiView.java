package app.views;

import com.williamcallahan.tui4j.compat.bubbles.textinput.TextInput;
import com.williamcallahan.tui4j.compat.bubbletea.*;
import com.williamcallahan.tui4j.compat.lipgloss.Renderer;
import com.williamcallahan.tui4j.compat.lipgloss.color.ColorProfile;
import java.io.PrintStream;
import java.util.function.BiConsumer;
import java.util.concurrent.atomic.AtomicReference;

/** Terminal presentation adapter; all rent operations remain in the existing MVC controllers. */
public final class TuiView implements Model {
    private final HeaderView header = new HeaderView();
    private final ContentView content = new ContentView();
    private final InputRowView inputRow = new InputRowView();
    private final ScreenView screen = new ScreenView();
    private TextInput input = new TextInput();
    private final TuiSession session;
    private final Runnable application;
    private int width = 80;
    private int height = 23;
    private String prompt = "Starting Rent Tracker…";
    private long requestId;

    /** Creates presentation widgets independently of domain models and controller state. */
    TuiView(TuiSession session, Runnable application) {
        this.session = session;
        this.application = application;
        input.setWidth(0);
        input.setPrompt("❯ ");
    }

    /** Runs existing MVC wiring on a worker while TUI4J owns terminal rendering and keys. */
    public static void run(BiConsumer<InputReader, PrintStream> application) {
        Renderer.defaultRenderer().setColorProfile(ColorProfile.TrueColor);
        Program[] program = new Program[1];
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Thread> controllerThread = new AtomicReference<>();
        try (TuiSession session = new TuiSession(message -> program[0].send(message))) {
            Runnable worker = () -> {
                controllerThread.set(Thread.currentThread());
                try { application.accept(new InputReader(session::readLine, session.output()), session.output()); }
                catch (Throwable error) { failure.set(error); }
                finally { session.output().close(); program[0].send(new TuiSession.Finished()); }
            };
            TuiView view = new TuiView(session, worker);
            program[0] = new Program(view).withAltScreen();
            try { program[0].run(); }
            finally {
                session.close();
                Thread workerThread = controllerThread.get();
                if (workerThread != null) {
                    try { workerThread.join(1000); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                }
            }
        }
        if (failure.get() != null) throw new IllegalStateException("Rent Tracker stopped unexpectedly.", failure.get());
    }

    /** Starts controller input outside the event loop, plus cursor and resize commands. */
    @Override public Command init() {
        Command start = () -> { Thread.ofPlatform().daemon(true).name("rent-tracker-controller").start(application); return null; };
        return Command.batch(start, input.focus(), TextInput::blink, Command.checkWindowSize());
    }

    /** Handles terminal events and presentation updates without interpreting business operations. */
    @Override public UpdateResult<? extends Model> update(Message message) {
        if (message instanceof TuiSession.Header menu) {
            header.set(menu.title(), menu.options());
        } else if (message instanceof TuiSession.Output output) {
            content.append(output.text());
        } else if (message instanceof TuiSession.Prompt question) {
            requestId = question.id();
            prompt = question.text();
            input.reset();
            return UpdateResult.from(this, Command.batch(input.focus(), TextInput::blink));
        } else if (message instanceof TuiSession.Finished) {
            return UpdateResult.from(this, QuitMessage::new);
        } else if (message instanceof WindowSizeMessage size) {
            if (size.width() > 0) width = size.width();
            // Leave space for the Windows host's last row and TUI4J's terminal cursor.
            if (size.height() > 0) height = Math.max(1, size.height() - 2);
        } else if (message instanceof KeyPressMessage key) {
            switch (key.key()) {
                case "ctrl+c" -> { session.close(); return UpdateResult.from(this, QuitMessage::new); }
                case "up", "down", "pgup", "pgdown", "ctrl+left", "ctrl+right" -> content.scroll(key.key());
                case "enter", "ctrl+j" -> {
                    if (requestId == 0) break;
                    String value = input.value();
                    if (value.trim().equalsIgnoreCase("/clear")) {
                        content.clear();
                        input.reset();
                    } else if (value.trim().equalsIgnoreCase("/help")) {
                        content.append(HelpView.DETAILS);
                        input.reset();
                    } else if (session.submit(requestId, value)) {
                        content.recordInput(prompt, value, header);
                        requestId = 0;
                        input.reset();
                    }
                }
                default -> { if (requestId != 0) return updateInput(message); }
            }
        } else {
            return updateInput(message);
        }
        return UpdateResult.from(this);
    }

    /** Delegates text editing and cursor blinking to TUI4J's text input widget. */
    private UpdateResult<? extends Model> updateInput(Message message) {
        UpdateResult<TextInput> result = input.update(message);
        input = result.model();
        return UpdateResult.from(this, result.command());
    }

    /** Draws the shared header, content viewport, prompt, and editable input row. */
    @Override public String view() {
        return screen.render(header, content, prompt,
                inputRow.render(input, Math.max(1, width - 2), requestId != 0,
                        prompt.equals("Choice: ") ? "command" : ""), width, height);
    }
}
