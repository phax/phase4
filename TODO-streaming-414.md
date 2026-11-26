# Streaming of incoming payloads (#414) - plan

Status: implemented (uncommitted) in ph-commons 12.9.0-SNAPSHOT, peppol-commons 13.4.0-SNAPSHOT and
phase4 5.0.0-SNAPSHOT. Covers C1 of #318.

Deviations from the proposal below:

- Peppol and HR eDelivery have separate types `Phase4PeppolIncomingSBD` and
  `Phase4HREDeliveryIncomingSBD` (the servlet modules are independent, and phase4-lib does not depend
  on ph-sbdh); the shared part is `AS4IncomingJAXBDocument` in phase4-lib.
- The handler types are classes, not interfaces.
- `StAXHelper.readCurrentElementAsDOM` declares the namespaces needed by element and attribute names
  of the subtree; prefixes used only in values (e.g. `xsi:type`) cannot be detected via StAX.
- Additional attachments in Peppol (only possible without rejection on non-compliance) are no longer
  read at all.

Measured: 256 MB SBD, GZIP compressed to 49 MB, `-Xmx64m`: pass 1 with all Peppol checks 1.1 s,
pass 2 (handler stream) 0.3 s, 23 MB heap used afterwards. The previous approach runs out of memory
with `-Xmx1g`.

Still open: phoss-ap adaptation (after a phase4 release), the AES-GCM and the signature digest heap
peaks (see the end of this file).

## Decisions taken

- Several passes over the payload (option A). The incoming attachment is above 64 KB on disk
  (`WSS4JAttachment.MAX_IN_MEMORY_BYTES`), decrypted attachments always are, and
  `getSourceStream ()` decompresses again with the limits on every call. Each pass needs constant
  heap and costs only CPU.
- The API is prepared independent of WSS-735 (workaround: Bouncy Castle as preferred AES-GCM
  provider).
- Peppol, HR eDelivery and DBNAlliance are done together.
- The handler object offers the DOM on demand.
- Versions: ph-commons 12.9.0, peppol-commons 13.4.0 (see below).

## Core idea: a "shallow" StAX reader

Instead of new validation hooks, the existing JAXB based code is fed by a filtering
`XMLStreamReader`:

- The underlying StAX parser reads the **whole** document, so every well-formedness error still
  fails the read. This replaces the separate well-formedness pass.
- Below selected container elements, the filter reports each direct child element as an **empty**
  element with its attributes and namespaces, and drops all its descendants and all text.
  - SBD: the business message (the child of `StandardBusinessDocument` that is not the
    `StandardBusinessDocumentHeader`).
  - XHE: the children and the text of every `Payloads/Payload/PayloadContent`.
- JAXB unmarshals the filtered events as today: `IJAXBReader.read (XMLStreamReader)` exists in
  ph-jaxb. The XSDs accept the result (SBD business message is `xs:any lax`, XHE `PayloadContent` is
  `mixed` with `xs:any lax`).
- Result: a `StandardBusinessDocument` / `XHE10XHEType` with full headers and a tiny business
  message element. `validateData` and all `isValid*` hooks taking an `Element` work unchanged,
  because they only use the namespace URI and the local name.

Memory: the parser must not be coalescing, otherwise a big text node (e.g. Base64 in
`BinaryContent` or in an XHE `PayloadContent`) is collected in memory before it is dropped.
`XMLFactory.DEFAULT_STAX_COALESCING` is `true` (confirmed by the spike: `OutOfMemoryError`).

## Layer 1: ph-commons (12.8.1-SNAPSHOT, becomes 12.9.0)

StAX is part of the JDK (`java.xml`), so no new dependency.

ph-xml, package `com.helger.xml.stax`:

- A factory method for a non-coalescing, otherwise equally hardened `XMLInputFactory`, next to
  `XMLFactory.createDefaultXMLInputFactory ()`.
- The shallow filter (a `StreamReaderDelegate`), configured with a predicate that decides per
  start element whether it is reported shallow. Name to be decided.
