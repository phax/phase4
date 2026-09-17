# AS4 relevance of the Spanish B2B e-invoicing mandate

Status: 2026-09-10 (updated after the AEAT webinar of the same day).
Based on the documents in this folder plus the slides shown in the AEAT webinar
"Actualizacion sobre la SPFE" of 2026-09-10, including the XML message examples on those
slides. The webinar material is **draft content presented from slides and not verified against
any published text** - everything attributed to it below is flagged as such and must be
re-checked against the final Ministerial Order and the technical documentation once AEAT
publishes it. Values transcribed from slide screenshots may contain reading errors.

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

In the 2026-09-10 webinar AS4 is mentioned exactly once, as a single bullet
("Compatible AS4 / Peppol") justifying AEAT's choice of an ebXML envelope for the SPFE's own
services - see section 2. That bullet says nothing about the platform-to-platform leg, and it is
the only occurrence of the word Peppol in that deck as well.

## 2. The SPFE speaks ebMS 3.0 over SOAP 1.2

Correction to the picture drawn by the May 2026 seminar, which mentioned only "servicios web"
and WSDLs. The slides of the AEAT webinar of 2026-09-10 show the SPFE web services using an
**ebXML envelope**, and AEAT devoted a slide ("Por que ebXML?") to justifying that choice:

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

The slides show real message examples, and they are **ebMS 3.0, not a loose interpretation**:

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
          <eb:Service>urn:cen.eu:en16931:cancelinvoice</eb:Service>
          <eb:Action>CANCELINVOICE</eb:Action>
          <eb:ConversationId>c73b7fdc-1ab0-4b1e-b295-ac9e4a35d764</eb:ConversationId>
        </eb:CollaborationInfo>
        <eb:PayloadInfo>
          <eb:PartInfo href="cid:anulacion.xml">
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

* **SOAP 1.2** (`http://www.w3.org/2003/05/soap-envelope`) plus the **ebMS 3.0 core namespace**
  (`http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/`) - the exact header model
  phase4 already implements.
* **Party identifiers use the eBCore scheme**
  `urn:oasis:names:tc:ebcore:partyid-type:iso6523:9920` - ICD 9920, Spanish VAT. The sender's
  PartyId is the NIF in its certificate; the SPFE's is AEAT's own NIF `Q2826000H`.
* Roles are `ebms:initiator` / `ebms:responder`.
* **The SOAP Body is empty** - every payload is a MIME part referenced by
  `PartInfo/@href="cid:..."`, with the `MimeType` part property, exactly as AS4 does it.
* The response reuses the same `UserMessage` structure with `RefToMessageId` pointing at the
  request's `MessageId`.
* **There is no WS-Security header at all** in any example, consistent with the
  "minimalist version without cryptographic signature".

Service / Action values seen (note the **CEN namespace, not a Spanish one**):

| Operation | Service | Action |
|---|---|---|
| Submit invoices | `urn:cen.eu:en16931:invoice` | `SUBMITINVOICE` |
| Cancel invoice | `urn:cen.eu:en16931:cancelinvoice` | `CANCELINVOICE` |
| Query by filter | `urn:cen.eu:en16931:queryinvoices` | `QUERYBYFILTER` |
| Status events | `urn:cen.eu:en16931:submitstatus` | `AccountingCustomerParty` / `AccountingSupplierParty` |

For the status service the `Action` is used to say whether the batch carries issuer-side or
recipient-side statuses; the slide annotates the envelope as "ebXML no relevante" because the
substance sits in the `ApplicationResponse` payload.

Query-by-filter parameters ride in `eb:MessageProperties`: `Role`
(`AccountingSupplierParty` | `AccountingCustomerParty`), `InvoiceCompanyID`,
`InvoiceRegistrationName`, `RegistrationStartDate` and `RegistrationEndDate` (the slide notes
that both dates must fall inside the same period and fiscal year, and flags the time zone).

Services and message shapes:

