package info.jab.jvmflag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps catalog flag strings to HotSpot internal names as listed by {@code -XX:+PrintFlagsFinal}.
 * Launcher options, system properties, and flags absent from a PrintFlagsFinal dump are excluded.
 */
final class JvmFlagPrintFlagsFinalMapper {

    private static final Set<String> EXCLUDED_FROM_SNAPSHOT_CHECK =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "-version",
                    "-showversion",
                    "-help",
                    "-X",
                    "-Xint",
                    "-Xcomp",
                    "-Xmixed",
                    "-Xbatch",
                    "-Xshare:on",
                    "-Xshare:off",
                    "-Xshare:auto",
                    "-Xverify:all",
                    "-Xverify:none",
                    "-Xloggc:<file>",
                    "-Djava.security.policy=<file>",
                    "-Dfile.encoding=UTF-8",
                    "-Duser.timezone=UTC",
                    "-Djava.io.tmpdir=<path>",
                    "-Djava.security.manager",
                    "-Djava.security.manager=allow",
                    "-Djdk.virtualThreadScheduler.parallelism=<n>",
                    "-Djdk.virtualThreadScheduler.maxPoolSize=<n>",
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+UseVectorizedMismatchIntrinsic",
                    "-XX:MonitorDeflationMax=<n>",
                    "-XX:MonitorUsedDeflationThreshold=<n>",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:G1NewSizePercent=<n>",
                    "-XX:G1MaxNewSizePercent=<n>",
                    "-XX:SharedArchiveFile=<file>",
                    "-XX:+PrintNMTStatistics",
                    "-XX:PermSize=<size>",
                    "-XX:MaxPermSize=<size>",
                    "-XX:ShenandoahUncommitDelay=<ms>",
                    "-XX:+UseEpsilonGC",
                    "-Xlog:gc",
                    "-Xlog:gc*",
                    "-Xlog:gc*:file=gc.log",
                    "-Xlog:class+load",
                    "-Xlog:safepoint",
                    "-Xlog:os",
                    "-Xlog:os+container",
                    "-Xlog:jit+compilation",
                    "-Xlog:cds",
                    "--module-path",
                    "--add-modules",
                    "--add-exports",
                    "--add-opens",
                    "--limit-modules",
                    "--patch-module")));

    private static final Map<String, String> ERGONOMIC_ALIASES = buildErgonomicAliases();
    private static final Map<String, String> JAVA_11_PRINT_FLAGS_FINAL_ALIASES = buildJava11PrintFlagsFinalAliases();

    private JvmFlagPrintFlagsFinalMapper() {}

    static boolean isExcludedFromSnapshotCheck(String catalogFlag) {
        return EXCLUDED_FROM_SNAPSHOT_CHECK.contains(catalogFlag);
    }

    static boolean isExcludedFromSnapshotCheck(String catalogFlag, int javaFeatureVersion) {
        return isExcludedFromSnapshotCheck(catalogFlag);
    }

    static Optional<String> toPrintFlagsFinalName(String catalogFlag) {
        return toPrintFlagsFinalName(catalogFlag, 8);
    }

    static Optional<String> toPrintFlagsFinalName(String catalogFlag, int javaFeatureVersion) {
        if (isExcludedFromSnapshotCheck(catalogFlag, javaFeatureVersion)) {
            return Optional.empty();
        }
        String alias = ERGONOMIC_ALIASES.get(catalogFlag);
        if (alias != null) {
            return Optional.of(alias);
        }
        if (javaFeatureVersion >= 11) {
            alias = JAVA_11_PRINT_FLAGS_FINAL_ALIASES.get(catalogFlag);
            if (alias != null) {
                return Optional.of(alias);
            }
        }
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

    private static Map<String, String> buildErgonomicAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("-Xms<size>", "InitialHeapSize");
        aliases.put("-Xmx<size>", "MaxHeapSize");
        aliases.put("-Xss<size>", "ThreadStackSize");
        aliases.put("-verbose:gc", "PrintGC");
        return Collections.unmodifiableMap(aliases);
    }

    private static Map<String, String> buildJava11PrintFlagsFinalAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("-XX:+UseAESIntrinsics", "UseAES");
        aliases.put("-XX:+UseCRC32Intrinsics", "UseCRC32");
        return Collections.unmodifiableMap(aliases);
    }
}
