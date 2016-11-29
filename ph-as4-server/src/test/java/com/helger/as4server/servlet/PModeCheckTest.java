/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import static org.junit.Assert.assertNotNull;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.crypto.ECryptoAlgorithmSign;
import com.helger.as4lib.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.error.EEbmsError;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.mock.MockPModeGenerator;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.as4lib.model.pmode.PModeConfig;
import com.helger.as4lib.model.pmode.PModeLeg;
import com.helger.as4lib.model.pmode.PModeLegProtocol;
import com.helger.as4lib.model.pmode.PModeManager;
import com.helger.as4lib.signing.SignedMessageCreator;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.AS4ResourceManager;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.url.URLHelper;
import com.helger.photon.jetty.JettyRunner;
import com.helger.xml.serialize.read.DOMReader;

public class PModeCheckTest extends AbstractUserMessageSetUp
{
  private static final int PORT = URLHelper.getAsURL (PROPS.getAsString ("server.address")).getPort ();
  private static final int STOP_PORT = PORT + 1000;
  private static JettyRunner s_aJetty = new JettyRunner (PORT, STOP_PORT);
  private static AS4ResourceManager s_aResMgr;

  @BeforeClass
  public static void startServer () throws Exception
  {
    s_aJetty.startServer ();
    s_aResMgr = new AS4ResourceManager ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr.close ();
    s_aJetty.shutDownServer ();
  }

  @Test
  public void testWrongPModeID () throws Exception
  {
    final Document aDoc = _modifyUserMessage ("this-is-a-wrong-id", null, null);
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testWrongPartyIDInitiator () throws Exception
  {
    final Document aDoc = _modifyUserMessage (null, "random_party_id120", null);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testWrongPartyIDResponder () throws Exception
  {
    final Document aDoc = _modifyUserMessage (null, null, "random_party_id121");

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }

  @Test
  public void testPModeLegNullReject () throws Exception
  {
    final String sPModeID = "pmode-" + GlobalIDFactory.getNewPersistentIntID ();
    final PMode aPMode = MockPModeGenerator.getTestPModeSetID (ESOAPVersion.AS4_DEFAULT, sPModeID);
    ((PModeConfig) aPMode.getConfig ()).setLeg1 (null);
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    try
    {
      aPModeMgr.createPMode (aPMode);

      final IPMode aPModeID = MetaAS4Manager.getPModeMgr ().findFirst (_getFirstPModeWithID (sPModeID));

      final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (_modifyUserMessage (aPModeID.getID (),
                                                                                                       null,
                                                                                                       null),
                                                                                   ESOAPVersion.AS4_DEFAULT,
                                                                                   null,
                                                                                   s_aResMgr,
                                                                                   false,
                                                                                   ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                   ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

      sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)),
                        false,
                        EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
    }
    finally
    {
      aPModeMgr.deletePMode (aPMode.getID ());
    }
  }

  /**
   * Is ESENS specific, EBMS3 specification is the protocol an optional element.
   * Maybe refactoring into a test if http and smtp addresses later on get
   * converted right
   *
   * @throws Exception
   */
  @Ignore
  @Test
  public void testPModeLegProtocolAddressReject () throws Exception
  {
    final String sPModeID = "pmode-" + GlobalIDFactory.getNewPersistentIntID ();
    final PMode aPMode = MockPModeGenerator.getTestPModeSetID (ESOAPVersion.AS4_DEFAULT, sPModeID);
    ((PModeConfig) aPMode.getConfig ()).setLeg1 (new PModeLeg (new PModeLegProtocol ("TestsimulationAddressWrong",
                                                                                     ESOAPVersion.AS4_DEFAULT),
                                                               null,
                                                               null,
                                                               null,
                                                               null));
    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    try
    {
      aPModeMgr.createPMode (aPMode);

      final Document aSignedDoc = new SignedMessageCreator ().createSignedMessage (_modifyUserMessage (sPModeID,
                                                                                                       null,
                                                                                                       null),
                                                                                   ESOAPVersion.AS4_DEFAULT,
                                                                                   null,
                                                                                   s_aResMgr,
                                                                                   false,
                                                                                   ECryptoAlgorithmSign.SIGN_ALGORITHM_DEFAULT,
                                                                                   ECryptoAlgorithmSignDigest.SIGN_DIGEST_ALGORITHM_DEFAULT);

      sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aSignedDoc)),
                        false,
                        EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
    }
    finally
    {
      aPModeMgr.deletePMode (aPMode.getID ());
    }
  }

  @Nonnull
  private Document _modifyUserMessage (@Nullable final String sWrongPModeID,
                                       @Nullable final String sWrongPartyIdInitiator,
                                       @Nullable final String sWrongPartyIdResponder) throws Exception
  {
    // If argument is set replace the default one
    final IPMode aPModeID = MetaAS4Manager.getPModeMgr ()
                                          .findFirst (_getFirstPModeWithID (MockPModeGenerator.PMODE_CONFIG_ID_SOAP12_TEST));
    final String sSetPModeID = sWrongPModeID == null ? aPModeID.getID () : sWrongPModeID;
    final String sSetPartyIDInitiator = sWrongPartyIdInitiator == null ? "APP_1000000101" : sWrongPartyIdInitiator;
    final String sSetPartyIDResponder = sWrongPartyIdResponder == null ? "APP_1000000101" : sWrongPartyIdResponder;

    final CreateUserMessage aUserMessage = new CreateUserMessage ();
    final Node aPayload = DOMReader.readXMLDOM (new ClassPathResource ("SOAPBodyPayload.xml"));

    // Add properties
    final ICommonsList <Ebms3Property> aEbms3Properties = new CommonsArrayList<> ();
    final Ebms3Property aOriginalSender = new Ebms3Property ();
    aOriginalSender.setName ("originalSender");
    aOriginalSender.setValue ("C1-test");
    final Ebms3Property aFinalRecipient = new Ebms3Property ();
    aFinalRecipient.setName ("finalRecipient");
    aFinalRecipient.setValue ("C4-test");
    aEbms3Properties.add (aOriginalSender);
    aEbms3Properties.add (aFinalRecipient);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo (CAS4.LIB_NAME);
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      sSetPModeID,
                                                                                                      "http://agreements.holodeckb2b.org/examples/agreement0");
    final Ebms3PartyInfo aEbms3PartyInfo = aUserMessage.createEbms3PartyInfo ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sender",
                                                                              sSetPartyIDInitiator,
                                                                              "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder",
                                                                              sSetPartyIDResponder);
    final Ebms3MessageProperties aEbms3MessageProperties = aUserMessage.createEbms3MessageProperties (aEbms3Properties);

    final AS4UserMessage aDoc = aUserMessage.createUserMessage (aEbms3MessageInfo,
                                                                aEbms3PayloadInfo,
                                                                aEbms3CollaborationInfo,
                                                                aEbms3PartyInfo,
                                                                aEbms3MessageProperties,
                                                                ESOAPVersion.AS4_DEFAULT)
                                            .setMustUnderstand (false);

    return aDoc.getAsSOAPDocument (aPayload);
  }

  @Nonnull
  private static Predicate <IPMode> _getFirstPModeWithID (@Nonnull final String sID)
  {
    return p -> p.getConfigID ().equals (sID);
  }
}
