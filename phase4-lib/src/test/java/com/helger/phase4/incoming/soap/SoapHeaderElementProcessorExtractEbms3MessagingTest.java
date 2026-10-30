/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.incoming.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.base.state.ESuccess;
import com.helger.base.system.SystemProperties;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phase4.AS4TestRule;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.AS4IncomingMessageState;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.error.EEbmsError;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link SoapHeaderElementProcessorExtractEbms3Messaging}.
 *
 * @author Philip Helger
 */
public final class SoapHeaderElementProcessorExtractEbms3MessagingTest
{
  private static final String NS_EBMS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

  @Rule
  public final TestRule m_aTestRule = new AS4TestRule ();

  @Before
  public void enableEnforcement ()
  {
    SystemProperties.setPropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_STRICT_MESSAGE_IDS, "true");
  }

  @After
  public void resetEnforcement ()
  {
    SystemProperties.removePropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_STRICT_MESSAGE_IDS);
  }

  @NonNull
  private static Document _readUserMessage (@NonNull final String sMessageID, @Nullable final String sRefToMessageID)
  {
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("external/soap11test/MessageInfoIDWhitespace.xml"));
    assertNotNull (aDoc);
    final Element aMessageID = (Element) aDoc.getElementsByTagNameNS (NS_EBMS, "MessageId").item (0);
    aMessageID.setTextContent (sMessageID);
    if (sRefToMessageID != null)
    {
      final Element aRefToMessageID = aDoc.createElementNS (NS_EBMS, "eb:RefToMessageId");
      aRefToMessageID.setTextContent (sRefToMessageID);
      aMessageID.getParentNode ().appendChild (aRefToMessageID);
    }
    return aDoc;
  }

  private static boolean _containsInvalidHeaderError (@NonNull final AS4ErrorList aErrorList)
  {
    return aErrorList.getAllMapped (x -> x.getEbmsError ().getErrorCode ())
                     .contains (EEbmsError.EBMS_INVALID_HEADER.getErrorCode ());
  }

  @NonNull
  private static AS4ErrorList _process (@NonNull final Document aDoc,
                                        @NonNull final EAS4MessageMode eMode,
                                        @Nullable final String sExpectedMessageID) throws Exception
  {
    final SoapHeaderElementProcessorExtractEbms3Messaging aProcessor = new SoapHeaderElementProcessorExtractEbms3Messaging (new AS4DefaultPModeResolver (null),
                                                                                                                            null,
                                                                                                                            new AS4IncomingReceiverConfiguration ());
    final Element aMessaging = (Element) aDoc.getElementsByTagNameNS (NS_EBMS, "Messaging").item (0);
    final AS4ErrorList aErrorList = new AS4ErrorList ();
    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4IncomingMessageState aState = new AS4IncomingMessageState (ESoapVersion.SOAP_11,
                                                                          aResHelper,
                                                                          Locale.US);
      aState.setMessageMode (eMode);
      final ESuccess eSuccess = aProcessor.processHeaderElement (aDoc,
                                                                 aMessaging,
                                                                 new CommonsArrayList <> (),
                                                                 aState,
                                                                 aErrorList);
      if (_containsInvalidHeaderError (aErrorList))
        assertTrue (eSuccess.isFailure ());
      assertEquals (sExpectedMessageID, aState.getMessageID ());
    }
    return aErrorList;
  }

  @Test
  public void testPushedLeg1InvalidMessageIDIsRejected () throws Exception
  {
    for (final String sMessageID : new String [] { "   ", "abc", "<a@b>", "a b@c" })
    {
      final AS4ErrorList aErrorList = _process (_readUserMessage (sMessageID, null),
                                                EAS4MessageMode.REQUEST,
                                                sMessageID.trim ().isEmpty () ? sMessageID : sMessageID.trim ());
      assertTrue ("MessageID '" + sMessageID + "': " + aErrorList, _containsInvalidHeaderError (aErrorList));
    }
  }

  @Test
  public void testPushedLeg1InvalidMessageIDIsAcceptedIfNotEnforced () throws Exception
  {
    SystemProperties.removePropertyValue (AS4Configuration.PROPERTY_PHASE4_INCOMING_STRICT_MESSAGE_IDS);

    for (final String sMessageID : new String [] { "   ", "abc", "<a@b>", "a b@c" })
    {
      final AS4ErrorList aErrorList = _process (_readUserMessage (sMessageID, null),
                                                EAS4MessageMode.REQUEST,
                                                sMessageID);
      assertFalse ("MessageID '" + sMessageID + "': " + aErrorList, _containsInvalidHeaderError (aErrorList));
    }
  }

  @Test
  public void testPushedLeg1ValidMessageIDIsTrimmed () throws Exception
  {
    final AS4ErrorList aErrorList = _process (_readUserMessage ("  a@b \n", null), EAS4MessageMode.REQUEST, "a@b");
    assertFalse (aErrorList.toString (), _containsInvalidHeaderError (aErrorList));
  }

  @Test
  public void testPushedLeg2InvalidMessageIDIsAccepted () throws Exception
  {
    // RefToMessageId to a different message means leg 2
    final AS4ErrorList aErrorList = _process (_readUserMessage ("abc", "x@y"), EAS4MessageMode.REQUEST, "abc");
    assertFalse (aErrorList.toString (), _containsInvalidHeaderError (aErrorList));
  }

  @Test
  public void testPulledInvalidMessageIDIsAccepted () throws Exception
  {
    final AS4ErrorList aErrorList = _process (_readUserMessage ("   ", null), EAS4MessageMode.RESPONSE, "   ");
    assertFalse (aErrorList.toString (), _containsInvalidHeaderError (aErrorList));
  }

  @Test
  public void testInvalidRefToMessageIDIsAccepted () throws Exception
  {
    final AS4ErrorList aErrorList = _process (_readUserMessage ("a@b", "  1234 "), EAS4MessageMode.REQUEST, "a@b");
    assertFalse (aErrorList.toString (), _containsInvalidHeaderError (aErrorList));
  }
}
