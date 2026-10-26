Packaged licence inventory
=========================

PonHub evolves SE-EDU AddressBook-Level3. The unchanged repository LICENSE
is packaged at META-INF/licenses/ponhub/LICENSE (MIT, including the original
2016 Software Engineering Education - FOSS Resources notice).
The existing audio/LICENSE.txt remains in place.

Jackson runtime resolution:
  jackson-annotations 2.7.0
  jackson-core 2.7.4
  jackson-databind 2.7.4
  jackson-datatype-jsr310 2.7.4
Each resolved component's META-INF/LICENSE and META-INF/NOTICE, where present,
are copied under a component/version-specific directory in META-INF/licenses.
Apache-2.0.txt supplies the complete Apache License 2.0 terms, obtained from
https://www.apache.org/licenses/LICENSE-2.0.txt
The original merged META-INF notices are also retained.

JavaFX runtime resolution: base, controls, fxml and graphics 17.0.7, for
Windows, macOS and Linux. No media, swing or web module is distributed.
javafx-17.0.7/ contains the four modules' unmodified SDK legal directories:
GPLv2 with Classpath Exception, ADDITIONAL_LICENSE_INFO, ASSEMBLY_EXCEPTION,
and graphics' JPEG and Mesa third-party notices. These are the complete legal
files for these modules in OpenJFX source tag 17.0.7+2; the notices apply across
the three platform classifiers. The Maven JARs omit these standalone files.

Provenance:
https://download2.gluonhq.com/openjfx/17.0.7/openjfx-17.0.7_windows-x64_bin-sdk.zip
SDK SHA-256: f6f7d2dca1fed6f990624fbdbbd3003be15e48f8bc1741ee1b01f8a147f6b51b
Version-matched source, including native code and build scripts:
https://github.com/openjdk/jfx17u/tree/17.0.7%2B2
https://github.com/openjdk/jfx17u/archive/refs/tags/17.0.7%2B2.zip
Source commit: 4e6636f843fd43ce59e4c8088537649a702cfd91
SDK legal bytes were compared with that source's root licence files and
modules/javafx.graphics/src/main/legal/{jpeg_fx.md,mesa3d.md}.

When upgrading a runtime dependency, refresh this inventory and its upstream
notices together. Run ./gradlew check: verifyPackagedLicenses checks actual
JAR entry uniqueness and exact source bytes, including component notices.
Release preparation must retain corresponding-source availability alongside
the downloadable binary and identify the final accepted JAR separately.

Prepared by Ernest Chua with OpenAI Codex assistance for issue #451.
