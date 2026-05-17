package info.jab.jvmflag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only view of a versioned JVM flag catalog graph ({@code nodes} + {@code edges}).
 */
public final class JvmFlagCatalogGraph {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, JsonNode> nodesById;
    private final Map<String, List<String>> targetsBySource;

    private JvmFlagCatalogGraph(Map<String, JsonNode> nodesById, Map<String, List<String>> targetsBySource) {
        this.nodesById = nodesById;
        this.targetsBySource = targetsBySource;
    }

    public static JvmFlagCatalogGraph loadForJavaVersion(int feature) throws IOException {
        String resourceName = "java-" + feature + ".json";
        try (InputStream input = JvmFlagCatalogGraph.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException(
                        "No JVM flag catalog for Java " + feature + " (missing resource " + resourceName + ")");
            }
            return fromRoot(MAPPER.readTree(input));
        }
    }

    static JvmFlagCatalogGraph fromRoot(JsonNode root) {
        Map<String, JsonNode> nodesById = new HashMap<>();
        for (JsonNode node : root.path("nodes")) {
            nodesById.put(node.path("id").asText(), node);
        }

        Map<String, List<String>> targetsBySource = new HashMap<>();
        for (JsonNode edge : root.path("edges")) {
            String source = edge.path("source").asText();
            String target = edge.path("target").asText();
            targetsBySource.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
        }

        return new JvmFlagCatalogGraph(nodesById, targetsBySource);
    }

    /**
     * Subcategories that are not linked to any flag node in {@code edges}.
     */
    public List<EmptyContainer> findEmptySubcategories() {
        List<EmptyContainer> empty = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            if (!"subcategory".equals(nodeType(node))) {
                continue;
            }
            String id = node.path("id").asText();
            if (countDirectFlags(id) == 0) {
                empty.add(new EmptyContainer(
                        id,
                        nodeLabel(node),
                        "subcategory",
                        textOrNull(node, "parent")));
            }
        }
        return empty;
    }

    /**
     * Top-level categories with no subcategory children and no direct flag children in {@code edges}.
     */
    public List<EmptyContainer> findEmptyLeafCategories() {
        List<EmptyContainer> empty = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            if (!"category".equals(nodeType(node))) {
                continue;
            }
            String id = node.path("id").asText();
            if (hasSubcategoryChild(id)) {
                continue;
            }
            if (countDirectFlags(id) == 0) {
                empty.add(new EmptyContainer(id, nodeLabel(node), "category", null));
            }
        }
        return empty;
    }

    private boolean hasSubcategoryChild(String nodeId) {
        for (String targetId : targetsFor(nodeId)) {
            if ("subcategory".equals(nodeType(targetId))) {
                return true;
            }
        }
        return false;
    }

    private int countDirectFlags(String nodeId) {
        int count = 0;
        for (String targetId : targetsFor(nodeId)) {
            if ("flag".equals(nodeType(targetId))) {
                count++;
            }
        }
        return count;
    }

    private List<String> targetsFor(String sourceId) {
        List<String> targets = targetsBySource.get(sourceId);
        return targets == null ? Collections.<String>emptyList() : targets;
    }

    private String nodeType(String nodeId) {
        JsonNode node = nodesById.get(nodeId);
        return node == null ? "" : nodeType(node);
    }

    private static String nodeType(JsonNode node) {
        return node.path("type").asText();
    }

    private static String nodeLabel(JsonNode node) {
        return node.path("label").asText();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static final class EmptyContainer {

        private final String id;
        private final String label;
        private final String containerType;
        private final String parentId;

        EmptyContainer(String id, String label, String containerType, String parentId) {
            this.id = id;
            this.label = label;
            this.containerType = containerType;
            this.parentId = parentId;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public String containerType() {
            return containerType;
        }

        public String parentId() {
            return parentId;
        }

        @Override
        public String toString() {
            if (parentId == null) {
                return containerType + " '" + label + "' (" + id + ")";
            }
            return containerType + " '" + label + "' (" + id + ", parent=" + parentId + ")";
        }
    }
}
