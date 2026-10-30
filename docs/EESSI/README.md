# EESSI - Electronic Exchange of Social Security Information

Assessment of what it would mean to add an EESSI AS4 profile to phase4.

Written 2026-09-21 against phase4 commit `94584c9ad` (version 4.6.2-SNAPSHOT).

**This folder contains no documents.** Unlike every other folder under `docs/`, there is
nothing to store here: the EESSI AS4 profile specification is not published anywhere on the
public Internet. Everything below is reconstructed from secondary artefacts and is flagged
accordingly. See [Specification availability](#specification-availability) for what was searched
and what was not found.

## Disambiguation

"EESSI" is an overloaded acronym. This document is about **Electronic Exchange of Social
Security Information** (European Commission, DG EMPL). It is *not*:

* EESSI - European Environment for Scientific Software Installations (`eessi.io`, HPC).
  Checked 2026-09-21: `https://www.eessi.io/docs/` is a scientific software stack for HPC
  clusters and contains no mention of AS4, ebMS, social security or DG EMPL. Not relevant.
* EESSI - European Electronic Signature Standardization Initiative (CEN/ETSI, 1999)

Several search engines conflate all three.

## What EESSI is

The EU system for cross-border exchange of social security data between national institutions
(pensions, unemployment, family benefits, healthcare entitlement). Legal basis: Regulations
(EC) 883/2004 and 987/2009.

As of Q2 2026: 3,120 institutions, 32 countries, 99 Business Use Cases, ca. 4.3 million
Structured Electronic Documents (SEDs) per month, full production targeted for September 2026.

Governance is intergovernmental - the Administrative Commission for the Coordination of Social
Security Systems and its Technical Commission for Data Processing (Decision No H15 of
2024-06-27). There is no OpenPeppol-equivalent with published specifications, a change request
process, or a vendor membership route.

Two legal decisions matter technically:

* **Decision No E5** (2017-03-16) - defines "EESSI-enabled" per Business Use Case: a Member
  State can send and receive all messages of that BUC via its Access Point(s).
* **Decision No E6** (2017-10-19, OJ C 355, 2018-10-04, p. 5) - legal delivery occurs at
  *"the endpoint of the **ebMS AS4** electronic data transport protocol in EESSI"*, on the date
  of *"the acknowledgement generated at the ebMS endpoint"*. Messages must be extracted from
  the national Access Point to the endpoint at least once every 24 hours.

E6 places the legally significant boundary at the **ebMS endpoint**, not at the Access Point.
An AS4 stack acting as an institution's endpoint therefore sits on the legally relevant side -
its receipt generation affects statutory deadlines.

### Historical note: EESSI before AS4

**Material for this assessment only in the negative sense - do not mine pre-2014 EESSI sources
for AS4 information.** EESSI in its first generation was a different system with a different
transport, and the vocabulary from that era still circulates in secondary sources, which is a
live source of confusion.

A CNPAS (Romania) presentation of April 2010 describes that architecture: Member States on
sTESTA, **Coordination Nodes (CN)**, Access Points (Romania deployed four, one per branch:
unemployment, family benefits, sickness, pensions/accidents), Competent Institutions, a
**"EESSI Protocol"** for international traffic with an **"Internal Bridging Protocol"** and an
**"ICD2 interface"** towards national systems, plus **WEBIC** and a **Master Directory**.

It contains **no** mention of AS4, ebMS, SOAP, web services or a PKI. That is a different design
from the 2017-and-later AS4 / RINA architecture described in the rest of this document. When and
why the transition happened is **not verified here** - only that the two architectures differ.

Practical consequence: the term **"Master Directory"** is genuine but belongs to the *old*
architecture. Today's primary sources say **Institution Repository (IR)**. A secondary source
that mixes "Master Directory", "Coordination Node" and "AS4" in one breath is blending two
generations and should not be trusted on detail.

## Architecture

| Corner | Role | Speaks AS4? |
|---|---|---|
| C1 / C4 | Institution / national application (RINA, or a national equivalent) | **Yes** - own institution ID, own ebMS signing certificate, own TLS client certificate |
| C2 / C3 | National Access Point | **Yes** - acts as an ebMS **intermediary** (i-MSH), forwarding via ebMS3 Part 2 multi-hop |
| CSN | Central Service Node, operated by DG EMPL | Directory / artefact hub - Institution Repository, BUC repository, data model; synchronised *to* the Access Points |

This is an unusual four-corner variant: C1 and C4 speak AS4 themselves, unlike Peppol where the
corner-1/corner-4 backends are non-AS4. That is why the *endpoint's* own send PMode carries
`IsMultiHop=true` - the message must already contain the routing headers the Access Point needs
in order to forward it.

The only public primary statement of the transport architecture found:

> "EESSI uses the AS4 protocol applying a **multi-hop** architecture for different flows, whereby
> **National nodes are interconnected by means of Access Points**, facilitated by **common
> directory services** offered by a Central Service Node operated by the European Commission's
> DG EMPL."
> -- Interoperability Test Bed news item, see Sources

Transport runs over sTESTA / TESTA-ng, the closed network for EU administrations.

Whether the CSN ever sits *in the message path* for any flow could not be determined - "for
different flows" leaves room and no source enumerates them.

## The EESSI AS4 profile parameters

> **Evidence quality warning.** None of the following comes from a specification. It is read off
> the PMode sample and conformance-test configuration XML shipped in the `eessi-as4.net`
> reference implementation. These are configuration artefacts, not normative text. Whether
> production still matches them is unverified - in particular the crypto may since have moved
> towards eDelivery AS4 2.0.

Verified first-hand on 2026-09-21 by cloning the repository (anonymous `git clone` works, ca.
227 MB). HEAD is commit `83cd0a85df7ae5cd730b91e63eb9b66438974340`, **2019-02-27**, author
Frederik Gheysels. Files read:

* `output/samples/pmodes/eessi/eessi-push-send-pmode.xml`
* `output/samples/pmodes/eessi/eessi-pull-send-pmode.xml`
* `output/samples/pmodes/eessi/eessi-pull-receive-pmode.xml`
* `output/config/eessi-conformancetest-settings/c2-settings.xml` and `C2/{send,receive}-pmodes/*`
  (the C3 set mirrors it)
* `output/doc/wiki/runtime/getting-started/sample-scenarios.md`
* `source/Eu.EDelivery.AS4/Constants.cs`,
  `source/Eu.EDelivery.AS4/Serialization/SoapEnvelopeSerializer.cs`

| Parameter | Observed value |
|---|---|
| MEP / binding | One-Way **Push** (send) **and** One-Way **Pull** (receive). The PullRequest itself is configured as a `OneWay`/`Push` sending PMode aimed at the AP Inbox. |
| Multi-hop | `<IsMultiHop>true</IsMultiHop>` - **only on the push user message send PMode**. The pull-send and pull-receive PModes do not set it. |
| MPC | `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC/UK:UK001` in the C2 config - i.e. the suffix is the **pulling institution's own party ID**, giving per-institution sub-channels. The sample placeholder reads "[Add your Institution's PULL endpoint here]". |
| PartyId type | `urn:eu:europa:ec:dgempl:eessi:ir` (ir = Institution Repository) |
| PartyId value | `<CC>:<INSTID>`, e.g. `UK:UK001` |
| Role | `urn:eu:europa:ec:dgempl:eessi:ir:institution` (both From and To) |
| Service | value `BusinessMessaging`, type `urn:eu:europa:ec:dgempl:eessi` |
| Action | `Send` |
| Signature | `http://www.w3.org/2001/04/xmldsig-more#rsa-sha256`, hash `http://www.w3.org/2001/04/xmlenc#sha256`, `<KeyReferenceMethod>BSTReference</KeyReferenceMethod>` |
| Signature verification | `<SigningVerification><Signature>Allowed</Signature>` - i.e. **not `Required`**. An unsigned inbound message is accepted by this configuration. |
| Encryption | `<Encryption>Ignored</Encryption>` on receive, and no `<Encryption>` block at all on send - **no WS-Security message layer encryption** on the institution-to-AP hop |
| Compression | **Not configured.** No `UseAS4Compression` element appears in any EESSI PMode, although other (eDelivery interop) PModes in the same repository do set it. |
| Receipts | `<UseNRRFormat>true</UseNRRFormat>`, reply pattern **`Callback`** - the receipt is signed and pushed to the AP Outbox URL as a separate request, not returned in the HTTP response |
| Transport security | **TLS 1.2 with a client certificate** (mutual TLS) on every hop, `FindBySerialNumber` against the Windows certificate store |
| Certificates | Two distinct certificates per institution: a **TLS client certificate** and an **ebMS signing certificate** |
| Endpoint URL shape | `https://<host>/EESSI/BusinessMessaging/v0.0/{Outbox,Inbox}/Service.svc` (conformance test host: `eessidev09.eessi.be`) |
| Pull polling | Adaptive interval 1 s to 25 s - increases while PullRequests come back empty, resets to the minimum when a message is received |
| Piggybacking | AS4.NET v4.0.0 added "sending response signal messages via reliable piggybacking in a pull receive scenario" - ebMS3 Part 2 bundling on the PullRequest |

