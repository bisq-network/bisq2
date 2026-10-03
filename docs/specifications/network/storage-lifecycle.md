# Network storage lifecycle

For each store type and registered class name, a node has one store instance and one persistence
client. Concurrent first requests must share that instance. Two stores for one name can diverge
and overwrite the same file with different data.

Creating a store includes loading its persisted entries and attaching its event listeners. A
lazily created store becomes available after this initialization completes. Store registration
and listener registration must complete as one operation before another caller obtains the store.

Stores found during application construction are loaded by the initial persistence read before
network services start handling messages.
