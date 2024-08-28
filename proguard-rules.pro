# Filter manifirst
#-injars  my.jar(!META-INF/MANIFEST.MF)

# careful when using dontwarn..
#-dontwarn javax.**
#-dontwarn java.**
#-dontwarn org.**
#-dontwarn bisq.**
#-dontwarn com.google.protobuf.**
#-dontwarn bisq.common.proto.**

#----------------------------------------------------------------
#Warning: bisq.common.jvm.JvmUtils: can't find referenced class java.lang.management.ManagementFactory
#Warning: bisq.common.jvm.JvmUtils: can't find referenced class java.lang.management.ManagementFactory
#Warning: bisq.common.jvm.JvmUtils: can't find referenced class java.lang.management.RuntimeMXBean
#Warning: bisq.common.jvm.JvmUtils: can't find referenced class java.lang.management.RuntimeMXBean
# found these the only acceptable warnings to just skip
-dontwarn java.lang.management.**
#----------------------------------------------------------------

#-printmapping build/libs/mapping.txt
#-printseeds build/libs/out.seeds

#-addconfigurationdebugging
#-verbose

-adaptresourcefilenames **.properties,**.gif,**.jpg,**.svg,**.png
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF

## GENERAL RULES
-verbose
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}
-keep public class * {
    public protected *;
}

-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

## **IMPORTANT** During the first addition of proguard, Optimization and preverification failed due to not finding the
## protobuf classes used as parent of our classes ()
## removing preverification helped to avoid but problems came down the line when testing final builds.
## uncomment the following lines only if you know what and why are you doing it.
## More information: https://community.guardsquare.com/t/facing-an-error-with-cant-find-common-super-class-of-while-obfuscating-a-maven-based-java-application/1437/3

# Controls for debugging

#-dontpreverify
#-dontshrink
#-dontoptimize
## Decision has been made to do not use obfuscation for security and deterministic size reasons.
## Keep rules will still be kept below but doesn't really serve much purpose until this gets commented out
-dontobfuscate

# 1 is the default - On the first attempt to setup proguard effort was invested in raising this number unsuccessfully
# as it would cause cause problems on the generated desktop app (with javafx)
-optimizationpasses 1
#-optimizations !code/allocation/variable

## BISQ MODULES
# PERSISTANCE
# As a general rule, we don't wont to obfuscate bisq. modules code. This could be improved but on the first attempt it proved to be very challenging
# If this were to be removed, its recommended to leave the specific keeps below in this file.
#-keep class bisq.** { *; }

# specific keeps on common
-keep class bisq.common.util.** { *; }
-keep class bisq.common.util.StringUtils { *; }
-keep class bisq.common.util.ClassUtils { *; }
-keep class bisq.common.logging.LogMaskConverter {
    public <methods>;
    *;
}

# General rules to keep Java core classes
-keep class java.lang.** { *; }
-keep class java.util.** { *; }
-keep class java.io.** { *; }
-keep class java.nio.** { *; }
-keep class java.lang.management.ManagementFactory { *; }
-keep class java.lang.management.RuntimeMXBean { *; }
# Add specific rules for referenced classes
-keep class bisq.user.reputation.requests.AuthorizeTimestampRequest { *; }

-keepclassmembers class bisq.common.util.ClassUtils {
    public static ** getEnclosingClass();
}

#Protobuf
-keepclassmembers class * {
    ** ADAPTER; ** Schema; ** Protobuf;
}
-keepclasseswithmembers class * {
    ** ADAPTER; ** Schema; ** Protobuf;
}
-keep class com.google.protobuf.** { *; }
-keep class com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class com.google.protobuf.GeneratedMessageLite$Builder { *; }
-keepclassmembers class com.google.protobuf.GeneratedMessageLite {
    public static <fields>;
}
-keepclassmembers class ** {
    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite);
}
-keepclassmembers class **$Builder {
    public ** build();
}
-keep class bisq.common.proto.** { *; }
# MacOS persistence tests would fail without the following specific keep
-keep class bisq.common.protobuf.** { *; }
-keep class bisq.common.protobuf.StringLongPair { *; }
-keep class bisq.common.protobuf.StringLongPair$1 { *; }
-keep class bisq.common.protobuf.StringLongPair$Builder { *; }

# Keep the interface and its methods
-keep interface bisq.common.proto.PersistableProto { *; }
# Keep all implementations of PersistableProto
-keep class * implements bisq.common.proto.PersistableProto { *; }
# Ensure that ProGuard does not strip any fields or methods of classes implementing PersistableProto
-keepclassmembers class * implements bisq.common.proto.PersistableProto {
    <fields>;
    <methods>;
}

