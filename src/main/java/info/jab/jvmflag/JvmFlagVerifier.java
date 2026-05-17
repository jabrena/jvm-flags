package info.jab.jvmflag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class JvmFlagVerifier {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final String JAVA_BIN =
            System.getProperty("java.home") + java.io.File.separator + "bin" + java.io.File.separator + "java";

    private JvmFlagVerifier() {}

    public static final class Result {

        private final int exitCode;
        private final String output;

        public Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int exitCode() {
            return exitCode;
        }

        public String output() {
            return output;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Result)) {
                return false;
            }
            Result that = (Result) other;
            return exitCode == that.exitCode && Objects.equals(output, that.output);
        }

        @Override
        public int hashCode() {
            return Objects.hash(exitCode, output);
        }

        @Override
        public String toString() {
            return "Result[exitCode=" + exitCode + ", output=" + output + "]";
        }
    }

    public static Result verify(List<String> jvmArguments) throws IOException, InterruptedException {
        return verify(jvmArguments, DEFAULT_TIMEOUT);
    }

    public static List<String> buildCommand(List<String> jvmArguments) {
        List<String> command = new ArrayList<>();
        command.add(JAVA_BIN);
        command.addAll(jvmArguments);
        command.add("-version");
        return Collections.unmodifiableList(new ArrayList<>(command));
    }

    public static String formatCommand(List<String> command) {
        return command.stream().map(JvmFlagVerifier::quoteIfNeeded).collect(Collectors.joining(" "));
    }

    public static Result verify(List<String> jvmArguments, Duration timeout)
            throws IOException, InterruptedException {
        List<String> command = buildCommand(jvmArguments);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Timed out verifying JVM arguments: " + jvmArguments);
        }
        String output = readUtf8(process.getInputStream());
        return new Result(process.exitValue(), output);
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static String quoteIfNeeded(String argument) {
        if (argument.indexOf(' ') < 0 && argument.indexOf('\t') < 0) {
            return argument;
        }
        return "\"" + argument.replace("\"", "\\\"") + "\"";
    }
}
