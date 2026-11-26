# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

phase4 is an embeddable, lightweight Java library to send and receive AS4 (Applicability Statement 4) messages. It implements the EBMS 3.0 / AS4 messaging standard with support for multiple network profiles (Peppol, BDEW, CEF, ENTSOG, etc.). Version 5.0.0-SNAPSHOT, Java 21+.

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
- **phase4-bom** - Bill of Materials with all phase4 library artefacts (no test or webapp modules). Add new library modules there too.
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

### Incoming Payload Streaming
The Peppol, HR eDelivery and DBNAlliance message processor SPIs never read the payload into memory (#414). Pass 1 reads the SBDH/XHE with `extractDataStreaming (InputStream)` from peppol-commons (a `ShallowXMLStreamReader` from ph-xml skips the business message / payload contents, but the whole document is parsed); the stream is wrapped in `AS4ReadErrorRecordingInputStream`, so that a read failure is still reported as `EBMS_DECOMPRESSION_FAILURE`. The handler SPIs then get `Phase4PeppolIncomingSBD` / `Phase4HREDeliveryIncomingSBD` / `Phase4DBNAllianceIncomingXHE` (based on `AS4IncomingJAXBDocument` in phase4-lib), which re-open the attachment's source stream provider on every read - incoming attachments are always readable multiple times (temporary file or in-memory copy). Keep it that way: never add a `byte []` copy of the payload in these SPIs. The data objects passed to the handlers (`PeppolSBDHData` etc.) contain only the business message name, not the business message.

### HTTP Retries
Outgoing HTTP retries happen only on the phase4 level, in `BasicHttpPoster.sendGenericMessageWithRetries`, configured by `HttpRetrySettings`. `HttpRetryClassifier` decides per exception and HTTP status code whether a retry is feasible (`EHttpRetryClassification`). A response handler can veto the retry of the current response via `HttpRetryVeto.vetoRetry ()` (used for received SOAP Faults with a permanent disposition; transient SOAP Faults are retried according to the HTTP status code). The automatic retries of Apache HttpClient are disabled via `HttpClientSettings.setDisableAutomaticRetries (true)` (ph-web) in all phase4 `*HttpClientSettings` classes and in `BasicHttpPoster.createDefaultHttpClientFactory ()` - keep it that way for new profiles, otherwise HttpClient silently retries 429/503 in addition. The only exception is `Phase4PeppolCRLHttpClientSettings`, which re-enables them, because CRL downloads are not retried on the phase4 level.

SMP lookups (`AS4EndpointDetailProvider*` in phase4-dynamic-discovery) happen once, outside the HTTP retry loop. Their retry feasibility (`Phase4SMPException.isRetryFeasible ()`, mapped to `EAS4UserMessageSendResult.TRANSPORT_ERROR` / `TRANSPORT_ERROR_NO_RETRY`) is decided by `SMPRetryClassifier`, which reuses `HttpRetryClassifier`. The HTTP level retries of the SMP lookup itself are configured in peppol-commons (`SMPHttpClientSettings`).

### Peppol Reporting
peppol-reporting v5: an `IPeppolReportingBackend` is created via `PeppolReportingBackend.createBackend (IConfig)` and must be closed. Never use the deprecated `withBackendDo` / `getBackendService`. `Phase4PeppolSender` takes a long-living backend via the builder method `peppolReportingBackend (...)` (it never closes it); without one, `createAndStorePeppolReportingItemAfterSending` creates and closes a temporary backend per item and logs a warning. The item ID is the AS4 message ID, truncated to `PeppolReportingItemWithID.MAX_LEN_ITEM_ID`. `phase4-peppol-server-webapp` holds its single backend in `AppReportingBackend` (created in `Phase4PeppolWebAppListener`, closed on context destruction); reading goes via `IPeppolReportingItemReader`.

### JAXB Code Generation
`phase4-lib` generates Java classes from EBMS/AS4 XSD schemas during `generate-sources` phase. Bindings in `src/main/jaxb/bindings.xjb`, schemas in `src/main/resources/external/schemas/`.

## Dependencies

Core libraries from the ph-* ecosystem: `ph-commons`, `ph-web`, `ph-oton` (Photon web framework), `ph-xsds-*`. Crypto via WSS4J 4.0.3 and XMLSec 4.0.4. Peppol modules use `peppol-commons` and `peppol-reporting`.
