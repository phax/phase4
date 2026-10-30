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
package com.helger.phase4.multihop.incoming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.ebms3header.Ebms3Messaging;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4ReceiptMessage;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.crypto.MultiHopSignatureCustomizer;
import com.helger.phase4.multihop.model.MultiHopRoutingInputMarshaller;
import com.helger.phase4.multihop.soap.MultiHopSoapHelper;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.photon.app.mock.PhotonAppWebTestRule;
import com.helger.xml.XMLHelper;

/**
 * Test class for {@link MultiHopResponseSignalCustomizer}.<br>
 * Covers R4 (the three headers), R5 (no change without multi-hop), R8 (role placement) and R9
 * (signature coverage).
 *
 * @author Philip Helger
 */
public final class MultiHopResponseSignalCustomizerTest
{
  @ClassRule
  public static final PhotonAppWebTestRule RULE = new PhotonAppWebTestRule ();

  @NonNull
  private static Ebms3UserMessage _createUserMessage ()
  {
    final Ebms3UserMessage ret = new Ebms3UserMessage ();
    ret.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    ret.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("roleS", "partyS", "roleR", "partyR"));
    ret.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo ("pm",
                                                                                 "agree",
                                                                                 null,
                                                                                 "st",
                                                                                 "s",
                                                                                 "theAction",
                                                                                 "cid"));
    ret.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    return ret;
  }

  @NonNull
  private static AS4IncomingMessageState _createState (@NonNull final AS4ResourceHelper aResHelper,
                                                       final boolean bMultiHop)
  {
    final AS4IncomingMessageState ret = new AS4IncomingMessageState (ESoapVersion.SOAP_12, aResHelper, Locale.US);

    final Ebms3Messaging aMessaging = new Ebms3Messaging ();
    aMessaging.addUserMessage (_createUserMessage ());
    if (bMultiHop)
      MultiHopSoapHelper.setNextMSHRole (aMessaging, ESoapVersion.SOAP_12);
    ret.setMessaging (aMessaging);
    return ret;
  }

  @NonNull
  private static Document _createReceiptDoc ()
  {
    return AS4ReceiptMessage.create (ESoapVersion.SOAP_12,
                                     MessageHelperMethods.createRandomMessageID (),
                                     null,
                                     null,
                                     false,
                                     MessageHelperMethods.createRandomMessageID ())
                            .getAsSoapDocument ();
  }

  @NonNull
  private static Element _getHeaderChild (@NonNull final Document aDoc,
                                          @NonNull final String sNamespace,
                                          @NonNull final String sLocalName)
  {
    final Element aHeader = XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                                  ESoapVersion.SOAP_12.getNamespaceURI (),
                                                                  ESoapVersion.SOAP_12.getHeaderElementName ());
    assertNotNull (aHeader);
    final Element ret = XMLHelper.getFirstChildElementOfName (aHeader, sNamespace, sLocalName);
    assertNotNull ("No " + sLocalName + " element found in the SOAP header", ret);
    return ret;
  }

  /**
   * R4 / R7 / R8 - a Receipt answering a message that arrived through an I-Cloud carries wsa:To,
   * wsa:Action and ebint:RoutingInput, with the role only on wsa:To and mustUnderstand on neither
   * of the two WS-A headers.
   */
  @Test
  public void testReceiptGetsAllThreeHeaders () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final Document aDoc = _createReceiptDoc ();
      final AS4SigningParams aSigningParams = new AS4SigningParams ();

      new MultiHopResponseSignalCustomizer (new AS4MultiHopConfig ()).customizeResponseSignal (_createState (aResHelper,
                                                                                                             true),
                                                                                               EAS4MessageType.RECEIPT,
                                                                                               ESoapVersion.SOAP_12,
                                                                                               aDoc,
                                                                                               aSigningParams);

      // 1. wsa:To - with role, without mustUnderstand (R8)
      final Element aTo = _getHeaderChild (aDoc, CAS4MultiHop.WSA_NS, "To");
      assertEquals (CAS4MultiHop.ICLOUD_URI, aTo.getTextContent ());
      assertEquals (CAS4MultiHop.NEXT_MSH_ROLE,
                    aTo.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "role"));
      assertEquals ("", aTo.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "mustUnderstand"));
      assertTrue (aTo.getAttributeNS (CAS4.WSU_NS, "Id").startsWith (CAS4MultiHop.ID_PREFIX));

      // 2. wsa:Action - no role, no mustUnderstand (R8)
      final Element aAction = _getHeaderChild (aDoc, CAS4MultiHop.WSA_NS, "Action");
      assertEquals (CAS4MultiHop.WSA_ACTION_ONEWAY_RECEIPT, aAction.getTextContent ());
      assertEquals ("", aAction.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "role"));
      assertEquals ("", aAction.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "mustUnderstand"));

      // 3. ebint:RoutingInput - with role AND mustUnderstand (R7)
      final Element aRI = _getHeaderChild (aDoc, CAS4MultiHop.EBINT_NS, "RoutingInput");
      assertEquals (CAS4MultiHop.NEXT_MSH_ROLE,
                    aRI.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "role"));
      assertEquals ("true", aRI.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "mustUnderstand"));
      assertEquals ("true", aRI.getAttributeNS (CAS4MultiHop.WSA_NS, "IsReferenceParameter"));

      // It must be schema valid
      assertNotNull ("The added RoutingInput does not validate against the OASIS multi-hop XSD",
                     MultiHopRoutingInputMarshaller.createWithValidation ()
                                                   .read (aRI));

      // R6 - the MPC got the .receipt suffix and the parties were swapped
      final var aRead = new MultiHopRoutingInputMarshaller ().read (aRI);
      assertNotNull (aRead);
      assertEquals (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_RECEIPT, aRead.getUserMessage ().getMpc ());
      assertEquals ("partyR", aRead.getUserMessage ().getPartyInfo ().getFrom ().getPartyIdAtIndex (0).getValue ());
      assertEquals ("partyS", aRead.getUserMessage ().getPartyInfo ().getTo ().getPartyIdAtIndex (0).getValue ());

      // R9 - all three are registered for signing
      assertTrue ("The added headers are not registered for signing",
                  aSigningParams.getWSSecSignatureCustomizer () instanceof MultiHopSignatureCustomizer);
      assertEquals (3,
                    ((MultiHopSignatureCustomizer) aSigningParams.getWSSecSignatureCustomizer ()).getAllIDsToSign ()
                                                                                                 .size ());
    }
  }

  /**
   * R4 - an Error uses the oneWay.error action and the .error MPC suffix.
   */
  @Test
  public void testErrorUsesErrorActionAndMPC () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final Document aDoc = _createReceiptDoc ();

      new MultiHopResponseSignalCustomizer (new AS4MultiHopConfig ()).customizeResponseSignal (_createState (aResHelper,
                                                                                                             true),
                                                                                               EAS4MessageType.ERROR_MESSAGE,
                                                                                               ESoapVersion.SOAP_12,
                                                                                               aDoc,
                                                                                               null);

      assertEquals (CAS4MultiHop.WSA_ACTION_ONEWAY_ERROR,
                    _getHeaderChild (aDoc, CAS4MultiHop.WSA_NS, "Action").getTextContent ());

      final var aRead = new MultiHopRoutingInputMarshaller ().read (_getHeaderChild (aDoc,
                                                                                      CAS4MultiHop.EBINT_NS,
                                                                                      "RoutingInput"));
      assertNotNull (aRead);
      assertEquals (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_ERROR, aRead.getUserMessage ().getMpc ());
    }
  }

  /**
   * R5 - a message that did NOT arrive through an I-Cloud must be answered exactly as before:
   * no header is added and the signing parameters stay untouched.
   */
  @Test
  public void testNoMultiHopMeansNoChange () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final Document aDoc = _createReceiptDoc ();
      final int nHeaderChildrenBefore = XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                                              ESoapVersion.SOAP_12.getNamespaceURI (),
                                                                              ESoapVersion.SOAP_12.getHeaderElementName ())
                                                 .getChildNodes ()
                                                 .getLength ();

      final AS4SigningParams aSigningParams = new AS4SigningParams ();
      new MultiHopResponseSignalCustomizer (new AS4MultiHopConfig ()).customizeResponseSignal (_createState (aResHelper,
                                                                                                             false),
                                                                                               EAS4MessageType.RECEIPT,
                                                                                               ESoapVersion.SOAP_12,
                                                                                               aDoc,
                                                                                               aSigningParams);

      assertEquals ("A SOAP header element was added although the message did not arrive via an I-Cloud",
                    nHeaderChildrenBefore,
                    XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                          ESoapVersion.SOAP_12.getNamespaceURI (),
                                                          ESoapVersion.SOAP_12.getHeaderElementName ())
                             .getChildNodes ()
                             .getLength ());
      assertNull ("The signing parameters were modified", aSigningParams.getWSSecSignatureCustomizer ());
    }
  }

  /**
   * D2 - signing of the added headers can be switched off.
   */
  @Test
  public void testSignAddressingHeadersCanBeDisabled () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ().setSignAddressingHeaders (false);
      final AS4SigningParams aSigningParams = new AS4SigningParams ();

      new MultiHopResponseSignalCustomizer (aCfg).customizeResponseSignal (_createState (aResHelper, true),
                                                                            EAS4MessageType.RECEIPT,
                                                                            ESoapVersion.SOAP_12,
                                                                            _createReceiptDoc (),
                                                                            aSigningParams);

      assertNull (aSigningParams.getWSSecSignatureCustomizer ());
    }
  }

  /**
   * A signal message that answers nothing (no User Message in the state) must be left alone.
   */
  @Test
  public void testNoUserMessageMeansNoChange () throws Exception
  {
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4IncomingMessageState aState = new AS4IncomingMessageState (ESoapVersion.SOAP_12,
                                                                          aResHelper,
                                                                          Locale.US);
      final Document aDoc = _createReceiptDoc ();
      final AS4SigningParams aSigningParams = new AS4SigningParams ();

      new MultiHopResponseSignalCustomizer (new AS4MultiHopConfig ()).customizeResponseSignal (aState,
                                                                                               EAS4MessageType.RECEIPT,
                                                                                               ESoapVersion.SOAP_12,
                                                                                               aDoc,
                                                                                               aSigningParams);

      assertNull (aSigningParams.getWSSecSignatureCustomizer ());
      assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());
    }
  }
}
