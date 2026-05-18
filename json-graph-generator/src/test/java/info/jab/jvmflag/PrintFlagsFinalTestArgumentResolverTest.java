package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrintFlagsFinalTestArgumentResolverTest {

    @Test
    void resolvesBoolFlagFromSnapshotDefault() throws IOException {
        JvmFlagEntry entry = pendingEntry("-XX:+AggressiveHeap");

        List<String> arguments = PrintFlagsFinalTestArgumentResolver.resolve(entry);

        assertThat(arguments).containsExactly("-XX:+AggressiveHeap");
    }

    @Test
    void resolvesNumericFlagUsingSnapshotValue() throws IOException {
        JvmFlagEntry entry = pendingEntry("-XX:AliasLevel=<n>");

        List<String> arguments = PrintFlagsFinalTestArgumentResolver.resolve(entry);

        assertThat(arguments).containsExactly("-XX:AliasLevel=3");
    }

    @Test
    void resolvesCmsFlagWithUseConcMarkSweepGc() throws IOException {
        JvmFlagEntry entry = pendingEntry("-XX:+CMSIncrementalMode");

        List<String> arguments = PrintFlagsFinalTestArgumentResolver.resolve(entry);

        assertThat(arguments).containsExactly("-XX:+UseConcMarkSweepGC", "-XX:+CMSIncrementalMode");
    }

    private static JvmFlagEntry pendingEntry(String flag) {
        JvmFlag jvmFlag = new JvmFlag("flag-pending-test", flag, JvmFlag.PENDING_CATEGORIZATION_DESCRIPTION);
        return new JvmFlagEntry(
                jvmFlag, JvmFlagTestSpec.defaults(), Collections.singletonList(JvmImplementation.HOTSPOT));
    }
}
