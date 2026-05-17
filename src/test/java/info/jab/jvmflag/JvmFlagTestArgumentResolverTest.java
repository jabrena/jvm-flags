package info.jab.jvmflag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class JvmFlagTestArgumentResolverTest {

    @Test
    void usesCatalogRequiresBeforePendingSnapshot() throws IOException {
        JvmFlag flag = new JvmFlag(
                "flag-pending-cmsincrementalmode",
                "-XX:+CMSIncrementalMode",
                JvmFlag.PENDING_CATEGORIZATION_DESCRIPTION);
        JvmFlagTestSpec spec = new JvmFlagTestSpec(
                "-XX:+CMSIncrementalMode",
                Collections.singletonList("-XX:+UseConcMarkSweepGC"),
                Collections.<String>emptyList(),
                false,
                true);
        JvmFlagEntry entry = new JvmFlagEntry(flag, spec);

        List<String> arguments = JvmFlagTestArgumentResolver.resolve(entry);

        assertThat(arguments).containsExactly("-XX:+UseConcMarkSweepGC", "-XX:+CMSIncrementalMode");
    }
}
