package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JvmFlagCatalogGraphTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void detectsEmptySubcategoryAndLeafCategory() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"logging\",\"type\":\"category\",\"label\":\"Logging\"},"
                        + "{\"id\":\"gc-empty\",\"type\":\"subcategory\",\"label\":\"Empty\",\"parent\":\"heap\"},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"flag\":\"-Xms\"}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"heap\"},"
                        + "{\"source\":\"root\",\"target\":\"logging\"},"
                        + "{\"source\":\"heap\",\"target\":\"gc-empty\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-xms\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findEmptySubcategories())
                .extracting(JvmFlagCatalogGraph.EmptyContainer::id)
                .containsExactly("gc-empty");
        assertThat(graph.findEmptyLeafCategories())
                .extracting(JvmFlagCatalogGraph.EmptyContainer::id)
                .containsExactly("logging");
    }
}
