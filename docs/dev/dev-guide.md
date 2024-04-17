# Developer guide lines

_Note: Some of the documents might not be up-to-date but still should serve for getting an overview._

## Contributing

See [contributing.md](contributing.md)

## General code guidelines

See [code-guidelines.md](code-guidelines.md)

## Dependencies

We try to avoid adding dependencies as far as possible to reduce the risk for supply chain attacks. Sticking to plain
Java libraries is preferred over using 3rd party libraries.

## Asynchronous handling

We use the CompletableFutures framework for dealing with asynchronous code.

## MVC pattern

See [mvc-model.md](mvc-model.md)

## Protobuf

See [protobuf-notes.md](protobuf-notes.md)

## P2P network

See [network.md](network.md)
