package app.views;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/** Bridges existing synchronous controllers to terminal input without blocking the TUI thread. */
final class TuiSession implements AutoCloseable {
    record Header(String title, String[] options) implements Message { }
    record Output(String text) implements Message { }
    record Prompt(long id, String text) implements Message { }
    record Finished() implements Message { }
    private record Reply(String text, boolean ended) { }
    private final BlockingQueue<Reply> replies = new LinkedBlockingQueue<>();
    private final Consumer<Message> publish;
    private long sequence;
    private long waiting;
    private boolean closed;
    private final ViewOutput output;

    /** Creates a session whose view changes are delivered to the terminal event loop. */
    TuiSession(Consumer<Message> publish) {
        this.publish = Objects.requireNonNull(publish);
        output = new ViewOutput(new LineOutput(), this);
    }

    /** Exposes the same PrintStream interface used by every existing menu view. */
    PrintStream output() { return output; }

    /** Waits on the controller thread for one submitted line, including an empty line. */
    String readLine(String prompt) {
        synchronized (this) {
            if (closed) throw new NoSuchElementException("Console input ended.");
            waiting = ++sequence;
            publish.accept(new Prompt(waiting, prompt));
        }
        try {
            Reply reply = replies.take();
            if (reply.ended()) throw new NoSuchElementException("Console input ended.");
            return reply.text();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Console input ended.");
        }
    }

    /** Accepts one reply per prompt so repeated Enter cannot answer a later question. */
    synchronized boolean submit(long id, String value) {
        if (closed || waiting == 0 || waiting != id) return false;
        waiting = 0;
        replies.offer(new Reply(value, false));
        return true;
    }

    /** Unblocks pending controller input when the terminal closes. */
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        waiting = 0;
        replies.offer(new Reply("", true));
    }

    /** Routes complete UTF-8 output lines to the scrolling content panel. */
    private final class LineOutput extends OutputStream {
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        @Override public synchronized void write(int value) {
            line.write(value);
            if (value == '\n') flushLine();
        }
        @Override public synchronized void write(byte[] values, int offset, int length) {
            for (int i = offset; i < offset + length; i++) write(values[i] & 255);
        }
        @Override public synchronized void close() { flushLine(); }
        private void flushLine() {
            if (line.size() == 0) return;
            publish.accept(new Output(line.toString(StandardCharsets.UTF_8)));
            line.reset();
        }
    }

    /** Marks the TUI output stream so menu views can update the fixed header. */
    static final class ViewOutput extends PrintStream {
        private final TuiSession session;
        ViewOutput(OutputStream stream, TuiSession session) {
            super(stream, true, StandardCharsets.UTF_8);
            this.session = session;
        }
        void showHeader(String title, String[] options) {
            session.publish.accept(new Header(title, options.clone()));
        }
    }
}
