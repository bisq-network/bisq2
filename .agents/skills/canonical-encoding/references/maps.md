# Map fields: entry order, key mapping, caching

Read this before you add or change a map field. The normative rules are in
`docs/specifications/encoding/canonical-encoding.md` (section "Map fields").

Bisq 2 avoids protobuf maps in data that must be deterministic (`docs/dev/protobuf-notes.md`); none of
the network storage classes has a map field. If you meet a map field, the rules below apply.

## Why the order needs a decision

Protobuf does not define an order for map entries. protobuf-java (default mode) writes the entries in
the insertion order of its internal map, which is the iteration order of the Java map that the builder
received with `putAllX(...)`. The protobuf bytes therefore depend on the map instance that the model
used. The canonical schema must declare the order explicitly, with a `CanonicalMapEntryIterator`. There
is no default.

## Choose the order

| Source map in the builder | Iterator |
| --- | --- |
| `TreeMap<String, ...>` with natural order | `TreeMapIterator.naturalOrder()` |
| A map type that defines its own deterministic order | source iteration order (`List::iterator`) |
| New map field | `TreeMapIterator.naturalOrder()` |
| A plain `HashMap` | none; stop and report it |

Notes:

- `TreeMapIterator.naturalOrder()` sorts strings with `String.compareTo` (UTF-16 code units). It rejects
  `null` keys and duplicate keys.
- `LegacyCollectorsToMapIterator` reproduces a Bisq 1 `HashMap` order for the Bisq 1 DAO state hash. Do
  not use it in Bisq 2.
- Source iteration order is correct only when the model's map type defines a deterministic order. It
  does not check duplicates, so use it only without a key mapper.
- A plain `HashMap` has an order that depends on its capacity history and on the insertion order of
  colliding keys. Report it to the developer; the solution is a sorted map in the model or a list of
  entries, as `docs/dev/protobuf-notes.md` recommends.
- Never use protobuf's deterministic serialization as the reference.

## Key and value mapping

When the source map has a different key or value type than the protobuf map, use the overload with a
key mapper and a value mapper. Two source keys that map to the same string are an error; use an
iterator that detects it (`TreeMapIterator`), never source iteration order.

## Absent maps

A getter must not return `null`. Return `Map.of()` or `Collections.emptyMap()` when the builder skips an
absent map. An empty map produces no record, as in protobuf.

## Entry bytes

Each entry is a message with the key as field `1` and the value as field `2`. Both are always written,
also when they are empty. Message values use the value schema.

## Caching encoded map bytes

`CanonicalMapEntryByteCache` lets the encoder reuse encoded map bytes. Bisq 2 does not use it. Add a
cache only when measurements show that it is needed, and follow the rules in the specification (bytes
identical with and without cache, invalidation on every content change, one entry encoding per map
instance, thread confinement). The encoder uses the cache only for the `DEFAULT` purpose, because the
cached bytes do not record the purpose that produced them.
