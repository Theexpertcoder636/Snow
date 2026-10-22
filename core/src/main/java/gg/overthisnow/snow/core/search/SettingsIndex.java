package gg.overthisnow.snow.core.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Universal settings search. Entries must be registered by the code that owns the
 * real setting, so a result can never point at something that does not exist.
 */
public final class SettingsIndex {
    public record Entry(String id, String title, String category, String location, List<String> keywords, Runnable open) {}

    public record Result(Entry entry, int score) {}

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public void register(Entry e) {
        if (e.open() == null) throw new IllegalArgumentException("Search entry '" + e.id() + "' has no action");
        if (entries.putIfAbsent(e.id(), e) != null) throw new IllegalArgumentException("Duplicate id " + e.id());
    }

    public int size() { return entries.size(); }

    public List<Result> search(String query, int limit) {
        String q = normalize(query);
        List<Result> out = new ArrayList<>();
        for (Entry e : entries.values()) {
            int s = q.isEmpty() ? 1 : score(e, q);
            if (s > 0) out.add(new Result(e, s));
        }
        out.sort(Comparator.comparingInt(Result::score).reversed().thenComparing(r -> r.entry().title()));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private static int score(Entry e, String q) {
        String title = normalize(e.title());
        int best = fieldScore(title, q) * 3;
        for (String k : e.keywords()) best = Math.max(best, fieldScore(normalize(k), q) * 2);
        best = Math.max(best, fieldScore(normalize(e.category()), q));
        return best;
    }

    /** Prefix > word-prefix > substring > in-order subsequence (fuzzy). 0 = no match. */
    private static int fieldScore(String field, String q) {
        if (field.startsWith(q)) return 100;
        for (String word : field.split(" ")) if (word.startsWith(q)) return 80;
        if (field.contains(q)) return 60;
        return isSubsequence(q, field) && q.length() >= 3 ? 30 : 0;
    }

    private static boolean isSubsequence(String q, String s) {
        int i = 0;
        for (int j = 0; j < s.length() && i < q.length(); j++) if (s.charAt(j) == q.charAt(i)) i++;
        return i == q.length();
    }

    private static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }
}
