package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Ensures every canonical subcategory is used: at least one Java version must link a flag
 * directly under it. Subcategories empty on every version are removable taxonomy shells.
 */
class JvmFlagCatalogEmptySubcategoriesTest {

    private static final List<Integer> JAVA_VERSIONS = Arrays.asList(8, 11, 17, 21, 25);

    private static CatalogTaxonomy taxonomy;

    @BeforeAll
    static void loadTaxonomy() throws IOException {
        taxonomy = CatalogTaxonomy.load();
    }

    @Test
    void everySubcategoryHasAtLeastOneFlagInSomeJavaVersion() throws IOException {
        List<String> emptyInAllVersions = new ArrayList<>();

        for (String subcategoryId : taxonomy.subcategoryIds()) {
            boolean hasDirectFlag = false;
            for (int featureVersion : JAVA_VERSIONS) {
                JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);
                if (graph.directFlagCount(subcategoryId) > 0) {
                    hasDirectFlag = true;
                    break;
                }
            }
            if (!hasDirectFlag) {
                CatalogTaxonomy.Subcategory subcategory = taxonomy.subcategory(subcategoryId);
                emptyInAllVersions.add(
                        subcategory.label()
                                + " ("
                                + subcategoryId
                                + ", parent="
                                + subcategory.parentId()
                                + ")");
            }
        }

        assertThat(emptyInAllVersions)
                .as(
                        "Subcategories with no direct flag in any of %s; remove from catalog-taxonomy.json and version catalogs",
                        JAVA_VERSIONS)
                .isEmpty();
    }
}
