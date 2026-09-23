# AS4 relevance of the Spanish B2B e-invoicing mandate

Status: 2026-09-23.

**Dear reader:** this file answers one question - *what does the Spanish mandatory B2B
e-invoicing system mean for an AS4 implementation such as phase4?* Read section 1 first: the
mandate contains exactly one sentence about AS4. Everything else follows from that. Section 2
describes the transport of the **public hub (SPFE)**, which is *not* AS4 but is genuine
ebMS 3.0 and therefore the part phase4 could plausibly speak. Section 3 describes the
**platform-to-platform leg**, which *is* the AS4 obligation but has no profile at all.
Sections 5 and 6 are the open questions and the practical position.

History of this file:

* **2026-09-10** - first version, written after the AEAT webinar "Actualizacion sobre la SPFE"
  of the same day, from notes taken on the live slides. No documents were published at that
  time, so all SPFE transport content was flagged as unverified.
* **2026-09-23** - AEAT published the three decks of that session on a new page of the developer
  portal. They are stored in `aeat/2026-09-10-actualizacion-spfe/`. Everything below has been
  re-checked against those PDFs; the XML examples in the decks are screenshots, so they were
  read from the rendered slides. Points that changed in this pass are marked
  **(corrected 2026-09-23)**.

Still unpublished as of 2026-09-23, and therefore still unknown:

* the final Ministerial Order - **not in the BOE**; the draft is unchanged in substance and is
  still scheduled to enter into force on 2026-10-01;
* the WSDLs, the XSDs of the Spanish extensions, the Schematron files, the service catalogue,
  the authentication/representation document, the limits and the error list. AEAT repeats that
  all of these appear on the developer portal *before* the test environment is deployed
  (reference date: October 2026).

The decks are draft material presented by AEAT, not normative text. Nothing below is a
specification; it is the best reading of what AEAT has shown so far.

## 1. The complete normative AS4 text

There is exactly **one** sentence about AS4 in the entire Spanish framework.
Real Decreto 238/2026, Art. 13.1 ("Requirements to operate as a private e-invoice exchange platform"):

> b) Utilizar protocolos seguros para la transmision de la informacion que cumplan con las
> especificaciones AS2 o AS4.

("Use secure protocols for the transmission of the information that comply with the AS2 or AS4 specifications.")

That is all. The Royal Decree does not name:

* an AS4 version or conformance profile (OASIS ebMS 3.0 AS4 Profile v1.0? eDelivery AS4? Peppol AS4?)
* a usage profile or PMode template
* MEPs, reliability, receipts, retries, duplicate detection
* WS-Security algorithms or certificate requirements at the transport layer
* addressing, party identifiers or any discovery mechanism
* payload naming, compression or MIME conventions
* any conformance test or accreditation

The word "AS4" does **not** appear at all in the AEAT developer seminar of 2026-05-19 -
neither in the technical deck (`Seminario_19_05_2026_DIT.pdf`), nor in the tax administration
deck, nor in the 150+ entry FAQ. Verified by full-text search over all four documents.

In the published deck of 2026-09-10 AS4 is mentioned exactly once, as a single bullet
("Compatible AS4 / Peppol") justifying AEAT's choice of an ebXML envelope for the SPFE's own
services - `DIT_FE_Seminario_10_septiembre.pdf`, slide 8; see section 2. That bullet says
nothing about the platform-to-platform leg, and it is the only occurrence of the word Peppol in
that deck as well.

## 2. The SPFE speaks ebMS 3.0

Correction to the picture drawn by the May 2026 seminar, which mentioned only "servicios web"
and WSDLs. The SPFE web services use an **ebXML envelope**, and AEAT devoted a slide
("Por que ebXML?", DIT slide 8) to justifying that choice:

* international standard - ebXML is an OASIS standard like UBL, so integrators implement
  nothing SPFE-exclusive, and AEAT avoids defining "a basic envelope of its own";
