# Applied automatically by R8 to anything depending on this module. Nothing in this repository shrinks or
# obfuscates, so these rules exist for consumers which do, such as the mobile app.
#
# A payload type declares its storage properties with @StoragePolicy, and MetaData.from reads them back at runtime.
# Two things have to survive for that to work, and both fail quietly rather than loudly.

# The annotation has to stay on the class, with its default values. A declaration which omits priority or
# maxMapSize takes them from the AnnotationDefault attribute on the annotation's own methods, so the members are
# kept as well as the type: losing one produces wrong storage properties, or an IncompleteAnnotationException,
# rather than an error at build time.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-keep @interface bisq.network.p2p.services.data.storage.StoragePolicy { *; }

# These appear only as annotation values, so shrinking does not see the use.
-keep enum bisq.network.p2p.services.data.storage.Ttl { *; }
-keep enum bisq.network.p2p.services.data.storage.Priority { *; }
-keep enum bisq.network.p2p.services.data.storage.MaxMapSize { *; }

# The type that carries the policy must survive as itself. @Inherited exists only in the declaring class file, so
# an abstract base merged into a subclass would take the policy with it and MetaData.from would throw at runtime.
# BisqEasyTradeMessage and MuSigTradeMessage each declare the policy for eleven subclasses this way. -keepnames
# would not cover it, because it allows shrinking and R8 counts merging a class away as shrinking.
-keep @bisq.network.p2p.services.data.storage.StoragePolicy class *

# MetaData derives className from Class.getSimpleName(), and that name is the store key a node publishes to its
# peers. A payload type that is renamed, or merged into another class, silently stores under a different key and
# stops interoperating with the rest of the network. -keep rather than -keepnames for the same reason as above:
# -keepnames permits merging. Nothing is lost by keeping them, since every one of these types is registered in
# ResolverConfig and therefore reachable anyway. The name requirement predates the annotation: see the javadoc of
# NetworkStorageWhiteList.
-keep class * implements bisq.network.p2p.services.data.storage.StoragePolicyAware