### Multi-hop mechanics as implemented by AS4.NET

Exact values, from `Constants.cs` and `SoapEnvelopeSerializer.SetMultiHopHeaders`:

| Constant | Value |
|---|---|
| Multi-hop namespace | `http://docs.oasis-open.org/ebxml-msg/ns/ebms/v3.0/multihop/200902/` |
| `nextmsh` | `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/nextmsh` |
| `icloud` | `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/part2/200811/icloud` |
| Receipt multi-hop action | `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay.receipt` |
| Error multi-hop action | `http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay.error` |
| WS-Addressing | `http://www.w3.org/2005/08/addressing` |

Behaviour:

* On a **user message** with `IsMultiHop=true`, the `eb:Messaging` header gets
  `@S12:role = <nextmsh>`.
* On a **signal message** that is a multi-hop signal, three SOAP headers are added: `wsa:To`
  with `@Role = <nextmsh>`, `wsa:Action` set to the `oneWay.receipt` / `oneWay.error` value
  above, and a `RoutingInput` header carrying a `RoutingInputUserMessage` (the reversed user
  message), with `mustUnderstand=false` and `IsReferenceParameter=true`.
* On receipt, the intermediary/endpoint locates the routing data with the XPath
  `//*[local-name()='RoutingInput']`.