| Operation | Endpoint | Request | Response |
|---|---|---|---|
| Submit invoices | `ws/SendInvoiceSOAP` | ebXML (`SUBMITINVOICE`) + N parts (`Invoice`) | ebXML + 1 part (`DocumentStatus`) |
| Cancel invoice | `ws/CancelInvoiceSOAP` | ebXML (`CANCELINVOICE`) + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |
| Download invoices | - | ebXML + 1 part (`ApplicationResponse` with N `DocumentReference`, ID only) | ebXML + **N parts (`Invoice`)** |
| Query invoices | - | ebXML only (by filter) or + 1 part (`ApplicationResponse`, by ID) | ebXML + 1 part (`DocumentStatus` with N `AdditionalDocumentResponse`) |
| Recipient status events | `ws/CustomerInvoiceEventsSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |
| Issuer status events | `ws/SupplierInvoiceEventsSOAP` | ebXML + 1 part (`ApplicationResponse`) | ebXML + 1 part (`DocumentStatus`) |

Limits: 1-100 invoices per batch (configurable); 5,120 KB max per invoice, applied per invoice
(configurable); **zero binary attachments** - `cbc:EmbeddedDocumentBinaryObject` and
`cbc:EmbeddedDocument` must be empty, while URL references via
`cac:Attachment/cac:ExternalReference/cbc:URI` are allowed; download capped at 100 invoices;
query capped at 1,000; up to 100 status events per `ApplicationResponse`, with issuer and
recipient actions neither mixed nor duplicated for the same invoice. Private platforms must act
as a "concentrador" and must not operate invoice-by-invoice or customer-by-customer.

### 2.1 Where this diverges from AS4

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
4. **Partial-success semantics.** `DocumentStatus` reports per-invoice outcomes across the batch,
   which has no equivalent in AS4's per-message all-or-nothing model. The response-code slide
   lists "200 all actions correct", "200 actions partially correct" and "400 all actions failed" -
   200 appears twice, so the middle value is probably 206 and needs confirming.
5. **Authentication is transport/business level** - a qualified electronic certificate plus the
   AEAT representation model (nombre propio / apoderamiento / colaborador social), not
   WS-Security tokens. The `From/PartyId` is expected to match the NIF in that certificate.
6. **Roles do not swap in the response.** In the reply the `From` is AEAT carrying
   `ebms:initiator` and the `To` is the original sender carrying `ebms:responder`, i.e. the role
   stays bound to the header position rather than to the party's role in the exchange. Worth
   raising with AEAT - it looks like a slide error, but if it is intentional a strict ebMS
   implementation will need to tolerate it.
7. **`ConversationId` is inconsistent** across the examples - a UUID in the cancel and query
   messages, empty in the submit response.

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
  (Art. 11.7).
* **Status messages travel over the same interconnection** (Art. 8.3): the interconnection must
  carry at least the invoices and the Art. 10.1 statuses (commercial acceptance/rejection plus
  date, full effective payment plus date). On the SPFE these are UBL `ApplicationResponse` /
  `DocumentStatus`. Over AS4 there is no defined Service/Action to distinguish an invoice from a
  status message.
* **Unique invoice code** (Art. 7.5): NIF of the issuer + invoice series/number + issue date.
  This is a natural business-level message identifier, but no mapping to ebMS message properties
  is defined.
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

9. ~~What exactly is "compatible with AS4/Peppol"?~~ Answered by the slide examples: SOAP 1.2
   plus the ebMS 3.0 core namespace, eBCore party identifiers, empty SOAP body, payloads as
   MIME parts - the AS4 header model minus security. What remains open is whether AEAT will
   also accept genuinely AS4-conformant messages (signed, with Receipts) from clients that
   send them.
10. Is the exchange a declared ebMS Two-Way/Sync MEP, and are there any ebMS signal messages
    (Receipt, Error) at all? Every response in the slides is a business `UserMessage`, so
    apparently not - but transport-level error handling is undocumented.
11. Are the response `Role` values (`From`=AEAT as `ebms:initiator`) intentional or a slide
    error, and is `ConversationId` mandatory? It is a UUID in some examples and empty in others.
12. What are the exact `DocumentStatus` response codes - is the second "200" actually 206?
13. Is message-level signing genuinely absent, or only absent in the first release? If it is
    later added, which policy?
14. How is the 24-hour-outage rule (submission allowed within the following four business days)
    expected to interact with client-side retry behaviour?
15. Will AEAT publish the ebXML envelope as a documented profile, or only as WSDLs plus examples?

## 6. Practical position for phase4

* Nothing has to be implemented today for the platform-to-platform leg: RD Art. 13.1.b) is
  satisfied by *any* conformant AS4 implementation, and phase4 is one.
* The SPFE leg is now the more interesting one. Its envelope is genuine ebMS 3.0 over SOAP 1.2
  with eBCore party identifiers and CID-referenced MIME payloads - the model phase4 already
  implements - and AEAT explicitly aims at AS4/Peppol compatibility. The divergences in
  section 2.1 (synchronous business responses, no WS-Security at all, no Receipts,
  partial-success batches) are what stand in the way. Worth a concrete feasibility check of
  how far the phase4 ebMS layer reaches once AEAT publishes the WSDLs and message examples,
  because the answer looks like "most of the way".
* The plausible convergence path for the platform-to-platform leg remains **Peppol** - the RD
  already blesses Peppol BIS as a syntax between private platforms, and Peppol supplies exactly
  the missing pieces (AS4 profile, SMP/SML discovery, identifier schemes, conformance testing).
  If the Spanish market converges there, `phase4-profile-peppol` / `phase4-peppol-client`
  already cover it with no new code.
* A `phase4-profile-spain` module only becomes meaningful once a Spanish AS4 usage profile
  actually exists - from the Ministry, AEAT, or an industry association. As of 2026-09-10
  there is none.
* The useful interim deliverable is documentation on running phase4 with **per-counterparty
  PModes**, which is what Art. 7.4 plus Art. 9 effectively force.

## 7. Sources

All quotes above come from:

* `legal/BOE-A-2026-7295_RD-238-2026.pdf` - Real Decreto 238/2026, de 25 de marzo (BOE 79, 2026-03-31)
* `legal/Proyecto-OM-SPFE_2026-04-16.pdf` - draft Ministerial Order on the SPFE (public consultation)
* `aeat/2026-05-19-seminario-spfe/` - AEAT developer seminar of 2026-05-19 (3 decks + FAQ)
* Notes taken from the slides of the AEAT webinar of 2026-09-10 (not stored here; draft,
  unpublished and unverified content - see the caveat at the top of this file)
