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

package bisq.network.p2p.services.data.storage;

import bisq.network.storage_policy.StoragePolicyProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The processor matches {@link StoragePolicyAware} and {@link StoragePolicy} by fully qualified name, and its own
 * tests compile against stubs declared in the same package. So if either type is renamed or moved, the processor
 * stops checking anything while all of its tests stay green.
 * <p>
 * This compiles a fixture against the real types instead, so the names are pinned from this side.
 */
class StoragePolicyProcessorNamesTest {
    @Test
    void theProcessorStillRecognisesTheRealTypes() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavaFileObject fixture = new SimpleJavaFileObject(URI.create("string:///Fixture.java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return "class Fixture implements "
                            + StoragePolicyAware.class.getName() + " { }";
                }
            };
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    List.of("-classpath", System.getProperty("java.class.path"), "-proc:only"),
                    null, List.of(fixture));
            task.setProcessors(List.of(new StoragePolicyProcessor()));
            task.call();
        }
        List<String> errors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == ERROR)
                .map(diagnostic -> diagnostic.getMessage(null))
                .toList();
        assertTrue(errors.stream().anyMatch(message -> message.contains("declares no storage properties")),
                "The processor did not recognise the real StoragePolicyAware. If either type was renamed or moved, "
                        + "update the names in StoragePolicyProcessor. Diagnostics: " + errors);
    }
}