This is the concrete shape phase4 would have to produce and consume.

### Documentation inconsistency in the reference implementation

`sample-scenarios.md` states that **four** PModes are required for an EESSI ebMS endpoint and
names them `eessi-push-send-pmode-AP`, `eessi-pull-send-pmode`, `eessi-pull-receive-pmode` and
`eessi-pull-response-send-pmode`. Only **three** skeleton files ship in
`output/samples/pmodes/eessi/`, and the push one is named `eessi-push-send-pmode.xml`, not
`...-AP.xml`. The `eessi-pull-response-send-pmode.xml` file is referenced by the documentation
but does not exist in the repository (the instructions also spell it two different ways). Its
content is implied by the `<ResponseConfiguration>` block already embedded in
`eessi-pull-receive-pmode.xml`.

### This is not "eDelivery AS4 with tweaks"

EESSI is a *sibling* profile of the eDelivery AS4 Common Profile, not a derivative of it. The
Common Profile:

* is explicitly **not** ebMS3 Part 2 multi-hop (its Four Corner enhancement is a message
  property convention, not a `RoutingInput` SOAP header)
* does **not** include Pull (optional enhancement only, since 1.14)
* mandates AES-128-GCM message layer encryption; EESSI uses none on this hop
* uses the `Response` receipt reply pattern, never `Callback`
* requires GZIP compression (1.15) or recommends it (2.0); no EESSI PMode configures it
* identifies parties with ebCore party ID types; EESSI uses its own
  `urn:eu:europa:ec:dgempl:eessi:ir` scheme

