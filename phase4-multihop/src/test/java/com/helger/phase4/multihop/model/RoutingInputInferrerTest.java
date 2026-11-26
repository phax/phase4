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
import static org.junit.Assert.assertSame;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.phase4.CAS4;
import com.helger.phase4.ebms3header.Ebms3AgreementRef;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.CAS4MultiHop;

/**
 * Test class for {@link RoutingInputInferrer}.<br>
 * Covers R6 (the inferred reverse RoutingInput) plus the design decisions D1 and D4.
 *
 * @author Philip Helger
 */
public final class RoutingInputInferrerTest
{
  @NonNull
  private static Ebms3UserMessage _createUserMessage (@Nullable final String sMPC)
  {
    final Ebms3UserMessage ret = new Ebms3UserMessage ();
    ret.setMessageInfo (MessageHelperMethods.createEbms3MessageInfo ());
    ret.setPartyInfo (MessageHelperMethods.createEbms3PartyInfo ("roleSender",
                                                                  "partySender",
                                                                  "roleReceiver",
                                                                  "partyReceiver"));
    ret.setCollaborationInfo (MessageHelperMethods.createEbms3CollaborationInfo ("thePModeID",
                                                                                 "theAgreement",
                                                                                 null,
                                                                                 "theServiceType",
                                                                                 "theService",
                                                                                 "theAction",
                                                                                 "theConversationID"));
    ret.setMessageProperties (MessageHelperMethods.createEbms3MessageProperties ());
    ret.setMpc (sMPC);
    return ret;
  }

  /**
   * R6 - From and To are swapped including their Role.
   */
  @Test
  public void testPartyInfoIsSwapped ()
  {
    final Ebms3UserMessage aUM = _createUserMessage (null);
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (aUM,
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());

    final var aPI = aRI.getUserMessage ().getPartyInfo ();
    assertNotNull (aPI);
    assertEquals ("partyReceiver", aPI.getFrom ().getPartyIdAtIndex (0).getValue ());
    assertEquals ("roleReceiver", aPI.getFrom ().getRole ());
    assertEquals ("partySender", aPI.getTo ().getPartyIdAtIndex (0).getValue ());
    assertEquals ("roleSender", aPI.getTo ().getRole ());
  }

  /**
   * R6 - with no MPC on the User Message the default MPC is used as the base.
   */
  @Test
  public void testMPCAbsentUsesDefault ()
  {
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (_createUserMessage (null),
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());
    assertEquals (CAS4.DEFAULT_MPC_ID + CAS4MultiHop.MPC_SUFFIX_RECEIPT, aRI.getUserMessage ().getMpc ());
  }

  /**
   * R6 - with an MPC on the User Message that one is used as the base.
   */
  @Test
  public void testMPCPresentIsUsed ()
  {
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (_createUserMessage ("urn:my:mpc"),
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());
    assertEquals ("urn:my:mpc" + CAS4MultiHop.MPC_SUFFIX_RECEIPT, aRI.getUserMessage ().getMpc ());
  }

  /**
   * R6 - the Error MPC suffix is ".error". The suffixes are normative and NOT configurable.
   */
  @Test
  public void testErrorUsesErrorMPCSuffix ()
  {
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (_createUserMessage ("urn:my:mpc"),
                                                                        EAS4MessageType.ERROR_MESSAGE,
                                                                        new AS4MultiHopConfig ());
    assertEquals ("urn:my:mpc" + CAS4MultiHop.MPC_SUFFIX_ERROR, aRI.getUserMessage ().getMpc ());
  }

