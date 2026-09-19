plugins {
    id("bisq.java-conventions")
}

// Deliberately has no dependency on any bisq module: it is applied to those modules, so depending on them would be
// circular. It matches the storage types by fully qualified name through javax.lang.model instead.