Which eDelivery AS4 base version (1.12 / 1.13 / 1.14 / 1.15 / 1.16 / 2.0) EESSI derives from, if
any, could not be determined. The 2016-2017 timing suggests the e-SENS 1.12 era, but no source
states it.

## Gap analysis against phase4

Verified in the working tree at commit `94584c9ad`.

| Requirement | Status in phase4 |
|---|---|
| RSA-SHA256 / SHA-256, BinarySecurityToken | OK - `ECryptoAlgorithmSign.RSA_SHA_256` |
| Signing without encryption | OK - exactly what `HREDeliveryPMode` already does |
| MIME payloads, empty SOAP body | OK |
| GZIP compression | OK, but **not required** - no EESSI PMode configures compression |
| NRR receipts (`ebbp:NonRepudiationInformation`) | OK |
| One-Way / Pull and the MPC model | OK - `EMEPBinding.PULL`, `com.helger.phase4.model.mpc`, `AbstractAS4PullRequestBuilder`, `IAS4IncomingPullRequestProcessorSPI` |
| Mutual TLS | OK - via `HttpClientSettings` |
| **ebMS3 Part 2 multi-hop** | **MISSING** - `grep -ril "multihop\|multi-hop\|RoutingInput\|ebint"` returns 0 hits across the whole repository |
| **`Callback` receipt reply pattern** | **MISSING** - `EPModeSendReceiptReplyPattern.CALLBACK` exists as an enum constant and is referenced nowhere else in the codebase |
| **Signal piggybacking on PullRequest** | **MISSING** - no bundling support |

The first seven rows are profile configuration. The last three are `phase4-lib` changes.

Multi-hop alone means the `RoutingInput` SOAP header in the
`http://docs.oasis-open.org/ebxml-msg/ns/ebms/v3.0/multihop/200902/` namespace, WS-Addressing
`wsa:To` / `wsa:Action` headers with the `nextmsh` / `oneWay.receipt` / `oneWay.error` values
listed above, `eb:Messaging/@role=nextmsh`, and multi-hop receipt routing - new XSDs, new JAXB
generation, and changes in both the incoming and the outgoing path. It is a core library
feature, not profile work. See
[Multi-hop mechanics as implemented by AS4.NET](#multi-hop-mechanics-as-implemented-by-as4net)
for the exact constants.

## What a phase4-profile-eessi module would look like

If the core library gaps were closed, the profile module itself is ordinary work, mirroring
`phase4-profile-hredelivery` (17 files, 2422 lines, commit `f3b1eb727`):

```
phase4-profile-eessi/
  pom.xml
  src/etc/javadoc.css
  src/etc/license-template.txt
  src/main/resources/LICENSE
  src/main/resources/NOTICE
  src/main/resources/META-INF/services/com.helger.phase4.profile.IAS4ProfileRegistrarSPI
  src/main/java/com/helger/phase4/profile/eessi/
      AS4EESSIProfileRegistarSPI.java      (~65 lines)
      EESSIPMode.java                      (~215 lines)
      EESSICompatibilityValidator.java     (~450 lines)
  src/test/java/com/helger/phase4/profile/eessi/
      SPITest.java
      EESSIPModeTest.java
      EESSICompatibilityValidatorTest.java
```

Only `IAS4ProfileRegistrarSPI` is strictly mandatory; `AS4Profile` is reusable as-is and every
method of `IAS4ProfileValidator` has an empty default.

Probably **two profile IDs** are needed (push and pull), registered from one SPI class - the
pattern `phase4-profile-entsog` (3 IDs) and `phase4-profile-edelivery2` (4 IDs) already use.

Registration touchpoints outside the new module: root `pom.xml` (`<modules>` and
`<dependencyManagement>`), `README.md` profile link list, `publiccode.yml`, `CLAUDE.md` profile
enumeration, and the wiki (`Profiles.md`, `_Sidebar.md`, a new `Profile-EESSI.md`,
`News-and-noteworthy.md`).

## Specification availability

**The EESSI AS4 Messaging Profile is not public.** It is named as a distinct document in
Commission-published material (the "EESSI AS4.NET Component" deck, and the reference
implementation README), but no copy is reachable.

What was searched, and the result:

| Index | Query | Result |
|---|---|---|
| TED (EU tenders) full-text API | "Architectural and Interface Specifications" + EESSI | 0 (control: `FT="EESSI"` returns 228 notices, so the index is covered) |
| EC Confluence REST `title~` | EESSI | 9 items, all AS4.NET artefacts plus the dashboard PDF - **no EESSI space exists** |
| GitHub code search | 3 phrase variants | 0 |
| OpenAlex | EESSI AIS | 0 |

A document titled "EESSI Architectural and Interface Specifications" could not be confirmed to
exist at all. What *does* exist by name is a documentation set formerly mirrored on the now-dead
`nordic-eea.eu` wiki:

* `EESSI - Architecture Overview Document v1.0.0.pdf` (2016-03-31)
* `EESSI - RINA National Information Exchange Interface (NIE) v1.0.4`
* `EESSI - RINA Identity and Access Management (IAM) v1.0.8`
* `EESSI - Certificate Management Guide v1.0.1.pdf` - the document that would settle the open
  PKI questions

The domain no longer resolves and the Internet Archive returns archived 404s.

The real documentation lives on a non-public Confluence. An INPS document cites its own source
verbatim as *"(Source: Confluence Portal / **EESSI Collaboration Space** / EESSI (Stakeholders
Space) / EESSI at a Glance / What is EESSI)"*. That space is not among the public EC Confluence
spaces. CIRCABC is the most likely home of the restricted set and is not API-searchable.

