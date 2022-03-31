package bisq.desktopapp;

import lombok.extern.slf4j.Slf4j;

// Todo run via gradle fails with a nullPointer at loading images. Seems resources are not on classpath
// jfoenix lib uses reflection in a way which is not supported in more recent java versions. Seems they have not fixed 
// that since long... need to be checked at some point how to deal with it. Atm it prints a warning and all still works. 

/**
 * The com.jfoenix.adapters.ReflectionHelper causes with Jav 16 an exception:
 * `java.base does not "opens java.lang.reflect" to unnamed module`
 * <p>
 * A workaround is to add the below JVM option:
 * `--add-opens java.base/java.lang.reflect=ALL-UNNAMED`
 * <p>
 * Better would be to get proper module support for the desktop module.
 */

@Slf4j
public class Main {
    // A class named Main is required as distribution's entry point.
    // See https://github.com/javafxports/openjdk-jfx/issues/236
    public static void main(String[] args) {
        new JavaFxExecutable(args);
    }
}
