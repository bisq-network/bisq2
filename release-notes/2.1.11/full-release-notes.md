# Bisq 2.1.11 Release Notes

These notes are written in the same practical style used by Bitcoin Core release notes: user and operator impact first, followed by a complete auditable commit inventory.

Commit range: after `1e32b0f21d46e1a5457a3b0342e276b9695ff21c` through `1fa9e79c109063f2e86e77966631f90d79d9a645`, generated with `git log --reverse 1e32b0f21d46e1a5457a3b0342e276b9695ff21c..1fa9e79c109063f2e86e77966631f90d79d9a645`. This excludes the starting commit `1e32b0f21d46` and includes later commits through `1fa9e79c10`. The inventory includes merge commits as separate entries because they are part of the release history.

- Total commits: 146
- Non-merge commits: 108
- Merge commits: 38
- Generated on: 2026-05-24

## Compatibility Notes

- Bisq version is set to `2.1.11`.
- The Java 21 toolchain is updated to Zulu `21.0.11`.
- OpenJFX runtime is updated to `21.0.11`.
- Tor was updated, macOS aarch64 support was added, and embedded Tor process handling was improved.
- Protobuf tool dependency verification metadata now includes explicit executable classifiers for Windows, Linux, macOS Intel, and macOS Apple Silicon.
- Dependency signature policy checks are now part of verification/check flows, and checksum-only artifacts must be explicitly reviewed and kept current.
- Release signing readiness now validates expected release signers, signing fingerprint configuration, and expected signable assets.

## Notable Changes

### Security, Verification, And Release Integrity

This release hardens the dependency, build, and release verification surface. Gradle dependency verification now covers source artifacts, validates checksum-only fallback entries against the resolved graph, rejects stale allowlist entries, hardens verification metadata parsing, and requires explicit metadata for executable build tools such as `protoc` and `protoc-gen-grpc-java`. The release process now has stronger checks for expected signers, signing fingerprints, expected signable assets, existing signatures, Gradle wrapper inputs, CVE scanning, and dependency signature policy.

### Runtime, Dependencies, And Packaging

The runtime and build stack receives broad updates: Java, OpenJFX, gRPC, Bouncy Castle, HttpClient5, Logback, Jackson, JNA, Lombok, JUnit, Mockito, I2P, JavaCV, Jersey, OkHttp, Swagger Core, and other dependencies were updated or cleaned up. Tor packaging was updated, embedded Tor process lifecycle handling was improved, inherited `LD_PRELOAD` is cleared for embedded Tor and installer tests, and local `jsocks`/`jtorctl` helper modules were added.

### Product And User-Facing Fixes

Bisq Easy offer amount constraints are enforced, REST take-offer amount handling uses the quote currency, banned users are prevented from sending messages or creating offers, QR-code text output was added for pairing codes, and the contacts flow was made less intrusive with wording/style improvements and removal of the contact-list auto-popup.

### Notifications, Webcam, And IPC

Relay push notifications now support mutable content and optional iOS-compatible symmetric encryption, normalize encoded content to base64, and use the newer relay API. Confidential message keys are bound to Bisq Easy identities. Webcam handling now verifies the webcam JAR before launch, authenticates IPC messages, hardens failure handling, and closes native resources on capture exit.

### CI, Build Workflow, And Documentation

GitHub Actions workflows were hardened, Apple Silicon macOS CI coverage was added, the archived Gradle build action was replaced, composite roots pin build environments, and `cleanAll`/`buildAll` were improved. JPMS and Java runtime packaging analysis documents were added for future packaging work.

## Complete Commit Inventory

Rows are ordered by `git log --reverse` over the release range. Some commit dates predate the starting commit because those side-branch commits were merged into the release history after the starting point.

