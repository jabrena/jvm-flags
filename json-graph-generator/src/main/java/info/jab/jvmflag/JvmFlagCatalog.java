package info.jab.jvmflag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class JvmFlagCatalog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JvmFlagCatalog() {}

    public static List<JvmFlagEntry> loadForCurrentJavaVersion() throws IOException {
        return loadForJavaVersion(currentJavaFeatureVersion());
    }

    public static int currentJavaFeatureVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, version.indexOf('.', 2)));
        }
        int separator = version.indexOf('.');
        if (separator < 0) {
            separator = version.indexOf('-');
        }
        return Integer.parseInt(version.substring(0, separator < 0 ? version.length() : separator));
    }

    public static List<JvmFlagEntry> loadForJavaVersion(int feature) throws IOException {
        String resourceName = "java-" + feature + ".json";
        try (InputStream input = JvmFlagCatalog.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException(
                        "No JVM flag catalog for Java " + feature + " (missing resource " + resourceName + ")");
            }
            return parseEntries(MAPPER.readTree(input));
        }
    }

    public static boolean hasCatalogForJavaVersion(int feature) {
        String resourceName = "java-" + feature + ".json";
        return JvmFlagCatalog.class.getClassLoader().getResource(resourceName) != null;
    }

    private static List<JvmFlagEntry> parseEntries(JsonNode root) {
        List<JvmFlagEntry> entries = new ArrayList<>();
        for (JsonNode node : root.path("nodes")) {
            if (!"flag".equals(node.path("type").asText())) {
                continue;
            }
            JvmFlag flag = new JvmFlag(
                    node.path("id").asText(),
                    node.path("flag").asText(),
                    textOrNull(node, "description"));
            JvmFlagTestSpec testSpec = new JvmFlagTestSpec(
                    textOrNull(node, "testValue"),
                    readStringArray(node, "requires"),
                    readStringArray(node, "testArgs"),
                    !node.has("testable") || node.path("testable").asBoolean(true),
                    node.path("acceptNonZeroExit").asBoolean(false));
            entries.add(new JvmFlagEntry(flag, testSpec, readJvmImplementations(node)));
        }
        return entries;
    }

    private static List<String> readJvmImplementations(JsonNode node) {
        JsonNode jvm = node.get("jvm");
        if (jvm == null || !jvm.isArray() || jvm.isEmpty()) {
            return Collections.singletonList(JvmImplementation.HOTSPOT);
        }
        List<String> values = new ArrayList<>();
        for (Iterator<JsonNode> it = jvm.elements(); it.hasNext(); ) {
            values.add(it.next().asText());
        }
        return values;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static List<String> readStringArray(JsonNode node, String field) {
        JsonNode array = node.get(field);
        if (array == null || !array.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
            values.add(it.next().asText());
        }
        return values;
    }
}