* **"Compatible AS4 / Peppol"** - a *minimalist version without cryptographic signature*,
  improving interoperability in the context of ViDA;
* one single envelope for everything - submission, statuses, query and download, with the
  invoices travelling back inside the response message;
* header with parameters - an index at message and invoice level, "useful in the medium term,
  e.g. invoice paid or C-VIES invoice";
* independent invoices - no links between invoices in the same envelope, so an XML syntax error
  in one does not affect the rest and no batch pre-processing is required;
* size control over the invoices submitted.

The published examples are **ebMS 3.0, not a loose interpretation**. Submission request
(DIT slides 21 and 22, reproduced with the two slides joined):

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:eb="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/">
  <soap:Header>
    <eb:Messaging>
      <eb:UserMessage>
        <eb:MessageInfo>
          <eb:Timestamp>2026-09-09T09:00:00.000Z</eb:Timestamp>
          <eb:MessageId>uuid-invoices-123</eb:MessageId>
        </eb:MessageInfo>
        <eb:PartyInfo>
          <eb:From>                              <!-- NIF of the sender's certificate -->
            <eb:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920"
              >99999910G</eb:PartyId>
            <eb:Role>ebms:initiator</eb:Role>
          </eb:From>
          <eb:To>                                <!-- NIF of the AEAT (SPFE) -->
            <eb:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920"
              >Q2826000H</eb:PartyId>
            <eb:Role>ebms:responder</eb:Role>
          </eb:To>
        </eb:PartyInfo>
        <eb:CollaborationInfo>
          <eb:Service>urn:cen.eu:en16931:submitinvoice</eb:Service>
          <eb:Action>SubmitInvoice</eb:Action>
          <eb:ConversationId>c73b7fdc-1ab0-4b1e-b295-ac9e4a35d764</eb:ConversationId>
        </eb:CollaborationInfo>
        <eb:PayloadInfo>
          <eb:PartInfo href="cid:factura1">
            <eb:PartProperties>
              <eb:Property name="MimeType">application/xml</eb:Property>
            </eb:PartProperties>
          </eb:PartInfo>
          <eb:PartInfo href="cid:factura2">
            <eb:PartProperties>
              <eb:Property name="MimeType">application/xml</eb:Property>
            </eb:PartProperties>
          </eb:PartInfo>
        </eb:PayloadInfo>
      </eb:UserMessage>
    </eb:Messaging>
  </soap:Header>
  <soap:Body/>                                   <!-- "Sin contenido" -->
</soap:Envelope>
```

Confirmed from the examples:

* the **ebMS 3.0 core namespace** `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/` -
  the exact header model phase4 already implements. The cancel and status examples declare it in
  an `xsi:schemaLocation` on `eb:Messaging` pointing at a local relative schema copy (the file
  name is cut off in the slide screenshots).
* **Party identifiers use the eBCore scheme**
  `urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920` - ICD 9920, Spanish VAT. The sender's
  PartyId is the NIF in its certificate; the SPFE's is AEAT's own NIF `Q2826000H`.
* Roles are `ebms:initiator` / `ebms:responder`.
* **The SOAP Body is empty** - every payload is a MIME part referenced by
  `PartInfo/@href="cid:..."`, with the `MimeType` part property, exactly as AS4 does it.
* The response reuses the same `UserMessage` structure with `RefToMessageId` pointing at the
  request's `MessageId`, and **swaps the two `PartyId` values** (slide 25 marks the swap with an
  arrow, and slide 20 states it in words).
* **There is no WS-Security header at all** in any example, consistent with the
  "minimalist version without cryptographic signature".

**The SOAP version is not consistent across the published examples (new 2026-09-23).** The
submission example uses SOAP 1.2 (`http://www.w3.org/2003/05/soap-envelope`, slide 21), while the
cancellation (slide 28) and the status-change (slide 38) examples use **SOAP 1.1**
(`http://schemas.xmlsoap.org/soap/envelope/`). AS4 and eBMS 3.0 are defined over SOAP 1.2. This
needs clarification from AEAT - see the open questions.

