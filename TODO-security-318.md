# Open items from the security audit #318

Tracking file for the remaining work of [issue #318](https://github.com/phax/phase4/issues/318).
The full analysis and the phase plan are in [this comment](https://github.com/phax/phase4/issues/318#issuecomment-5246306305).

Phase A (signature coverage verification, decompression limits, message level limits) is **done** and was
committed with `f591a1bea` - it is part of v4.6.0.

## Phase B - defense in depth and consistency

- [ ] **B1 - Move the security enforcement into the core.**
  The "message must be signed" resp. "message must be encrypted" check is currently duplicated in the
  message processors of the Peppol, DBNAlliance and HR eDelivery profiles - the other 6 profiles have
  no such check at all. It should be derived from the PMode leg security configuration and performed
  centrally in `AS4IncomingHandler`, so that all profiles benefit from it.
  The profile specific checks (BST `DIRECT_REF`, CA checker) stay where they are.
- [ ] **B2 - Real revocation toggle in the core.**
  `SoapHeaderElementProcessorWSS4J:201` contains `if (false) aRequestData.setEnableRevocation (true);`.
  Replace this dead code with a real configuration property (e.g.
  `phase4.incoming.signature.checkrevocation`) for the non-Peppol profiles. Only the Peppol profile
  performs its own revocation check today.
- [ ] **B3 - `AS4CertificateOnlySignatureTrustValidator` for all profiles.**
  It is currently only installed in `Phase4PeppolAS4Servlet:79`. It should become part of the default
  `WSSConfig` created by `WSSConfigManager`, so that no profile accepts a signature that is based on a
  bare public key instead of a certificate.
- [ ] **B4 - Strict timestamp handling.**
  `Phase4PeppolServletMessageProcessorSPI:468` only logs a warning and substitutes the current date
  time, if neither the AS4 `MessageInfo/Timestamp` nor the SBDH `CreationDateTime` is present.
  Add an optional flag to reject such a message instead.
- [ ] **B5 - Security documentation.** This was the second explicit request of the reporter and is
  still open.
    - `SECURITY.md` in the repository root: supported versions and a private disclosure channel.
    - A new Wiki page "Security Hardening" covering truststore setup, revocation configuration, all
      limits introduced in phase A, the duplicate disposal period (`phase4.incoming.duplicatedisposal`,
      default 10 minutes) as *the* replay protection control, logging hygiene, and an explicit warning
      to never deploy the `phase4-test` keystore in production.

## Phase C - v5.0.0 - requires a backwards incompatible API change

- [ ] **C1 - Streaming payload handover.**
  `IPhase4PeppolIncomingSBDHandlerSPI`, `IPhase4DBNAllianceIncomingXHEHandlerSPI` and
  `IPhase4HREDeliveryIncomingSBDHandlerSPI` hand the payload over as a `byte []`, which forces the
  message processors to materialize the whole decompressed payload on the heap
  (see the `// And yes, for very large files, this is not a good idea` comment in
  `Phase4PeppolServletMessageProcessorSPI`).
  Add `IHasInputStream` based methods, deprecate the `byte []` based ones and remove them in v5.0.0.
  The phase A decompression limits bound the damage, but the underlying API problem remains.

## Loose ends from the phase A implementation

- [ ] **Decide on `IAS4IncomingMessageState`.** The two new methods `getAllSignedElements ()` and
  `getAllSignedAttachmentIDs ()` are abstract, which is a source incompatible change for anybody
  implementing the interface outside of phase4 (in-tree there is only the final class
  `AS4IncomingMessageState`). Making them `default` methods returning empty collections would keep
  such implementors compiling and would not change anything in-tree.
- [ ] **No dedicated test for the SOAP Body coverage check.** The checks for the ebMS `Messaging`
  element and for attachments are covered by `UserMessageFailureForgeryTest`. For the SOAP `Body` no
  test could be constructed from the client side, because phase4 signs the Body via a name based
  `WSEncryptionPart` and removing it from the signing parts still left the Body signed. The check
  itself is active in `AS4IncomingHandler`.
- [ ] **Migration note for peppol-commons 12.8.0.** `SMPClientReadOnly.getServiceGroupOrNull` and
  `getServiceMetadataOrNull` now throw `SMPClientSMPUnavailableException` instead of returning `null`,
  if the SMP could not be contacted. This is in the v4.6.0 news entry, but not yet in the
  `4.5.x to 4.6.x` section of the `Migrations` Wiki page. phase4 itself is not affected - the only
  call site (`Phase4HREDeliveryServletMessageProcessorSPI:291`) uses a BDXR1 client and catches
  `Exception` anyway - but users doing Peppol dynamic discovery are.
