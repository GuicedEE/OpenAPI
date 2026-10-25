# Official Swagger Core 3 migration

Validated on 2026-10-25 with Java 25 and Maven 4.0.0-rc-6.
Upstream discussion: https://github.com/swagger-api/swagger-core/issues/4991.
Validation used the existing workspace dependency versions. Unrelated pending
Versioner updates were preserved and excluded from the migration commits.

## Dependency and integration changes

The GuicedEE Swagger BOM now manages `io.swagger.core.v3` instead of the
`io.github.vpelikh` fork. All six entries use the official timestamped snapshot
`3.0.0-20261023.100341-12`. The final 3.0.0 release was not available on Maven
Central when this migration was validated. The snapshot repository is declared
on the Swagger wrapper build; its published POM has no upstream Swagger runtime
dependency because the implementation is shaded into the named wrapper.

The REST artifact is `swagger-rest`, annotations are `swagger-annotations`, and
the servlet initializer is `swagger-rest-servlet-initializer`. REST implementation
packages changed from `io.swagger.v3.jaxrs2` to `io.swagger.v3.rest`. The reader,
scanner exclusions, logging category, module exports/opens, and extension SPI now
use these names. GuicedEE's own module names and Swagger annotations remain the
same.

The wrapper explicitly publishes its ClassGraph dependency and embeds SnakeYAML
Engine, without the obsolete classic SnakeYAML JAR. The CXF OpenAPI wrapper
relocates its compiled Swagger REST references and context builder to the new
names and excludes upstream Swagger 2 artifacts.

## Validation

| Check | Result |
| --- | --- |
| Versioner and Swagger BOM install | Passed |
| Swagger wrapper install, including sources and Javadocs | Passed |
| CXF OpenAPI wrapper install, including sources and Javadocs | Passed |
| REST suite | 74 tests, zero failures/errors/skips |
| OpenAPI suite | 7 tests, zero failures/errors/skips |
| Swagger UI suite | 1 test, zero failures/errors/skips |
| Official Swagger class provenance | All 332 upstream classes byte-identical |
| Effective Swagger BOM | All six official artifacts resolve to the pinned build |
| Wrapper runtime artifact | Build, local repository and Maven project cache hashes match |
| CXF classpath smoke | Extension service discovery, feature construction and official REST context passed |

OpenAPI coverage includes the official REST reader, JSON/YAML round trips with
typed map schemas and `$ref` values, remote inline-map schema merging, and matching
JSON/YAML documents served over HTTP. Wrapper builds use
`-Dmaven.jar.forceCreation=true` to prevent stale shaded content.

The local evidence directory is `DevSuite/target/swagger-official-migration/`.
It contains Maven logs, the effective wrapper POM, artifact comparison script and
results, and the CXF smoke program. Representative commands, run from DevSuite:

```powershell
mvn.cmd -B -N -f GuicedEE/bom/Versioner/pom.xml install
mvn.cmd -B -N -f GuicedEE/bom/SwaggerBOM/pom.xml install
mvn.cmd -B -f GuicedEE/services/Libraries/swagger/pom.xml install '-Dmaven.jar.forceCreation=true'
mvn.cmd -B -f GuicedEE/services/Apache/CXF/apache-cxf-rest-openapi/pom.xml install '-Dmaven.jar.forceCreation=true'
mvn.cmd -B -f GuicedEE/rest/pom.xml install '-Dmaven.javadoc.skip=true' '-Dmaven.jar.forceCreation=true'
mvn.cmd -B -f GuicedEE/openapi/pom.xml install '-Dmaven.jar.forceCreation=true'
mvn.cmd -B -f GuicedEE/swagger-ui/pom.xml test
```

## Existing limitations observed

- The injector suite reports 6 errors with shared test JVMs. With
  `-DreuseForks=false`, 19 of 21 tests pass; `IGuiceConfigTest` still reports a
  stopped context and `PsvmTest` times out. Both errors reproduce in a detached
  checkout of the pre-migration injector commit `70d3766`. The injector package
  was installed with tests skipped after recording these failures; its suite is
  not reported as passing.
- The standalone CXF module-path smoke cannot initialize its boot layer because
  the existing `org.apache.cxf.rest` descriptor provides
  `jakarta.servlet.ServletContainerInitializer` without reading its exporting
  module. Its descriptor was not changed here. The successful CXF smoke used the
  classpath with the normal supplemental Guava, failureaccess and Jackson XML
  binding dependencies. This does not establish full CXF JPMS or Servlet parity.
- Registry merging already prefixes component names without rewriting their
  `$ref` targets. An exploratory fixture demonstrated that `Pong` becomes
  `remote_Pong` while its response reference remains `#/components/schemas/Pong`.
  The merge implementation is unchanged. Codec reference preservation and
  inline-map merging are covered separately; reference rewriting needs its own
  fix and regression coverage.

These results are local build and test evidence, not hosted CI, deployment, or
production acceptance. When the final upstream release is published, replace
the snapshot pin, remove the snapshot repository, and rerun the same checks.
