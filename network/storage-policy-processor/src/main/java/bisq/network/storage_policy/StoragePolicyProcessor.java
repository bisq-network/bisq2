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
    private static final String ACCESSOR_FIELD = "metaData";
    private static final String META_DATA = "bisq.network.p2p.services.data.storage.MetaData";
    private static final String LOMBOK_GETTER = "lombok.Getter";

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
        if (declaresPolicy && type.getKind() == ElementKind.INTERFACE) {
            error(type, "@StoragePolicy has no effect on an interface: it is @Inherited, which does not reach a type"
                    + " through an interface, so " + type.getSimpleName() + " declares a policy nothing resolves."
                    + " Declare it on the implementing classes, or on a shared abstract base");
            return;
        }
        if (!aware) {
            return;
        }

        // Declaring and overriding are alternatives for an abstract base too, where the policy would sit dormant
        // until someone removed the override and every entry then filed under the base's own name.
        boolean providesAccessor = providesOwnAccessor(type);
        if (declaresPolicy && providesAccessor) {
            error(type, type.getSimpleName() + " both declares @StoragePolicy and provides its own " + ACCESSOR
                    + "(). Remove one: a type either declares its policy or passes on somebody else's");
            return;
        }

        // Only a concrete type has to have a policy. An abstract one may leave it to its subclasses.
        boolean concreteType = (type.getKind() == ElementKind.CLASS || type.getKind() == ElementKind.RECORD)
                && !type.getModifiers().contains(Modifier.ABSTRACT);
        if (concreteType && !declaresPolicy && !providesAccessor && !inheritsPolicy(type)) {
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
     * True when the type takes its properties from elsewhere rather than declaring them. The default on the
     * interface does not count: that is the one we are checking a policy exists for.
     * <p>
     * A field named {@code metaData} counts as well as a method. The accessor for such a field is usually generated
     * by Lombok, and a generated method is only visible here if Lombok ran first, so reading the method alone would
     * make the answer depend on the order processors happen to be declared in.
     */
    private boolean providesOwnAccessor(TypeElement type) {
        for (TypeElement classInHierarchy : superClasses(type)) {
            for (Element member : classInHierarchy.getEnclosedElements()) {
                if (member.getKind() == ElementKind.METHOD
                        && ACCESSOR.contentEquals(member.getSimpleName())
                        && ((ExecutableElement) member).getParameters().isEmpty()
                        && !member.getModifiers().contains(Modifier.ABSTRACT)) {
                    return true;
                }
                if (member.getKind() == ElementKind.FIELD
                        && ACCESSOR_FIELD.contentEquals(member.getSimpleName())
                        && !member.getModifiers().contains(Modifier.STATIC)
                        && isMetaData(member.asType())
                        && generatesAccessors(classInHierarchy, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * A field only implies an accessor if something generates one from it. Without a Lombok {@code @Getter} on the
     * field or its class there is no accessor at all, and the type would fall back to the interface default and
     * throw on its first payload.
     */
    private boolean generatesAccessors(TypeElement enclosing, Element field) {
        // A @Getter on the field decides for that field, which is how a class level one is suppressed for it.
        return lombokGetter(field)
                .map(StoragePolicyProcessor::generatesPublicAccessor)
                .orElseGet(() -> lombokGetter(enclosing)
                        .map(StoragePolicyProcessor::generatesPublicAccessor)
                        .orElse(false));
    }

    /**
     * {@code @Getter(AccessLevel.NONE)} is how a class level getter is suppressed for one field, so an explicit
     * level that is not PUBLIC does not give the accessor the interface needs. PRIVATE and PROTECTED would not
     * compile anyway, since the method cannot reduce the visibility of the one it implements, but NONE generates
     * nothing at all and would otherwise pass.
     */
    private Optional<AnnotationMirror> lombokGetter(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(mirror -> LOMBOK_GETTER.contentEquals(
                        ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName()))
                .map(mirror -> (AnnotationMirror) mirror)
                .findFirst();
    }

    private static boolean generatesPublicAccessor(AnnotationMirror getter) {
        return getter.getElementValues().values().stream()
                .allMatch(value -> "PUBLIC".equals(String.valueOf(value.getValue())));
    }

    /** The Lombok reasoning only applies to the field an accessor would be generated from, so the type matters. */
    private boolean isMetaData(TypeMirror mirror) {
        return asTypeElement(mirror)
                .map(element -> META_DATA.contentEquals(element.getQualifiedName()))
                .orElse(false);
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
