package info.jab.jvmflag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JvmFlagTestSpec {

    private final String testValue;
    private final List<String> requires;
    private final List<String> testArgs;
    private final boolean testable;
    private final boolean acceptNonZeroExit;

    public JvmFlagTestSpec(
            String testValue,
            List<String> requires,
            List<String> testArgs,
            boolean testable,
            boolean acceptNonZeroExit) {
        this.testValue = testValue;
        this.requires = requires == null ? Collections.<String>emptyList() : copyList(requires);
        this.testArgs = testArgs == null ? Collections.<String>emptyList() : copyList(testArgs);
        this.testable = testable;
        this.acceptNonZeroExit = acceptNonZeroExit;
    }

    public String testValue() {
        return testValue;
    }

    public List<String> requires() {
        return requires;
    }

    public List<String> testArgs() {
        return testArgs;
    }

    public boolean testable() {
        return testable;
    }

    public boolean acceptNonZeroExit() {
        return acceptNonZeroExit;
    }

    public static JvmFlagTestSpec defaults() {
        return new JvmFlagTestSpec(null, Collections.<String>emptyList(), Collections.<String>emptyList(), true, false);
    }

    private static List<String> copyList(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JvmFlagTestSpec)) {
            return false;
        }
        JvmFlagTestSpec that = (JvmFlagTestSpec) other;
        return testable == that.testable
                && acceptNonZeroExit == that.acceptNonZeroExit
                && Objects.equals(testValue, that.testValue)
                && Objects.equals(requires, that.requires)
                && Objects.equals(testArgs, that.testArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testValue, requires, testArgs, testable, acceptNonZeroExit);
    }

    @Override
    public String toString() {
        return "JvmFlagTestSpec[testValue="
                + testValue
                + ", requires="
                + requires
                + ", testArgs="
                + testArgs
                + ", testable="
                + testable
                + ", acceptNonZeroExit="
                + acceptNonZeroExit
                + "]";
    }
}