| Date | Commit | Type | Summary | Author |
| --- | --- | --- | --- | --- |
| 2026-05-19 | [890c2fafd9](https://github.com/bisq-network/bisq2/commit/890c2fafd913daebbb63b7df7fc3dfa9cb32c3c0) | Commit | Update Java 21 toolchain to Zulu 21.0.11 | HenrikJannsen |
| 2026-05-19 | [0056ecc2bf](https://github.com/bisq-network/bisq2/commit/0056ecc2bf3608110eda9313bda055297d905393) | Commit | Update gRPC to 1.81.0 | HenrikJannsen |
| 2026-05-19 | [62a3c43e07](https://github.com/bisq-network/bisq2/commit/62a3c43e07e40d4daf5e44ab7fb860457ac64be6) | Commit | Update Bouncy Castle to 1.84 | HenrikJannsen |
| 2026-05-19 | [9caff90bb2](https://github.com/bisq-network/bisq2/commit/9caff90bb2bc76b7826a050b2655cfdc3de649c5) | Commit | Update HttpClient5 to 5.6.1 | HenrikJannsen |
| 2026-05-19 | [cd741962e2](https://github.com/bisq-network/bisq2/commit/cd741962e2754f47a97a050fc02d0dc1e66758cb) | Commit | Update Logback to 1.5.32 | HenrikJannsen |
| 2026-05-19 | [c7e327a24f](https://github.com/bisq-network/bisq2/commit/c7e327a24f775e3e2b8ee90909b7de61176f2d43) | Commit | Update Commons Lang to 3.20.0 | HenrikJannsen |
| 2026-05-19 | [881c4bd003](https://github.com/bisq-network/bisq2/commit/881c4bd00329f5cdcc0f4d477a8dbc1106bf567d) | Commit | Update Jackson to 2.21.3 | HenrikJannsen |
| 2026-05-19 | [b9308b0190](https://github.com/bisq-network/bisq2/commit/b9308b019075e4dd389e84ff4e2db28888fef2e5) | Commit | Update JetBrains annotations to 26.1.0 | HenrikJannsen |
| 2026-05-19 | [17bb99d1a5](https://github.com/bisq-network/bisq2/commit/17bb99d1a5ca96a431ec2bdd18e38c345423b6a6) | Commit | Update AssertJ to 3.27.7 | HenrikJannsen |
| 2026-05-19 | [2b7fad456b](https://github.com/bisq-network/bisq2/commit/2b7fad456b6eab520a9e1e456d529ee12bc2551a) | Commit | Update Commons Codec to 1.22.0 | HenrikJannsen |
| 2026-05-19 | [7c46885220](https://github.com/bisq-network/bisq2/commit/7c4688522021e36f15207beeaaa70f2a8b356a8d) | Commit | Update Failsafe to 3.3.2 | HenrikJannsen |
| 2026-05-19 | [4a82b4876a](https://github.com/bisq-network/bisq2/commit/4a82b4876a7be83184f46379b33749c208398983) | Commit | Update libphonenumber to 9.0.30 | HenrikJannsen |
| 2026-05-19 | [4a35f2d4bf](https://github.com/bisq-network/bisq2/commit/4a35f2d4bfc825fc4545483f5e8d92c9729bb87b) | Commit | Update JNA to 5.18.1 | HenrikJannsen |
| 2026-05-19 | [879f049125](https://github.com/bisq-network/bisq2/commit/879f049125276597e07605ccd603f3a2cba613da) | Commit | Update JeroMQ to 0.6.0 | HenrikJannsen |
| 2026-05-19 | [b63c0f3f73](https://github.com/bisq-network/bisq2/commit/b63c0f3f731bcce9b33bd69b584bfc44b06b33e8) | Commit | Update Lombok to 1.18.46 | HenrikJannsen |
| 2026-05-19 | [c143616c41](https://github.com/bisq-network/bisq2/commit/c143616c41524be7ef5450ffc60f31f386b83445) | Commit | Update JUnit Jupiter to 6.0.3 | HenrikJannsen |
| 2026-05-19 | [623208ffbf](https://github.com/bisq-network/bisq2/commit/623208ffbf574495e855e6c21e4b105ddfd12b2f) | Commit | Update Mockito to 5.23.0 | HenrikJannsen |
| 2026-05-19 | [67a6de6624](https://github.com/bisq-network/bisq2/commit/67a6de66249a9681dc2b8bba807765280d67797f) | Commit | Update Moshi to 1.15.2 | HenrikJannsen |
| 2026-05-19 | [9b32f5aed8](https://github.com/bisq-network/bisq2/commit/9b32f5aed8863bb4d684caec3e6a1d608e6f50b2) | Commit | Update XZ for Java to 1.12 | HenrikJannsen |
| 2026-05-19 | [74075a1c0b](https://github.com/bisq-network/bisq2/commit/74075a1c0ba52bbce76154cb85332672c94caaf2) | Commit | Update Typesafe Config to 1.4.8 | HenrikJannsen |
| 2026-05-19 | [18b09c2576](https://github.com/bisq-network/bisq2/commit/18b09c25769191d696860e6d75327ae706b48a80) | Commit | Update ZXing to 3.5.4 | HenrikJannsen |
| 2026-05-19 | [5b74c602a4](https://github.com/bisq-network/bisq2/commit/5b74c602a42de1e51c4f8678f0d20327a8ff8c0b) | Commit | Update RichTextFX to 0.11.7 | HenrikJannsen |
| 2026-05-19 | [7f1e502ad8](https://github.com/bisq-network/bisq2/commit/7f1e502ad80ce64ff3b67ab7993ae618c49b31f0) | Commit | Update JAXB runtime to 4.0.8 | HenrikJannsen |
| 2026-05-19 | [2d2f357024](https://github.com/bisq-network/bisq2/commit/2d2f3570242bd47086171bb14b2b4fa387093283) | Commit | Update TestFX to 4.0.18 | HenrikJannsen |
| 2026-05-19 | [0b39303da8](https://github.com/bisq-network/bisq2/commit/0b39303da89d4da5848f4a50454d88bd40ea871d) | Commit | Update OpenJFX Monocle to 21.0.2 | HenrikJannsen |
| 2026-05-19 | [de70c4badb](https://github.com/bisq-network/bisq2/commit/de70c4badbcee49c7bc951220a3f44b5b78a3ff9) | Commit | Update OpenJFX runtime to 21.0.11 | HenrikJannsen |
| 2026-05-19 | [3da067fa91](https://github.com/bisq-network/bisq2/commit/3da067fa91f10ab39dfd16992056f8913bb7b439) | Commit | Update OpenJFX Gradle plugin to 0.1.0 | HenrikJannsen |
| 2026-05-19 | [ab363f205e](https://github.com/bisq-network/bisq2/commit/ab363f205e08e60e98f261ee2c4fe08636db4bb5) | Commit | Update Swagger Core to 2.2.50 | HenrikJannsen |
| 2026-05-19 | [1a97b2f47e](https://github.com/bisq-network/bisq2/commit/1a97b2f47eec2d7d242b6fb3acef4e2a83528971) | Commit | Update OkHttp BOM to 5.3.2 | HenrikJannsen |
| 2026-05-19 | [f5a43a4bd5](https://github.com/bisq-network/bisq2/commit/f5a43a4bd53a2d3d190c29d119c7a1c819975178) | Commit | Update Jersey to 4.0.2 | HenrikJannsen |
| 2026-05-19 | [a543bfd2af](https://github.com/bisq-network/bisq2/commit/a543bfd2af7487a5ed098921c592425a8629919f) | Commit | Update Grizzly websockets to 5.0.1 | HenrikJannsen |
| 2026-05-19 | [7e96ecdd78](https://github.com/bisq-network/bisq2/commit/7e96ecdd7820c706fa42981522387fa5f4c0e167) | Commit | Update Spring Boot starter web to 4.0.6 | HenrikJannsen |
| 2026-05-19 | [e5aef29c00](https://github.com/bisq-network/bisq2/commit/e5aef29c00323c22b79f69755ac8db4a3e29d20d) | Commit | Update I2P libraries to 2.12.0 | HenrikJannsen |
| 2026-05-19 | [068328b253](https://github.com/bisq-network/bisq2/commit/068328b253c440465b7cbbe3512815c7b0cc25e6) | Commit | Update JavaCV runtime to 1.5.13 | HenrikJannsen |
| 2026-05-19 | [27f31c9b0b](https://github.com/bisq-network/bisq2/commit/27f31c9b0b0fc5962beabd4ef21b22b638c1236f) | Commit | Update Shadow plugin to 9.4.1 | HenrikJannsen |
| 2026-05-19 | [c421532681](https://github.com/bisq-network/bisq2/commit/c4215326815049a93a0aa538abeb70e67ed023bf) | Commit | Update Kotlin DSL plugin to 6.7.0 | HenrikJannsen |
| 2026-05-19 | [e8b937e057](https://github.com/bisq-network/bisq2/commit/e8b937e0579eabe71050d9d2f5cc16e563062b29) | Commit | Update FontAwesomeFX icon pack | HenrikJannsen |
| 2026-05-19 | [7903b26cbd](https://github.com/bisq-network/bisq2/commit/7903b26cbdc16d0b16d840b2e78c3f0b96572432) | Commit | Update jsocks JitPack pin | HenrikJannsen |
| 2026-05-19 | [8329f93031](https://github.com/bisq-network/bisq2/commit/8329f9303156586daa0454b24505e026d1f1a714) | Commit | Update jtorctl to 1.5 | HenrikJannsen |
| 2026-05-19 | [1fc6de068a](https://github.com/bisq-network/bisq2/commit/1fc6de068a68e43fc29485bc0d655f5d58301153) | Commit | Remove unused Spring Boot catalog alias | HenrikJannsen |
| 2026-05-19 | [5b9a9d7df9](https://github.com/bisq-network/bisq2/commit/5b9a9d7df9d734f52322c324012e56932db07c7a) | Commit | Update platform dependency constraints | HenrikJannsen |
| 2026-05-19 | [b368feb558](https://github.com/bisq-network/bisq2/commit/b368feb558b62e52d11a35463d34de1e29c28c22) | Commit | Remove unused dependency catalog entries | HenrikJannsen |
| 2026-05-19 | [2952bca483](https://github.com/bisq-network/bisq2/commit/2952bca483d7ee509e73da32f106ec718ba8fc9f) | Commit | Remove MaterialPasswordFieldTest which is disabled anyway and the only consumer of the monocle dependency. With that removal we can remove monocle dependency as well. | HenrikJannsen |
| 2026-05-19 | [f2796f91cb](https://github.com/bisq-network/bisq2/commit/f2796f91cb91eb9db5e05135164801e2316c377d) | Merge | Merge pull request #4732 from HenrikJannsen/update-dependencies | HenrikJannsen |
| 2026-05-19 | [11ba89c814](https://github.com/bisq-network/bisq2/commit/11ba89c814ac655069a5b57831539542af3d78ca) | Commit | Use quote currency for REST take-offer amount | KimStrand |
| 2026-05-19 | [3f8e3148da](https://github.com/bisq-network/bisq2/commit/3f8e3148daae7b35d0786bead8ab07baf621bcee) | Commit | Revert "Update FontAwesomeFX icon pack" | HenrikJannsen |
| 2026-05-19 | [e44ebae0a0](https://github.com/bisq-network/bisq2/commit/e44ebae0a075b72ea1340f7a0681e9646d199473) | Merge | Merge pull request #4739 from HenrikJannsen/Revert-Update-FontAwesomeFX-icon-pack | HenrikJannsen |
| 2026-05-19 | [2db4542924](https://github.com/bisq-network/bisq2/commit/2db4542924c5327203e604269360243937fc82c1) | Commit | Set v2.1.11 | HenrikJannsen |
| 2026-05-19 | [84f5ea2ae8](https://github.com/bisq-network/bisq2/commit/84f5ea2ae8d3ef7ea9fbb6dc54a6b837dd1e7133) | Merge | Merge pull request #4740 from HenrikJannsen/set-v-2.1.11 | HenrikJannsen |
| 2026-05-19 | [47323c618a](https://github.com/bisq-network/bisq2/commit/47323c618a8f44da5a71d4b774842e108083e5b8) | Commit | Update Tor, support aarch64 macos and fix Tor packaging and embedded process lifecycle handling | HenrikJannsen |
| 2026-05-19 | [716ed91153](https://github.com/bisq-network/bisq2/commit/716ed911539d3b7169fe6c876a4eb380ab2a7ec3) | Commit | Improve cleanAll and buildAll tasks | HenrikJannsen |
| 2026-05-19 | [5bccf3d047](https://github.com/bisq-network/bisq2/commit/5bccf3d047babde593adcdab70eb3c873af0ce86) | Commit | Add temp directory to gitignore | HenrikJannsen |
| 2026-05-19 | [2a92aee693](https://github.com/bisq-network/bisq2/commit/2a92aee6931323d56530f930283adc043a52bcd1) | Merge | Merge pull request #4743 from HenrikJannsen/Add-temp-directory-to-gitignore | HenrikJannsen |
| 2026-05-19 | [ee2c3dbc51](https://github.com/bisq-network/bisq2/commit/ee2c3dbc51adad7232c024ca4e05ba0e73210920) | Commit | Clear inherited LD_PRELOAD for embedded Tor | HenrikJannsen |
| 2026-05-19 | [508816c87e](https://github.com/bisq-network/bisq2/commit/508816c87eb555f146c751f2dd9caa36b7116992) | Commit | Clear inherited LD_PRELOAD in Tor installer integration test | HenrikJannsen |
| 2026-05-19 | [80823eb42f](https://github.com/bisq-network/bisq2/commit/80823eb42fef2df7e0a062c5054191e9cf95a02c) | Merge | Merge pull request #4741 from HenrikJannsen/update-tor-and-add-aarch64-support | HenrikJannsen |
| 2026-05-19 | [71c05271f4](https://github.com/bisq-network/bisq2/commit/71c05271f4aaf797a7ed5b1a56d746b19d444daa) | Merge | Merge pull request #4735 from HenrikJannsen/improve-cleanAll-and-buildAll-tasks | HenrikJannsen |
| 2026-05-20 | [fcfa5e76d8](https://github.com/bisq-network/bisq2/commit/fcfa5e76d8791ffcb3be3f47e6208b4cb6836018) | Merge | Merge pull request #4737 from KimStrand/rest-take-offer-quote-currency | HenrikJannsen |
| 2026-05-19 | [4e9b8778bb](https://github.com/bisq-network/bisq2/commit/4e9b8778bb5fbf0e14ef2523ed453cb7b3caa2db) | Commit | Harden Gradle dependency signature verification | HenrikJannsen |
| 2026-05-19 | [ba4bfec456](https://github.com/bisq-network/bisq2/commit/ba4bfec45627a643b3f26b9c312643c737a1a7fa) | Commit | Refresh dependency verification after dependency updates | HenrikJannsen |
| 2026-05-19 | [3b3584f01e](https://github.com/bisq-network/bisq2/commit/3b3584f01e2fa3ff8210853a30f8bcc99d0f98cf) | Commit | Add dependency verification for source artifacts | HenrikJannsen |
| 2026-05-19 | [ed01dcf8bf](https://github.com/bisq-network/bisq2/commit/ed01dcf8bf2c3e90c369fa8994469dbac27c039f) | Commit | Verify IDE source artifacts in dependency metadata | HenrikJannsen |
| 2026-05-19 | [20892d6abe](https://github.com/bisq-network/bisq2/commit/20892d6abe31e46092142187c542e9f290744ed2) | Commit | Pin jtorctl to requested commit | HenrikJannsen |
| 2026-05-19 | [2fb69683bf](https://github.com/bisq-network/bisq2/commit/2fb69683bf36c3cbb73fdd7d2ddb8d79a7a8bfbe) | Commit | Refresh dependency verification after 2.1.11 rebase | HenrikJannsen |
| 2026-05-19 | [75b487db6c](https://github.com/bisq-network/bisq2/commit/75b487db6c91cda41bf3c3e09b1ae26ac4785a6a) | Commit | Enforce Bisq Easy offer amount constraints | KimStrand |
| 2026-05-20 | [0597bd7904](https://github.com/bisq-network/bisq2/commit/0597bd79047b8e7feaaafa0185f883e06528e3d3) | Commit | Validate checksum fallback coordinates | HenrikJannsen |
| 2026-05-20 | [3c2fbff691](https://github.com/bisq-network/bisq2/commit/3c2fbff691a0943ee2e45c0893f818bde9d44cb6) | Commit | Harden verification metadata XML parsing | HenrikJannsen |
| 2026-05-20 | [cb72fd5709](https://github.com/bisq-network/bisq2/commit/cb72fd5709000c2d07dee2007b49e4ad31df63b8) | Merge | Merge pull request #4744 from HenrikJannsen/Add-Dependency-Signature-Verification | HenrikJannsen |
| 2026-05-20 | [62c67dee8f](https://github.com/bisq-network/bisq2/commit/62c67dee8ff9272ee2a687b09229fccc61ab1342) | Merge | Merge pull request #4736 from KimStrand/bisq-easy-offer-amount-validation | HenrikJannsen |
| 2026-05-19 | [1098936362](https://github.com/bisq-network/bisq2/commit/1098936362785ffb7990084549e65c93aa9309dc) | Commit | Tighten input validation in MetaData, Persistence and StorageService | wodoro |
| 2026-05-20 | [88c731eaff](https://github.com/bisq-network/bisq2/commit/88c731eaffc505a91700e9923174da4f76a9eba4) | Merge | Merge pull request #4747 from wodoro/restrict-metadata-classname-allowlist | HenrikJannsen |
| 2026-05-20 | [8a3a8794f8](https://github.com/bisq-network/bisq2/commit/8a3a8794f84edbd91e92389406dad5284a45109f) | Commit | Add release artifact GPG signing task | HenrikJannsen |
| 2026-05-20 | [8ddf3e52bb](https://github.com/bisq-network/bisq2/commit/8ddf3e52bb5ac9c2cbb4385218aabbc14fc6b5b6) | Commit | Preserve existing release signatures | HenrikJannsen |
| 2026-05-20 | [d7f3fecd4a](https://github.com/bisq-network/bisq2/commit/d7f3fecd4ab5da8e872d7954a699dddfaa7314c7) | Commit | Document GPG fingerprint override property | HenrikJannsen |
| 2026-05-20 | [0c58547fa4](https://github.com/bisq-network/bisq2/commit/0c58547fa4cb72653cf5a17542869569c3c87cb7) | Merge | Merge pull request #4748 from HenrikJannsen/Add-release-artifact-GPG-signing-task | HenrikJannsen |
| 2026-05-19 | [b22a500053](https://github.com/bisq-network/bisq2/commit/b22a500053eb345e186764ef92150f6bf484a44f) | Commit | Close webcam native resources on capture exit | KimStrand |
| 2026-05-20 | [8a36e54695](https://github.com/bisq-network/bisq2/commit/8a36e546951cb43284fc23f694d4a8d250e8fe25) | Merge | Merge pull request #4746 from KimStrand/webcam-resource-cleanup | HenrikJannsen |
| 2026-05-20 | [daaf56c7ba](https://github.com/bisq-network/bisq2/commit/daaf56c7baf871e1d8d7e462ca850e72bbe1c957) | Commit | Harden GitHub Actions workflows | HenrikJannsen |
| 2026-05-20 | [7232b7b3de](https://github.com/bisq-network/bisq2/commit/7232b7b3de8939466d2d33a95a730b912921983d) | Commit | Add Apple Silicon macOS CI coverage | HenrikJannsen |
| 2026-05-20 | [cb1259298f](https://github.com/bisq-network/bisq2/commit/cb1259298f14fc999492ed7d68b3538670b46424) | Commit | Replace archived Gradle build action | HenrikJannsen |
| 2026-05-20 | [e4e6f22151](https://github.com/bisq-network/bisq2/commit/e4e6f221518a03fb1ab9ad65412064cab2367ad5) | Commit | Harden workflow step boundary scanning | HenrikJannsen |
| 2026-05-20 | [664d82812f](https://github.com/bisq-network/bisq2/commit/664d82812fdd105963f7b30ef90da1abab81055c) | Merge | Merge pull request #4750 from HenrikJannsen/Add-Apple-Silicon-macOS-CI-coverage | HenrikJannsen |
| 2026-05-20 | [7acd6f6f86](https://github.com/bisq-network/bisq2/commit/7acd6f6f8673a39be6872df4baeeb25454bc440e) | Commit | Add Gradle wrapper security verification | HenrikJannsen |
| 2026-05-20 | [c1781c18c7](https://github.com/bisq-network/bisq2/commit/c1781c18c7802d377e9578b3765d178e320ea4db) | Commit | Enforce Gradle wrapper verification in check | HenrikJannsen |
| 2026-05-20 | [0cea68a982](https://github.com/bisq-network/bisq2/commit/0cea68a9822a28177eef1f135650fe89ef7af471) | Merge | Merge pull request #4751 from HenrikJannsen/Add-Gradle-wrapper-security-verification | HenrikJannsen |
| 2026-05-20 | [a77c5f79eb](https://github.com/bisq-network/bisq2/commit/a77c5f79eb389e413cc2c5c6b2a798790eb989aa) | Commit | Pin build toolchain versions | HenrikJannsen |
| 2026-05-20 | [dc86198e24](https://github.com/bisq-network/bisq2/commit/dc86198e2414c4ea807cbc6b09d04caca7d5390a) | Commit | Remove legacy Bisq 1 packaging toolchain branch | HenrikJannsen |
| 2026-05-20 | [fbc97c9dc9](https://github.com/bisq-network/bisq2/commit/fbc97c9dc9898a55e3f3bf73b543085f6ae4fe09) | Merge | Merge pull request #4752 from HenrikJannsen/Pin-build-toolchain-versions | HenrikJannsen |
| 2026-05-20 | [17d8dd262e](https://github.com/bisq-network/bisq2/commit/17d8dd262eb6b514d52a22604ca63b67ff8cc50e) | Commit | Authenticate webcam IPC messages | KimStrand |
| 2026-05-20 | [63025796ce](https://github.com/bisq-network/bisq2/commit/63025796ceb6e35fc604bd642496ca842500c1b6) | Commit | Add isolated CVE scan tool | HenrikJannsen |
| 2026-05-20 | [52f3ff275f](https://github.com/bisq-network/bisq2/commit/52f3ff275f7de799347af1777b6c555e47e88158) | Commit | Gate checksum-only dependency artifacts | HenrikJannsen |
| 2026-05-20 | [f3b4470a33](https://github.com/bisq-network/bisq2/commit/f3b4470a3391158bad44dfe04583fc22f6dc2d08) | Commit | Honor CVE scan rerun requests | HenrikJannsen |
| 2026-05-20 | [acd79d7d20](https://github.com/bisq-network/bisq2/commit/acd79d7d20acd8c45c1cb4d79d0bf5962f5f16b7) | Merge | Merge pull request #4756 from HenrikJannsen/Gate-checksum-only-dependency-artifacts | HenrikJannsen |
| 2026-05-20 | [9b7f958dae](https://github.com/bisq-network/bisq2/commit/9b7f958daedf93724d1bbc50fa0d46f13f796182) | Commit | Harden Bisq 2 release signing readiness | HenrikJannsen |
| 2026-05-20 | [4e5ca4f031](https://github.com/bisq-network/bisq2/commit/4e5ca4f031ba69cfd0449d8b61607457c6a3e7ed) | Commit | Enforce release signing fingerprint by default | HenrikJannsen |
| 2026-05-20 | [1e6d1fdd60](https://github.com/bisq-network/bisq2/commit/1e6d1fdd606866addac911f621960e35cde9dfa8) | Commit | Rename active signing key marker copy task | HenrikJannsen |
| 2026-05-20 | [6e0d0bbd9c](https://github.com/bisq-network/bisq2/commit/6e0d0bbd9c59a5619068a2159bf3eebcf2e0ea0f) | Commit | Expect all signable Bisq 2 release assets | HenrikJannsen |
| 2026-05-20 | [e7ef38a8ba](https://github.com/bisq-network/bisq2/commit/e7ef38a8bafa308276b6677209b3ba03d20e1110) | Commit | Assert expected release signature signers | HenrikJannsen |
| 2026-05-20 | [0f3eb62cff](https://github.com/bisq-network/bisq2/commit/0f3eb62cff5e00fcbbab9e1394b8992514b2ff34) | Merge | Merge pull request #4757 from HenrikJannsen/Harden-Bisq-2-release-signing-readiness | HenrikJannsen |
| 2026-05-20 | [a606f46516](https://github.com/bisq-network/bisq2/commit/a606f465165c901effd189d5836f129a00b35df4) | Commit | Validate checksum allowlist against resolved graph | HenrikJannsen |
| 2026-05-20 | [a43024b14a](https://github.com/bisq-network/bisq2/commit/a43024b14a636be076fd6ddadaaf09e92fa7db92) | Commit | Run dependency signature policy during checks | HenrikJannsen |
| 2026-05-20 | [8ee8162e96](https://github.com/bisq-network/bisq2/commit/8ee8162e9617dd4602b96d0fa60f25c0367433a1) | Commit | Pin build environment in composite roots | HenrikJannsen |
| 2026-04-19 | [7237c9b0dd](https://github.com/bisq-network/bisq2/commit/7237c9b0dd885f813666a5e84d91d04b31030ae9) | Commit | feat: implement QRcode in text for pairing code | wodoro |
| 2026-05-20 | [db8e674de1](https://github.com/bisq-network/bisq2/commit/db8e674de1057e088ef126904631f663cb139217) | Merge | Merge pull request #4759 from HenrikJannsen/various-build-system-improvements | HenrikJannsen |
| 2026-05-20 | [d6c628b4cc](https://github.com/bisq-network/bisq2/commit/d6c628b4cc98d0a1271d5328647f18df831d9183) | Commit | Verify update signatures with each key source | HenrikJannsen |
| 2026-05-19 | [9b0dad2353](https://github.com/bisq-network/bisq2/commit/9b0dad2353e3e10b614763800bfe92a1a1780527) | Commit | Bind confidential message keys to Bisq Easy identities | KimStrand |
| 2026-05-20 | [952d60d29b](https://github.com/bisq-network/bisq2/commit/952d60d29b0fae603031df5ce6aa54398aa613e5) | Merge | Merge pull request #4758 from rodvar/chore/bring_missing_qr_pairing_ascii | HenrikJannsen |
| 2026-05-20 | [be3dc0b5cd](https://github.com/bisq-network/bisq2/commit/be3dc0b5cd8bb6492516491f3d94bbb15623888b) | Merge | Merge pull request #4734 from KimStrand/bisq-easy-confidential-sender-binding | HenrikJannsen |
| 2026-05-20 | [99a1a2c7f4](https://github.com/bisq-network/bisq2/commit/99a1a2c7f4dadaba0349f281373dc9ac2b693e7b) | Merge | Merge pull request #4738 from KimStrand/webcam-ipc-auth | HenrikJannsen |
| 2026-05-20 | [e0cde10b4b](https://github.com/bisq-network/bisq2/commit/e0cde10b4bbeb26efb66f783182fc204e724822e) | Commit | Fix release readiness temp dir reset | HenrikJannsen |
| 2026-05-20 | [b3bfcd6de9](https://github.com/bisq-network/bisq2/commit/b3bfcd6de93baa80ef1b1f15c12616b76aa64d5d) | Commit | Harden webcam IPC failure handling | HenrikJannsen |
| 2026-05-20 | [1b052aec61](https://github.com/bisq-network/bisq2/commit/1b052aec61687908151ea3c267f880bdc00b6702) | Merge | Merge pull request #4760 from HenrikJannsen/Verify-update-signatures-with-each-key-source | HenrikJannsen |
| 2026-05-19 | [40c3c485ea](https://github.com/bisq-network/bisq2/commit/40c3c485ea6364eb5f0bbc28be19ddbf167a47a7) | Commit | Verify webcam jar before launch | KimStrand |
| 2026-05-20 | [db25b41c6a](https://github.com/bisq-network/bisq2/commit/db25b41c6a5c2bf3e1e586da58e5da555c58d7d5) | Merge | Merge pull request #4745 from KimStrand/webcam-jar-verification | HenrikJannsen |
| 2026-05-20 | [40def5cf75](https://github.com/bisq-network/bisq2/commit/40def5cf75c99857460f642560100f7a74a7819b) | Merge | Merge pull request #4761 from HenrikJannsen/improve-webcam-icp-message-handling | HenrikJannsen |
| 2026-05-20 | [94ad3dedda](https://github.com/bisq-network/bisq2/commit/94ad3dedda09763802fb946383f28d4052ce57a3) | Commit | Harden dependency surface and security overrides | HenrikJannsen |
| 2026-05-20 | [36d290945d](https://github.com/bisq-network/bisq2/commit/36d290945dda2833866222f6f3ba37d331fe31b0) | Merge | Merge pull request #4762 from HenrikJannsen/Harden-dependency-surface-and-security-overrides | HenrikJannsen |
| 2026-05-20 | [b36246b73d](https://github.com/bisq-network/bisq2/commit/b36246b73d2787c1d5696465e02019f1464f7aaa) | Commit | Disable launcher update jar loading | HenrikJannsen |
| 2026-05-20 | [01c811c330](https://github.com/bisq-network/bisq2/commit/01c811c330b03b21458d2670ea91e553c9861b26) | Merge | Merge pull request #4763 from HenrikJannsen/Disable-launcher-update-jar-loading | HenrikJannsen |
| 2026-05-20 | [966e63ba5e](https://github.com/bisq-network/bisq2/commit/966e63ba5e9ab131ed2c95c97f617fd115c96bb4) | Commit | Add source module shells for Tor libraries | HenrikJannsen |
| 2026-05-20 | [ea20c8b3ff](https://github.com/bisq-network/bisq2/commit/ea20c8b3ff5c4822604d729c54fe5b6011885277) | Commit | Add source code from jsocks and jtorctl projects. | HenrikJannsen |
| 2026-05-20 | [3069038c62](https://github.com/bisq-network/bisq2/commit/3069038c62a1197c527dc793e665bbc3f2478101) | Commit | Use local Tor helper modules | HenrikJannsen |
| 2026-05-20 | [26bd79afd5](https://github.com/bisq-network/bisq2/commit/26bd79afd503763c6421380be4fc00dc17afdaa0) | Commit | Add readme files | HenrikJannsen |
| 2026-05-20 | [01f2346771](https://github.com/bisq-network/bisq2/commit/01f234677137832a0ca2c945641304985ef93eab) | Commit | Remove auto popup in contact list. | HenrikJannsen |
| 2026-05-20 | [90e4069249](https://github.com/bisq-network/bisq2/commit/90e40692498b3961a5bd8e1242c7750eeb927d6c) | Merge | Merge pull request #4765 from HenrikJannsen/Remove-auto-popup-in-contact-list | HenrikJannsen |
| 2026-05-20 | [3b26d351e3](https://github.com/bisq-network/bisq2/commit/3b26d351e3b2e0e2e560ec9ed499e2ad5e2ee79a) | Merge | Merge pull request #4764 from HenrikJannsen/add-jsocks-and-jtroctrl-modules | HenrikJannsen |
| 2026-04-21 | [a48ae59f00](https://github.com/bisq-network/bisq2/commit/a48ae59f0043900cca53398c99b79ffa2d286df3) | Commit | Add relay notification mutable-content support, iOS-compatible encryption support, base64 encoding fixes, relay API migration, and AesGcm reuse | rodvar |
| 2026-05-21 | [3cbf2d73ec](https://github.com/bisq-network/bisq2/commit/3cbf2d73ec10f87eed48867d3a95faa374a6fe6e) | Merge | Merge pull request #4766 from rodvar/chore/bring_missing_push_notifications_code_from_2.1.10 | HenrikJannsen |
| 2026-05-21 | [c3f00066c3](https://github.com/bisq-network/bisq2/commit/c3f00066c3421b52b6e82129970ca79a56469df5) | Commit | Improve wording and style | HenrikJannsen |
| 2026-05-21 | [3b77e242b6](https://github.com/bisq-network/bisq2/commit/3b77e242b62857fd75452a24717696a60b3d1143) | Merge | Merge pull request #4767 from HenrikJannsen/improve-contacts-view | HenrikJannsen |
| 2026-05-21 | [ebdf53ee6c](https://github.com/bisq-network/bisq2/commit/ebdf53ee6c2e9c56eafae7ab89cb6dcd61c7361b) | Commit | Don't send a message or create offer when user is banned. | HenrikJannsen |
| 2026-05-21 | [af0798d750](https://github.com/bisq-network/bisq2/commit/af0798d7508df6f3c1f1713543f1f85ae02657e9) | Commit | Update Logback conversion rule syntax | HenrikJannsen |
| 2026-05-21 | [4e42568cb1](https://github.com/bisq-network/bisq2/commit/4e42568cb1721ddee82cb07bfb3c203b6c000454) | Merge | Merge pull request #4768 from HenrikJannsen/avoid-error-popup-at-banned-user | HenrikJannsen |
| 2026-05-21 | [d4d6186cd8](https://github.com/bisq-network/bisq2/commit/d4d6186cd8d89acd1b00f15af59af69dc8df998c) | Merge | Merge pull request #4770 from HenrikJannsen/Update-Logback-conversion-rule-syntax | HenrikJannsen |
| 2026-05-21 | [90fcb760b1](https://github.com/bisq-network/bisq2/commit/90fcb760b19bcbb6face1ff931cd69bebdd8d7af) | Commit | Dont log InterruptedException if node has started shutdown | HenrikJannsen |
| 2026-05-21 | [98160e7b13](https://github.com/bisq-network/bisq2/commit/98160e7b13b3762824f56f18ccf3cf9e606aeb47) | Merge | Merge pull request #4771 from HenrikJannsen/dont-log-interrupted-exception-if-node-started-shutdown | HenrikJannsen |
| 2026-05-21 | [374080af47](https://github.com/bisq-network/bisq2/commit/374080af47c70e32fb923814e1e59c3f17a34c6e) | Commit | Add JPMS feasibility analysis doc | HenrikJannsen |
| 2026-05-21 | [224c1f4d9b](https://github.com/bisq-network/bisq2/commit/224c1f4d9b4e20557f961c406045534a1f53ce53) | Merge | Merge pull request #4773 from HenrikJannsen/Add-JPMS-feasibility-analysis-doc | HenrikJannsen |
| 2026-05-21 | [6b1d7071a7](https://github.com/bisq-network/bisq2/commit/6b1d7071a71ef712e40f49dca8e38ecca37d0642) | Commit | Add Java runtime packaging analysis doc | HenrikJannsen |
| 2026-05-21 | [b8c1b65ea1](https://github.com/bisq-network/bisq2/commit/b8c1b65ea13cd3ef8367fdbc751333d5e1eb76d7) | Merge | Merge pull request #4774 from HenrikJannsen/Add-Java-runtime-packaging-analysis-doc | HenrikJannsen |
| 2026-05-23 | [e877a2f823](https://github.com/bisq-network/bisq2/commit/e877a2f8233a485573e5d32bae76d78043d51d4d) | Commit | Tighten executable dependency verification metadata | HenrikJannsen |
| 2026-05-23 | [bb3f750ca3](https://github.com/bisq-network/bisq2/commit/bb3f750ca35ff09195a6a6429baf844b72415cfd) | Merge | Merge pull request #4778 from HenrikJannsen/Tighten-executable-dependency-verification-metadata | HenrikJannsen |
| 2026-05-24 | [fa18b01e35](https://github.com/bisq-network/bisq2/commit/fa18b01e35fcf5c557f9200dce1a9b1bb72e906c) | Commit | Add Windows protobuf tool verification metadata | HenrikJannsen |
| 2026-05-24 | [8d9c3a0186](https://github.com/bisq-network/bisq2/commit/8d9c3a0186078b2e9bdec8f35cb3e1914a1702af) | Merge | Merge pull request #4779 from HenrikJannsen/Add-Windows-protobuf-tool-verification-metadata | HenrikJannsen |
| 2026-05-24 | [70c5836969](https://github.com/bisq-network/bisq2/commit/70c583696989ec7910f69a9d137a3ad69d28ba42) | Commit | Add Linux and macOS protobuf tool verification metadata | HenrikJannsen |
| 2026-05-24 | [1fa9e79c10](https://github.com/bisq-network/bisq2/commit/1fa9e79c109063f2e86e77966631f90d79d9a645) | Merge | Merge pull request #4780 from HenrikJannsen/Add-Linux-and-macOS-protobuf-tool-verification-metadata | HenrikJannsen |
