# Network storage lifecycle

For each store type and registered class name, a node has one store instance and one persistence
client. Concurrent first requests must share that instance. Two stores for one name can diverge
and overwrite the same file with different data.

Creating a store includes loading its persisted entries and attaching its event listeners. A
lazily created store becomes available after this initialization completes. Store registration
and listener registration must complete as one operation before another caller obtains the store.

Stores found during application construction are loaded by the initial persistence read before
network services start handling messages.

A store removes the entries which it no longer keeps, for example expired entries, when it loads
them and then periodically while the node runs. The periodic removal starts when the network
services start, after the initial persistence read, and it stops when they shut down. Creating the
storage does not start it. Before the initial read the stores hold no entries, so an earlier run
would remove nothing, and it could overlap the read which replaces the entries of a store. A storage
which was only created, for example in a test, must not change its entries in the background.
