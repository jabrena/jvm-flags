package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PrintFlagsFinalCatalogTest {

    @Test
    @EnabledIf("hasPrintFlagsFinalSnapshotForCurrentJava")
    void allCatalogFlagsAppearInPrintFlagsFinalSnapshot() throws IOException {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        assertPrintFlagsFinalCatalogMatchesSnapshot(featureVersion, JvmImplementation.current());
    }

    static boolean hasPrintFlagsFinalSnapshotForCurrentJava() {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        String implementation = JvmImplementation.current();
        return JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion)
                && PrintFlagsFinalSnapshot.hasSnapshotForJavaVersion(featureVersion, implementation);
    }

    @ParameterizedTest(name = "java-{0}.json vs hotspot snapshot")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void printFlagsFinalHotspotSnapshotMatchesCatalog(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));
        assumeTrue(PrintFlagsFinalSnapshot.hasSnapshotForJavaVersion(featureVersion, JvmImplementation.HOTSPOT));

        assertPrintFlagsFinalCatalogMatchesSnapshot(featureVersion, JvmImplementation.HOTSPOT);
    }

    @ParameterizedTest(name = "java-{0}.json vs graalvm snapshot")
    @ValueSource(ints = {17, 21, 25})
    void printFlagsFinalGraalvmSnapshotMatchesCatalog(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));
        assumeTrue(PrintFlagsFinalSnapshot.hasGraalvmSnapshotForJavaVersion(featureVersion));

        assertPrintFlagsFinalCatalogMatchesSnapshot(featureVersion, JvmImplementation.GRAALVM);
    }

    private static void assertPrintFlagsFinalCatalogMatchesSnapshot(
            int featureVersion, String jvmImplementation) throws IOException {
        String snapshotResource = PrintFlagsFinalSnapshot.snapshotResourceName(featureVersion, jvmImplementation);
        Set<String> snapshotNames = PrintFlagsFinalSnapshot.parseFlagNames(
                PrintFlagsFinalSnapshot.loadText(featureVersion, jvmImplementation));
        List<JvmFlagEntry> entriesForJvm = JvmFlagCatalog.loadForJavaVersion(featureVersion).stream()
                .filter(entry -> entry.supportsJvm(jvmImplementation))
                .collect(Collectors.toList());

        assertThat(entriesForJvm)
                .as(
                        "java-%s.json flags for %s must match %s (%d expected, no launcher or extra -XX options)",
                        featureVersion,
                        jvmImplementation,
                        snapshotResource,
                        snapshotNames.size())
                .hasSize(snapshotNames.size());

        assertThat(JvmFlagPrintFlagsFinalMapper.catalogFlagsOutsideSnapshot(
                        entriesForJvm, snapshotNames, featureVersion))
                .as(
                        "java-%s.json must not contain %s flags outside %s",
                        featureVersion,
                        jvmImplementation,
                        snapshotResource)
                .isEmpty();

        Map<String, JvmFlagEntry> catalogByPrintFlagsFinalName =
                JvmFlagPrintFlagsFinalMapper.catalogEntriesByPrintFlagsFinalName(
                        entriesForJvm, snapshotNames, featureVersion);

        assertThat(catalogByPrintFlagsFinalName)
                .as(
                        "java-%s.json must have exactly one catalog entry per PrintFlagsFinal flag in %s",
                        featureVersion,
                        snapshotResource)
                .hasSize(snapshotNames.size())
                .containsOnlyKeys(snapshotNames);
    }
}