Corroborating the gated distribution model: the RINA source is EUPL 1.2 but is released only to
Participating Countries via an "EESSI RINA External GIT Access Request procedure", with binaries
on an FTP server whose credentials are issued by `EMPL-EESSI-SERVICE-DESK@ec.europa.eu` to
designated EESSI SPOC / Access Point SPOC persons.

## PKI and registry

Verified:

* Certificates are **per institution**, split into a **TLS client certificate** and an **ebMS
  signing certificate** - distinct entries in every EESSI PMode sample.
* Certificates are registered in the **Institution Repository (IR)**. The DG EMPL onboarding
  checklist requires "get digital certificate(s) for the relevant environment (DEVELOPMENT,
  TEST, ACCEPTANCE, PRODUCTION)" and "Verify that dedicated institutions exist in the IR
  (including certificates)".
* The IR is centrally maintained at the CSN and synchronised outward to Access Points and RINA
  instances. An **IR SPOC** per country maintains the institution data.

Not determined:

* **Who issues EESSI AS4 certificates.** No public certificate policy, certificate profile,
  naming constraint, key algorithm/length, policy OID, validity or CRL/OCSP requirement was
  found. Searched: EC pages, national implementer pages (DSRV, RINIS, REGOS, NORA), and the
  AS4.NET configuration manual. This is a genuine gap, not a negative finding.

**There is no EESSI SMP/SML.** No evidence of any Peppol-style distributed service metadata
layer was found. Routing metadata lives in the centrally maintained IR and is pushed to the
Access Points; party identification uses `urn:eu:europa:ec:dgempl:eessi:ir`. AS4.NET does
support SMP/SML and OASIS BDX dynamic discovery, but that is its generic eDelivery capability
and is not evidence that EESSI uses it.

## Conformance testing

Operated by **DG EMPL**, with the platform supplied by DG DIGIT's **Interoperability Test Bed
(ITB)**. Since Q3 2024 DG EMPL ships an **EESSI Conformance Testing Tool**: a preconfigured,
self-contained package that national implementation teams run on-premise, with the real EESSI
backbone replaced by an emulator driven by the ITB test engine. Before that, national nodes
tested against DG EMPL-hosted environments.

Whether that package is obtainable by a software vendor, or only by a Member State national
implementation team, could not be determined. Given the RINA distribution model, vendor access
is probably brokered through a Member State - but that is an inference.

**eDelivery AS4 conformance cannot substitute for it.** The eDelivery Market Guide v1.06 s5.2
states the conformance tests "do not cover features that are not used in that profile. Examples
of those untested features are use of username tokens, or the **Pull binding**." EESSI requires
both Pull and multi-hop.

