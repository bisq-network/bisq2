### How to update the Gradle Wrapper

The wrapper can update itself using:

```
./gradlew wrapper --gradle-version 7.4.1 \
    --distribution-type all \
    --gradle-distribution-sha256-sum a9a7b7baba105f6557c9dcf9c3c6e8f7e57e6b49889c5f1d133f015d0727e4be
```

- `--gradle-version`: the version you wish to update to
- `--distribution-type`: `bin` or `all`. Use the `all` distribution to provide the sources needed for the IDE for 
code-completion and the ability to navigate the Gradle source code.
- `--gradle-distribution-sha256-sum`: the checksum for the chosen distribution from https://gradle.org/release-checksums/