  /**
   * D1 - the Action suffix defaults to ".response" and is configurable per signal type.
   */
  @Test
  public void testActionSuffix ()
  {
    final AS4MultiHopConfig aCfg = new AS4MultiHopConfig ();

    // Default
    assertEquals ("theAction" + CAS4MultiHop.DEFAULT_ACTION_SUFFIX,
                  RoutingInputInferrer.inferReverse (_createUserMessage (null), EAS4MessageType.RECEIPT, aCfg)
                                      .getUserMessage ()
                                      .getCollaborationInfo ()
                                      .getAction ());

    // Configured to the alternative interpretation
    aCfg.setReverseActionSuffixReceipt (CAS4MultiHop.MPC_SUFFIX_RECEIPT);
    aCfg.setReverseActionSuffixError (CAS4MultiHop.MPC_SUFFIX_ERROR);

    assertEquals ("theAction.receipt",
                  RoutingInputInferrer.inferReverse (_createUserMessage (null), EAS4MessageType.RECEIPT, aCfg)
                                      .getUserMessage ()
                                      .getCollaborationInfo ()
                                      .getAction ());
    assertEquals ("theAction.error",
                  RoutingInputInferrer.inferReverse (_createUserMessage (null), EAS4MessageType.ERROR_MESSAGE, aCfg)
                                      .getUserMessage ()
                                      .getCollaborationInfo ()
                                      .getAction ());
  }

  /**
   * D4 - AgreementRef including its pmode and type attributes, Service including its type, and the
   * ConversationId are copied.
   */
  @Test
  public void testCollaborationInfoIsCopied ()
  {
    final Ebms3UserMessage aUM = _createUserMessage (null);
    aUM.getCollaborationInfo ().getAgreementRef ().setType ("theAgreementType");

    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (aUM,
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());
    final var aCI = aRI.getUserMessage ().getCollaborationInfo ();
    assertNotNull (aCI);

    final Ebms3AgreementRef aAR = aCI.getAgreementRef ();
    assertNotNull (aAR);
    assertEquals ("theAgreement", aAR.getValue ());
    assertEquals ("thePModeID", aAR.getPmode ());
    assertEquals ("theAgreementType", aAR.getType ());

    assertEquals ("theService", aCI.getService ().getValue ());
    assertEquals ("theServiceType", aCI.getService ().getType ());
    assertEquals ("theConversationID", aCI.getConversationId ());
  }

  /**
   * D4 - MessageInfo, MessageProperties and PayloadInfo are omitted.
   */
  @Test
  public void testOmittedElements ()
  {
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (_createUserMessage (null),
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());
    assertNull (aRI.getUserMessage ().getMessageInfo ());
    assertNull (aRI.getUserMessage ().getMessageProperties ());
    assertNull (aRI.getUserMessage ().getPayloadInfo ());
  }

  /**
   * The source User Message must never be modified.
   */
  @Test
  public void testSourceIsNotModified ()
  {
    final Ebms3UserMessage aUM = _createUserMessage ("urn:my:mpc");
    final var aSrcPartyInfo = aUM.getPartyInfo ();
    final var aSrcFrom = aSrcPartyInfo.getFrom ();

    RoutingInputInferrer.inferReverse (aUM, EAS4MessageType.RECEIPT, new AS4MultiHopConfig ());

    assertSame (aSrcPartyInfo, aUM.getPartyInfo ());
    assertSame (aSrcFrom, aUM.getPartyInfo ().getFrom ());
    assertEquals ("partySender", aUM.getPartyInfo ().getFrom ().getPartyIdAtIndex (0).getValue ());
    assertEquals ("roleSender", aUM.getPartyInfo ().getFrom ().getRole ());
    assertEquals ("theAction", aUM.getCollaborationInfo ().getAction ());
    assertEquals ("urn:my:mpc", aUM.getMpc ());
  }

  /**
   * The inferred RoutingInput must validate against the OASIS multi-hop XSD.
   */
  @Test
  public void testInferredIsSchemaValid ()
  {
    final MultiHopRoutingInput aRI = RoutingInputInferrer.inferReverse (_createUserMessage (null),
                                                                        EAS4MessageType.RECEIPT,
                                                                        new AS4MultiHopConfig ());
    aRI.setStandardAttributes (ESoapVersion.SOAP_12, CAS4MultiHop.createID ("x"));

    assertNotNull ("The inferred RoutingInput does not validate against the OASIS multi-hop XSD",
                   MultiHopRoutingInputMarshaller.createWithValidation ().getAsDocument (aRI));
  }
}
