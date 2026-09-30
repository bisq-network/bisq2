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

package bisq.persistence;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the contract every PersistableStore has to satisfy for its file to be readable again.
 * <p>
 * PersistableStoreResolver registers a store under a name derived from the runtime class of the instance, while the
 * file on disk carries the type URL of the protobuf the store's resolver unpacks. If those two disagree the read fails,
 * and PersistableStoreReaderWriter treats that as a damaged file: it moves the file and all its backups into
 * corruptedFilesAtRead and the application starts on defaults, which rotates the key bundle and hides local trade data.
 * The two ways to make them disagree are a store whose java package or class name no longer matches its protobuf, and a
 * subclass inheriting a parent's resolver, so this asserts both.
 * <p>
 * The sources are parsed with the java compiler rather than matched with a regular expression, because a text pattern
 * silently misses legal declarations it was not written for, and a guard which misses a case is worse than no guard.
 * Sources are used rather than the classpath because the stores are spread over 16 modules and no module depends on all
 * of them. They are the same files the test task declares as inputs, so a store in another module cannot change without
 * rerunning this.
 */
class PersistableStoreContractTest {
    private static final String STORE_INTERFACE = "PersistableStore";
    private static final String MAIN_JAVA = File.separator + "src" + File.separator + "main" + File.separator
            + "java" + File.separator;
    private static final String BUILD_DIR = File.separator + "build" + File.separator;
    private static final Pattern PROTO_PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern PROTO_JAVA_PACKAGE =
            Pattern.compile("^\\s*option\\s+java_package\\s*=\\s*\"([\\w.]+)\"\\s*;", Pattern.MULTILINE);
    private static final Pattern PROTO_MESSAGE = Pattern.compile("^\\s*message\\s+(\\w+)", Pattern.MULTILINE);

    private record Store(String qualifiedName, String simpleName, boolean isFinal, boolean isNested,
                         String unpackedProtoClass) {
    }

    @Test
    void everyStoreIsFinalAndRegistersTheNameOfTheProtoItUnpacks() throws IOException {
        Path repoRoot = findRepoRoot();
        List<Store> stores = findStores(repoRoot);
        Map<String, String> protoTypeByJavaClass = findProtoTypes(repoRoot);

        // A scan which reached nothing would let every assertion below pass for the wrong reason.
        assertTrue(protoTypeByJavaClass.containsKey("bisq.settings.protobuf.SettingsStore"),
                "No protobuf definitions found under " + repoRoot);
        List<String> names = stores.stream().map(Store::simpleName).toList();
        assertTrue(names.contains("SettingsStore") && names.contains("BisqEasyTradeStore"),
                "Source scan did not reach the known stores. Root was " + repoRoot + ", found " + names);

        List<String> violations = new ArrayList<>();
        for (Store store : stores) {
            if (store.isNested()) {
                violations.add(store.qualifiedName() + " is nested, which makes its simple name ambiguous as a key");
            }
            if (!store.isFinal()) {
                violations.add(store.qualifiedName() + " is not final, so a subclass would inherit its resolver and "
                        + "register a key which does not match the proto that resolver unpacks");
            }
            if (store.unpackedProtoClass() == null) {
                violations.add(store.qualifiedName() + " does not unpack a proto in its own getResolver()");
                continue;
            }
            String protoType = protoTypeByJavaClass.get(store.unpackedProtoClass());
            if (protoType == null) {
                violations.add(store.qualifiedName() + " unpacks " + store.unpackedProtoClass()
                        + ", for which no protobuf message was found");
            } else if (!protoType.equals(protoType(store.qualifiedName()))) {
                violations.add(store.qualifiedName() + " registers as " + protoType(store.qualifiedName())
                        + " but its file carries " + protoType);
            }
        }
        assertEquals(List.of(), violations);
    }

    /**
     * The rule ProtoResolver#getProtoType applies, restated for a name read from source rather than a loaded class. The
     * production rule itself is pinned by ProtoResolverTest, so the two together cover it.
     */
    private static String protoType(String qualifiedName) {
        String[] tokens = qualifiedName.split("\\.");
        return tokens[1] + "." + tokens[tokens.length - 1];
    }

