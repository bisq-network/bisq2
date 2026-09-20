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
# peers. A renamed payload type silently stores under a different key and stops interoperating. This one predates
# the annotation: see the javadoc of NetworkStorageWhiteList.
-keepnames class * implements bisq.network.p2p.services.data.storage.StoragePolicyAware
