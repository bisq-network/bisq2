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

# MetaData derives className from Class.getSimpleName(), and that name is the store key a node publishes to its
# peers, so a renamed payload type silently stores under a different key and stops interoperating. Verified by
# removing this line and running R8: the store key of an inheriting subclass became "a".
#
# This also covers the types that carry the policy, including the two abstract bases which declare it for eleven
# subclasses each through @Inherited, because the processor rejects @StoragePolicy on anything that is not
# StoragePolicyAware. -keep rather than -keepnames, since -keepnames permits merging and nothing is lost by
# keeping these: every one is registered in ResolverConfig and therefore reachable anyway.
#
# The name requirement predates the annotation: see the javadoc of NetworkStorageWhiteList.
-keep class * implements bisq.network.p2p.services.data.storage.StoragePolicyAware
