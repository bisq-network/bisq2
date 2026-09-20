/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.storage_policy;

import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles fixtures with the processor attached and asserts what it reports.
 * <p>
 * Without this nothing checks the processor itself. A rule that stops firing, a missing service registration or an
 * early return that skips a kind of type all leave every other build green, because the processor's only output is
 * a diagnostic that is no longer produced.
 * <p>
 * The interface and the annotation are declared in the fixture rather than depended on, because this module is
 * applied to the module that defines them and so cannot depend on it.
 */
class StoragePolicyProcessorTest {
    private static final String PACKAGE = "package bisq.network.p2p.services.data.storage;\n";

    // Top level in that package, because the processor matches them by fully qualified name.
    private static final String AWARE = PACKAGE + """
            public interface StoragePolicyAware { default Object getMetaData() { return null; } }
            """;

    private static final String META_DATA = PACKAGE + """
            public class MetaData { }
            """;

    private static final String LOMBOK = """
            package lombok;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.FIELD})
            public @interface Getter { AccessLevel value() default AccessLevel.PUBLIC; }
            """;

    private static final String ACCESS_LEVEL = """
            package lombok;
            public enum AccessLevel { PUBLIC, NONE }
            """;

    private static final String POLICY = PACKAGE + """
            import java.lang.annotation.*;
            @Inherited
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
            public @interface StoragePolicy { }
            """;

    @Test
    void storedTypeWithoutAPolicyIsRejected() {
        assertError("declares no storage properties", """
                class Payload implements StoragePolicyAware { }
                """);
    }

    @Test
    void policyOnATypeTheStorageIgnoresIsRejected() {
        assertError("has no effect here", """
                @StoragePolicy
                class NotStored { }
                """);
    }

    @Test
    void declaringAndOverridingIsRejected() {
        assertError("both declares", """
                @StoragePolicy
                class Both implements StoragePolicyAware {
                    public Object getMetaData() { return null; }
                }
                """);
    }

    @Test
    void aDeclaredPolicyIsAccepted() {
        assertNoError("""
                @StoragePolicy
                class Declared implements StoragePolicyAware { }
                """);
    }

    @Test
    void anInheritedPolicyIsAccepted() {
        assertNoError("""
                @StoragePolicy
                abstract class Base implements StoragePolicyAware { }
                class Leaf extends Base { }
                """);
    }

    @Test
    void anOverrideInsteadOfAPolicyIsAccepted() {
        assertNoError("""
                class Wrapper implements StoragePolicyAware {
                    public Object getMetaData() { return null; }
                }
                """);
    }

    /** Lombok generates the accessor from this field, and only if it runs first, so the field has to count. */
    @Test
    void aMetaDataFieldWithALombokGetterCountsAsAnOverride() {
        assertNoError("""
                @lombok.Getter
                class LombokStyle implements StoragePolicyAware {
                    private final MetaData metaData = null;
                }
                """);
    }

    @Test
    void aFieldLevelLombokGetterAlsoCounts() {
        assertNoError("""
                class FieldLevel implements StoragePolicyAware {
                    @lombok.Getter
                    private final MetaData metaData = null;
                }
                """);
    }

    /** AccessLevel.NONE is how a class level getter is suppressed, so it generates no accessor at all. */
    @Test
    void aGetterSuppressedWithAccessLevelNoneDoesNotCount() {
        assertError("declares no storage properties", """
                @lombok.Getter
                class Suppressed implements StoragePolicyAware {
                    @lombok.Getter(lombok.AccessLevel.NONE)
                    private final MetaData metaData = null;
                }
                """);
    }

    /** Without a @Getter nothing generates an accessor, so the type would fall back to the failing default. */
    @Test
    void aMetaDataFieldWithNoGetterDoesNotCount() {
        assertError("declares no storage properties", """
                class NoAccessor implements StoragePolicyAware {
                    private final MetaData metaData = null;
                }
                """);
    }

    /** @Inherited does not reach a type through an interface, so a policy declared there resolves for nobody. */
    @Test
    void aPolicyOnAnInterfaceIsRejected() {
        assertError("no effect on an interface", """
                @StoragePolicy
                interface Inert extends StoragePolicyAware { }
                """);
    }

    /** Only the field an accessor would be generated from counts, so the name alone must not exempt a type. */
    @Test
    void aFieldNamedMetaDataOfAnotherTypeDoesNotCount() {
        assertError("declares no storage properties", """
                class Misleading implements StoragePolicyAware {
                    private final String metaData = null;
                }
                """);
    }

    @Test
    void aRecordIsCheckedLikeAClass() {
        assertError("declares no storage properties", """
                record Rec(int x) implements StoragePolicyAware { }
                """);
    }

    @Test
    void aTypeNestedTwoLevelsDeepIsChecked() {
        assertError("declares no storage properties", """
                class Outer { static class Middle { static class Inner implements StoragePolicyAware { } } }
                """);
    }

    @Test
    void theProcessorIsRegisteredAsAService() throws IOException {
        Path service = Path.of("src/main/resources/META-INF/services/javax.annotation.processing.Processor");
        assertTrue(Files.exists(service), "javac discovers the processor through this file");
        assertEquals(StoragePolicyProcessor.class.getName(), Files.readString(service).strip());
    }

    @Test
    void theProcessorIsDeclaredIncremental() throws IOException {
        Path declaration = Path.of("src/main/resources/META-INF/gradle/incremental.annotation.processors");
        assertTrue(Files.exists(declaration), "without this Gradle turns off incremental compilation");
        assertTrue(Files.readString(declaration).contains(StoragePolicyProcessor.class.getName() + ",ISOLATING"));
    }

    private void assertError(String expected, String source) {
        List<String> errors = compile(source);
        assertTrue(errors.stream().anyMatch(message -> message.contains(expected)),
                "Expected an error containing \"" + expected + "\" but got " + errors);
    }

    private void assertNoError(String source) {
        assertEquals(List.of(), compile(source));
    }

    private List<String> compile(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Path classes = Files.createTempDirectory("storage-policy-processor-test");
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classes.toFile()));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
                    List.of(inMemory("StoragePolicyAware", AWARE), inMemory("StoragePolicy", POLICY),
                            inMemory("MetaData", META_DATA), inMemory("Getter", LOMBOK), inMemory("AccessLevel", ACCESS_LEVEL),
                            inMemory("Fixture", PACKAGE + source)));
            task.setProcessors(List.of(new StoragePolicyProcessor()));
            task.call();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == ERROR)
                .map(diagnostic -> diagnostic.getMessage(null))
                .collect(Collectors.toList());
    }

    private static JavaFileObject inMemory(String name, String source) {
        return new SimpleJavaFileObject(URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
    }
}
