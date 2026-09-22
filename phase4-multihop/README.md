# phase4-multihop

AS4 **Multi-Hop Endpoint** support for phase4 - the AS4 Profile Multi-Hop Endpoint Conformance
Clause (AS4 Profile v1.0 OS section 6.6, which references all of section 4) on top of ebMS3
Part 2 Advanced Features section 2.

Adding this module to the classpath enables an endpoint to send messages through an I-Cloud and
to answer messages that arrived through one. Without it, phase4 behaves exactly as before.

## What is supported

| Feature | Spec |
|---|---|
| Sending a User Message via an I-Cloud - the `nextmsh` SOAP role (1.2) / actor (1.1) on `eb:Messaging` | AS4 section 4.3 `AddActorOrRoleAttribute` |
| Answering such a message with a Receipt or Error that carries `wsa:To`, `wsa:Action` and `ebint:RoutingInput` | AS4 section 4.2 and 4.4 |
| The inferred RoutingInput for the reverse path - From/To swapped, `.receipt` / `.error` MPC suffix | AS4 section 4.4, Part 2 section 2.6.2 case 4 |
| The added headers are covered by the signature | Part 2 section 2.4.5 via WS-I RSP |
| Receiving routed Receipts and Errors, synchronously and asynchronously | Part 2 section 2.5 |
| Callback Receipts for pulled User Messages ("First-push-last-pull") | Part 2 section 2.4.7.1 |
| Recognition of EBMS:0020 - 0023 | Part 2 section 2.5.6, appendix H |
| `eb:AgreementRef/@pmode` without the `.init` / `.resp` unit suffix, and resolving it back | Part 2 section 2.7.2 |

## What is NOT supported

* **The ebMS Intermediary conformance clause.** This module implements the *endpoint* side only -
  no forwarding, no routing function, no store-and-forward, no sub-channels. The
  `TestForwardingIntermediary` in the test sources is a test fixture, not a product feature.
* WS-ReliableMessaging, WS-SecureConversation, two-way MEPs, bundling, splitting.
* `wsa:ReplyTo` and `wsa:FaultTo` - AS4 Profile section 4.4 explicitly does not require an
  endpoint to support them.

## Usage

### Receiving

One line per servlet. A fresh request handler is created per request and pre-seeded with the
defaults before the customizer runs, so this is safe:

```java
final AS4XServletHandler aHandler = new AS4XServletHandler ();
aHandler.setRequestHandlerCustomizer (new AS4MultiHopRequestHandlerCustomizer ());
```

That is all. Whether a response gets the routing headers is decided **per message**, by the
role/actor attribute of the incoming message - never by configuration. A message that did not
arrive through an I-Cloud is answered byte-identically to a phase4 without this module.

The three SOAP header element processors for `wsa:To`, `wsa:Action` and `ebint:RoutingInput`
register themselves via SPI, so simply having the module on the classpath is enough for an
incoming routed signal to be accepted.

### Sending

Sending is opt-in per P-Mode ID:

```java
final AS4MultiHopConfig aCfg = AS4MultiHopConfig.getDefaultInstance ();
aCfg.addAddActorOrRoleAttributePModeID ("my-pmode.init");

final MultiHopSendOutcomeHolder aOutcome = new MultiHopSendOutcomeHolder ();
final var aBuilder = AS4Sender.builderUserMessage ()
                              .agreementRef ("urn:my:agreement")   // required, see below
                              ... ;

AS4MultiHopSender.configure (aBuilder,
                             aPMode,
                             aOutcome,
                             aCfg,
                             MultiHopSentMessageStore.getDefaultInstance ());

final EAS4MultiHopSendOutcome eOutcome =
  AS4MultiHopSender.interpret (aBuilder.sendMessageAndCheckForReceipt (), aOutcome);
```

Two things are worth knowing:

* **An `agreementRef` value is mandatory.** phase4 only emits `eb:AgreementRef` - and therefore
  its `@pmode` attribute - when the AgreementRef value is non-empty. Without it the P-Mode ID
  would never reach the wire, so `configure()` fails fast rather than producing a message that
  silently violates the spec.
* **An empty HTTP 2xx is not a failure.** When an edge intermediary accepts a message for later
  delivery it answers with an empty body. phase4 reports that as
  `NO_SIGNAL_MESSAGE_RECEIVED`; `interpret()` turns it into `ACCEPTED_BY_ICLOUD_ASYNC`.

## Limitations

* **The sent message store is in-memory only.** It maps a sent message ID to the P-Mode ID so
  that an asynchronously arriving Receipt can be related back. A restart loses it, and in a
  cluster a routed signal that lands on a different node cannot be resolved. Size and age are
  bounded and configurable (10,000 entries / 7 days by default). A persistent implementation is
  out of scope.
* Only One-Way MEPs are covered.

## Implementation notes

Two findings from building this are worth recording, because both are easy to get wrong:

1. **WSS4J ignores the `"Content"` / `"Element"` modifier of a `WSEncryptionPart` for
   *signature* parts.** `WSSecSignatureBase.addReferencesToSign` always builds
   `newReference("#" + id, digest, [exclusive c14n])`, which covers the element **including its
   attributes**. That is why the `nextmsh` role attribute on `eb:Messaging` really is signed -
   `MultiHopSignatureCoverageTest` proves it by comparing digests.
2. **A prefixed attribute needs its namespace declared on the element that carries it.** The
   hand-built `wsa:To` and `wsa:Action` elements initially failed signature verification at the
   receiver, while the JAXB-marshalled `ebint:RoutingInput` did not. Cause: `wsu:Id` and the SOAP
   role attribute had no namespace declaration in scope, so the serializer invented one and the
   canonical form no longer matched what was signed. `MultiHopSoapHelper` now declares `wsa`,
   `wsu` and the SOAP namespace explicitly.

## Specifications

* AS4 Profile of ebMS 3.0 v1.0, OASIS Standard, 23 January 2013 -
  <https://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html>
* ebMS 3.0 Part 2 Advanced Features, CS01, 19 May 2011 -
  <https://docs.oasis-open.org/ebxml-msg/ebms/v3.0/part2/201004/cs01/ebms-v3.0-part2-cs01.html>
* RoutingInput XSD -
  <https://docs.oasis-open.org/ebxml-msg/ebms/v3.0/part2/201004/cs01/ebms-multihop-1_0-200902_refactored.xsd>
* WS-Addressing 1.0 Core - <https://www.w3.org/TR/2006/REC-ws-addr-core-20060509/>

The two OASIS Part 2 schemas are shipped in `src/main/resources/external/schemas/multihop/` so
that validation works offline. WS-Addressing comes from `ph-xsds-wsaddr`, `xml.xsd` from
`ph-xsds-xml` and the SOAP envelopes from `phase4-lib`.

## Maven

```xml
<dependency>
  <groupId>com.helger.phase4</groupId>
  <artifactId>phase4-multihop</artifactId>
  <version>x.y.z</version>
</dependency>
```
