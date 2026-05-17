package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class PrintFlagsFinalCatalogTest {

    @Test
    @EnabledIf("hasPrintFlagsFinalSnapshotForCurrentJava")
    void allCatalogFlagsAppearInPrintFlagsFinalSnapshot() throws IOException {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        assertCatalogCoveredBySnapshot(featureVersion);
    }

    static boolean hasPrintFlagsFinalSnapshotForCurrentJava() {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        return JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion)
                && PrintFlagsFinalSnapshot.hasSnapshotForJavaVersion(featureVersion);
    }

    private static void assertCatalogCoveredBySnapshot(int featureVersion) throws IOException {
        List<JvmFlagEntry> entries = JvmFlagCatalog.loadForJavaVersion(featureVersion);
        String snapshotResource = PrintFlagsFinalSnapshot.snapshotResourceName(featureVersion);
        Set<String> printFlagsFinalNames =
                PrintFlagsFinalSnapshot.parseFlagNames(PrintFlagsFinalSnapshot.loadText(featureVersion));

        List<String> missing = new ArrayList<>();
        List<String> unmapped = new ArrayList<>();
        for (JvmFlagEntry entry : entries) {
            String catalogFlag = entry.flag().flag();
            if (JvmFlagPrintFlagsFinalMapper.isExcludedFromSnapshotCheck(catalogFlag, featureVersion)) {
                continue;
            }
            Optional<String> printFlagsFinalName =
                    JvmFlagPrintFlagsFinalMapper.toPrintFlagsFinalName(catalogFlag, featureVersion);
            if (!printFlagsFinalName.isPresent()) {
                unmapped.add(catalogFlag);
                continue;
            }
            if (!printFlagsFinalNames.contains(printFlagsFinalName.get())) {
                missing.add(catalogFlag + " (expected PrintFlagsFinal name: " + printFlagsFinalName.get() + ")");
            }
        }

        assertThat(unmapped)
                .as("catalog flags without a PrintFlagsFinal mapping (add alias or exclusion)")
                .isEmpty();
        assertThat(missing)
                .as("catalog flags missing from %s", snapshotResource)
                .isEmpty();
    }
}
