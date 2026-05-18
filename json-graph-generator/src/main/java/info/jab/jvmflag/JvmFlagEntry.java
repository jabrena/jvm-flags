package info.jab.jvmflag;

import java.util.Objects;

public final class JvmFlagEntry {

    private final JvmFlag flag;
    private final JvmFlagTestSpec testSpec;

    public JvmFlagEntry(JvmFlag flag, JvmFlagTestSpec testSpec) {
        this.flag = flag;
        this.testSpec = testSpec;
    }

    public JvmFlag flag() {
        return flag;
    }

    public JvmFlagTestSpec testSpec() {
        return testSpec;
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
        return Objects.equals(flag, that.flag) && Objects.equals(testSpec, that.testSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flag, testSpec);
    }

    @Override
    public String toString() {
        return "JvmFlagEntry[flag=" + flag + ", testSpec=" + testSpec + "]";
    }
}
