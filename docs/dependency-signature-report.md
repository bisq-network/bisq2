# Dependency Signature Report

Generated from `gradle/verification-metadata.xml` after resolving 703 configurations.

Signer metadata is loaded from `gradle/verification-keyring.keys`; names and emails come from the first OpenPGP user ID, and creation dates come from the public key packet.

Checksum-only review rationales are loaded from `gradle/dependency-checksum-fallback-allowlist.tsv`.

Refresh the metadata before regenerating this report:

```bash
./gradlew refreshDependencyVerificationKeyring
./gradlew resolveAndVerifyDependencies --write-verification-metadata pgp,sha256
./gradlew verifyDependencySignaturePolicy
./gradlew dependencySignatureReport
```

## Summary

| Metric | Count |
| --- | ---: |
| Resolved external modules | 224 |
| Modules with PGP-signed artifacts only | 220 |
| Modules using checksum-only artifacts only | 4 |
| Modules with mixed signed/checksum-only artifacts | 0 |
| Modules missing verification metadata | 0 |
| Verified artifacts | 324 |
| PGP-signed artifacts | 318 |
| Checksum-only artifacts | 6 |
| Resolved executable artifacts missing explicit metadata | 0 |
| Stale executable metadata entries for unresolved versions | 0 |
| Signer keys found in exported keyring | 82 / 82 |
| Signer keys with name or email | 51 / 82 |
| Signer keys with creation date | 82 / 82 |

## Handling Transitive Dependencies

Treat transitive dependencies the same as direct dependencies. Gradle verifies the resolved artifact graph, so a transitive artifact with a published signature should be PGP-verified, and an unsigned artifact should keep an explicit SHA-256 checksum-only fallback. Review metadata changes separately when dependency versions change, especially new trusted keys and new checksum-only artifacts.

## Checksum-Only Dependencies

| Dependency | Scope | Checksum-only artifacts and review rationale |
| --- | --- | --- |
| `com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.4.1` | direct | `com.gradleup.shadow.gradle.plugin-9.4.1.pom`<br>Gradle plugin marker POM has no detached signature; marker is pinned by SHA-256. |
| `org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:4.4.0` | direct | `org.gradle.kotlin.kotlin-dsl.gradle.plugin-4.4.0.pom`<br>Gradle plugin marker POM has no detached signature; marker is pinned by SHA-256. |
| `org.openjfx.javafxplugin:org.openjfx.javafxplugin.gradle.plugin:0.1.0` | direct | `org.openjfx.javafxplugin.gradle.plugin-0.1.0.pom`<br>Gradle plugin marker POM has no detached signature; marker is pinned by SHA-256. |
| `org.openjfx:javafx-plugin:0.1.0` | transitive | `javafx-plugin-0.1.0-sources.jar`<br>Publisher does not provide a detached PGP signature for this Gradle plugin source artifact; source artifact is pinned by SHA-256.<br><br>`javafx-plugin-0.1.0.jar`<br>Publisher does not provide a detached PGP signature for this Gradle plugin artifact; artifact is pinned by SHA-256.<br><br>`javafx-plugin-0.1.0.module`<br>Publisher does not provide a detached PGP signature for this Gradle module metadata; module metadata is pinned by SHA-256. |

## Full Resolved Dependency Inventory

