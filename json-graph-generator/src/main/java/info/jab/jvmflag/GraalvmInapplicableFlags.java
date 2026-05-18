package info.jab.jvmflag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Flags listed by GraalVM {@code -XX:+PrintFlagsFinal} that reject {@code -version} with
 * {@code flag is not applicable for this configuration} (DTrace is HotSpot/macOS-specific).
 */
public final class GraalvmInapplicableFlags {

    private static final Set<String> CATALOG_IDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "flag-pending-dtraceallocprobes",
            "flag-pending-dtracemethodprobes",
            "flag-pending-dtracemonitorprobes",
            "flag-pending-extendeddtraceprobes")));

    private GraalvmInapplicableFlags() {}

    public static boolean isVerifiable(JvmFlagEntry entry) {
        return !CATALOG_IDS.contains(entry.flag().id());
    }
}
