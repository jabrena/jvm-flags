package info.jab.jvmflag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class JvmFlagTestArgumentResolver {

    private JvmFlagTestArgumentResolver() {}

    static List<String> resolve(JvmFlagEntry entry) throws IOException {
        JvmFlagTestSpec spec = entry.testSpec();
        if (hasCatalogTestMetadata(spec)) {
            return resolveFromCatalog(entry, spec);
        }
        if (entry.flag().isPendingCategorization()) {
            return PrintFlagsFinalTestArgumentResolver.resolve(entry);
        }
        return resolveFromCatalog(entry, spec);
    }

    private static boolean hasCatalogTestMetadata(JvmFlagTestSpec spec) {
        return !spec.requires().isEmpty() || spec.testValue() != null || !spec.testArgs().isEmpty();
    }

    private static List<String> resolveFromCatalog(JvmFlagEntry entry, JvmFlagTestSpec spec) {
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
