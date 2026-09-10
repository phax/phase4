# AS4 relevance of the Spanish B2B e-invoicing mandate

Status: 2026-09-10. Based on the documents in this folder.

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

## 2. The SPFE itself is not AS4

The public hub uses synchronous **web services** (WSDL to be published on the AEAT Sede
electronica), plus an interactive web form, authenticated with electronic certificates
(draft Ministerial Order Art. 10; Cl@ve additionally allowed for the form only).
Its syntax is **UBL only** (RD Art. 11.2), aligned to EN 16931 - according to the AEAT the
2026 edition of EN 16931 based on UBL 2.5, extended in Annex I of the Order to cover Spanish
invoicing requirements (RD 1619/2012) that EN 16931:2019 cannot express (retentions,
disbursements/suplidos, reductions, equivalence surcharge, large-company special regime).

phase4 is therefore **not** applicable to the SPFE leg. It is only applicable to the
platform-to-platform leg.

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

1. Which AS4 baseline is meant - the OASIS AS4 Profile of ebMS 3.0 v1.0 (ebHandler / Light Client),
   the eDelivery AS4 profile, or the Peppol AS4 profile? The phrasing "AS2 or AS4" reads like a
   generic reference to EDI transport, not a conformance profile.
2. MEP: One-Way/Push only, or is Pull required for platforms without a public endpoint?
3. Are AS4 signed Receipts with non-repudiation information mandatory, and are they the legal
   proof of delivery between platforms? Retry and duplicate-detection windows?
4. Which certificates at the transport layer, and which trust anchors? The RD only regulates
   certificates for *invoice signing* and for *SPFE access*. Is there to be a trust list for
   platform-to-platform AS4, or is it purely bilateral trust?
5. How are ebMS PartyIds formed - NIF based? Which identifier scheme
   (e.g. `iso6523-actorid-upis` / 9920 as used for Spanish VAT in Peppol)?
6. Which Service/Action values separate invoice, credit note, status message and error?
7. Payload profile: single payload or multiple, compression, and how is the syntax
   (UBL / CII / EDIFACT / Facturae) signalled?
8. Conformance testing: Art. 9.3 makes every platform run its own test bed for its counterparties.
   There is no national conformance test comparable to CEF eDelivery or the Peppol test bed.
9. Does the eventual Ministerial Order, or a later technical annex, add any of this? The current
   draft Order covers only the SPFE, not the platform-to-platform leg.

## 6. Practical position for phase4

* Nothing needs to be implemented in phase4 to satisfy the Spanish rules today - Art. 13.1.b) is
  satisfied by *any* conformant AS4 implementation, and phase4 is one.
* The plausible convergence path is **Peppol**: the RD already blesses Peppol BIS as a syntax
  between private platforms, and Peppol brings the missing pieces (AS4 profile, SMP/SML discovery,
  identifier schemes, conformance testing). If the Spanish market converges there, the existing
  `phase4-profile-peppol` and `phase4-peppol-client` modules cover it without new code.
* A dedicated `phase4-profile-spain` module only becomes meaningful if a Spanish AS4 usage profile
  is actually published - by the Ministry, by AEAT, or by an industry association. As of
  2026-09-10 no such document exists publicly.
* What would be genuinely useful in the meantime is documenting how to run phase4 with
  **per-counterparty PModes**, which is what Art. 7.4 plus Art. 9 effectively require.

## 7. Sources

All quotes above come from:

* `legal/BOE-A-2026-7295_RD-238-2026.pdf` - Real Decreto 238/2026, de 25 de marzo (BOE 79, 2026-03-31)
* `legal/Proyecto-OM-SPFE_2026-04-16.pdf` - draft Ministerial Order on the SPFE (public consultation)
* `aeat/2026-05-19-seminario-spfe/` - AEAT developer seminar of 2026-05-19 (3 decks + FAQ)
