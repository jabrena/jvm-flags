package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JvmFlagCatalogJvmSupportTest {

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11, 17, 21, 25})
    void flagNodesDeclareJvmSupport(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);

        assertThat(graph.findFlagsMissingJvmProperty())
                .as("java-%s.json flag nodes must declare a non-empty jvm array", featureVersion)
                .isEmpty();

        assertThat(graph.findFlagsWithUnknownJvmValues())
                .as("java-%s.json jvm values must be hotspot and/or graalvm", featureVersion)
                .isEmpty();

        assertThat(graph.findNonFlagNodesWithJvmProperty())
                .as("java-%s.json must attach jvm only to flag nodes", featureVersion)
                .isEmpty();
    }

    @ParameterizedTest(name = "java-{0}.json")
    @ValueSource(ints = {8, 11})
    void preGraalvmCatalogsAreHotspotOnly(int featureVersion) throws IOException {
        assumeTrue(JvmFlagCatalog.hasCatalogForJavaVersion(featureVersion));

        JvmFlagCatalogGraph graph = JvmFlagCatalogGraph.loadForJavaVersion(featureVersion);

        assertThat(graph.findFlagsWithoutHotspotJvm())
                .as(
                        "java-%s.json flag nodes must declare jvm with %s support",
                        featureVersion,
                        JvmFlagCatalogGraph.JVM_HOTSPOT)
                .isEmpty();
    }
}
