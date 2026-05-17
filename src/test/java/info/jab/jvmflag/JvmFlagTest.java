package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JvmFlagTest {

    private static final Logger log = LoggerFactory.getLogger(JvmFlagTest.class);

    private static List<JvmFlagEntry> entries;

    @BeforeAll
    static void loadFlags() throws IOException {
        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        log.info(
                "Testing JVM flags with Java {} (java.version={})",
                featureVersion,
                System.getProperty("java.version"));
        entries = JvmFlagCatalog.loadForJavaVersion(featureVersion);
        assertThat(entries).isNotEmpty();
    }

    static Stream<Arguments> jvmFlags() {
        return entries.stream()
                .filter(entry -> entry.testSpec().testable())
                .map(entry -> Arguments.of(entry.flag().flag(), entry));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jvmFlags")
    @EnabledIf("hasCatalogForCurrentJavaVersion")
    void flagIsAcceptedByJvm(String flagName, JvmFlagEntry entry) throws IOException, InterruptedException {
        List<String> jvmArguments = JvmFlagTestArgumentResolver.resolve(entry);
        List<String> command = JvmFlagVerifier.buildCommand(jvmArguments);
        log.info("Verifying flag '{}' with command: {}", entry.flag().flag(), JvmFlagVerifier.formatCommand(command));
        JvmFlagVerifier.Result result = JvmFlagVerifier.verify(jvmArguments);

        int featureVersion = JvmFlagCatalog.currentJavaFeatureVersion();
        assertThat(result.output())
                .as(
                        "JVM output for %s with arguments %s (Java %s)",
                        entry.flag().flag(),
                        jvmArguments,
                        featureVersion)
                .doesNotContain("Unrecognized VM option")
                .doesNotContain("Improperly specified VM option")
                .doesNotContain("experimental and must be enabled via -XX:+UnlockExperimentalVMOptions")
                .doesNotContain("diagnostic and must be enabled via -XX:+UnlockDiagnosticVMOptions");
        if (!entry.testSpec().acceptNonZeroExit()) {
            assertThat(result.exitCode())
                    .as(
                            "exit code for %s with arguments %s (Java %s)\n%s",
                            entry.flag().flag(),
                            jvmArguments,
                            featureVersion,
                            result.output())
                    .isZero();
        }
    }

    static boolean hasCatalogForCurrentJavaVersion() {
        return JvmFlagCatalog.hasCatalogForJavaVersion(JvmFlagCatalog.currentJavaFeatureVersion());
    }

    @AfterAll
    static void removeJvmArtifacts() throws IOException {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        try (Stream<Path> paths = Files.list(projectDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(JvmFlagTest::isJvmArtifact)
                    .forEach(JvmFlagTest::deleteJvmArtifact);
        }
    }

    private static boolean isJvmArtifact(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".jfr") || name.endsWith(".log") || name.contains(".log.");
    }

    private static void deleteJvmArtifact(Path path) {
        try {
            if (Files.deleteIfExists(path)) {
                log.info("Removed JVM artifact: {}", path.getFileName());
            }
        } catch (IOException exception) {
            log.warn("Failed to remove {}: {}", path.getFileName(), exception.getMessage());
        }
    }
}