Service / Action values (note the **CEN namespace, not a Spanish one**)
**(corrected 2026-09-23** - the earlier table was taken from the response example only and had
the status service wrong):

| Operation | Service | Action | Source |
|---|---|---|---|
| Submit invoices - request | `urn:cen.eu:en16931:submitinvoice` | `SubmitInvoice` | DIT slide 22 |
| Submit invoices - response | `urn:cen.eu:en16931:invoice` | `SUBMITINVOICE` | DIT slide 25 |
| Cancel invoice | `urn:cen.eu:en16931:cancelinvoice` | `CANCELINVOICE` | DIT slide 28 |
| Query by ID | `urn:cen.eu:en16931:queryinvoice` | `QUERYBYID` | DIT slide 30 |
| Query by filter | `urn:cen.eu:en16931:queryinvoices` | `QUERYBYFILTER` | DIT slide 31 |
| Status events - recipient | `urn:cen.eu:en16931:submitaction` | `AccountingCustomerPartyAction` | DIT slide 38 |
| Status events - issuer | `urn:cen.eu:en16931:submitaction` | `AccountingSupplierPartyAction` (*not shown - inferred*) | - |

The submission **request and response do not use the same Service/Action pair**
(`submitinvoice` / `SubmitInvoice` going out, `invoice` / `SUBMITINVOICE` coming back), and the
casing convention differs between the two. Either the deck is inconsistent or the values really
are asymmetric; the WSDLs will tell.

For the status service the `Action` says whether the batch carries issuer-side or recipient-side
statuses; the slide annotates the envelope as "ebXML no relevante" because the substance sits in
the `ApplicationResponse` payload.

Query-by-filter parameters ride in `eb:MessageProperties`: `Role`
(`AccountingSupplierParty` | `AccountingCustomerParty`), `InvoiceCompanyID`,
`InvoiceRegistrationName`, `RegistrationStartDate` and `RegistrationEndDate` (the slide notes
that both dates must fall inside the same period and fiscal year, and flags the time zone).
Query-by-ID uses the same `Role` property plus an `ApplicationResponse` payload.

Services and message shapes **(completed 2026-09-23** - the endpoint names for query and
download were unknown before):

