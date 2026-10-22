package gg.overthisnow.snow.core.notify;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Notification queue with grouping, caps, per-category toggles and history.
 * Time is passed in explicitly (seconds) so behaviour is deterministic and testable.
 * Callers must only post SUCCESS after the operation actually completed.
 */
public final class NotificationCenter {
    public enum Category { MODULE, PROFILE, CONFIG, VISION, WARNING }
    public enum Level { INFO, SUCCESS, WARNING, ERROR }

    public static final class Notification {
        public final Category category;
        public final Level level;
        public final String message;
        public int count = 1;
        public double postedAt;
        public double expiresAt;

        Notification(Category c, Level l, String m, double now, double ttl) {
            category = c; level = l; message = m; postedAt = now; expiresAt = now + ttl;
        }

        public String displayText() { return count > 1 ? message + " (x" + count + ")" : message; }
    }

    private static final int HISTORY_LIMIT = 200;

    private final List<Notification> active = new ArrayList<>();
    private final Deque<Notification> history = new ArrayDeque<>();
    private final Set<Category> enabled = EnumSet.allOf(Category.class);
    private int maxVisible = 4;
    private double durationSeconds = 4.0;

    public void setMaxVisible(int n) { maxVisible = Math.max(1, Math.min(8, n)); }
    public void setDurationSeconds(double s) { durationSeconds = Math.max(1.0, Math.min(15.0, s)); }
    public void setCategoryEnabled(Category c, boolean on) { if (on) enabled.add(c); else enabled.remove(c); }
    public boolean isCategoryEnabled(Category c) { return enabled.contains(c); }

    /** Returns the notification shown, or null if the category is disabled. */
    public Notification post(Category category, Level level, String message, double now) {
        if (!enabled.contains(category)) return null;
        // Group an identical visible notification instead of stacking duplicates.
        for (Notification n : active) {
            if (n.category == category && n.level == level && n.message.equals(message)) {
                n.count++;
                n.expiresAt = now + durationSeconds;
                return n;
            }
        }
        Notification n = new Notification(category, level, message, now, durationSeconds);
        active.add(n);
        history.addFirst(n);
        while (history.size() > HISTORY_LIMIT) history.removeLast();
        // Over the cap: retire the oldest visible one early.
        while (active.size() > maxVisible) active.remove(0);
        return n;
    }

    /** Drop expired notifications. Call once per frame/tick. */
    public void update(double now) { active.removeIf(n -> now >= n.expiresAt); }

    public List<Notification> visible() { return List.copyOf(active); }
    public List<Notification> history() { return List.copyOf(history); }
    public void clearHistory() { history.clear(); }
}
