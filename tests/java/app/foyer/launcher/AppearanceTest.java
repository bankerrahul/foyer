package app.foyer.launcher;

public final class AppearanceTest {
    private static void equal(int actual, int expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        Appearance defaults = new Appearance("auto", "normal", "gradient", false, true, true);
        equal(defaults.tileSize(3), 232);
        equal(defaults.tileSize(20), 172);
        equal(defaults.columns(20), 6);
        equal(defaults.gap(3), 64);
        equal(defaults.stride(20), 228);
        Appearance invalid = new Appearance("unknown", "unknown", "unknown", false, true, true);
        equal(invalid.tileSize(3), 232);
        if (!invalid.theme.equals("gradient")) throw new AssertionError("Unknown background must use default");
        int cases = 0;
        for (String size : new String[] {"auto", "small", "medium", "large"}) {
            for (String spacing : new String[] {"compact", "normal", "spacious"}) {
                Appearance a = new Appearance(size, spacing, "black", true, false, false);
                for (int count : new int[] {0, 1, 3, 6, 7, 40}) {
                    int columns = a.columns(count), tile = a.tileSize(count), gap = a.gap(count);
                    if (columns < 1 || columns * tile + (columns - 1) * gap > 1824)
                        throw new AssertionError("Tiles overflow for " + size + "/" + spacing);
                    if (gap < tile * .08 + 16) throw new AssertionError("Focused tiles overlap");
                    if (a.stride(count) < tile + 48) throw new AssertionError("Captions overlap rows");
                    cases++;
                }
            }
        }
        System.out.println(cases + " appearance layout cases passed");
    }
}
