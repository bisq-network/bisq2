package bisq.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeForHash {
    // If not empty, the field will only be excluded if at least one version matches the value returned from getVersion()
    int[] excludeOnlyInVersions() default {};
}
