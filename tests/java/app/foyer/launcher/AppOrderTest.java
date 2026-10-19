package app.foyer.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AppOrderTest {
    private static void equal(Object actual, Object expected) {
        if (!actual.equals(expected)) throw new AssertionError(actual + " != " + expected);
    }

    public static void main(String[] args) {
        List<String> installed = Arrays.asList("apple", "plex", "tv", "youtube", "zee");
        equal(AppOrder.reconcile(installed, Arrays.asList()), installed);
        equal(AppOrder.reconcile(installed, Arrays.asList("tv", "plex")),
                Arrays.asList("tv", "plex", "apple", "youtube", "zee"));
        equal(AppOrder.reconcile(installed, Arrays.asList("missing", "plex", "plex")),
                Arrays.asList("plex", "apple", "tv", "youtube", "zee"));
        equal(AppOrder.reconcile(Arrays.asList("apple", "apple", "tv"), Arrays.asList()),
                Arrays.asList("apple", "tv"));
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) many.add("app" + i);
        equal(AppOrder.reconcile(many, Arrays.asList()).size(), 40);
        equal(AppOrder.move(installed, "youtube", 0), Arrays.asList("youtube", "apple", "plex", "tv", "zee"));
        equal(AppOrder.move(installed, "apple", 99), Arrays.asList("plex", "tv", "youtube", "zee", "apple"));
        equal(AppOrder.move(installed, "zee", -10), Arrays.asList("zee", "apple", "plex", "tv", "youtube"));
        equal(AppOrder.move(installed, "absent", 0), installed);
        equal(installed, Arrays.asList("apple", "plex", "tv", "youtube", "zee"));
        equal(AppOrder.reconcile(Arrays.asList("tv", "new"), Arrays.asList("old", "tv")), Arrays.asList("tv", "new"));
        equal(AppOrder.home(installed, Arrays.asList("tv", "plex"), Arrays.asList("plex", "zee")),
                Arrays.asList("tv", "apple", "youtube"));
        equal(installed, Arrays.asList("apple", "plex", "tv", "youtube", "zee"));
        equal(AppOrder.home(installed, Arrays.asList("tv", "plex"), Arrays.asList()),
                Arrays.asList("tv", "plex", "apple", "youtube", "zee"));
        equal(AppOrder.home(installed, Arrays.asList(), installed), Arrays.asList());
        equal(AppOrder.home(Arrays.asList("tv", "new"), Arrays.asList("old", "tv"), Arrays.asList("tv", "old")),
                Arrays.asList("new"));
        equal(AppOrder.reconcile(installed, Arrays.asList()).size(), 5);
        equal(AppOrder.mergePresent(Arrays.asList("hdmi1", "missing", "hdmi2"), Arrays.asList("hdmi2", "hdmi1")),
                Arrays.asList("hdmi2", "missing", "hdmi1"));
        equal(AppOrder.mergePresent(Arrays.asList("missing", "hdmi1"), Arrays.asList("new", "hdmi1")),
                Arrays.asList("missing", "new", "hdmi1"));
        equal(AppOrder.mergePresent(Arrays.asList("hdmi1", "missing"), Arrays.asList()), Arrays.asList("hdmi1", "missing"));
        System.out.println("20 app-order cases passed");
    }
}
