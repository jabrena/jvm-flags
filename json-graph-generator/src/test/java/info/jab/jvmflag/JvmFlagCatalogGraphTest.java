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

    @Test
    void detectsOrphanFlag() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"label\":\"-Xms\",\"flag\":\"-Xms\"},"
                        + "{\"id\":\"flag-orphan\",\"type\":\"flag\",\"label\":\"-orphan\",\"flag\":\"-orphan\"}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"heap\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-xms\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findOrphanFlags())
                .extracting(JvmFlagCatalogGraph.OrphanFlag::id)
                .containsExactly("flag-orphan");
    }

    @Test
    void detectsCategoryWithoutDomain() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"domain-memory\",\"type\":\"domain\",\"label\":\"Memory\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"logging\",\"type\":\"category\",\"label\":\"Logging\",\"parent\":\"domain-memory\"}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"domain-memory\"},"
                        + "{\"source\":\"domain-memory\",\"target\":\"logging\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findCategoriesWithoutDomain())
                .extracting(JvmFlagCatalogGraph.OrphanCategory::id)
                .containsExactly("heap");
    }

    @Test
    void detectsCategoryWithDirectFlag() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"heap-size\",\"type\":\"subcategory\",\"label\":\"Heap Size\",\"parent\":\"heap\"},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"label\":\"-Xms\",\"flag\":\"-Xms\"},"
                        + "{\"id\":\"flag-xmx\",\"type\":\"flag\",\"label\":\"-Xmx\",\"flag\":\"-Xmx\"}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"heap\"},"
                        + "{\"source\":\"heap\",\"target\":\"heap-size\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-xms\"},"
                        + "{\"source\":\"heap-size\",\"target\":\"flag-xmx\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findCategoriesWithDirectFlags())
                .extracting(JvmFlagCatalogGraph.CategoryWithDirectFlags::id)
                .containsExactly("heap");
        assertThat(graph.findCategoriesWithDirectFlags().get(0).directFlagCount()).isEqualTo(1);
    }

    @Test
    void detectsFlagMissingJvmProperty() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"label\":\"-Xms\",\"flag\":\"-Xms\","
                        + "\"jvm\":[\"hotspot\"]},"
                        + "{\"id\":\"flag-missing\",\"type\":\"flag\",\"label\":\"-missing\",\"flag\":\"-missing\"}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"heap\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-xms\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-missing\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findFlagsMissingJvmProperty())
                .extracting(JvmFlagCatalogGraph.OrphanFlag::id)
                .containsExactly("flag-missing");
    }

    @Test
    void detectsFlagWithoutHotspotJvm() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\"},"
                        + "{\"id\":\"heap\",\"type\":\"category\",\"label\":\"Heap\"},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"label\":\"-Xms\",\"flag\":\"-Xms\","
                        + "\"jvm\":[\"hotspot\"]},"
                        + "{\"id\":\"flag-missing\",\"type\":\"flag\",\"label\":\"-missing\",\"flag\":\"-missing\"},"
                        + "{\"id\":\"flag-graal\",\"type\":\"flag\",\"label\":\"-graal\",\"flag\":\"-graal\","
                        + "\"jvm\":[\"graalvm\"]}"
                        + "],"
                        + "\"edges\":["
                        + "{\"source\":\"root\",\"target\":\"heap\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-xms\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-missing\"},"
                        + "{\"source\":\"heap\",\"target\":\"flag-graal\"}"
                        + "]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findFlagsWithoutHotspotJvm())
                .extracting(JvmFlagCatalogGraph.OrphanFlag::id)
                .containsExactly("flag-graal", "flag-missing");
    }

    @Test
    void detectsJvmPropertyOnNonFlagNode() throws Exception {
        String json =
                "{"
                        + "\"nodes\":["
                        + "{\"id\":\"root\",\"type\":\"root\",\"label\":\"Root\",\"jvm\":[\"hotspot\"]},"
                        + "{\"id\":\"flag-xms\",\"type\":\"flag\",\"label\":\"-Xms\",\"flag\":\"-Xms\","
                        + "\"jvm\":[\"hotspot\"]}"
                        + "],"
                        + "\"edges\":[]}";

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.fromRoot(MAPPER.readTree(json));

        assertThat(graph.findNonFlagNodesWithJvmProperty()).containsExactly("root (root)");
        assertThat(graph.findFlagsWithoutHotspotJvm()).isEmpty();
    }
}
