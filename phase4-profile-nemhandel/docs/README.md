# Nemhandel eDelivery specifications

Official specifications of the Danish Business Authority (Erhvervsstyrelsen), retrieved on 2026-10-19 from the
[Nemhandel documentation repository](https://rep.erst.dk/git/openebusiness/common/-/tree/master/guidelines/specification):

| File | Title | Version |
|------|-------|---------|
| [specification_-_network.pdf](specification_-_network.pdf) | Nemhandel eDelivery Network Specification | 1.2.0 (document 1.2.1) |
| [specification_-_access_point.pdf](specification_-_access_point.pdf) | Nemhandel eDelivery Access Point Specification | 1.2.2 |
| [specification_-_error_codes.pdf](specification_-_error_codes.pdf) | Nemhandel eDelivery Error Code List | 1.1.0 |

Further pages (nemhandel.dk):

* [FAQ regarding Nemhandel eDelivery](https://nemhandel.dk/vejledning-qa-regarding-transition-nemhandel-edelivery) (last updated 2023-04-27)
* [En teknisk introduktion til Nemhandel](https://nemhandel.dk/vejledning-en-teknisk-introduktion-til-nemhandel)
* [Guide til Nemhandel dokumentations-repository](https://nemhandel.dk/vejledning-guide-til-nemhandel-dokumentations-repository)
* [Vejledning til Nemhandel Demo-miljø](https://nemhandel.dk/vejledning-vejleding-til-nemhandel-demo-miljoe-test-miljoe)
* Legal basis: [BEK 1811 of 28/12/2023](https://www.retsinformation.dk/eli/lta/2023/1811)

## Relation to Peppol

Nemhandel eDelivery is based on the Peppol AS4 profile 2.0.2 (plus Peppol SML 1.2.0, SMP 1.3.0, SBDH 2.0.1 and the
Peppol policies for identifiers and transport security). The Nemhandel specifications do not define any AS4/PMode level
deviation, so this module reuses the Peppol PMode and the Peppol compatibility validator.

## What differs and is *not* covered by this module (yet)

| Topic | Nemhandel | Source |
|-------|-----------|--------|
| SMP transport profile | `nemhandel-transport-as4-v2_0` (see `CNemhandel`) | Network, "SMP" |
| SML | `https://sml.erst.dk/nemhandel-sml/` (see `CNemhandel`) | Network, "SML" |
| AP certificates | MitID system certificates (FOCES3), CVR number in the subject; no Peppol seat ID | Network, "Identification schemes" |
| Payload signature | Detached XAdES-BASELINE-B in `StandardBusinessDocumentHeader/BusinessScope/Scope` of type `NEMHANDEL_EDELIVERY_SIGNATURE` | Network, "Document signatures" |
| Validation | XSD error: synchronous rejection; Schematron error: asynchronous MLR/AR | Network, "Validation" |
| Error codes | `E-APR21xxx` list | Error Code List |
| Logging, uptime, payload size | 5 years, 99.65 %, 100 MB | Access Point |

The Peppol specific `PartyId` pattern `P[A-Z]{2}[0-9]{6}` (Peppol seat ID) is intentionally not applied to Nemhandel; this module does not check it.

Open points in the sources: the Network specification requires error code list 1.2.0, but only 1.1.0 is published, and
that version still refers to ASiC-E / transfer delegation, which the Network specification no longer uses.