| Dependency | Scope | Status | Artifacts | Signer key and metadata |
| --- | --- | --- | ---: | --- |
| `ch.qos.logback:logback-classic:1.5.32` | direct | PGP signed | 1 signed / 0 checksum | `60200AC4AE761F1614D6C46766D68DAA073BE985`<br>Ceki Gulcu<br>`ceki@qos.ch`<br>created 2022-08-08 |
| `ch.qos.logback:logback-core:1.5.32` | direct | PGP signed | 1 signed / 0 checksum | `60200AC4AE761F1614D6C46766D68DAA073BE985`<br>Ceki Gulcu<br>`ceki@qos.ch`<br>created 2022-08-08 |
| `com.fasterxml.jackson.core:jackson-annotations:2.21` | direct | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.core:jackson-core:2.21.3` | direct | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.core:jackson-databind:2.21.3` | direct | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.3` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.21.3` | direct | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.3` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-base:2.21.3` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13<br><br>`8A10792983023D5D14C93B488D7F1BEC1E2ECAE7`<br>created 2020-08-01 |
| `com.fasterxml.jackson.jakarta.rs:jackson-jakarta-rs-json-provider:2.21.3` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13<br><br>`8A10792983023D5D14C93B488D7F1BEC1E2ECAE7`<br>created 2020-08-01 |
| `com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations:2.21.3` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.jackson:jackson-bom:2.21.3` | transitive | PGP signed | 0 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.fasterxml.woodstox:woodstox-core:7.1.1` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13 |
| `com.github.jai-imageio:jai-imageio-core:1.4.0` | transitive | PGP signed | 2 signed / 0 checksum | `0A60B3F1FCB211175300EC206E50BB68CC1699A6`<br>Stian Soiland-Reyes<br>`s.soilandreyes@uva.nl`<br>created 2002-01-20 |
| `com.google.android:annotations:4.1.1.4` | transitive | PGP signed | 2 signed / 0 checksum | `0F07D1201BDDAB67CFB84EB479752DB6C966F0B8`<br>Rob Manning<br>`robert.m.manning@gmail.com`<br>created 2009-08-28<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.api.grpc:proto-google-common-protos:2.64.1` | transitive | PGP signed | 1 signed / 0 checksum | `47504B76CF89C15C0512D9AFE16AB52D79FD224F`<br>created 2013-04-30<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.code.findbugs:jsr305:3.0.2` | direct | PGP signed | 2 signed / 0 checksum | `7616EB882DAF57A11477AAF559A252FB1199D873`<br>Tagir Valeev<br>`lany@ngs.ru`<br>created 2015-03-12<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.code.gson:gson:2.13.2` | transitive | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`C7BE5BCC9FEC15518CFDA882B0F3710FA64900E7`<br>created 2016-06-10 |
| `com.google.code.gson:gson:2.14.0` | direct | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`C7BE5BCC9FEC15518CFDA882B0F3710FA64900E7`<br>created 2016-06-10 |
| `com.google.errorprone:error_prone_annotations:2.47.0` | transitive | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`EE0CA873074092F806F59B65D364ABAA39A47320`<br>created 2022-01-28 |
| `com.google.errorprone:error_prone_annotations:2.49.0` | transitive | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`EE0CA873074092F806F59B65D364ABAA39A47320`<br>created 2022-01-28 |
| `com.google.gradle:osdetector-gradle-plugin:1.7.3` | transitive | PGP signed | 2 signed / 0 checksum | `1DBB44E80F61493D6369B5FB95C15058A5EDA4F1`<br>Eric Anderson (Maven Central)<br>`ejona@google.com`<br>created 2015-03-17<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.guava:failureaccess:1.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.guava:guava:33.6.0-jre` | direct | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` | transitive | PGP signed | 2 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.j2objc:j2objc-annotations:3.1` | transitive | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`C3259D20DAEC4ACE6D57CC83340B090F727518D8`<br>Thomas Ball<br>`tball@google.com`<br>created 2025-08-13 |
| `com.google.protobuf:protobuf-gradle-plugin:0.9.4` | direct | PGP signed | 2 signed / 0 checksum | `1A55F091AD28C07F831FA44D7905DE25C78AD456`<br>created 2023-07-10<br><br>`B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.protobuf:protobuf-java-util:3.25.8` | transitive | PGP signed | 1 signed / 0 checksum | `1A55F091AD28C07F831FA44D7905DE25C78AD456`<br>created 2023-07-10<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.protobuf:protobuf-java:4.28.2` | direct | PGP signed | 2 signed / 0 checksum | `1A55F091AD28C07F831FA44D7905DE25C78AD456`<br>created 2023-07-10<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.protobuf:protoc:4.28.2` | direct | PGP signed | 5 signed / 0 checksum | `1A55F091AD28C07F831FA44D7905DE25C78AD456`<br>created 2023-07-10<br><br>`BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16 |
| `com.google.zxing:core:3.5.4` | direct | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`CE3285F320685193D11FEA01F6CE9695C9318406`<br>Sean Owen (ZXing)<br>`srowen@gmail.com`<br>created 2013-07-05 |
| `com.google.zxing:javase:3.5.4` | direct | PGP signed | 1 signed / 0 checksum | `BDB5FA4FE719D787FB3D3197F6D4A1D411E9D1AE`<br>Christopher Povirk<br>`cpovirk@google.com`<br>created 2012-02-16<br><br>`CE3285F320685193D11FEA01F6CE9695C9318406`<br>Sean Owen (ZXing)<br>`srowen@gmail.com`<br>created 2013-07-05 |
| `com.googlecode.libphonenumber:libphonenumber:9.0.30` | direct | PGP signed | 3 signed / 0 checksum | `4759F2DC7BBF3AEC20B12D894DE0E3360046F74C`<br>rohini nidhi<br>`rnidhi@google.com`<br>created 2024-07-10<br><br>`76DF4612A2C7D5821832A8A2665487B2DBDA9781`<br>Kavitha Keshava<br>`kkeshava@google.com`<br>created 2025-09-25 |
| `com.gradleup.shadow:com.gradleup.shadow.gradle.plugin:9.4.1` | direct | checksum fallback | 0 signed / 1 checksum | - |
| `com.gradleup.shadow:shadow-gradle-plugin:9.4.1` | transitive | PGP signed | 1 signed / 0 checksum | `4857D1CE04E78FAB2A172E8F39B48E1BADDB933F`<br>GradleUp (GradleUp sonatype key)<br>`martin@mbonnin.net`<br>created 2021-09-13 |
| `com.sun.istack:istack-commons-runtime:4.1.2` | transitive | PGP signed | 2 signed / 0 checksum | `04543577D6A9CC626239C50C7ECBD740FF06AEB5`<br>created 2018-09-11 |
| `com.typesafe:config:1.4.8` | direct | PGP signed | 1 signed / 0 checksum | `DFD60A08EC2B0CECED24A01F36A22209606109DC`<br>Secure Bot<br>`lightbend-tools-secure@lightbend.com`<br>created 2025-07-10 |
| `commons-codec:commons-codec:1.21.0` | transitive | PGP signed | 1 signed / 0 checksum | `2DB4F1EF0FA761ECC4EA935C86FDC7E2A11262CB`<br>Gary David Gregory (Code signing key)<br>`ggregory@apache.org`<br>created 2011-04-12<br><br>`F4DD59C90148BDC52BEB90A4530AA5F25C25011F`<br>created 2025-10-27 |
| `commons-codec:commons-codec:1.22.0` | direct | PGP signed | 1 signed / 0 checksum | `2DB4F1EF0FA761ECC4EA935C86FDC7E2A11262CB`<br>Gary David Gregory (Code signing key)<br>`ggregory@apache.org`<br>created 2011-04-12<br><br>`F4DD59C90148BDC52BEB90A4530AA5F25C25011F`<br>created 2025-10-27 |
| `commons-io:commons-io:2.21.0` | transitive | PGP signed | 1 signed / 0 checksum | `2DB4F1EF0FA761ECC4EA935C86FDC7E2A11262CB`<br>Gary David Gregory (Code signing key)<br>`ggregory@apache.org`<br>created 2011-04-12 |
| `de.jensd:fontawesomefx-commons:9.1.2` | direct | PGP signed | 2 signed / 0 checksum | `E6A6566630749F995E4703542C3097C79005FF30`<br>Jens Deters<br>`mail@jensd.de`<br>created 2017-03-27 |
| `de.jensd:fontawesomefx-materialdesignfont:2.0.26-9.1.2` | direct | PGP signed | 2 signed / 0 checksum | `E6A6566630749F995E4703542C3097C79005FF30`<br>Jens Deters<br>`mail@jensd.de`<br>created 2017-03-27 |
| `de.jensd:fontawesomefx:8.0.0` | direct | PGP signed | 2 signed / 0 checksum | `D98E48D9AB0116441BBC16CD0F64F95CEF5F0629`<br>Jens Deters<br>`mail@jensd.de`<br>created 2013-07-29<br><br>`E6A6566630749F995E4703542C3097C79005FF30`<br>Jens Deters<br>`mail@jensd.de`<br>created 2017-03-27 |
| `dev.failsafe:failsafe:3.3.2` | direct | PGP signed | 3 signed / 0 checksum | `C0ED5428ABA8C44ED059C5D408A167AD13E23BEF`<br>Jonathan Halterman<br>`jhalterman@gmail.com`<br>created 2022-02-15<br><br>`CDE577234159E222BC56E50F9C61220417AAA02E`<br>Jonathan Halterman<br>`jhalterman@gmail.com`<br>created 2023-03-20 |
| `io.github.classgraph:classgraph:4.8.184` | transitive | PGP signed | 1 signed / 0 checksum | `1F47744C9B6E14F2049C2857F1F111AF65925306`<br>Luke Hutchison<br>`luke.hutch@gmail.com`<br>created 2015-01-14 |
| `io.grpc:grpc-api:1.81.0` | transitive | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-context:1.81.0` | transitive | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-core:1.81.0` | transitive | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-inprocess:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-netty-shaded:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-protobuf-lite:1.81.0` | transitive | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-protobuf:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-services:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-stub:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-testing:1.81.0` | direct | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:grpc-util:1.81.0` | transitive | PGP signed | 1 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.grpc:protoc-gen-grpc-java:1.81.0` | direct | PGP signed | 5 signed / 0 checksum | `B02335AA54CCF21E52BBF9ABD9C565AA72BA2FDD`<br>created 2018-04-27 |
| `io.perfmark:perfmark-api:0.27.0` | transitive | PGP signed | 1 signed / 0 checksum | `C6F7D1C804C821F49AF3BFC13AD93C3C677A106E`<br>Carl Mastrangelo<br>`carl@carlmastrangelo.com`<br>created 2019-05-31 |
| `io.swagger.core.v3:swagger-annotations-jakarta:2.2.50` | transitive | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `io.swagger.core.v3:swagger-annotations:2.2.50` | direct | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `io.swagger.core.v3:swagger-core-jakarta:2.2.50` | transitive | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `io.swagger.core.v3:swagger-integration-jakarta:2.2.50` | transitive | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `io.swagger.core.v3:swagger-jaxrs2-jakarta:2.2.50` | direct | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `io.swagger.core.v3:swagger-models-jakarta:2.2.50` | transitive | PGP signed | 1 signed / 0 checksum | `3E61D8C230332482009D7F0EDB901B24CAD38BC4`<br>swaggerapi<br>`devops@smartbear.com`<br>created 2020-07-21 |
| `jakarta.activation:jakarta.activation-api:2.1.4` | transitive | PGP signed | 1 signed / 0 checksum | `6DD3B8C64EF75253BEB2C53AD908A43FB7EC07AC`<br>Eclipse Project for JAF<br>`jaf-dev@eclipse.org`<br>created 2018-10-03 |
| `jakarta.annotation:jakarta.annotation-api:3.0.0` | transitive | PGP signed | 1 signed / 0 checksum | `59A8E169739301FD48139CA00E325BECB6962A24`<br>created 2018-10-03 |
| `jakarta.inject:jakarta.inject-api:2.0.1` | transitive | PGP signed | 2 signed / 0 checksum | `4021EEEAFF5DE8404DCD0A270AA3E5C3D232E79B`<br>created 2019-07-24 |
| `jakarta.servlet:jakarta.servlet-api:6.1.0` | direct | PGP signed | 3 signed / 0 checksum | `7464550A61C90BA385FC97A76D9567281201E5E3`<br>created 2018-09-29 |
| `jakarta.validation:jakarta.validation-api:3.1.0` | transitive | PGP signed | 3 signed / 0 checksum | `21D09437979F5953FFEEA59C2753A900612D3FF1`<br>created 2023-10-02 |
| `jakarta.websocket:jakarta.websocket-api:2.2.0` | direct | PGP signed | 2 signed / 0 checksum | `F674EBA7B6EC777BDB58942DE0E92C40A43A012A`<br>created 2018-10-03 |
| `jakarta.ws.rs:jakarta.ws.rs-api:4.0.0` | transitive | PGP signed | 1 signed / 0 checksum | `FFD433E89FCB22C79A8DD011B4BF94F677CAA76F`<br>Eclipse JAX-RS Project<br>`jaxrs-dev@eclipse.org`<br>created 2018-08-10 |
| `jakarta.xml.bind:jakarta.xml.bind-api:4.0.5` | transitive | PGP signed | 1 signed / 0 checksum | `FC411CD3CB7DCB0ABC9801058118B3BCDB1A5000`<br>created 2018-10-03 |
| `junit:junit:4.13.2` | transitive | PGP signed | 2 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `kr.motd.maven:os-maven-plugin:1.7.1` | transitive | PGP signed | 2 signed / 0 checksum | `8858D45BE9B276802318155B96FB9DB219F3338D`<br>created 2010-06-14 |
| `net.bytebuddy:byte-buddy-agent:1.17.7` | transitive | PGP signed | 1 signed / 0 checksum | `A7892505CF1A58076453E52D7999BEFBA1039E8B`<br>Rafael Winterhalter<br>`rafael.wth@gmail.com`<br>created 2019-11-07 |
| `net.bytebuddy:byte-buddy:1.18.3` | transitive | PGP signed | 1 signed / 0 checksum | `A7892505CF1A58076453E52D7999BEFBA1039E8B`<br>Rafael Winterhalter<br>`rafael.wth@gmail.com`<br>created 2019-11-07 |
| `net.i2p.client:mstreaming:2.12.0` | transitive | PGP signed | 1 signed / 0 checksum | `70D2060738BEF80523ACAFF7D75C03B39B5E14E1`<br>eyedeekay<br>`eyedeekay@safe-mail.net`<br>created 2017-07-11 |
| `net.i2p.client:streaming:2.12.0` | direct | PGP signed | 1 signed / 0 checksum | `70D2060738BEF80523ACAFF7D75C03B39B5E14E1`<br>eyedeekay<br>`eyedeekay@safe-mail.net`<br>created 2017-07-11 |
| `net.i2p:i2p:2.12.0` | direct | PGP signed | 1 signed / 0 checksum | `70D2060738BEF80523ACAFF7D75C03B39B5E14E1`<br>eyedeekay<br>`eyedeekay@safe-mail.net`<br>created 2017-07-11 |
| `net.i2p:router:2.12.0` | direct | PGP signed | 1 signed / 0 checksum | `70D2060738BEF80523ACAFF7D75C03B39B5E14E1`<br>eyedeekay<br>`eyedeekay@safe-mail.net`<br>created 2017-07-11 |
| `net.java.dev.jna:jna:5.18.1` | direct | PGP signed | 1 signed / 0 checksum | `FA7929F83AD44C4590F6CC6815C71C0A4E0B8EDD`<br>Matthias Bläsing<br>`mblaesing@doppel-helix.eu`<br>created 2016-05-10 |
| `org.apache.ant:ant-launcher:1.10.15` | transitive | PGP signed | 1 signed / 0 checksum | `0A123C1ED3F13A6A0140E166C71FB765CD9DE313`<br>created 2022-12-11 |
| `org.apache.ant:ant:1.10.15` | transitive | PGP signed | 1 signed / 0 checksum | `0A123C1ED3F13A6A0140E166C71FB765CD9DE313`<br>created 2022-12-11 |
| `org.apache.commons:commons-lang3:3.20.0` | direct | PGP signed | 1 signed / 0 checksum | `2DB4F1EF0FA761ECC4EA935C86FDC7E2A11262CB`<br>Gary David Gregory (Code signing key)<br>`ggregory@apache.org`<br>created 2011-04-12<br><br>`F4DD59C90148BDC52BEB90A4530AA5F25C25011F`<br>created 2025-10-27 |
| `org.apache.httpcomponents.client5:httpclient5:5.6.1` | direct | PGP signed | 1 signed / 0 checksum | `0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5`<br>Oleg Kalnichevski<br>`oleg@ural.ru`<br>created 2006-02-07 |
| `org.apache.httpcomponents.core5:httpcore5-h2:5.4` | transitive | PGP signed | 1 signed / 0 checksum | `0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5`<br>Oleg Kalnichevski<br>`oleg@ural.ru`<br>created 2006-02-07 |
| `org.apache.httpcomponents.core5:httpcore5:5.4` | transitive | PGP signed | 1 signed / 0 checksum | `0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5`<br>Oleg Kalnichevski<br>`oleg@ural.ru`<br>created 2006-02-07 |
| `org.apache.httpcomponents.core5:httpcore5:5.4.2` | direct | PGP signed | 1 signed / 0 checksum | `0785B3EFF60B1B1BEA94E0BB7C25280EAE63EBE5`<br>Oleg Kalnichevski<br>`oleg@ural.ru`<br>created 2006-02-07 |
| `org.apache.logging.log4j:log4j-api:2.25.4` | transitive | PGP signed | 1 signed / 0 checksum | `077E8893A6DCC33DD4A4D5B256E73BA9A0B592D0`<br>ASF Logging Services RM<br>`private@logging.apache.org`<br>created 2023-01-10 |
| `org.apache.logging.log4j:log4j-core:2.25.4` | direct | PGP signed | 1 signed / 0 checksum | `077E8893A6DCC33DD4A4D5B256E73BA9A0B592D0`<br>ASF Logging Services RM<br>`private@logging.apache.org`<br>created 2023-01-10 |
| `org.apache.maven:maven-api-annotations:4.0.0-rc-5` | transitive | PGP signed | 1 signed / 0 checksum | `0181A4828FA27B6BE6F1F5A68611CD28F472E006`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18<br><br>`073F7A9345756F3B40CDB99E6C70A3B7599C5736`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18 |
| `org.apache.maven:maven-api-xml:4.0.0-rc-5` | transitive | PGP signed | 1 signed / 0 checksum | `0181A4828FA27B6BE6F1F5A68611CD28F472E006`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18<br><br>`073F7A9345756F3B40CDB99E6C70A3B7599C5736`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18 |
| `org.apache.maven:maven-xml:4.0.0-rc-5` | transitive | PGP signed | 1 signed / 0 checksum | `0181A4828FA27B6BE6F1F5A68611CD28F472E006`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18<br><br>`073F7A9345756F3B40CDB99E6C70A3B7599C5736`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18 |
| `org.apache.tomcat:annotations-api:6.0.53` | direct | PGP signed | 2 signed / 0 checksum | `713DA88BE50911535FE716F5208B0AB1D63011C7`<br>Violeta Georgieva Georgieva (CODE SIGNING KEY)<br>`violetagg@apache.org`<br>created 2013-09-19 |
| `org.apiguardian:apiguardian-api:1.1.2` | transitive | PGP signed | 2 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.assertj:assertj-core:3.27.7` | direct | PGP signed | 1 signed / 0 checksum | `BE685132AFD2740D9095F9040CC0B712FEE75827`<br>created 2020-10-12 |
| `org.bouncycastle:bcpg-jdk18on:1.84` | direct | PGP signed | 1 signed / 0 checksum | `7B121B76A7ED6CE6E60AD51784E913A8E3A748C0`<br>The Legion of the Bouncy Castle Inc. (Maven Repository Artifact Signer)<br>`bcmavensync@bouncycastle.org`<br>created 2023-06-07 |
| `org.bouncycastle:bcpkix-jdk18on:1.84` | direct | PGP signed | 1 signed / 0 checksum | `7B121B76A7ED6CE6E60AD51784E913A8E3A748C0`<br>The Legion of the Bouncy Castle Inc. (Maven Repository Artifact Signer)<br>`bcmavensync@bouncycastle.org`<br>created 2023-06-07 |
| `org.bouncycastle:bcprov-jdk18on:1.84` | direct | PGP signed | 1 signed / 0 checksum | `7B121B76A7ED6CE6E60AD51784E913A8E3A748C0`<br>The Legion of the Bouncy Castle Inc. (Maven Repository Artifact Signer)<br>`bcmavensync@bouncycastle.org`<br>created 2023-06-07 |
| `org.bouncycastle:bcutil-jdk18on:1.84` | transitive | PGP signed | 1 signed / 0 checksum | `7B121B76A7ED6CE6E60AD51784E913A8E3A748C0`<br>The Legion of the Bouncy Castle Inc. (Maven Repository Artifact Signer)<br>`bcmavensync@bouncycastle.org`<br>created 2023-06-07 |
| `org.bytedeco.gradle-javacpp-platform:org.bytedeco.gradle-javacpp-platform.gradle.plugin:1.5.10` | direct | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:gradle-javacpp:1.5.10` | transitive | PGP signed | 2 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:javacpp-platform:1.5.13` | transitive | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:javacpp:1.5.10` | transitive | PGP signed | 3 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:javacpp:1.5.13` | transitive | PGP signed | 2 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:javacv-platform:1.5.13` | direct | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:javacv:1.5.13` | transitive | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:openblas-platform:0.3.31-1.5.13` | transitive | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:openblas:0.3.31-1.5.13` | transitive | PGP signed | 2 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:opencv-platform:4.13.0-1.5.13` | transitive | PGP signed | 1 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.bytedeco:opencv:4.13.0-1.5.13` | transitive | PGP signed | 2 signed / 0 checksum | `18DF0EB86C2DB754FB4F00C3EFE7D4CEC12C4DDD`<br>Bytedeco Release<br>`contact@bytedeco.org`<br>created 2018-03-26 |
| `org.codehaus.mojo:animal-sniffer-annotations:1.27` | transitive | PGP signed | 3 signed / 0 checksum | `32118CF76C9EC5D918E54967CA80D1F0EB6CA4BA`<br>created 2020-05-09<br><br>`84789D24DF77A32433CE1F079EB80E92EB2135B1`<br>Slawomir Jaranowski<br>`s.jaranowski@gmail.com`<br>created 2021-12-22 |
| `org.codehaus.plexus:plexus-utils:4.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `0181A4828FA27B6BE6F1F5A68611CD28F472E006`<br>Guillaume Nodet<br>`gnodet@gmail.com`<br>created 2006-04-18<br><br>`84789D24DF77A32433CE1F079EB80E92EB2135B1`<br>Slawomir Jaranowski<br>`s.jaranowski@gmail.com`<br>created 2021-12-22 |
| `org.codehaus.plexus:plexus-xml:4.1.1` | transitive | PGP signed | 3 signed / 0 checksum | `32118CF76C9EC5D918E54967CA80D1F0EB6CA4BA`<br>created 2020-05-09<br><br>`84789D24DF77A32433CE1F079EB80E92EB2135B1`<br>Slawomir Jaranowski<br>`s.jaranowski@gmail.com`<br>created 2021-12-22 |
| `org.codehaus.woodstox:stax2-api:4.2.2` | transitive | PGP signed | 1 signed / 0 checksum | `28118C070CB22A0175A2E8D43D12CA2AC19F3181`<br>Tatu Saloranta (cowtowncoder)<br>`tatu.saloranta@iki.fi`<br>created 2022-08-13<br><br>`6214760097DC5CFAD0175AC2C9FBAA83A8753994`<br>created 2016-07-26 |
| `org.eclipse.angus:angus-activation:2.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `BCA1F17506AF088F3A964A9C0459A2B383ED8C11`<br>created 2021-07-30 |
| `org.fxmisc.easybind:easybind:1.0.3` | direct | PGP signed | 2 signed / 0 checksum | `5E13B520F0BCBB359C6ED54A4A23006A8E767508`<br>Tomas Mikula<br>`tomas.mikula@gmail.com`<br>created 2014-03-05<br><br>`E22F7D9D0428D1C4B38DCA7BF6665E960DA9AF2A`<br>Jurgen Doll<br>`admedfx@gmail.com`<br>created 2018-11-07 |
| `org.fxmisc.flowless:flowless:0.7.4` | transitive | PGP signed | 1 signed / 0 checksum | `E22F7D9D0428D1C4B38DCA7BF6665E960DA9AF2A`<br>Jurgen Doll<br>`admedfx@gmail.com`<br>created 2018-11-07 |
| `org.fxmisc.richtext:richtextfx:0.11.7` | direct | PGP signed | 1 signed / 0 checksum | `E22F7D9D0428D1C4B38DCA7BF6665E960DA9AF2A`<br>Jurgen Doll<br>`admedfx@gmail.com`<br>created 2018-11-07 |
| `org.fxmisc.undo:undofx:2.1.1` | transitive | PGP signed | 2 signed / 0 checksum | `E22F7D9D0428D1C4B38DCA7BF6665E960DA9AF2A`<br>Jurgen Doll<br>`admedfx@gmail.com`<br>created 2018-11-07 |
| `org.fxmisc.wellbehaved:wellbehavedfx:0.3.3` | transitive | PGP signed | 2 signed / 0 checksum | `A5AA37CCE9E6C2D4A0C1496AF5B2A0D5457ADC48`<br>Jordan Martinez (FXMisc 2018)<br>`jordanalex.martinez@gmail.com`<br>created 2018-01-12<br><br>`E22F7D9D0428D1C4B38DCA7BF6665E960DA9AF2A`<br>Jurgen Doll<br>`admedfx@gmail.com`<br>created 2018-11-07 |
| `org.glassfish.grizzly:grizzly-core:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-framework:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http-ajp:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http-server-core:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http-server-multipart:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http-server:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http2:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-http:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-npn-api:2.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-portunif:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-websockets-server:5.0.1` | direct | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.grizzly:grizzly-websockets:5.0.1` | transitive | PGP signed | 1 signed / 0 checksum | `3AC8633E97ACA1E8F2A7ED239052174975143979`<br>created 2018-04-05<br><br>`D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.hk2.external:aopalliance-repackaged:4.0.0-M3` | transitive | PGP signed | 1 signed / 0 checksum | `D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.hk2:hk2-api:4.0.0-M3` | transitive | PGP signed | 1 signed / 0 checksum | `D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.hk2:hk2-locator:4.0.0-M3` | transitive | PGP signed | 1 signed / 0 checksum | `D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.hk2:hk2-utils:4.0.0-M3` | transitive | PGP signed | 1 signed / 0 checksum | `D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.hk2:osgi-resource-locator:3.0.0` | transitive | PGP signed | 1 signed / 0 checksum | `D4A77129F00F736293BE5A51AFC18A2271EDDFE1`<br>created 2018-10-02 |
| `org.glassfish.jaxb:jaxb-core:4.0.8` | transitive | PGP signed | 1 signed / 0 checksum | `04543577D6A9CC626239C50C7ECBD740FF06AEB5`<br>created 2018-09-11 |
| `org.glassfish.jaxb:jaxb-runtime:4.0.8` | direct | PGP signed | 1 signed / 0 checksum | `04543577D6A9CC626239C50C7ECBD740FF06AEB5`<br>created 2018-09-11 |
| `org.glassfish.jaxb:txw2:4.0.8` | transitive | PGP signed | 1 signed / 0 checksum | `04543577D6A9CC626239C50C7ECBD740FF06AEB5`<br>created 2018-09-11 |
| `org.glassfish.jersey.containers:jersey-container-grizzly2-http:4.0.2` | direct | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.containers:jersey-container-jdk-http:4.0.2` | direct | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.core:jersey-client:4.0.2` | transitive | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.core:jersey-common:4.0.2` | transitive | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.core:jersey-server:4.0.2` | direct | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.ext:jersey-entity-filtering:4.0.2` | transitive | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.inject:jersey-hk2:4.0.2` | direct | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.glassfish.jersey.media:jersey-media-json-jackson:4.0.2` | direct | PGP signed | 1 signed / 0 checksum | `0B743A794876D3C78AB542A118D239B1CBCD2236`<br>created 2018-10-03 |
| `org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:4.4.0` | direct | checksum fallback | 0 signed / 1 checksum | - |
| `org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.4.0` | transitive | PGP signed | 2 signed / 0 checksum | `1BD97A6A154E7810EE0BC832E2F38302C8075E3D`<br>Gradle Inc.<br>`maven-publishing@gradle.com`<br>created 2022-12-29 |
| `org.hamcrest:hamcrest-core:1.3` | transitive | PGP signed | 2 signed / 0 checksum | `4DB1A49729B053CAF015CEE9A6ADFC93EF34893E`<br>created 2012-05-14 |
| `org.hamcrest:hamcrest:2.1` | transitive | PGP signed | 1 signed / 0 checksum | `4DB1A49729B053CAF015CEE9A6ADFC93EF34893E`<br>created 2012-05-14<br><br>`E3A9F95079E84CE201F7CF60BEDE11EAF1164480`<br>created 2018-11-21 |
| `org.hamcrest:hamcrest:3.0` | direct | PGP signed | 1 signed / 0 checksum | `4DB1A49729B053CAF015CEE9A6ADFC93EF34893E`<br>created 2012-05-14<br><br>`E3A9F95079E84CE201F7CF60BEDE11EAF1164480`<br>created 2018-11-21 |
| `org.javassist:javassist:3.30.2-GA` | transitive | PGP signed | 2 signed / 0 checksum | `E5C3B1929191DF06136CCB2B164779204E106A76`<br>Shigeru Chiba (Javassist Developer)<br>`chiba@javassist.org`<br>created 2021-05-07 |
| `org.jcommander:jcommander:1.85` | transitive | PGP signed | 3 signed / 0 checksum | `1D85469D8559C2E1DF5F925131D2D79DF7E85DD3`<br>Markus KARG<br>`markus@headcrashing.eu`<br>created 2023-08-04 |
| `org.jdom:jdom2:2.0.6.1` | transitive | PGP signed | 2 signed / 0 checksum | `78DF98EF7F95578FD545B9A159A7C2A1BD98C013`<br>Jason Hunter<br>`jhunter@servlets.com`<br>created 2021-12-06 |
| `org.jetbrains.intellij.deps:trove4j:1.0.20200330` | transitive | PGP signed | 2 signed / 0 checksum | `8756C4F765C9AC3CB6B85D62379CE192D401AB61`<br>Bintray (by JFrog)<br>`bintray@bintray.com`<br>created 2015-02-17 |
| `org.jetbrains.kotlin:kotlin-android-extensions:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-assignment-compiler-plugin-embeddable:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-assignment:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-build-common:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-build-tools-api:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-build-tools-impl:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-compiler-runner:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-daemon-client:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-daemon-embeddable:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-annotations:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.9.23` | transitive | PGP signed | 3 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-idea-proto:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-idea:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-model:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-gradle-plugins-bom:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-klib-commonizer-api:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-klib-commonizer-embeddable:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20` | transitive | PGP signed | 1 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-native-utils:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-project-model:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-reflect:1.6.10` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-reflect:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-sam-with-receiver-compiler-plugin-embeddable:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-sam-with-receiver:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-script-runtime:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-scripting-common:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.23` | direct | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-stdlib:1.9.23` | direct | PGP signed | 4 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-tooling-core:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-util-io:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlin:kotlin-util-klib:1.9.23` | transitive | PGP signed | 2 signed / 0 checksum | `6F538074CCEBF35F28AF9B066A0975F8B1127B83`<br>Kotlin Release<br>`kt-a@jetbrains.com`<br>created 2019-06-01 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0` | transitive | PGP signed | 2 signed / 0 checksum | `E7DC75FC24FB3C8DFE8086AD3D5839A2262CBBFB`<br>created 2020-12-09 |
| `org.jetbrains:annotations:13.0` | direct | PGP signed | 2 signed / 0 checksum | `2E3A1AFFE42B5F53AF19F780BCF4173966770193`<br>IntelliJ IDEA Sign Key<br>`intellij-idea-sign-key-noreply@jetbrains.com`<br>created 2013-12-10 |
| `org.jetbrains:annotations:26.1.0` | direct | PGP signed | 1 signed / 0 checksum | `33FD4BFD33554634053D73C0C2148900BCD3C2AF`<br>Download<br>`download@jetbrains.com`<br>created 2021-03-15 |
| `org.jspecify:jspecify:1.0.0` | transitive | PGP signed | 3 signed / 0 checksum | `41CD49B4EF5876F9E9F691DABAC30622339994C4`<br>Chris Povirk<br>`cpovirk@google.com`<br>created 2017-06-22 |
| `org.junit.jupiter:junit-jupiter-api:6.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.jupiter:junit-jupiter-engine:6.0.3` | direct | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.jupiter:junit-jupiter-params:6.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.jupiter:junit-jupiter:6.0.3` | direct | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.platform:junit-platform-commons:6.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.platform:junit-platform-engine:6.0.3` | transitive | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit.platform:junit-platform-launcher:6.0.3` | direct | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.junit:junit-bom:6.0.3` | transitive | PGP signed | 0 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.mockito:mockito-core:5.23.0` | direct | PGP signed | 1 signed / 0 checksum | `147B691A19097624902F4EA9689CBE64F4BC997F`<br>Szczepan Faber<br>`szczepiq@gmail.com`<br>created 2021-02-14 |
| `org.mockito:mockito-junit-jupiter:5.23.0` | direct | PGP signed | 1 signed / 0 checksum | `147B691A19097624902F4EA9689CBE64F4BC997F`<br>Szczepan Faber<br>`szczepiq@gmail.com`<br>created 2021-02-14 |
| `org.objenesis:objenesis:3.3` | transitive | PGP signed | 2 signed / 0 checksum | `E85AED155021AF8A6C6B7A4A7C7D8456294423BA`<br>created 2010-05-13 |
| `org.openjfx.javafxplugin:org.openjfx.javafxplugin.gradle.plugin:0.1.0` | direct | checksum fallback | 0 signed / 1 checksum | - |
| `org.openjfx:javafx-base:21.0.11` | transitive | PGP signed | 1 signed / 0 checksum | `81CCDC71C7D61C179B27002D6A9FBE152D4C64D1`<br>created 2013-08-12 |
| `org.openjfx:javafx-controls:21.0.11` | direct | PGP signed | 1 signed / 0 checksum | `81CCDC71C7D61C179B27002D6A9FBE152D4C64D1`<br>created 2013-08-12 |
| `org.openjfx:javafx-graphics:21.0.11` | transitive | PGP signed | 1 signed / 0 checksum | `81CCDC71C7D61C179B27002D6A9FBE152D4C64D1`<br>created 2013-08-12 |
| `org.openjfx:javafx-media:21.0.11` | direct | PGP signed | 1 signed / 0 checksum | `81CCDC71C7D61C179B27002D6A9FBE152D4C64D1`<br>created 2013-08-12 |
| `org.openjfx:javafx-plugin:0.1.0` | transitive | checksum fallback | 0 signed / 3 checksum | - |
| `org.opentest4j:opentest4j:1.3.0` | transitive | PGP signed | 1 signed / 0 checksum | `FF6E2C001948C5F2F38B0CC385911F425EC61B51`<br>Marc Philipp<br>`marc@junit.org`<br>created 2018-04-08 |
| `org.osgi:org.osgi.core:6.0.0` | transitive | PGP signed | 1 signed / 0 checksum | `1AF010D408C1852AB6EFD4B651CDB4A9DAAC1CBC`<br>Raymond Auge (I love OSS!)<br>`rotty3000@gmail.com`<br>created 2014-07-09 |
| `org.projectlombok:lombok:1.18.46` | direct | PGP signed | 1 signed / 0 checksum | `D421D1DF4570BFB13E485D0BF95ADD0A28D2F139`<br>created 2011-03-08 |
| `org.reactfx:reactfx:2.0-M5` | transitive | PGP signed | 2 signed / 0 checksum | `5E13B520F0BCBB359C6ED54A4A23006A8E767508`<br>Tomas Mikula<br>`tomas.mikula@gmail.com`<br>created 2014-03-05 |
| `org.slf4j:slf4j-api:2.0.18` | direct | PGP signed | 1 signed / 0 checksum | `475F3B8E59E6E63AA78067482C7B12F2A511E325`<br>created 2012-04-26<br><br>`60200AC4AE761F1614D6C46766D68DAA073BE985`<br>Ceki Gulcu<br>`ceki@qos.ch`<br>created 2022-08-08 |
| `org.testfx:testfx-core:4.0.18` | transitive | PGP signed | 3 signed / 0 checksum | `8756C4F765C9AC3CB6B85D62379CE192D401AB61`<br>Bintray (by JFrog)<br>`bintray@bintray.com`<br>created 2015-02-17 |
| `org.testfx:testfx-junit5:4.0.18` | direct | PGP signed | 3 signed / 0 checksum | `8756C4F765C9AC3CB6B85D62379CE192D401AB61`<br>Bintray (by JFrog)<br>`bintray@bintray.com`<br>created 2015-02-17 |
| `org.tukaani:xz:1.12` | direct | PGP signed | 1 signed / 0 checksum | `3690C240CE51B4670D30AD1C38EE757D69184620`<br>Lasse Collin<br>`lasse.collin@tukaani.org`<br>created 2010-10-24 |
| `org.vafer:jdependency:2.15` | transitive | PGP signed | 1 signed / 0 checksum | `79156E0351AF8604DE9B186B09A79E1E15A04694`<br>Torsten Curdt (Code Signing Key)<br>`tcurdt@vafer.org`<br>created 2021-02-10 |
| `org.yaml:snakeyaml:2.6` | transitive | PGP signed | 1 signed / 0 checksum | `2FC53E6B1F681184F4CCD637F5C81DE10A0B8ECC`<br>created 2024-05-11 |