Note that phase4 *is* on the eDelivery conformant products list - "phase4 version 0.9.0,
2019-08-08, pre-1.15", alongside "EESSI AS4.NET, 2018-10-22, pre-1.15". Per the caveat above,
that listing says nothing about EESSI.

## Implementations in the field

* **AS4.NET** ("EESSI AS4.NET") - the DG EMPL reference implementation. C# / .NET Framework,
  **EUPL v1.1**, hosted on the Commission's own Bitbucket. Specified by DG EMPL, implemented by
  Codit (the commit history is overwhelmingly `@codit.eu`; no document states it, so treat the
  attribution as inference). **Last upstream commit `83cd0a85` of 2019-02-27** by Frederik
  Gheysels, verified by cloning the repository on 2026-09-21 (v4.0.1) - effectively
  unmaintained. DIGIT's Market Guide files it under "User Implementations of AS4 Messaging
  Software" with the caveat that such solutions "are not officially supported for third
  parties". Contact: `empl-eessi-edelivery@ec.europa.eu`.
* **Holodeck B2B** (Java, GPL3) - claimed to be part of the EC-supplied EESSI kit by Holodeck
  itself, by Chasquis Consulting (which sells a commercial EESSI endpoint), and by two
  integrators (Proexes for the German Federal Employment Agency, Phoenix IT for Romania). **The
  EC has never stated this**: the EC-hosted Holodeck fact sheet contains zero occurrences of
  "EESSI" or "RINA", and the eDelivery Market Guide lists Holodeck's domains as e-SENS, EU-CEG,
  ENTSOG, IATA and ATO - not EESSI. Treat as secondary-source only.
* **Domibus** - no EESSI evidence whatsoever. It appears in EESSI context only as an AS4.NET
  interop test partner. Search engine summaries claiming otherwise could not be traced to any
  page and appear to conflate EESSI with eDelivery generally.
* National operators / integrators: DE - Bundesagentur fuer Arbeit (2 APs), DSRV Wuerzburg,
  DVKA, DGUV, with ITSG operating for DVKA; NL - RINIS; HR - REGOS; FI - Kela; PL - Asseco for
  ZUS; RO - Phoenix IT; DE - Proexes.
* **RINA handover**: the Commission decided in January 2020 to stop maintaining RINA. A
  20-country Joint Procurement Agreement (INPS as central purchasing body) awarded a three-year
  contract in May 2023 to an Engineering Ingegneria Informatica-led consortium for EUR 5.74m
  excl. VAT. **The AS4 component was not in scope.**

**Whether a third party may replace the EC-supplied Access Point is undetermined.** The
frequently quoted sentence "Member States ... can also develop alternative implementations based
on this reference architecture" sits inside the *RINA* paragraph of the EESSI dashboard PDF and
continues "...will be interoperable by default with RINA". It grants freedom over the national
application, not over the Access Point. DYPA states the same restriction explicitly. No EC
statement was found either permitting or forbidding substitution of the AP itself, and there is
no public list of EESSI-conformant AS4 products.

## Existing phase4 trace

The only occurrence of "eessi" in the phase4 working tree is a test host name:

`phase4-test/src/test/java/com/helger/phase4/server/external/AS4_NETFuncTest.java:56`

```java
public static final String DEFAULT_AS4_NET_URI = "http://eessidev10.westeurope.cloudapp.azure.com:7070/as4-net-c3";
```

That test is `@Ignore("Working! Requires external proxy and Peppol pilot certificate!")` and uses
the plain CEF / e-SENS one-way PMode, not an EESSI PMode. So phase4 has interoperated with the
AS4.NET software in an EESSI development environment, but never with the EESSI profile.

## Conclusion

The engineering is tractable; the specification access is not.

1. The profile module is a week of ordinary work, once the parameters are known.
2. `phase4-lib` needs ebMS3 Part 2 multi-hop, the `Callback` receipt reply pattern, and
   PullRequest signal piggybacking. Multi-hop is the large item and is reusable beyond EESSI.
