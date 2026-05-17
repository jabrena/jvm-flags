package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JvmFlagCatalogTaxonomyTest {

    private static CatalogTaxonomy taxonomy;

    @BeforeAll
    static void loadTaxonomy() throws IOException {
        taxonomy = CatalogTaxonomy.load();
    }

    @Test
    void taxonomyDefinesExpectedContainerCounts() {
        assertThat(taxonomy.domainCount()).isEqualTo(6);
        assertThat(taxonomy.categoryCount()).isEqualTo(17);
        assertThat(taxonomy.subcategoryCount()).isEqualTo(97);
    }

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void catalogMatchesCanonicalTaxonomy(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);
        Set<String> domainIds = new HashSet<>();
        Set<String> categoryIds = new HashSet<>();
        Set<String> subcategoryIds = new HashSet<>();

        for (JsonNode node : graph.nodes()) {
            String type = node.path("type").asText();
            if ("domain".equals(type)) {
                domainIds.add(node.path("id").asText());
            } else if ("category".equals(type)) {
                categoryIds.add(node.path("id").asText());
            } else if ("subcategory".equals(type)) {
                subcategoryIds.add(node.path("id").asText());
            }
        }

        assertThat(domainIds)
                .as("java-%s.json domains", featureVersion)
                .containsExactlyInAnyOrderElementsOf(taxonomy.domainIds());
        assertThat(categoryIds)
                .as("java-%s.json categories", featureVersion)
                .containsExactlyInAnyOrderElementsOf(taxonomy.categoryIds());
        assertThat(subcategoryIds)
                .as("java-%s.json subcategories", featureVersion)
                .containsExactlyInAnyOrderElementsOf(taxonomy.subcategoryIds());

        assertThat(graph.findCategoriesLinkedDirectlyToRoot())
                .as("java-%s.json must link categories under domains, not root", featureVersion)
                .isEmpty();
    }
}
