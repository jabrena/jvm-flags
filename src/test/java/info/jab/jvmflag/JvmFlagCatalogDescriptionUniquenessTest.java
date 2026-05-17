package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JvmFlagCatalogDescriptionUniquenessTest {

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void flagDescriptionsAreUnique(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        Map<String, List<String>> flagsByDescription = new LinkedHashMap<>();
        for (JvmFlagEntry entry : JvmFlagCatalog.loadForJavaVersion(featureVersion)) {
            flagsByDescription
                    .computeIfAbsent(entry.flag().description(), ignored -> new ArrayList<>())
                    .add(entry.flag().flag());
        }

        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<String>> group : flagsByDescription.entrySet()) {
            if (group.getValue().size() > 1) {
                violations.add(formatDuplicate(group.getKey(), group.getValue()));
            }
        }

        assertThat(violations)
                .as("java-%s.json must not assign the same description to multiple flags", featureVersion)
                .isEmpty();
    }

    private static String formatDuplicate(String description, List<String> flags) {
        String label = description == null ? "<missing>" : "'" + description + "'";
        return label + " used by " + flags;
    }
}
