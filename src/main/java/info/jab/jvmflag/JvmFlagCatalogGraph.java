package info.jab.jvmflag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
    private final Map<String, List<String>> sourcesByTarget;

    private JvmFlagCatalogGraph(
            Map<String, JsonNode> nodesById,
            Map<String, List<String>> targetsBySource,
            Map<String, List<String>> sourcesByTarget) {
        this.nodesById = nodesById;
        this.targetsBySource = targetsBySource;
        this.sourcesByTarget = sourcesByTarget;
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
        Map<String, List<String>> sourcesByTarget = new HashMap<>();
        for (JsonNode edge : root.path("edges")) {
            String source = edge.path("source").asText();
            String target = edge.path("target").asText();
            targetsBySource.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
            sourcesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        }

        return new JvmFlagCatalogGraph(nodesById, targetsBySource, sourcesByTarget);
    }

    /** All graph nodes keyed by id (categories, subcategories, flags, root). */
    public Collection<JsonNode> nodes() {
        return nodesById.values();
    }

    /**
     * Flag nodes with no incoming edge in {@code edges} (not linked to any category or subcategory).
     */
    public List<OrphanFlag> findOrphanFlags() {
        List<OrphanFlag> orphans = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            if (!"flag".equals(nodeType(node))) {
                continue;
            }
            String id = node.path("id").asText();
            if (!sourcesByTarget.containsKey(id)) {
                orphans.add(new OrphanFlag(id, nodeLabel(node), textOrNull(node, "flag")));
            }
        }
        orphans.sort((a, b) -> a.id().compareTo(b.id()));
        return orphans;
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
     * Categories with no incoming edge from a {@code domain} node (and/or invalid {@code parent} metadata).
     */
    public List<OrphanCategory> findCategoriesWithoutDomain() {
        List<OrphanCategory> orphans = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            if (!"category".equals(nodeType(node))) {
                continue;
            }
            String id = node.path("id").asText();
            String parentField = textOrNull(node, "parent");
            String domainIdFromEdge = null;
            for (String sourceId : sourcesFor(id)) {
                if ("domain".equals(nodeType(sourceId))) {
                    domainIdFromEdge = sourceId;
                    break;
                }
            }
            boolean missingDomainEdge = domainIdFromEdge == null;
            boolean invalidParentField =
                    parentField == null || parentField.isEmpty() || !"domain".equals(nodeType(parentField));
            boolean parentEdgeMismatch =
                    domainIdFromEdge != null
                            && parentField != null
                            && !parentField.isEmpty()
                            && !domainIdFromEdge.equals(parentField);
            if (missingDomainEdge || invalidParentField || parentEdgeMismatch) {
                orphans.add(new OrphanCategory(id, nodeLabel(node), parentField, domainIdFromEdge));
            }
        }
        orphans.sort((a, b) -> a.id().compareTo(b.id()));
        return orphans;
    }

    /**
     * Categories still linked directly from {@code root} (expected: none; use domains).
     */
    public List<String> findCategoriesLinkedDirectlyToRoot() {
        List<String> ids = new ArrayList<>();
        for (String targetId : targetsFor("root")) {
            if ("category".equals(nodeType(targetId))) {
                ids.add(targetId);
            }
        }
        Collections.sort(ids);
        return ids;
    }

    /**
     * Categories that still have flag nodes linked directly (expected: none; flags belong under subcategories).
     */
    public List<CategoryWithDirectFlags> findCategoriesWithDirectFlags() {
        List<CategoryWithDirectFlags> result = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            if (!"category".equals(nodeType(node))) {
                continue;
            }
            String id = node.path("id").asText();
            List<String> flagIds = new ArrayList<>();
            for (String targetId : targetsFor(id)) {
                if ("flag".equals(nodeType(targetId))) {
                    flagIds.add(targetId);
                }
            }
            if (!flagIds.isEmpty()) {
                flagIds.sort(String::compareTo);
                result.add(new CategoryWithDirectFlags(id, nodeLabel(node), flagIds.size()));
            }
        }
        result.sort((a, b) -> a.id().compareTo(b.id()));
        return result;
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

    /** Number of flag nodes linked directly from {@code nodeId} (category or subcategory). */
    public int directFlagCount(String nodeId) {
        int count = 0;
        for (String targetId : targetsFor(nodeId)) {
            if ("flag".equals(nodeType(targetId))) {
                count++;
            }
        }
        return count;
    }

    private int countDirectFlags(String nodeId) {
        return directFlagCount(nodeId);
    }

    private List<String> targetsFor(String sourceId) {
        List<String> targets = targetsBySource.get(sourceId);
        return targets == null ? Collections.<String>emptyList() : targets;
    }

    private List<String> sourcesFor(String targetId) {
        List<String> sources = sourcesByTarget.get(targetId);
        return sources == null ? Collections.<String>emptyList() : sources;
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

    public static final class OrphanCategory {

        private final String id;
        private final String label;
        private final String parentField;
        private final String domainIdFromEdge;

        OrphanCategory(String id, String label, String parentField, String domainIdFromEdge) {
            this.id = id;
            this.label = label;
            this.parentField = parentField;
            this.domainIdFromEdge = domainIdFromEdge;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public String parentField() {
            return parentField;
        }

        public String domainIdFromEdge() {
            return domainIdFromEdge;
        }

        @Override
        public String toString() {
            return "category '" + label + "' (" + id + ", parent=" + parentField + ", domainEdge=" + domainIdFromEdge + ")";
        }
    }

    public static final class OrphanFlag {

        private final String id;
        private final String label;
        private final String flag;

        OrphanFlag(String id, String label, String flag) {
            this.id = id;
            this.label = label;
            this.flag = flag;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public String flag() {
            return flag;
        }

        @Override
        public String toString() {
            String name = flag != null ? flag : label;
            return name + " (" + id + ")";
        }
    }

    public static final class CategoryWithDirectFlags {

        private final String id;
        private final String label;
        private final int directFlagCount;

        CategoryWithDirectFlags(String id, String label, int directFlagCount) {
            this.id = id;
            this.label = label;
            this.directFlagCount = directFlagCount;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public int directFlagCount() {
            return directFlagCount;
        }

        @Override
        public String toString() {
            return "category '" + label + "' (" + id + ", " + directFlagCount + " direct flags)";
        }
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
