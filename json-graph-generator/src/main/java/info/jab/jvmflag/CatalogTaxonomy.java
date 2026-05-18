package info.jab.jvmflag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Canonical domain, category, and subcategory ids shared by every versioned JVM flag catalog.
 */
public final class CatalogTaxonomy {

    private static final String RESOURCE = "catalog-taxonomy.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> domainLabels;
    private final Map<String, String> categoryLabels;
    private final Map<String, String> categoryDomainIds;
    private final Map<String, Subcategory> subcategories;

    private CatalogTaxonomy(
            Map<String, String> domainLabels,
            Map<String, String> categoryLabels,
            Map<String, String> categoryDomainIds,
            Map<String, Subcategory> subcategories) {
        this.domainLabels = domainLabels;
        this.categoryLabels = categoryLabels;
        this.categoryDomainIds = categoryDomainIds;
        this.subcategories = subcategories;
    }

    public static CatalogTaxonomy load() throws IOException {
        try (InputStream input = CatalogTaxonomy.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing classpath resource " + RESOURCE);
            }
            return fromRoot(MAPPER.readTree(input));
        }
    }

    static CatalogTaxonomy fromRoot(JsonNode root) {
        Map<String, String> domains = new LinkedHashMap<>();
        for (JsonNode node : root.path("domains")) {
            domains.put(node.path("id").asText(), node.path("label").asText());
        }

        Map<String, String> categories = new LinkedHashMap<>();
        Map<String, String> categoryDomains = new LinkedHashMap<>();
        for (JsonNode node : root.path("categories")) {
            String id = node.path("id").asText();
            categories.put(id, node.path("label").asText());
            categoryDomains.put(id, node.path("parent").asText());
        }

        Map<String, Subcategory> subcategories = new LinkedHashMap<>();
        for (JsonNode node : root.path("subcategories")) {
            String id = node.path("id").asText();
            subcategories.put(
                    id,
                    new Subcategory(id, node.path("label").asText(), node.path("parent").asText()));
        }

        return new CatalogTaxonomy(
                Collections.unmodifiableMap(domains),
                Collections.unmodifiableMap(categories),
                Collections.unmodifiableMap(categoryDomains),
                Collections.unmodifiableMap(subcategories));
    }

    public Set<String> domainIds() {
        return domainLabels.keySet();
    }

    public Set<String> categoryIds() {
        return categoryLabels.keySet();
    }

    public Set<String> subcategoryIds() {
        return subcategories.keySet();
    }

    public String domainLabel(String id) {
        return domainLabels.get(id);
    }

    public String categoryLabel(String id) {
        return categoryLabels.get(id);
    }

    public String categoryDomainId(String categoryId) {
        return categoryDomainIds.get(categoryId);
    }

    public Subcategory subcategory(String id) {
        return subcategories.get(id);
    }

    public int domainCount() {
        return domainLabels.size();
    }

    public int categoryCount() {
        return categoryLabels.size();
    }

    public int subcategoryCount() {
        return subcategories.size();
    }

    public static final class Subcategory {

        private final String id;
        private final String label;
        private final String parentId;

        Subcategory(String id, String label, String parentId) {
            this.id = id;
            this.label = label;
            this.parentId = parentId;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public String parentId() {
            return parentId;
        }
    }
}