3. The binding constraint is that the profile is unpublished. Any implementation would be
   reverse-engineered from 2017-2019 configuration artefacts and could only be validated inside
   the EESSI test environment, which requires EESSI PKI credentials and an EESSI SPOC
   relationship.

Suggested order if this is pursued:

1. Ask `empl-eessi-edelivery@ec.europa.eu` / `EMPL-EESSI-SERVICE-DESK@ec.europa.eu` directly:
   (a) can the EESSI AS4 Messaging Profile be released to an open source implementer,
   (b) is the EESSI Conformance Testing Tool obtainable outside a Member State team,
   (c) may a third-party AS4 product serve as an Access Point or as an institution endpoint.
2. Look for a Member State operator willing to sponsor access (DSRV, RINIS, REGOS, ITSG, ...).
   That appears to be how the one commercial EESSI endpoint vendor got there.
3. Only then: multi-hop in `phase4-lib` first, then callback receipts, then the profile module.

## Open questions

1. Does a document titled "EESSI Architectural and Interface Specifications" exist at all?
2. The EESSI AS4 Messaging Profile document - named on EC sources, no public copy, no known
   version number.
3. Which eDelivery AS4 base version, if any, EESSI derives from.
4. May a Member State substitute a third-party AS4 product for the EC-supplied Access Point?
5. Which AS4 engine is inside the EC-supplied EESSI Access Point (Holodeck B2B and AS4.NET are
   both plausible; the EC has never said).
6. Does the CSN ever sit in the message path for any flow?
7. Does EESSI use SMP/SML anywhere? (No evidence for; no explicit statement against.)
8. SED / business-layer encryption parameters - algorithm, key transport, MGF. **Do not
   substitute the eDelivery `rsa-oaep-mgf1p` / `aes128-gcm` values**: those belong to the
   eDelivery and Minder configurations in the same repository and must not be attributed to
   EESSI.
9. Production certificate details - issuing CA(s), key algorithm and length, policy OIDs,
   validity, CRL/OCSP requirements. The `EESSI - Certificate Management Guide v1.0.1` would
   settle this; it is unreachable.
10. Does the AP-to-AP leg use the same mutual TLS configuration as the institution-to-AP leg?
11. Does the live profile still match the 2017-2019 parameters, or has it moved to TLS 1.3 and
    eDelivery AS4 2.0 cryptography?
