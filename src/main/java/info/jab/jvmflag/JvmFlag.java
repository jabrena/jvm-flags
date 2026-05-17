package info.jab.jvmflag;

import java.util.Objects;

public final class JvmFlag {

    /** Catalog placeholder for flags imported from PrintFlagsFinal and not yet categorized. */
    public static final String PENDING_CATEGORIZATION_DESCRIPTION =
            "Present in Java 8 PrintFlagsFinal; categorization pending.";

    private final String id;
    private final String flag;
    private final String description;

    public JvmFlag(String id, String flag, String description) {
        this.id = id;
        this.flag = flag;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String flag() {
        return flag;
    }

    public String description() {
        return description;
    }

    public boolean isPendingCategorization() {
        return PENDING_CATEGORIZATION_DESCRIPTION.equals(description);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JvmFlag)) {
            return false;
        }
        JvmFlag that = (JvmFlag) other;
        return Objects.equals(id, that.id)
                && Objects.equals(flag, that.flag)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, flag, description);
    }

    @Override
    public String toString() {
        return "JvmFlag[id=" + id + ", flag=" + flag + ", description=" + description + "]";
    }
}
