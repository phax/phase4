# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

phase4 is an embeddable, lightweight Java library to send and receive AS4 (Applicability Statement 4) messages. It implements the EBMS 3.0 / AS4 messaging standard with support for multiple network profiles (Peppol, BDEW, CEF, ENTSOG, etc.). Version 4.4.2-SNAPSHOT, Java 17+.

## Build Commands

```bash
# Full build (compile + test)
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build a single module
mvn clean install -pl phase4-lib -DskipTests

# Build a module and its dependencies
mvn clean install -pl phase4-peppol-client -am -DskipTests

# Run a single test class
mvn test -pl phase4-lib -Dtest=com.helger.phase4.model.pmode.PModeTest

# Run a single test method
mvn test -pl phase4-lib -Dtest=com.helger.phase4.model.pmode.PModeTest#testBasic

# JAXB code generation (phase4-lib only, runs during generate-sources)
mvn generate-sources -pl phase4-lib
```

Tests use JUnit 4. Some integration tests require an embedded Jetty server (ph-oton-jetty).

## Module Architecture

### Core
- **phase4-lib** - Core AS4 library: SOAP/EBMS marshalling, crypto (WSS4J), PMode model, incoming request handling, outgoing client, servlet integration. Everything else depends on this.

### Profile Modules (one per AS4 network)
- **phase4-profile-{name}** - Profile definition, PMode factory, validation. Registers via SPI (`IAS4ProfileRegistrarSPI`).
- **phase4-{name}-client** - Sender utilities for the profile (e.g., `Phase4PeppolSender`).
- **phase4-{name}-servlet** - Incoming message handler SPI implementation for the profile.

Profiles: `peppol`, `bdew`, `cef`, `edelivery2`, `dbnalliance`, `entsog`, `euctp`, `hredelivery`, `eudamed` (client-only).

### Webapps
- **phase4-test-webapp**, **phase4-server-webapp**, **phase4-peppol-webapp** - Deployable WARs for testing/production.

### Other
- **phase4-test** - Shared test utilities and resources.
- **phase4-dynamic-discovery** - SMP-based dynamic discovery support.

## Key Source Paths (within phase4-lib)

| Path (under `src/main/java/com/helger/phase4/`) | Purpose |
|---|---|
| `incoming/AS4RequestHandler.java` | Core incoming message processing (2200 lines) |
| `incoming/AS4IncomingHandler.java` | Incoming message parsing and security validation |
| `sender/AbstractAS4UserMessageBuilder.java` | Base class for building outgoing user messages |
| `client/AbstractAS4Client.java` | Base client with crypto, retry, HTTP configuration |
| `servlet/AS4Servlet.java` | POST endpoint, delegates to `AS4XServletHandler` |
| `model/pmode/PMode.java` | PMode configuration model (parties, MEP, legs) |
| `mgr/MetaAS4Manager.java` | Global singleton managing all sub-managers |
| `profile/IAS4ProfileRegistrarSPI.java` | SPI interface for profile auto-discovery |
| `crypto/` | WSS4J integration, signing/encryption params, key management |

## Key Architectural Patterns

### Profile SPI System
Profiles register via Java ServiceLoader (`META-INF/services/com.helger.phase4.profile.IAS4ProfileRegistrarSPI`). Adding a profile JAR to the classpath auto-registers it. Each profile provides: ID, validator, PMode template factory, and PMode ID provider.

### Manager Factory
`MetaAS4Manager` uses `IAS4ManagerFactory` to create sub-managers (PMode, MPC, duplicate detection, timestamps). Two implementations: `AS4ManagerFactoryInMemory` (development) and `AS4ManagerFactoryPersistingFileSystem` (production, XML files).

### Message Flow
- **Sending**: Profile-specific sender (e.g., `Phase4PeppolSender`) -> `AbstractAS4UserMessageBuilder` -> `AbstractAS4Client` -> HTTP POST with SOAP+attachments
- **Receiving**: `AS4Servlet` -> `AS4XServletHandler` -> `AS4RequestHandler` (parses SOAP, validates security, invokes `IAS4IncomingMessageProcessorSPI` implementations)

### JAXB Code Generation
`phase4-lib` generates Java classes from EBMS/AS4 XSD schemas during `generate-sources` phase. Bindings in `src/main/jaxb/bindings.xjb`, schemas in `src/main/resources/external/schemas/`.

## Dependencies

Core libraries from the ph-* ecosystem: `ph-commons`, `ph-web`, `ph-oton` (Photon web framework), `ph-xsds-*`. Crypto via WSS4J 4.0.1 and XMLSec 4.0.4. Peppol modules use `peppol-commons` and `peppol-reporting`.
