package info.jab.jvmflag;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JvmFlagEntry {

    private final JvmFlag flag;
    private final JvmFlagTestSpec testSpec;
    private final List<String> jvmImplementations;

    public JvmFlagEntry(JvmFlag flag, JvmFlagTestSpec testSpec, List<String> jvmImplementations) {
        this.flag = flag;
        this.testSpec = testSpec;
        this.jvmImplementations = Collections.unmodifiableList(jvmImplementations);
    }

    public JvmFlag flag() {
        return flag;
    }

    public JvmFlagTestSpec testSpec() {
        return testSpec;
    }

    /** JVM implementations that expose this flag ({@code hotspot}, {@code graalvm}, or both). */
    public List<String> jvmImplementations() {
        return jvmImplementations;
    }

    public boolean supportsJvm(String implementation) {
        return jvmImplementations.contains(implementation);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JvmFlagEntry)) {
            return false;
        }
        JvmFlagEntry that = (JvmFlagEntry) other;
        return Objects.equals(flag, that.flag)
                && Objects.equals(testSpec, that.testSpec)
                && Objects.equals(jvmImplementations, that.jvmImplementations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flag, testSpec, jvmImplementations);
    }

    @Override
    public String toString() {
        return "JvmFlagEntry[flag=" + flag + ", testSpec=" + testSpec + ", jvm=" + jvmImplementations + "]";
    }
}
