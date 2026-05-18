package info.jab.jvmflag;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** JVM implementations tracked in catalog {@code jvm} metadata on flag nodes. */
public final class JvmImplementation {

    public static final String HOTSPOT = "hotspot";
    public static final String GRAALVM = "graalvm";

    private static final List<String> KNOWN = Collections.unmodifiableList(Arrays.asList(HOTSPOT, GRAALVM));

    private JvmImplementation() {}

    public static List<String> knownImplementations() {
        return KNOWN;
    }

    /**
     * Detects the running JVM for catalog verification ({@link JvmFlagTest}) and snapshot sync.
     * GraalVM builds report {@code GraalVM} in {@code java.vm.name} and/or {@code java.vendor}.
     */
    public static String current() {
        String vmName = System.getProperty("java.vm.name", "");
        String vendor = System.getProperty("java.vendor", "");
        if (vmName.contains("GraalVM") || vendor.contains("GraalVM")) {
            return GRAALVM;
        }
        return HOTSPOT;
    }

    public static boolean isKnown(String implementation) {
        return KNOWN.contains(implementation);
    }
}
