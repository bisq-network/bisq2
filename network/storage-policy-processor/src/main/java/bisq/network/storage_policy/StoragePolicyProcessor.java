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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Optional;
import java.util.Set;

/**
 * Enforces at compile time that the storage properties of a network payload type are declared exactly once and in
 * the right place.
 * <p>
 * Three ways of getting it wrong compile without this, and each one only shows up at runtime:
 * <ol>
 *     <li>A stored type with no policy and no accessor of its own. It throws when the first payload of that type is
 *     handled.</li>
 *     <li>A policy on a type the storage never resolves one for. It has no effect, which reads as if it did.</li>
 *     <li>A policy on a wrapper which also passes on somebody else's policy. It is dormant until the accessor is
 *     removed, and then every entry is filed under the wrapper's own name.</li>
 * </ol>
 * The types are matched by name rather than by class, because this processor is applied to the module that defines
 * them and so cannot depend on it.
 */
@SupportedAnnotationTypes("*")
public class StoragePolicyProcessor extends AbstractProcessor {
    private static final String POLICY = "bisq.network.p2p.services.data.storage.StoragePolicy";
    private static final String AWARE = "bisq.network.p2p.services.data.storage.StoragePolicyAware";
    private static final String ACCESSOR = "getMetaData";

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement type) {
                checkIncludingNested(type);
            }
        }
        return false;
    }

    /** Only top level types are root elements, so nested ones are reached through their enclosing type. */
    private void checkIncludingNested(TypeElement type) {
        check(type);
        type.getEnclosedElements().stream()
                .filter(enclosed -> enclosed instanceof TypeElement)
                .forEach(enclosed -> checkIncludingNested((TypeElement) enclosed));
    }

    private void check(TypeElement type) {
        boolean aware = isStoragePolicyAware(type);
        boolean declaresPolicy = findPolicy(type).isPresent();

        if (declaresPolicy && !aware) {
            error(type, "@StoragePolicy has no effect here: " + type.getSimpleName()
                    + " is not a StoragePolicyAware type, so the storage never resolves a policy for it");
            return;
        }
        if (!aware || type.getKind() != ElementKind.CLASS || type.getModifiers().contains(Modifier.ABSTRACT)) {
            return;
        }

        boolean providesAccessor = providesOwnAccessor(type);
        if (declaresPolicy && providesAccessor) {
            error(type, type.getSimpleName() + " both declares @StoragePolicy and provides its own " + ACCESSOR
                    + "(). Remove one: a type either declares its policy or passes on somebody else's");
        } else if (!declaresPolicy && !providesAccessor && !inheritsPolicy(type)) {
            error(type, type.getSimpleName() + " is stored but declares no storage properties. Add @StoragePolicy,"
                    + " or override " + ACCESSOR + "() if its policy comes from elsewhere");
        }
    }

    /** Only a policy declared on the type itself, mirroring {@code getDeclaredAnnotation}. */
    private Optional<AnnotationMirror> findPolicy(TypeElement type) {
        return type.getAnnotationMirrors().stream()
                .filter(mirror -> POLICY.contentEquals(
                        ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName()))
                .map(mirror -> (AnnotationMirror) mirror)
                .findFirst();
    }

    /** The annotation is {@code @Inherited}, so an abstract base may declare the policy for its subclasses. */
    private boolean inheritsPolicy(TypeElement type) {
        for (TypeElement superType : superClasses(type)) {
            if (findPolicy(superType).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the accessor is implemented by a class in the hierarchy. The default on the interface does not count:
     * that is the one we are checking a policy exists for.
     */
    private boolean providesOwnAccessor(TypeElement type) {
        for (TypeElement classInHierarchy : superClasses(type)) {
            boolean declared = classInHierarchy.getEnclosedElements().stream()
                    .filter(member -> member.getKind() == ElementKind.METHOD)
                    .map(member -> (ExecutableElement) member)
                    .anyMatch(method -> ACCESSOR.contentEquals(method.getSimpleName())
                            && method.getParameters().isEmpty()
                            && !method.getModifiers().contains(Modifier.ABSTRACT));
            if (declared) {
                return true;
            }
        }
        return false;
    }

    /** The type itself and every class above it, excluding interfaces. */
    private Iterable<TypeElement> superClasses(TypeElement type) {
        java.util.List<TypeElement> classes = new java.util.ArrayList<>();
        TypeElement current = type;
        while (current != null) {
            classes.add(current);
            current = asTypeElement(current.getSuperclass()).orElse(null);
        }
        return classes;
    }

    private boolean isStoragePolicyAware(TypeElement type) {
        if (AWARE.contentEquals(type.getQualifiedName())) {
            return false;
        }
        for (TypeMirror supertype : processingEnv.getTypeUtils().directSupertypes(type.asType())) {
            Optional<TypeElement> element = asTypeElement(supertype);
            if (element.isEmpty()) {
                continue;
            }
            if (AWARE.contentEquals(element.get().getQualifiedName()) || isStoragePolicyAware(element.get())) {
                return true;
            }
        }
        return false;
    }

    private Optional<TypeElement> asTypeElement(TypeMirror mirror) {
        return mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element
                ? Optional.of(element)
                : Optional.empty();
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
