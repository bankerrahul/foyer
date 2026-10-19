package app.foyer.launcher;

final class Appearance {
    final String size, spacing, theme;
    final boolean labels, date, clock;

    Appearance(String size, String spacing, String theme, boolean labels, boolean date, boolean clock) {
        this.size = choice(size, "auto", "small", "medium", "large");
        this.spacing = choice(spacing, "normal", "compact", "spacious");
        this.theme = choice(theme, "gradient", "charcoal", "black");
        this.labels = labels;
        this.date = date;
        this.clock = clock;
    }

    private static String choice(String value, String... choices) {
        for (String candidate : choices) if (candidate.equals(value)) return value;
        return choices[0];
    }

    int tileSize(int count) {
        if (size.equals("small")) return 172;
        if (size.equals("medium")) return 216;
        if (size.equals("large")) return 264;
        return count > 6 ? 172 : 232;
    }

    int gap(int count) {
        if (spacing.equals("compact")) return 40;
        if (spacing.equals("spacious")) return 88;
        return size.equals("auto") && count > 6 ? 60 : 64;
    }

    int columns(int count) {
        int fitting = (1824 + gap(count)) / (tileSize(count) + gap(count));
        return Math.max(1, Math.min(size.equals("auto") ? 6 : 8, fitting));
    }

    int stride(int count) { return tileSize(count) + 56 + (spacing.equals("spacious") ? 24 : 0); }
}
