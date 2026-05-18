package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JvmFlagCatalogOrphanCategoriesTest {

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void catalogHasNoCategoryWithoutDomain(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);

        assertThat(graph.findCategoriesWithoutDomain())
                .as(
                        "java-%s.json must link every category to a domain (edge and parent field)",
                        featureVersion)
                .isEmpty();
    }
}
