package network.misq.desktop;

// Todo run via gradle fails with a nullPointer at loading images. Seems resources are not on classpath
// jfoenix lib uses reflection in a way which is not supported in more recent java versions. Seems they have not fixed 
// that since long... need to be checked at some point how to deal with it. Atm it prints a warning and all still works. 
public class Main {
    // A class named Main is required as distribution's entry point.
    // See https://github.com/javafxports/openjdk-jfx/issues/236
    public static void main(String[] args) {
        new DesktopApplication(args);
    }
}
