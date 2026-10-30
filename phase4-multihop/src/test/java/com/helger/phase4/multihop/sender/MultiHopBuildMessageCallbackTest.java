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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.phase4.CAS4;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.ebms3header.Ebms3PullRequest;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4PullRequestMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.CAS4MultiHop;
import com.helger.phase4.multihop.soap.MultiHopSoapHelper;
import com.helger.xml.XMLHelper;

/**
 * Test class for {@link MultiHopBuildMessageCallback}.<br>
 * Covers R1 (the nextmsh role/actor attribute) and R2 (Pull Requests never get it).
 *
 * @author Philip Helger
 */
public final class MultiHopBuildMessageCallbackTest
{
  @NonNull
  private static AS4UserMessage _createUserMessage (@NonNull final ESoapVersion eSoapVersion)
  {
    final Ebms3UserMessage aEbms3 = new Ebms3UserMessage ();
    aEbms3.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    aEbms3.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("r1", "p1", "r2", "p2"));
    aEbms3.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo ("pm",
                                                                                    "agree",
                                                                                    null,
                                                                                    "st",
                                                                                    "s",
                                                                                    "a",
                                                                                    "cid"));
    aEbms3.setPayloadInfo (MessageHelperMethods.createEbms3PayloadInfo (false, new CommonsArrayList <> ()));
    return AS4UserMessage.create (eSoapVersion, aEbms3);
  }

  @NonNull
  private static Element _getMessagingElement (@NonNull final Document aDoc,
                                               @NonNull final ESoapVersion eSoapVersion)
  {
    final Element aHeader = XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                                  eSoapVersion.getNamespaceURI (),
                                                                  eSoapVersion.getHeaderElementName ());
    assertNotNull (aHeader);
    final Element aMessaging = XMLHelper.getFirstChildElementOfName (aHeader, CAS4.EBMS_NS, "Messaging");
    assertNotNull (aMessaging);
    return aMessaging;
  }

  @NonNull
  private static String _getRoleOrActor (@NonNull final Document aDoc, @NonNull final ESoapVersion eSoapVersion)
  {
    return _getMessagingElement (aDoc, eSoapVersion).getAttributeNS (eSoapVersion.getNamespaceURI (),
                                                                     MultiHopSoapHelper.getRoleOrActorQName (eSoapVersion)
                                                                                       .getLocalPart ());
  }

  /**
   * R1 - SOAP 1.2 uses the "role" attribute.
   */
  @Test
  public void testSoap12Role ()
  {
    final AS4UserMessage aMsg = _createUserMessage (ESoapVersion.SOAP_12);
    new MultiHopBuildMessageCallback (null, true).onAS4Message (aMsg);

    assertEquals (CAS4MultiHop.NEXT_MSH_ROLE, _getRoleOrActor (aMsg.getAsSoapDocument (null), ESoapVersion.SOAP_12));
  }

  /**
   * R1 - SOAP 1.1 uses the "actor" attribute instead.
   */
  @Test
  public void testSoap11Actor ()
  {
    final AS4UserMessage aMsg = _createUserMessage (ESoapVersion.SOAP_11);
    new MultiHopBuildMessageCallback (null, true).onAS4Message (aMsg);

    assertEquals (CAS4MultiHop.NEXT_MSH_ROLE, _getRoleOrActor (aMsg.getAsSoapDocument (null), ESoapVersion.SOAP_11));
  }

  /**
   * R1 - with the callback inactive the message must be exactly as before.
   */
  @Test
  public void testInactiveAddsNothing ()
  {
    final AS4UserMessage aMsg = _createUserMessage (ESoapVersion.SOAP_12);
    new MultiHopBuildMessageCallback (null, false).onAS4Message (aMsg);

    assertEquals ("", _getRoleOrActor (aMsg.getAsSoapDocument (null), ESoapVersion.SOAP_12));
  }

  /**
   * R2 - a Pull Request must never get the role attribute, even with the callback active.
   */
  @Test
  public void testPullRequestNeverGetsTheRole ()
  {
    final Ebms3SignalMessage aSignal = new Ebms3SignalMessage ();
    aSignal.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    final Ebms3PullRequest aPR = new Ebms3PullRequest ();
    aPR.setMpc (CAS4.DEFAULT_MPC_ID);
    aSignal.setPullRequest (aPR);

    final AS4PullRequestMessage aMsg = new AS4PullRequestMessage (ESoapVersion.SOAP_12, aSignal);
    new MultiHopBuildMessageCallback (null, true).onAS4Message (aMsg);

    final Document aDoc = aMsg.getAsSoapDocument ();
    assertEquals ("A Pull Request must never carry the nextmsh role",
                  "",
                  _getRoleOrActor (aDoc, ESoapVersion.SOAP_12));

    // R2 - and it must certainly not carry a RoutingInput
    assertEquals (0, aDoc.getElementsByTagNameNS (CAS4MultiHop.EBINT_NS, "RoutingInput").getLength ());
  }

  /**
   * D6 - the detection used on the responding side must recognise what the sender produced.
   */
  @Test
  public void testRoundTripDetection ()
  {
    for (final ESoapVersion eSoapVersion : new ESoapVersion [] { ESoapVersion.SOAP_11, ESoapVersion.SOAP_12 })
    {
      final AS4UserMessage aActive = _createUserMessage (eSoapVersion);
      new MultiHopBuildMessageCallback (null, true).onAS4Message (aActive);
      assertTrue ("Not detected for " + eSoapVersion,
                  MultiHopSoapHelper.isTargetedToNextMSH (aActive.getMessaging (), eSoapVersion));

      final AS4UserMessage aInactive = _createUserMessage (eSoapVersion);
      new MultiHopBuildMessageCallback (null, false).onAS4Message (aInactive);
      assertFalse ("Falsely detected for " + eSoapVersion,
                   MultiHopSoapHelper.isTargetedToNextMSH (aInactive.getMessaging (), eSoapVersion));
    }
  }

  /**
   * The delegate must always be invoked, active or not.
   */
  @Test
  public void testDelegateIsInvoked ()
  {
    final boolean [] aCalled = new boolean [] { false };
    final IAS4ClientBuildMessageCallback aDelegate = new IAS4ClientBuildMessageCallback ()
    {
      @Override
      public void onAS4Message (@NonNull final AbstractAS4Message <?> aMsg)
      {
        aCalled[0] = true;
      }
    };
    final MultiHopBuildMessageCallback aCB = new MultiHopBuildMessageCallback (aDelegate, false);
    aCB.onAS4Message (_createUserMessage (ESoapVersion.SOAP_12));
    assertTrue ("The delegate was not invoked", aCalled[0]);
  }
}