- A helper to build a DOM of the current element subtree from an `XMLStreamReader`, declaring all
  namespaces in scope (for the DBNA payload DOM on request).
- A `Reader` over the text content of the current element, chunk by chunk (needed for textual and
  Base64 DBNA payloads; Base64 decoding then via the existing ph-commons Base64 stream classes).

ph-jaxb: no change (verified in the spike).

## Layer 2: peppol-commons (13.3.1-SNAPSHOT, becomes 13.4.0)

- `edelivery-sbdh` `AbstractSBDHDataReader`: `parseSBDShallow (InputStream)` next to `parseSBD`,
  using the shallow filter and `SBDMarshaller`.
- `PeppolSBDHDataReader`, `HREDeliverySBDHDataReader`: `extractDataShallow (InputStream)` (name to
  be decided) = `parseSBDShallow` + the existing `extractData (StandardBusinessDocument)`.
- `DBNAllianceXHEDataReader`: the same with `XHE10Marshaller`. The payload classification
  (element, text, Base64) keeps working: an XML payload still has a (shallow) first element, a
  text or binary payload has none.
- Data objects: after a streaming read only namespace URI and local name of the business message
  (Peppol, HR) / XML payload root (DBNA) are stored, never the shallow element.
- Bug fixes (done, uncommitted, 13.3.1-SNAPSHOT): `PeppolSBDHData.isNonXMLSBDHPayload` NPE for
  `null`; `DBNAllianceXHEDataReader.extractDataUnchecked` stored the To party as From party.
- Existing DOM based `extractData` methods stay, because they are still needed (e.g. phoss-ap reads
  stored documents and uses `getBusinessMessageNoClone ()` for MLS and `BinaryContent`).

## Layer 3: phase4 v5

- Pass 1 in the three `*ServletMessageProcessorSPI`: `extractDataShallow (aAttachment.getSourceStream ())`
  replaces reading into `byte []` plus `SBDMarshaller` / `XHE10Marshaller`. The error mapping
  (`EBMS:0004`, `EBMS:0009` etc.) stays as today.
- New handler object in phase4-lib (name to be decided), backed by the attachment's
  `IHasInputStream`:
  - `getInputStream ()` - a new decompressed stream per call, byte identical to today's
    `aSBDBytes`.
  - Lazy, cached `StandardBusinessDocument` (Peppol, HR) / `XHE10XHEType` (DBNA), parsed from a new
    stream on first request.
  - DBNA only: per payload access without re-serialisation - see the API proposal.
- New signatures of `IPhase4PeppolIncomingSBDHandlerSPI.handleIncomingSBD`,
  `IPhase4HREDeliveryIncomingSBDHandlerSPI.handleIncomingSBD` and
  `IPhase4DBNAllianceIncomingXHEHandlerSPI.handleIncomingXHE`: the handler object replaces
  `byte [] aSBDBytes` plus `StandardBusinessDocument aSBD` / `XHE10XHEType aXHE`.
- Update `StoringPeppolIncomingSBDHandlerSPI`, `StoringDBNAllianceIncomingXHEHandlerSPI` and all
  test implementations.
- Tests: the existing ones, plus a large payload (e.g. 256 MB) received with a small heap.
- Wiki: News, Migrations (new handler signatures), Profile pages if they show handler code.
- Afterwards: adapt phoss-ap `Phase4InboundMessageProcessorSPI` (only uses `aSBDBytes` today).

## Spike results (2026-11-26)

Test files: 256 MB SBD with one Base64 `BinaryContent` text node, 256 MB SBD with 3.8 million small
elements, a malformed one (wrong end tag near the end), one without `HeaderVersion`. Run with
`-Xmx32m`, JDK 21 parser (`com.sun.xml.internal.stream.XMLInputFactoryImpl`) and as cross-check
Woodstox 7.3.0 (phase4 does not depend on Woodstox).

