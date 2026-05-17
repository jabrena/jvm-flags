package info.jab.jvmflag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps catalog {@code -XX:} flag strings to HotSpot internal names from {@code -XX:+PrintFlagsFinal}.
 * Launcher options ({@code -Xms}, {@code -version}, module flags, etc.) do not map and are outside
 * PrintFlagsFinal catalog sync.
 */
final class JvmFlagPrintFlagsFinalMapper {

    private JvmFlagPrintFlagsFinalMapper() {}

    /**
     * One catalog entry per PrintFlagsFinal internal name present in {@code snapshotNames}.
     * Fails fast when two catalog flags map to the same snapshot name.
     */
    static Map<String, JvmFlagEntry> catalogEntriesByPrintFlagsFinalName(
            List<JvmFlagEntry> entries, Set<String> snapshotNames, int javaFeatureVersion) {
        Map<String, JvmFlagEntry> byName = new LinkedHashMap<>();
        for (JvmFlagEntry entry : entries) {
            Optional<String> printFlagsFinalName = toPrintFlagsFinalName(entry.flag().flag(), javaFeatureVersion);
            if (!printFlagsFinalName.isPresent()) {
                continue;
            }
            String name = printFlagsFinalName.get();
            if (!snapshotNames.contains(name)) {
                continue;
            }
            JvmFlagEntry previous = byName.put(name, entry);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate catalog mapping to PrintFlagsFinal flag "
                                + name
                                + ": "
                                + previous.flag().flag()
                                + " and "
                                + entry.flag().flag());
            }
        }
        return Collections.unmodifiableMap(byName);
    }

    static Optional<String> toPrintFlagsFinalName(String catalogFlag) {
        return toPrintFlagsFinalName(catalogFlag, 8);
    }

    static Optional<String> toPrintFlagsFinalName(String catalogFlag, int javaFeatureVersion) {
        if (!catalogFlag.startsWith("-XX:")) {
            return Optional.empty();
        }
        String rest = catalogFlag.substring(4);
        if (rest.startsWith("+") || rest.startsWith("-")) {
            return Optional.of(rest.substring(1));
        }
        int equalsIndex = rest.indexOf('=');
        String name = equalsIndex >= 0 ? rest.substring(0, equalsIndex) : rest;
        return Optional.of(name);
    }
}
