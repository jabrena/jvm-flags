package info.jab.jvmflag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PrintFlagsFinalSnapshot {

    private static final String SNAPSHOT_RESOURCE_PREFIX = "printflagsfinal/java-";
    private static final String SNAPSHOT_RESOURCE_SUFFIX = "-hotspot.md";

    static final class Flag {
        private final String name;
        private final String type;
        private final String value;
        private final String tags;

        Flag(String name, String type, String value, String tags) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.tags = tags;
        }

        String name() {
            return name;
        }

        String type() {
            return type;
        }

        String value() {
            return value;
        }

        String tags() {
            return tags;
        }
    }

    private static final Pattern FLAG_LINE = Pattern.compile(
            "\\s+(bool|int|intx|uint|uintx|uint64_t|ccstr|ccstrlist|double|size_t)\\s+(\\S+)\\s+(?:=|:=)\\s*(\\S*)\\s+\\{([^}]*)\\}");

    private PrintFlagsFinalSnapshot() {}

    static String snapshotResourceName(int javaFeatureVersion) {
        return SNAPSHOT_RESOURCE_PREFIX + javaFeatureVersion + SNAPSHOT_RESOURCE_SUFFIX;
    }

    static boolean hasSnapshotForJavaVersion(int javaFeatureVersion) {
        return PrintFlagsFinalSnapshot.class
                        .getClassLoader()
                        .getResource(snapshotResourceName(javaFeatureVersion))
                != null;
    }

    static String loadText(int javaFeatureVersion) throws IOException {
        String resource = snapshotResourceName(javaFeatureVersion);
        try (InputStream input = PrintFlagsFinalSnapshot.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }

    static Map<String, Flag> parseFlags(String printFlagsFinalOutput) {
        Map<String, Flag> flags = new LinkedHashMap<>();
        Matcher matcher = FLAG_LINE.matcher(printFlagsFinalOutput);
        while (matcher.find()) {
            flags.put(
                    matcher.group(2),
                    new Flag(matcher.group(2), matcher.group(1), matcher.group(3), matcher.group(4).trim()));
        }
        return Collections.unmodifiableMap(flags);
    }

    static Set<String> parseFlagNames(String printFlagsFinalOutput) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(parseFlags(printFlagsFinalOutput).keySet()));
    }
}
