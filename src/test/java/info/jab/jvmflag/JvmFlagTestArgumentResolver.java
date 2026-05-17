package info.jab.jvmflag;

import java.util.ArrayList;
import java.util.List;

final class JvmFlagTestArgumentResolver {

    private JvmFlagTestArgumentResolver() {}

    static List<String> resolve(JvmFlagEntry entry) {
        JvmFlagTestSpec spec = entry.testSpec();
        List<String> arguments = new ArrayList<>(spec.requires());
        if (!spec.testArgs().isEmpty()) {
            arguments.addAll(spec.testArgs());
            return arguments;
        }
        if (spec.testValue() != null) {
            arguments.add(spec.testValue());
            return arguments;
        }
        arguments.add(entry.flag().flag());
        return arguments;
    }
}
