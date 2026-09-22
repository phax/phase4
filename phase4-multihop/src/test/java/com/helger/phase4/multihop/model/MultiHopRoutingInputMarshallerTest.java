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
package com.helger.phase4.multihop.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3CollaborationInfo;
import com.helger.phase4.ebms3header.Ebms3MessageInfo;
import com.helger.phase4.ebms3header.Ebms3PartyInfo;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Test class for {@link MultiHopRoutingInputMarshaller}.<br>
 * Covers R7 - the exact shape of the <code>ebint:RoutingInput</code> element.
 *
 * @author Philip Helger
 */
public final class MultiHopRoutingInputMarshallerTest
{
  @NonNull
  private static MultiHopRoutingInput _createValid ()
  {
    final MultiHopRoutingUserMessage aUM = new MultiHopRoutingUserMessage ();
    aUM.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("fromRole", "fromParty", "toRole", "toParty"));
    aUM.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo ("pmodeid",
                                                                                 "urn:as4:agreement",
                                                                                 null,
                                                                                 "svcType",
                                                                                 "svc",
                                                                                 "action.response",
                                                                                 "convid"));
    aUM.setMpc (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_RECEIPT);

    final MultiHopRoutingInput ret = new MultiHopRoutingInput ();
    ret.setUserMessage (aUM);
    ret.setStandardAttributes (ESoapVersion.SOAP_12, CAS4MultiHop.createID ("test"));
    return ret;
  }

  /**
   * R7 - a complete RoutingInput must validate against the OASIS multi-hop XSD, fully offline, and
   * it must survive a DOM round trip.
   */
  @Test
  public void testRoundTripWithValidation ()
  {
    final MultiHopRoutingInput aSrc = _createValid ();

    final MultiHopRoutingInputMarshaller aMarshaller = MultiHopRoutingInputMarshaller.createWithValidation ();
    final Document aDoc = aMarshaller.getAsDocument (aSrc);
    assertNotNull ("The RoutingInput did not validate against the OASIS multi-hop XSD", aDoc);

    final Element aRoot = aDoc.getDocumentElement ();
    assertEquals (CAS4MultiHop.EBINT_NS, aRoot.getNamespaceURI ());
    assertEquals ("RoutingInput", aRoot.getLocalName ());

    // R7 - the mandatory attributes
    assertEquals ("true", aRoot.getAttributeNS (CAS4MultiHop.WSA_NS, "IsReferenceParameter"));
    assertEquals (CAS4MultiHop.NEXT_MSH_ROLE,
                  aRoot.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "role"));
    assertEquals ("true", aRoot.getAttributeNS (ESoapVersion.SOAP_12.getNamespaceURI (), "mustUnderstand"));
    assertTrue (aRoot.getAttributeNS (CAS4.WSU_NS, "Id").startsWith (CAS4MultiHop.ID_PREFIX));

    // Read it back
    final MultiHopRoutingInput aRead = new MultiHopRoutingInputMarshaller ().read (aDoc);
    assertNotNull (aRead);
    assertNotNull (aRead.getUserMessage ());
    assertEquals (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_RECEIPT, aRead.getUserMessage ().getMpc ());
    assertEquals ("action.response", aRead.getUserMessage ().getCollaborationInfo ().getAction ());
    assertEquals ("fromParty",
                  aRead.getUserMessage ().getPartyInfo ().getFrom ().getPartyIdAtIndex (0).getValue ());
    assertEquals ("toParty", aRead.getUserMessage ().getPartyInfo ().getTo ().getPartyIdAtIndex (0).getValue ());
  }

  /**
   * R7 / ebMS3 Part 2 section 2.5.5 NOTE - MessageInfo is optional.
   */
  @Test
  public void testMessageInfoIsOptional ()
  {
    final MultiHopRoutingInput aSrc = _createValid ();
    assertNull (aSrc.getUserMessage ().getMessageInfo ());

    assertNotNull ("A RoutingInput without MessageInfo must be valid",
                   MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aSrc));

    // Adding it must still be valid
    final Ebms3MessageInfo aMI = MessageHelperMethods.createEbms3MessageInfo ();
    aSrc.getUserMessage ().setMessageInfo (aMI);
    assertNotNull ("A RoutingInput with MessageInfo must be valid",
                   MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aSrc));
  }

  /**
   * R7 - PartyInfo is mandatory.
   */
  @Test
  public void testPartyInfoIsMandatory ()
  {
    final MultiHopRoutingInput aSrc = _createValid ();
    aSrc.getUserMessage ().setPartyInfo ((Ebms3PartyInfo) null);

    assertNull ("A RoutingInput without PartyInfo must NOT validate",
                MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aSrc));
  }

  /**
   * R7 - CollaborationInfo is mandatory.
   */
  @Test
  public void testCollaborationInfoIsMandatory ()
  {
    final MultiHopRoutingInput aSrc = _createValid ();
    aSrc.getUserMessage ().setCollaborationInfo ((Ebms3CollaborationInfo) null);

    assertNull ("A RoutingInput without CollaborationInfo must NOT validate",
                MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aSrc));
  }

  /**
   * R7 - SOAP 1.1 uses actor instead of role, and mustUnderstand="1".
   */
  @Test
  public void testSoap11UsesActor ()
  {
    final MultiHopRoutingInput aSrc = _createValid ();
    aSrc.getOtherAttributes ().clear ();
    aSrc.setStandardAttributes (ESoapVersion.SOAP_11, CAS4MultiHop.createID ("s11"));

    final Document aDoc = MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aSrc);
    assertNotNull (aDoc);

    final Element aRoot = aDoc.getDocumentElement ();
    assertEquals (CAS4MultiHop.NEXT_MSH_ROLE,
                  aRoot.getAttributeNS (ESoapVersion.SOAP_11.getNamespaceURI (), "actor"));
    // Correction 5 - SOAP 1.1 uses "1", not "true"
    assertEquals ("1", aRoot.getAttributeNS (ESoapVersion.SOAP_11.getNamespaceURI (), "mustUnderstand"));
  }
}
