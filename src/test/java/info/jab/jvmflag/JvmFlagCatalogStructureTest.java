package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JvmFlagCatalogStructureTest {

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void catalogHasNoEmptyCategoriesOrSubcategories(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);

        List<JvmFlagCatalogGraph.EmptyContainer> violations = new ArrayList<>();
        violations.addAll(graph.findEmptySubcategories());
        violations.addAll(graph.findEmptyLeafCategories());

        assertThat(violations)
                .as("java-%s.json must not contain empty categories or subcategories", featureVersion)
                .isEmpty();
    }
}