-keep interface bisq.common.proto.Proto { *; }
-keep class * implements bisq.common.proto.Proto { *; }
-keep class bisq.persistence.TimestampStore { *; }
-keep class bisq.persistence.PersistableStoreReaderWriter { *; }
-keep class bisq.persistence.PersistableStoreReaderWriterTests { *; }
-keep public class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep public class * extends com.google.protobuf.GeneratedMessage { *; }
-keep class **.protobuf.** { *; }
-keep class **.protobuf.**$Builder { *; }
-keepclassmembers class **.protobuf.** { *; }
-keepclassmembers class **.protobuf.**$Builder { *; }

# Guava
-dontwarn javax.lang.model.element.Modifier
-dontnote sun.misc.SharedSecrets
-keep class sun.misc.** { *; }
-dontwarn java.lang.SafeVarargs
-keep class com.google.common.base.Joiner {
    public static Joiner on(java.lang.String);
    public ** join(...);
}

-keep class java.lang.Throwable {
    void addSuppressed(...);
}

-keepclassmembers class com.google.common.util.concurrent.AbstractFuture** {
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater waiters;
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater value;
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater listeners;
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater thread;
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater next;
}

-keepclassmembers class com.google.common.util.concurrent.AtomicDouble {
    double value;
}

-keepclassmembers class com.google.common.util.concurrent.AggregateFutureState {
    int remaining;
    java.util.concurrent.atomic.AtomicReferenceFieldUpdater seenExceptions;
}

-dontwarn java.lang.ClassValue
-dontnote com.google.appengine.api.ThreadManager
-keep class com.google.appengine.api.ThreadManager {
    static java.util.concurrent.ThreadFactory currentRequestThreadFactory(...);
}

-dontnote com.google.apphosting.api.ApiProxy
-keep class com.google.apphosting.api.ApiProxy {
    static com.google.apphosting.api.ApiProxy.Environment getCurrentEnvironment(...);
}

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }

#Spring

#-keepclassmembers class * {
#    @org.springframework.beans.factory.annotation.Autowired *;
#}
#-keep class * implements java.lang.reflect.InvocationHandler { *; }
#-keep class org.springframework.beans.factory.annotation.**
#-dontwarn org.springframework.beans.factory.annotation.**

-keepclassmembers class * {
    @javax.annotation.Resource *;
}
-keep class javax.annotation.**
-dontwarn javax.annotation.**

-keepattributes *Annotation*, Signature, EnclosingMethod
-keep class * extends java.lang.annotation.Annotation { *; }

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class com.google.gson.annotations.**
-dontwarn com.google.gson.annotations.**
-keepclasseswithmembers,allowobfuscation,includedescriptorclasses class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Jackson
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# JUnit
-keepattributes *Annotation*
-keep class org.junit.** { *; }
-dontwarn org.junit.**

# Apache HttpComponents
-dontwarn org.apache.http.**
-keep class org.apache.http.** { *; }

# BouncyCastle & security
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# Commons Codec
-dontwarn org.apache.commons.codec.**
-keep class org.apache.commons.codec.** { *; }

# Typesafe Config
-dontwarn com.typesafe.config.**
-keep class com.typesafe.config.** { *; }

# Tests

# very important for persistance tests
-keepattributes StackMapTable,LocalVariableTable

-keep class *Test { *; }
-keepclassmembers class * {
    @org.junit.** <methods>;
}

-keep class org.gradle.** { *; }
-keep class org.junit.** { *; }
-keep class org.testng.** { *; }
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-keepclassmembers class * {
    @org.junit.jupiter.api.Test <methods>;
    @org.junit.jupiter.api.BeforeEach <methods>;
    @org.junit.jupiter.api.AfterEach <methods>;
    void set*();
}

# Desktop app

-keep class bisq.user.identity.UserIdentityStore {
    *;
}
-keep class bisq.security.AesSecretKey {
    *;
}
-keepclassmembers,allowshrinking class * {
    *** lambda*(...);
}

# Java Fx

-keep class javafx.** { *; }
-keep class com.sun.** { *; }

# Proto
-keep class com.google.protobuf.** { *; }
-keep class bisq.common.proto.Proto {
    public <methods>;
}
-keep class bisq.common.proto.PersistableProtoResolverMap { *; }
-keep class bisq.common.proto.ProtobufUtils { *; }

# Rest-API

-keep class com.sun.net.httpserver.** { *; }