| Operation | Endpoint | Request | Response |
|---|---|---|---|
| Submit invoices | `ws/SendInvoiceSOAP` | ebXML + N parts (`Invoice`) | ebXML + 1 part (`DocumentStatus`) |
| Cancel invoice | `ws/CancelInvoiceSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |
| Query by ID | `ws/GetInvoicesSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus` with N `AdditionalDocumentResponse`) |
| Query by filter | `ws/GetRegisteredInvoiceSOAP` | ebXML only | ebXML + 1 part (`DocumentStatus` with N `AdditionalDocumentResponse`) |
| Download by ID | `ws/DownloadInvoicesByIDSOAP` | ebXML + 1 part (`ApplicationResponse` with N `DocumentReference`, ID only) | ebXML + **N parts (`Invoice`)** |
| Download by localizador | `ws/DownloadInvoicesByLOCSOAP` | ebXML + 1 part (`ApplicationResponse` with N `DocumentReference`, localizador) | ebXML + **N parts (`Invoice`)** |
| Recipient status events | `ws/CustomerInvoiceEventsSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |
| Issuer status events | `ws/SupplierInvoiceEventsSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |

Note the endpoint naming: `GetInvoicesSOAP` is the query **by ID** and
`GetRegisteredInvoiceSOAP` the query **by filter**, i.e. the plural/singular is the opposite of
what the semantics suggest.

### 2.1 Results and error reporting

* The submission response carries a UBL `DocumentStatus` whose `cbc:ID` is the **CSV**
  (Codigo Seguro de Verificacion) of the acuse de recibo, plus `cbc:IssueDate`.
* Batch-level outcome in `cac:DocumentResponse/cac:Response/cbc:ResponseCode`: `200` all correct,
  **`206` "Facturas parcialmente correctas"**, `400` all failed **(answered 2026-09-23** - the
  earlier note guessed 206 because the slide read from the live session showed "200" twice).
* Per invoice, one `cac:DocumentReference` carrying the three key identifiers (`cbc:ID`,
  `cbc:IssueDate`, `IssuerParty/.../cbc:CompanyID`), the localizador in
  `cbc:ReferencedDocumentInternalAddress`, and `cbc:DocumentStatusCode` - `200` on success, or an
  SPFE error code such as `SPFE-GEN-45` with a `cbc:DocumentDescription`. The slide states that
  no description is sent for code 200; it is only used for errors.
* The **download response reports status per payload inside the ebMS header** (DIT slide 35):
  each `eb:PartInfo` carries `eb:PartProperties` with `statusCode` (`200` / `400`),
  `statusDescription` and `id`. That is a non-standard use of `PartProperties` and is the one
  place where the SPFE puts business results into the ebMS layer rather than into UBL.
* Query responses paginate through `DocumentStatus/cac:DocumentResponse/cac:Response/cac:Status`
  with `cbc:StatusReasonCode`, `cbc:StatusReason` and a `cbc:SequenceID` identifying the last
  invoice returned.
* The localizador is an encrypted string of the form `LOCGENV1:O:<base64>`, where the flag marks
  original or copy. AEAT recommends it over the unique invoice ID for retrieval.

Limits: 1-100 invoices per batch (configurable); 5,120 KB max per invoice, applied per invoice
(configurable); **zero binary attachments** - `cbc:EmbeddedDocumentBinaryObject` and
`cbc:EmbeddedDocument` must be empty, while URL references via
`cac:Attachment/cac:ExternalReference/cbc:URI` are allowed; download capped at 100 invoices;
query capped at 1,000; up to 100 status events per `ApplicationResponse`, with issuer and
recipient actions neither mixed nor duplicated for the same invoice. Private platforms must act
as a "concentrador" and must not operate invoice-by-invoice or customer-by-customer.

### 2.2 Where this diverges from AS4

The envelope is ebMS 3.0, but the exchange is not the AS4 profile:

1. **Synchronous request/response carrying business payloads.** The download service returns
   N `Invoice` payloads in the HTTP response of the same exchange, and every other service
   returns a `DocumentStatus`. The OASIS AS4 profile is One-Way/Push with signal messages
   (Receipt/Error) as the response; a synchronous Two-Way exchange whose reply is a business
   `UserMessage` is an ebMS 3.0 Part 2 feature outside the AS4 profile. phase4's client is
   built around One-Way/Push plus Pull.
2. **No message-level signature and no WS-Security header.** AEAT's own words: "minimalist
   version without cryptographic signature". The AS4 default security policy signs the SOAP
   header and payloads; without it there is no message-level non-repudiation, and AS4's
   signed-Receipt mechanism cannot apply.
3. **No AS4 Receipt.** Acknowledgement is business-level - a `DocumentStatus` payload plus an
   "acuse de recibo" carrying a Codigo Seguro de Verificacion (CSV), not an ebMS Receipt.
4. **Partial-success semantics.** `DocumentStatus` reports per-invoice outcomes across the batch
   (`206` at batch level, per-invoice codes below it), which has no equivalent in AS4's
   per-message all-or-nothing model.
5. **Authentication is transport/business level** - a qualified electronic certificate plus the
   AEAT representation model (nombre propio / apoderamiento / colaborador social), not
   WS-Security tokens. The `From/PartyId` is expected to match the NIF in that certificate.
6. **Roles do not swap in the response.** The response swaps the two `PartyId` values but keeps
   `ebms:initiator` on `From` and `ebms:responder` on `To`, so in the reply AEAT is the
   "initiator". Confirmed from the published slide, i.e. **not** a transcription error
   **(confirmed 2026-09-23)**; whether it is intentional is still unknown, and a strict ebMS
   implementation must tolerate it.
7. **`ConversationId` is inconsistent** across the examples - a UUID in the request messages,
   an empty element in the submit response.
8. **SOAP 1.1 appears in two of the three envelope examples** - see above. AS4 requires SOAP 1.2.
9. **Business status inside `PartProperties`** on download responses, which no AS4 profile defines.

Net effect: phase4 cannot talk to the SPFE out of the box, but this is much closer than the May
material suggested. The ebMS 3.0 marshalling, `PartyInfo` / `CollaborationInfo` / `PayloadInfo`
handling and the MIME multipart payload model in phase4 all apply directly; what is missing is a
synchronous Two-Way exchange and a "no security" profile. Re-assess once AEAT publishes the
WSDLs and message examples.

## 3. Why "their own AS4 profile" is the real risk

RD Arts. 8 and 9 create a **bilateral N x N interconnection mesh**, not a four-corner network:

* Art. 8.1 - a platform operator must interconnect with any other platform when one of its own
  customers asks for it; the interconnection must cover all users of both platforms.
* Art. 9.1 - every incoming interconnection request must be accepted.
* Art. 9.3 - the receiving operator has **max. one month** to make it operational, and must supply
  "all the necessary technical specifications", provide a test bed, and allocate staff.
* Art. 9.4 - parallel requests that cannot be handled simultaneously are served strictly first-come.
* Art. 9.6 - all of this must be **free of charge** for the requesting platform.
* Art. 9.5 - until the interconnection is live, the sender must deposit the invoices in the SPFE,
  and the other platform is obliged to collect them there.
* Art. 8.2 - the SPFE may also be used permanently as the interconnection medium.
* Art. 7.4 - between two private platforms, syntax **and technical specifications are whatever
  sender and receiver agree**; only in the absence of an agreement do the SPFE specifications apply.

Consequence for an AS4 implementation: since no national AS4 usage profile exists and Art. 7.4
delegates the "technical specifications" to bilateral agreement, each platform pair can define
its own PMode, its own security policy and its own payload conventions. The realistic outcome is
**one PMode per counterparty**, not one Spanish profile - unless the industry converges on an
existing profile (see section 6).

Also note that the RD contains **no accreditation, no registry and no directory of private
platforms** (verified by full-text search). There is no SMP/SML equivalent. The definition of a
private platform (Art. 2.e) mentions "direccionamiento" (addressing) as a required capability,
but the mechanism to discover a counterparty's endpoint and certificate is not regulated.

## 4. Payload issues that hit the AS4 layer

* **Four admitted syntaxes** (Art. 7.1): CII, UBL, EDIFACT invoice message, Facturae.
  Every platform must be able to exchange **all** of them and to convert between them while
  preserving authenticity of origin and integrity of content (Art. 7.2).
  EDIFACT is not XML - so an AS4 payload profile cannot assume XML.
* **Peppol BIS** messages are explicitly considered valid between private platforms
  (RD preamble), because they use UBL and conform to EN 16931. This is a statement about
  *syntax*, not about Peppol transport.
* **Mandatory payload signature** (Art. 7.3): every invoice issued via a private platform must
  carry an **advanced electronic signature**, applied by the issuer or by an authorised delegated
  signature (Art. 10.1.a of RD 1619/2012). This is a signature *inside* the payload
  (XAdES for UBL/CII, Facturae's own signature, something else for EDIFACT) and is independent
  of the AS4/WS-Security message signature - both are needed.
  Note the asymmetry: according to the AEAT FAQ, an invoice sent **through the SPFE** does not
  need to be signed; there, integrity and non-repudiation are guaranteed by AEAT's own procedures
  (Art. 11.7). The updated draft Order states it as an option - invoices interconnected through
  the SPFE "podran enviarse con firma electronica" (may be sent with an electronic signature).
* **Status messages travel over the same interconnection** (Art. 8.3): the interconnection must
  carry at least the invoices and the Art. 10.1 statuses (commercial acceptance/rejection plus
  date, full effective payment plus date). On the SPFE these are UBL `ApplicationResponse` /
  `DocumentStatus`, with `cbc:ResponseCode` one of `PAYMENT`, `CANCELPAYMENT`, `REJECTION`,
  `CANCELREJECTION` (recipient) or `SETTLEMENT`, `CANCELSETTLEMENT`, `DEFAULT`, `CANCELDEFAULT`
  (issuer), an `cbc:EffectiveDate` and dates qualified via
  `cac:Status/cac:Condition/cbc:AttributeID` (`DueDate`, `ShipmentDate`,
  `FinancingArrangement`). Payments are reported for the full invoice amount only - no partials.
  Over AS4 there is no defined Service/Action to distinguish an invoice from a status message.
* **Unique invoice code** (Art. 7.5): NIF of the issuer + invoice series/number + issue date.
  This is a natural business-level message identifier, but no mapping to ebMS message properties
  is defined.
* **Faithful copies are flagged in the payload**, not in the envelope: the draft Anexo I adds
  `BT-ES-1` mapped to `/in:Invoice/cbc:CopyIndicator` (`true` = copy, `false` or absent =
  original).
* A **cancellation message** (UBL `ApplicationResponse`) is now part of Anexo I, matching
  Art. 3.5 of the draft Order: an invoice that turns out to be improcedente because the
  underlying operation does not exist can be withdrawn, with traceability kept.
* Platform requirement Art. 13.1.c: capability for eIDAS advanced electronic signature **and seal**.
  Art. 13.1.a: ISO/IEC 27001 (or equivalent). Art. 13.2: data governance may be evidenced via
  UNE 0080 maturity level 2.

## 5. Open questions for an AS4 implementer

### Platform-to-platform leg (the AS2/AS4 obligation)

1. Which AS4 baseline - OASIS AS4 Profile of ebMS 3.0 v1.0 (ebHandler / Light Client),
   eDelivery AS4, or Peppol AS4? The phrasing "AS2 or AS4" reads like a generic reference to
   EDI transport, not a conformance profile.
2. MEP: One-Way/Push only, or is Pull required for platforms without a public endpoint?
3. Are signed AS4 Receipts with non-repudiation information mandatory, and are they the legal
   proof of delivery between platforms? Retry and duplicate-detection windows?
4. Which certificates and trust anchors at the transport layer? The RD only regulates
   certificates for *invoice signing* and for *SPFE access*. Trust list, or purely bilateral trust?
5. How are ebMS PartyIds formed - NIF based? The SPFE leg answers this for itself
   (`urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920`, see section 2); would platforms
   reuse the same eBCore/9920 scheme bilaterally?
6. Which Service/Action values separate invoice, credit note, status message and error? Would
   platforms reuse the SPFE's `urn:cen.eu:en16931:*` naming bilaterally?
7. Payload profile: single vs. multiple payloads, compression, and how the syntax
   (UBL / CII / EDIFACT / Facturae) is signalled.
8. Conformance testing: Art. 9.3 makes every platform run its own test bed for its
   counterparties. There is no national conformance test comparable to CEF eDelivery or the
   Peppol test bed.

### SPFE leg (the ebXML envelope)

9. ~~What exactly is "compatible with AS4/Peppol"?~~ Answered by the published examples: the
   ebMS 3.0 core namespace, eBCore party identifiers, empty SOAP body, payloads as MIME parts -
   the AS4 header model minus security. What remains open is whether AEAT will also accept
   genuinely AS4-conformant messages (signed, with Receipts) from clients that send them.
10. Is the exchange a declared ebMS Two-Way/Sync MEP, and are there any ebMS signal messages
    (Receipt, Error) at all? Every response in the decks is a business `UserMessage`, so
    apparently not - but transport-level error handling is undocumented.
11. Are the response `Role` values (`From`=AEAT as `ebms:initiator`) intentional? Confirmed to
    be what AEAT published, so no longer a suspected reading error - but still not explained.
    Is `ConversationId` mandatory? It is a UUID in the requests and empty in the submit response.
12. ~~What are the exact `DocumentStatus` response codes - is the second "200" actually 206?~~
    Answered: `200` / `206` / `400` at batch level, per-invoice `cbc:DocumentStatusCode` with
    SPFE error codes such as `SPFE-GEN-45`. The full error list is still unpublished.
13. Is message-level signing genuinely absent, or only absent in the first release? If it is
    later added, which policy?
14. How is the 24-hour-outage rule (submission allowed within the following four business days)
    expected to interact with client-side retry behaviour?
15. Will AEAT publish the ebXML envelope as a documented profile, or only as WSDLs plus examples?
16. **Which SOAP version?** The submission example is SOAP 1.2, the cancellation and
    status-change examples are SOAP 1.1. AS4 and ebMS 3.0 require SOAP 1.2.
17. **Why do request and response use different Service/Action values** for submission
    (`submitinvoice`/`SubmitInvoice` vs `invoice`/`SUBMITINVOICE`), and which casing is
    normative?
18. Are the `statusCode` / `statusDescription` / `id` `PartProperties` of the download response
    a fixed vocabulary, and are they expected on other services too?

## 6. Practical position for phase4

* Nothing has to be implemented today for the platform-to-platform leg: RD Art. 13.1.b) is
  satisfied by *any* conformant AS4 implementation, and phase4 is one.
* The SPFE leg is the more interesting one. Its envelope is genuine ebMS 3.0 with eBCore party
  identifiers and CID-referenced MIME payloads - the model phase4 already implements - and AEAT
  explicitly aims at AS4/Peppol compatibility. The divergences in section 2.2 (synchronous
  business responses, no WS-Security at all, no Receipts, partial-success batches, SOAP version
  inconsistency) are what stand in the way. Worth a concrete feasibility check of how far the
  phase4 ebMS layer reaches once AEAT publishes the WSDLs, because the answer looks like
  "most of the way".
* The plausible convergence path for the platform-to-platform leg remains **Peppol** - the RD
  already blesses Peppol BIS as a syntax between private platforms, and Peppol supplies exactly
  the missing pieces (AS4 profile, SMP/SML discovery, identifier schemes, conformance testing).
  If the Spanish market converges there, `phase4-profile-peppol` / `phase4-peppol-client`
  already cover it with no new code.
* A `phase4-profile-spain` module only becomes meaningful once a Spanish AS4 usage profile
  actually exists - from the Ministry, AEAT, or an industry association. As of 2026-09-23
  there is none.
* The useful interim deliverable is documentation on running phase4 with **per-counterparty
  PModes**, which is what Art. 7.4 plus Art. 9 effectively force.

## 7. Sources

All quotes above come from:

* `legal/BOE-A-2026-7295_RD-238-2026.pdf` - Real Decreto 238/2026, de 25 de marzo (BOE 79, 2026-03-31)
* `legal/Proyecto-OM-SPFE_2026-04-16.pdf` - draft Ministerial Order on the SPFE (public consultation)
* `aeat/2026-05-19-seminario-spfe/` - AEAT developer seminar of 2026-05-19 (3 decks + FAQ)
* `aeat/2026-09-10-actualizacion-spfe/` - AEAT seminar of 2026-09-10, published 2026-09:
  * `DIT_FE_Seminario_10_septiembre.pdf` - technical deck, 45 slides, source of all of section 2
  * `Actualizacion_proyecto_OM_Seminario_10-09-2026.pdf` - updated draft Ministerial Order
  * `Novedades_AnexoI_Seminario_10-9-26.pdf` - changes to Anexo I (UBL content)

Slide numbers cited above refer to the PDF page numbers of `DIT_FE_Seminario_10_septiembre.pdf`.
