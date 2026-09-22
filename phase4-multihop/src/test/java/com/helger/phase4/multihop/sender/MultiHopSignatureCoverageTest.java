/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.multihop.sender;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;

import org.apache.wss4j.dom.WSConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.xml.XMLHelper;

/**
 * Measures whether the <code>nextmsh</code> role attribute that R1 puts on
 * <code>eb:Messaging</code> is actually covered by the XML signature.
 * <p>
 * This settles an open question of the design phase. WSS4J 4.0.1 ignores the "Content" /
 * "Element" modifier of a {@code WSEncryptionPart} for <b>signature</b> parts - see
 * {@code WSSecSignatureBase.addReferencesToSign}, which always creates
 * {@code newReference("#" + id, digest, [exclusive c14n])}. A same document reference with
 * exclusive canonicalization covers the element node <b>including all of its attributes</b>.
 * </p>
 * <p>
 * The test proves it without re-implementing signature verification: the same message is signed
 * twice, once with and once without the role attribute, and the DigestValue of the
 * <code>eb:Messaging</code> reference must differ.
 * </p>
 *
 * @author Philip Helger
 */
public final class MultiHopSignatureCoverageTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  @NonNull
  private static AS4UserMessage _createUserMessage ()
  {
    final Ebms3UserMessage aEbms3 = new Ebms3UserMessage ();
    // Use fixed values everywhere, so that the only difference between the two
    // signed documents is the role attribute
    aEbms3.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ("const-message-id", null));
    aEbms3.getMessageInfo ().setTimestamp (XMLOffsetDateTime.of (2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));
    aEbms3.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("r1", "p1", "r2", "p2"));
    aEbms3.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo ("pm",
                                                                                    "agree",
                                                                                    null,
                                                                                    "st",
                                                                                    "s",
                                                                                    "a",
                                                                                    "const-conversation-id"));
    aEbms3.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    return AS4UserMessage.create (ESoapVersion.SOAP_12, aEbms3);
  }

  @Nullable
  private static String _getMessagingDigestValue (@NonNull final Document aSignedDoc,
                                                  @NonNull final String sMessagingID)
  {
    final NodeList aRefs = aSignedDoc.getElementsByTagNameNS (WSConstants.SIG_NS, "Reference");
    for (int i = 0; i < aRefs.getLength (); ++i)
    {
      final Element aRef = (Element) aRefs.item (i);
      if (("#" + sMessagingID).equals (aRef.getAttribute ("URI")))
      {
        final Element aDigest = XMLHelper.getFirstChildElementOfName (aRef, WSConstants.SIG_NS, "DigestValue");
        return aDigest == null ? null : aDigest.getTextContent ();
      }
    }
    return null;
  }

  @NonNull
  private static Document _sign (@NonNull final AS4UserMessage aMsg,
                                 @NonNull final AS4ResourceHelper aResHelper) throws Exception
  {
    return AS4Signer.createSignedMessage (AS4CryptoFactoryConfiguration.getDefaultInstance (),
                                          aMsg.getAsSoapDocument (null),
                                          ESoapVersion.SOAP_12,
                                          aMsg.getMessagingID (),
                                          null,
                                          aResHelper,
                                          false,
                                          AS4SigningParams.createDefault ());
  }

  /**
   * R1 - the role attribute is inside the signed content of eb:Messaging.
   */
  @Test
  public void testRoleAttributeIsCoveredByTheSignature () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      // (1) Without the role attribute
      final AS4UserMessage aPlain = _createUserMessage ();
      final Document aPlainSigned = _sign (aPlain, aResHelper);
      final String sPlainDigest = _getMessagingDigestValue (aPlainSigned, aPlain.getMessagingID ());
      assertNotNull ("No ds:Reference to eb:Messaging found in the unmodified message", sPlainDigest);

      // (2) With the role attribute, everything else identical
      final AS4UserMessage aMultiHop = _createUserMessage ();
      new MultiHopBuildMessageCallback (null, true).onAS4Message (aMultiHop);
      final Document aMultiHopSigned = _sign (aMultiHop, aResHelper);
      final String sMultiHopDigest = _getMessagingDigestValue (aMultiHopSigned, aMultiHop.getMessagingID ());
      assertNotNull ("No ds:Reference to eb:Messaging found in the multi-hop message", sMultiHopDigest);

      // If the digests differ, the role attribute is part of the signed content
      assertNotEquals ("The nextmsh role attribute is NOT covered by the signature - " +
                       "the eb:Messaging digest did not change when it was added",
                       sPlainDigest,
                       sMultiHopDigest);
    }
  }

  /**
   * Sanity check for the test method itself: signing the very same message twice must produce the
   * very same eb:Messaging digest, otherwise the assertion above would be meaningless.
   */
  @Test
  public void testDigestIsStableForIdenticalInput () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4UserMessage aMsg1 = _createUserMessage ();
      final AS4UserMessage aMsg2 = _createUserMessage ();

      // The messaging ID is random per instance - align it so the documents really are identical
      assertTrue ("Unexpected messaging ID format", aMsg1.getMessagingID ().length () > 0);

      final String sDigest1 = _getMessagingDigestValue (_sign (aMsg1, aResHelper), aMsg1.getMessagingID ());
      final String sDigest2 = _getMessagingDigestValue (_sign (aMsg2, aResHelper), aMsg2.getMessagingID ());
      assertNotNull (sDigest1);
      assertNotNull (sDigest2);

      // The wsu:Id differs between the two, so the digests differ as well. That is expected -
      // this test only documents that the digest really is content sensitive.
      assertNotEquals (sDigest1, sDigest2);
    }
  }
}
