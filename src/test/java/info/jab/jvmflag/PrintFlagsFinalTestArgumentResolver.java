package info.jab.jvmflag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PrintFlagsFinalTestArgumentResolver {

    private static final String JAVA8_SNAPSHOT_RESOURCE = "printflagsfinal/java8.md";

    private static volatile Map<String, PrintFlagsFinalSnapshot.Flag> java8SnapshotFlags;

  /** Flags that need other VM options before they can be passed on the command line. */
    private static final Map<String, List<String>> EXTRA_REQUIRES = buildExtraRequires();

    private PrintFlagsFinalTestArgumentResolver() {}

    private static Map<String, List<String>> buildExtraRequires() {
        Map<String, List<String>> requires = new HashMap<>();
        for (String name :
                Arrays.asList(
                        "CMSIncrementalMode",
                        "CMSIncrementalOffset",
                        "CMSIncrementalDutyCycle",
                        "CMSIncrementalDutyCycleMin",
                        "CMSIncrementalSafetyFactor")) {
            requires.put(name, Collections.singletonList("-XX:+UseConcMarkSweepGC"));
        }
        return Collections.unmodifiableMap(requires);
    }

    static List<String> resolve(JvmFlagEntry entry) throws IOException {
        String internalName = JvmFlagPrintFlagsFinalMapper.toPrintFlagsFinalName(entry.flag().flag())
                .orElseThrow(() -> new IllegalStateException(
                        "Pending flag has no PrintFlagsFinal mapping: " + entry.flag().flag()));

        PrintFlagsFinalSnapshot.Flag snapshotFlag = loadJava8SnapshotFlags().get(internalName);
        if (snapshotFlag == null) {
            throw new IllegalStateException(
                    "Pending flag " + internalName + " missing from " + JAVA8_SNAPSHOT_RESOURCE);
        }

        List<String> arguments = new ArrayList<>();
        List<String> extra = EXTRA_REQUIRES.get(internalName);
        if (extra != null) {
            arguments.addAll(extra);
        }
        arguments.addAll(requiresFor(snapshotFlag));
        arguments.add(testArgument(snapshotFlag));
        return Collections.unmodifiableList(arguments);
    }

    private static List<String> requiresFor(PrintFlagsFinalSnapshot.Flag snapshotFlag) {
        String tags = snapshotFlag.tags();
        List<String> requires = new ArrayList<>(2);
        if (tags.contains("diagnostic")) {
            requires.add("-XX:+UnlockDiagnosticVMOptions");
        }
        if (tags.contains("experimental")) {
            requires.add("-XX:+UnlockExperimentalVMOptions");
        }
        return requires;
    }

    private static String testArgument(PrintFlagsFinalSnapshot.Flag snapshotFlag) {
        if ("bool".equals(snapshotFlag.type())) {
            return "-XX:+" + snapshotFlag.name();
        }
        return "-XX:" + snapshotFlag.name() + "=" + snapshotFlag.value();
    }

    private static Map<String, PrintFlagsFinalSnapshot.Flag> loadJava8SnapshotFlags() throws IOException {
        Map<String, PrintFlagsFinalSnapshot.Flag> cached = java8SnapshotFlags;
        if (cached != null) {
            return cached;
        }
        synchronized (PrintFlagsFinalTestArgumentResolver.class) {
            cached = java8SnapshotFlags;
            if (cached != null) {
                return cached;
            }
            cached = PrintFlagsFinalSnapshot.parseFlags(loadJava8SnapshotText());
            java8SnapshotFlags = cached;
            return cached;
        }
    }

    private static String loadJava8SnapshotText() throws IOException {
        try (InputStream input =
                PrintFlagsFinalTestArgumentResolver.class.getClassLoader().getResourceAsStream(JAVA8_SNAPSHOT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource " + JAVA8_SNAPSHOT_RESOURCE);
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
}