12. When and why did EESSI move from the 2010 architecture (Coordination Nodes, "EESSI
    Protocol", WEBIC, Master Directory) to the AS4 / RINA architecture? Not established here.
    Relevant only for dating a source correctly - if a document predates the switch, its
    transport content is worthless for this purpose.

## Sources

Primary:

* Reference implementation (EUPL v1.1, last commit 2019-02-27):
  https://ec.europa.eu/digital-building-blocks/code/projects/EDELIVERY/repos/eessi-as4.net/browse
  * Anonymous `git clone --depth 1 https://ec.europa.eu/digital-building-blocks/code/scm/edelivery/eessi-as4.net.git`
    works, takes ca. 227 MB, and is by far the most productive route - it is how every parameter
    in this document was verified. The Bitbucket code search API returns 0 for this repository
    (not indexed); the REST browse API and `/raw/<path>?at=refs%2Fheads%2FRelease%2Fv4.0.1` both work.
  * GitHub mirror (2021 fork, one commit on top): https://github.com/thomsonreuters/eessi-as4.net
* EESSI AS4.NET Component deck (DIGIT):
  https://ec.europa.eu/digital-building-blocks/sites/download/attachments/467110150/EESSI%20AS4.NET%20Component.pdf
* EESSI dashboard (Digital Building Blocks):
  https://ec.europa.eu/digital-building-blocks/sites/download/attachments/684630922/EESSI%20(dashboard).pdf
* ITB in support of EESSI (the transport architecture statement):
  https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed/news/itb-support-eessi
* EESSI Factsheet 2026Q2:
  https://employment-social-affairs.ec.europa.eu/document/download/5059e6b0-4cc3-48ea-8c62-d8ceb30068d9_en?filename=EESSI%20Factsheet-2026Q2.pdf
* DG EMPL EESSI policy page:
  https://employment-social-affairs.ec.europa.eu/policies-and-activities/moving-working-europe/eu-social-security-coordination/digitalisation-social-security-coordination/electronic-exchange-social-security-information-eessi_en
* RINA external GIT access procedure:
  https://ec.europa.eu/newsroom/eessi/items/723852/en

Legal:

* Decision No E5 (2017-03-16): https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A32017D0719%2801%29
* Decision No E6 (2017-10-19): https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32018D1004(02)
* Decision No H15 (2024-06-27): https://eur-lex.europa.eu/eli/C/2024/6845/oj/eng
* Decision No E8 (2024-03-14) - full text could not be fetched; EUR-Lex blocked the request

eDelivery AS4 context (prefix `https://ec.europa.eu/digital-building-blocks/sites/spaces/DIGITAL`):

* e-SENS AS4 1.12 (2017-10-18): `/pages/467117691/e-SENS+AS4+-+1.12`
* eDelivery AS4 1.13 (2018-05-30): `/pages/467117673/eDelivery+AS4+-+1.13`
* eDelivery AS4 1.14 (2018-10-31): `/pages/467117656/eDelivery+AS4+-+1.14`
* eDelivery AS4 1.15 (2020-11-11): `/pages/467117638/eDelivery+AS4+-+1.15`
* eDelivery AS4 2.0 (2024-12-04): `/pages/845480153/eDelivery+AS4+-+2.0`
* eDelivery AS4 1.16 (2026-01-28): `/pages/943227070/eDelivery+AS4+-+1.16`
* AS4 v1.x conformant products: `/pages/721846393/eDelivery+AS4+v1.x+conformant+products`

Secondary (treat with care):

* Chasquis Consulting EESSI endpoint: https://www.chasquis-consulting.com/as4-gateways/eessi-endpoint/
* Proexes: https://www.proexes.com/eessi/
* Phoenix IT: https://www.phoenix-it.ro/eessi/
* DG EMPL "Keys to Production" transition deck (2017-09), mirrored by INPS:
  http://serviziweb2.inps.it/safeportal/downloadAttachment?idContentBlob=103
* INPS document citing the non-public EESSI Collaboration Space:
  https://serviziweb2.inps.it/safeportal/downloadAttachment?idContentBlob=121
* Bianca Culea (CNPAS, Romania), "EESSI - Electronic Exchange of Social Security Information",
  April 2010, 14 slides - the pre-AS4 architecture, see
  [Historical note](#historical-note-eessi-before-as4):
  https://de.slideshare.net/slideshow/2010-eessi-electronic-exchange-of-social-security-information/15568177
* DYPA (Greece) on national RINA alternatives:
  https://www.dypa.gov.gr/en/oaed-eessi-hlektroniki-antallaghi-pliroforiwn-koinonikis-asfalisis-metaksy-ton-khorwn-tis-ee-1

Research limits during this assessment (2026-09-21): the Internet Archive was down for the whole
session - both the Wayback availability API (HTTP 429) and the CDX API ("Internet Archive
services are temporarily offline"), re-checked twice. That blocked retrieval of the archived
`EESSI - Architecture Overview Document v1.0.0`, whose reference list is the single most likely
place to settle open question 1, and is the first thing to retry when the Archive is back.
Google was unreachable and fallback search engines served CAPTCHAs. Several EC and EDPS PDFs
return HTTP 403 to plain fetching and need `curl` with a browser user agent.

Leads that were checked and are dead ends, so as not to repeat them:

* `https://www.eessi.io/docs/` - the HPC project, unrelated (see
  [Disambiguation](#disambiguation)).
* Bitbucket *code search* API for this repository - returns 0 results, the repository is not
  indexed. Clone it instead.
* Public Digital Building Blocks Confluence - no EESSI space exists; only AS4.NET artefacts and
  the dashboard PDF are indexed.
* Pre-2014 EESSI material (conference decks, national presentations, the 2010 CNPAS slides)
  - describes a different architecture with no AS4 content at all. See
  [Historical note](#historical-note-eessi-before-as4).