1. Non-coalescing parsers deliver the huge text node in chunks: JDK at most 16384 characters,
   Woodstox at most 4000. With `IS_COALESCING = true` both the plain scan and the shallow read
   fail with `OutOfMemoryError` at `-Xmx64m` - so non-coalescing is mandatory.
2. `SBDMarshaller.read (XMLStreamReader)` validates against the XSD like the other paths (same
   `Unmarshaller` from `GenericJAXBMarshaller._createUnmarshaller`): the missing `HeaderVersion`
   is rejected with `cvc-complex-type.2.4.a`, the malformed document with an
   `XMLStreamException` from the parser.
3. Shallow read of the 256 MB documents: about 4 to 6 MB heap used, 0.3 s (Base64) and 0.7 s
   (small elements). The business message comes back as an `Element` with correct namespace URI,
   local name and attributes (`BinaryContent/@mimeType`) and no children.
4. XHE (ph-xhe sample files): `XHE10Marshaller.read (XMLStreamReader)` with the shallow filter
   validates; an XML payload gives an empty root element, a text payload empty content. A payload
   without `PayloadContent` (external reference) exists and must be handled.

Spike code: scratchpad only, not part of any repo.

## Decisions on the open points

1. Data object after a shallow read: store namespace URI and local name only, never the shallow
   element.
2. No re-serialisation of DBNA payloads. The handler object gets the whole XHE stream (byte
   identical) and is extended for the payloads instead - see proposal below.
3. `getAllBytes ()` convenience: yes, it is a one-liner on top of `getInputStream ()`.
4. Names: proposal below.
5. Versions (decided): ph-commons **12.9.0** (new public API), peppol-commons **13.4.0**
   (additive only), phase4 5.0.0.

## Name and API proposal (to be confirmed)

ph-xml, `com.helger.xml.stax`:

- `XMLFactory.createNonCoalescingXMLInputFactory ()` - like `createDefaultXMLInputFactory ()` but
  with `IS_COALESCING = false`.
- `ShallowXMLStreamReader extends StreamReaderDelegate`, configured with an
  `IShallowElementSelector`:
  - `boolean isShallowElement (List <QName> aAncestors, QName aElement)` - the element is reported
    with attributes and namespaces, its content is skipped.
  - `default boolean isSkipText (List <QName> aAncestors)` - text directly inside the current
    element is skipped (XHE `PayloadContent`).
- `StAXHelper` (or similar) with the text-content `Reader` of the current element.

peppol-commons:

- `AbstractSBDHData`: `getBusinessMessageNamespaceURI ()`, `getBusinessMessageLocalName ()`,
  filled from the DOM if present, otherwise from the streaming read.
- `DBNAlliancePayload`: the same for the XML payload root element, plus an explicit content kind
  (XML, text, binary, none), because without content `hasPayloadContentXML ()` etc. cannot decide.
- Readers: `extractDataStreaming (InputStream)` - header data, business message only as namespace
  URI and local name.

phase4:

- phase4-lib: `IAS4IncomingDocument` with `getInputStream ()` (new stream per call) and
  `getAllBytes ()`.
- Peppol and HR eDelivery: `IPhase4IncomingSBD extends IAS4IncomingDocument` with the lazy
  `getStandardBusinessDocument ()`.
- DBNAlliance: `IPhase4DBNAllianceIncomingXHE extends IAS4IncomingDocument` with the lazy
  `getXHE ()` and per payload (by index):
  - text payload: `openPayloadTextReader (int)` - the characters as they are in the document;
  - binary payload: `openPayloadBinaryStream (int)` - Base64 decoded while reading;
  - XML payload: `openPayloadXMLStreamReader (int)` - an `XMLStreamReader` positioned on the
    payload root element, so the handler consumes the events itself; and
    `getPayloadElement (int)` - a DOM of just that payload, built on request.
  Each call reads the XHE again from the start, so nothing is re-serialised by phase4.

## Not part of this plan (still #414)

- AES-GCM heap peak during decryption (WSS-735 or the Bouncy Castle workaround).
- Heap use of the signature digest.
