package app.foyer.launcher;

import java.util.ArrayList;
import java.util.List;

final class AppOrder {
    static List<String> mergePresent(List<String> saved, List<String> present) {
        List<String> result = new ArrayList<>(saved);
        for (String key : present) if (!result.contains(key)) result.add(key);
        int next = 0;
        for (int i = 0; i < result.size(); i++)
            if (present.contains(result.get(i))) result.set(i, present.get(next++));
        return result;
    }

    static List<String> reconcile(List<String> available, List<String> preferred) {
        List<String> result = new ArrayList<>();
        for (String pkg : preferred) if (available.contains(pkg) && !result.contains(pkg)) result.add(pkg);
        for (String pkg : available) if (!result.contains(pkg)) result.add(pkg);
        return result;
    }

    static List<String> home(List<String> available, List<String> preferred, List<String> hidden) {
        List<String> result = reconcile(available, preferred);
        result.removeAll(hidden);
        return result;
    }

    static List<String> move(List<String> order, String pkg, int target) {
        List<String> result = new ArrayList<>(order);
        if (!result.remove(pkg)) return result;
        result.add(Math.max(0, Math.min(target, result.size())), pkg);
        return result;
    }
}