    private static List<Store> findStores(Path repoRoot) throws IOException {
        List<Path> candidates;
        try (Stream<Path> paths = Files.walk(repoRoot)) {
            candidates = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains(MAIN_JAVA))
                    .filter(path -> !path.toString().contains(BUILD_DIR))
                    .filter(PersistableStoreContractTest::mentionsStoreInterface)
                    .toList();
        }

        List<Store> stores = new ArrayList<>();
        for (CompilationUnitTree unit : parse(candidates)) {
            String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
            new TreeScanner<Void, Integer>() {
                @Override
                public Void visitClass(ClassTree node, Integer depth) {
                    if (implementsStore(node)) {
                        stores.add(new Store(packageName + "." + node.getSimpleName(),
                                node.getSimpleName().toString(),
                                node.getModifiers().getFlags().contains(Modifier.FINAL),
                                depth > 0,
                                unpackedProtoClass(node)));
                    }
                    return super.visitClass(node, depth + 1);
                }
            }.scan(unit, 0);
        }
        return stores;
    }

    private static boolean mentionsStoreInterface(Path path) {
        try {
            return Files.readString(path).contains(STORE_INTERFACE);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + path, e);
        }
    }

    private static boolean implementsStore(ClassTree classTree) {
        return classTree.getImplementsClause().stream().anyMatch(tree -> STORE_INTERFACE.equals(rootName(tree)));
    }

    private static String rootName(Tree tree) {
        Tree type = tree instanceof ParameterizedTypeTree parameterized ? parameterized.getType() : tree;
        return type instanceof MemberSelectTree memberSelect
                ? memberSelect.getIdentifier().toString()
                : type.toString();
    }

    /**
     * The proto class the store's own getResolver() unpacks. Null when the class does not declare getResolver, which
     * means it inherits one and would serialize a proto other than the one its key names.
     */
    private static String unpackedProtoClass(ClassTree classTree) {
        for (Tree member : classTree.getMembers()) {
            if (member instanceof MethodTree method && "getResolver".contentEquals(method.getName())) {
                String[] found = new String[1];
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        if (node.getMethodSelect() instanceof MemberSelectTree select
                                && "unpack".contentEquals(select.getIdentifier())
                                && node.getArguments().size() == 1
                                && node.getArguments().get(0) instanceof MemberSelectTree classLiteral) {
                            found[0] = classLiteral.getExpression().toString();
                        }
                        return super.visitMethodInvocation(node, unused);
                    }
                }.scan(method, null);
                return found[0];
            }
        }
        return null;
    }

    private static Iterable<? extends CompilationUnitTree> parse(List<Path> files) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "No java compiler available, the test needs a JDK rather than a JRE");
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostic -> {
            }, List.of("-proc:none"), null, fileManager.getJavaFileObjectsFromPaths(files));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            return units;
        }
    }

    private static Map<String, String> findProtoTypes(Path repoRoot) throws IOException {
        Map<String, String> protoTypeByJavaClass = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(repoRoot)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".proto"))
                    .filter(p -> !p.toString().contains(BUILD_DIR))
                    .toList()) {
                String source = Files.readString(path);
                Matcher protoPackage = PROTO_PACKAGE.matcher(source);
                Matcher javaPackage = PROTO_JAVA_PACKAGE.matcher(source);
                if (!protoPackage.find() || !javaPackage.find()) {
                    continue;
                }
                Matcher message = PROTO_MESSAGE.matcher(source);
                while (message.find()) {
                    protoTypeByJavaClass.put(javaPackage.group(1) + "." + message.group(1),
                            protoPackage.group(1) + "." + message.group(1));
                }
            }
        }
        return protoTypeByJavaClass;
    }

    private static Path findRepoRoot() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.exists(path.resolve("settings.gradle.kts"))) {
            path = path.getParent();
        }
        if (path == null) {
            throw new IllegalStateException("No settings.gradle.kts found above " + Path.of("").toAbsolutePath());
        }
        return path;
    }
}
