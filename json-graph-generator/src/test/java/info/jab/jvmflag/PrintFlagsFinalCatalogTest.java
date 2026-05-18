package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PrintFlagsFinalCatalogTest {

    @Test
    @EnabledIf("hasPrintFlagsFinalSnapshotForCurrentJava")
    void allCatalogFlagsAppearInPrintFlagsFinalSnapshot() throws IOException {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        assertPrintFlagsFinalCatalogMatchesSnapshot(featureVersion);
    }

    static boolean hasPrintFlagsFinalSnapshotForCurrentJava() {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        return JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion)
                && PrintFlagsFinalSnapshot.hasSnapshotForJavaVersion(featureVersion);
    }

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void printFlagsFinalSnapshotMatchesCatalog(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));
        assumeTrue(PrintFlagsFinalSnapshot.hasSnapshotForJavaVersion(featureVersion));

        assertPrintFlagsFinalCatalogMatchesSnapshot(featureVersion);
    }

    private static void assertPrintFlagsFinalCatalogMatchesSnapshot(int featureVersion) throws IOException {
        String snapshotResource = PrintFlagsFinalSnapshot.snapshotResourceName(featureVersion);
        Set<String> snapshotNames =
                PrintFlagsFinalSnapshot.parseFlagNames(PrintFlagsFinalSnapshot.loadText(featureVersion));
        List<JvmFlagEntry> entries = JvmFlagCatalog.loadForJavaVersion(featureVersion);

        assertThat(entries)
                .as(
                        "java-%s.json must contain only PrintFlagsFinal flags from %s (%d expected, no launcher or extra -XX options)",
                        featureVersion,
                        snapshotResource,
                        snapshotNames.size())
                .hasSize(snapshotNames.size());

        assertThat(JvmFlagPrintFlagsFinalMapper.catalogFlagsOutsideSnapshot(
                        entries, snapshotNames, featureVersion))
                .as(
                        "java-%s.json must not contain flags outside %s",
                        featureVersion,
                        snapshotResource)
                .isEmpty();

        Map<String, JvmFlagEntry> catalogByPrintFlagsFinalName =
                JvmFlagPrintFlagsFinalMapper.catalogEntriesByPrintFlagsFinalName(
                        entries, snapshotNames, featureVersion);

        assertThat(catalogByPrintFlagsFinalName)
                .as(
                        "java-%s.json must have exactly one catalog entry per PrintFlagsFinal flag in %s",
                        featureVersion,
                        snapshotResource)
                .hasSize(snapshotNames.size())
                .containsOnlyKeys(snapshotNames);
    }
}
