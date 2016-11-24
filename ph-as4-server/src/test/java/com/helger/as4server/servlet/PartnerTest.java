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
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.ebms3header.Ebms3CollaborationInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageInfo;
import com.helger.as4lib.ebms3header.Ebms3MessageProperties;
import com.helger.as4lib.ebms3header.Ebms3PartyInfo;
import com.helger.as4lib.ebms3header.Ebms3PayloadInfo;
import com.helger.as4lib.ebms3header.Ebms3Property;
import com.helger.as4lib.message.AS4UserMessage;
import com.helger.as4lib.message.CreateUserMessage;
import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.model.pmode.IPMode;
import com.helger.as4lib.partner.IPartner;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.as4lib.util.IOHelper;
import com.helger.as4lib.util.StringMap;
import com.helger.as4lib.xml.AS4XMLHelper;
import com.helger.as4server.mock.ServletTestPMode;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.photon.jetty.JettyRunner;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author bayerlma
 */
public class PartnerTest extends AbstractUserMessageSetUp
{
  private static final int PORT = URLHelper.getAsURL (PROPS.getAsString ("server.address")).getPort ();
  private static final int STOP_PORT = PORT + 1000;
  private static JettyRunner s_aJetty = new JettyRunner (PORT, STOP_PORT);
  private final PartnerManager aPM = MetaAS4Manager.getPartnerMgr ();
  private static final String PARTNER_ID = "testpartner";

  @BeforeClass
  public static void startServer () throws Exception
  {
    s_aJetty.startServer ();
    final StringMap aStringMap = new StringMap ();
    aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, PARTNER_ID);
    final byte [] aCertBytes = Files.readAllBytes (Paths.get (new ClassPathResource ("cert.txt").getAsFile ()
                                                                                                .getAbsolutePath ()));
    final X509Certificate aUsedCertificate = CertificateHelper.convertByteArrayToCertficate (aCertBytes);
    aStringMap.setAttribute (Partner.ATTR_CERT, IOHelper.getPEMEncodedCertificate (aUsedCertificate));
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    aPartnerMgr.createOrUpdatePartner (PARTNER_ID, aStringMap);
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aJetty.shutDownServer ();
  }

  @Test
  public void testPartnerCertificateSavedSuccess () throws CertificateException
  {
    final IPartner aPartner = aPM.getPartnerOfID (PARTNER_ID);
    assertNotNull (aPartner);

    final String sCertificate = aPartner.getAllAttributes ().get (Partner.ATTR_CERT);
    assertTrue (StringHelper.hasText (sCertificate));

    final X509Certificate aCert = CertificateHelper.convertStringToCertficate (sCertificate);
    assertNotNull (aCert);
    aCert.checkValidity ();
  }

  @Test
  public void testAddingNewUnkownPartner () throws Exception
  {
    final String sPartnerID = "TestPartnerUnkown";

    final Document aDoc = _modifyUserMessage (sPartnerID, null);
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
    final IPartner aPartner = aPM.getPartnerOfID (sPartnerID);
    assertNotNull (aPartner);
  }

  @Nonnull
  private Document _modifyUserMessage (@Nullable final String sDifferentPartyIdInitiator,
                                       @Nullable final String sDifferentPartyIdResponder) throws Exception
  {
    // If argument is set replace the default one
    // final IPMode aPModeID = MetaAS4Manager.getPModeMgr ()
    // .findFirst (_getFirstPModeWithID
    // (ServletTestPMode.PMODE_CONFIG_ID_SOAP12_TEST));
    final ESOAPVersion eSetESOAPVersion = ESOAPVersion.AS4_DEFAULT;
    final String sSetPartyIDInitiator = sDifferentPartyIdInitiator == null ? "APP_1000000101"
                                                                           : sDifferentPartyIdInitiator;
    final String sSetPartyIDResponder = sDifferentPartyIdResponder == null ? "APP_1000000101"
                                                                           : sDifferentPartyIdResponder;

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
    aEbms3Properties.add (aFinalRecipient);
    aEbms3Properties.add (aOriginalSender);

    final Ebms3MessageInfo aEbms3MessageInfo = aUserMessage.createEbms3MessageInfo (CAS4.LIB_NAME);
    final Ebms3PayloadInfo aEbms3PayloadInfo = aUserMessage.createEbms3PayloadInfo (aPayload, null);
    final Ebms3CollaborationInfo aEbms3CollaborationInfo = aUserMessage.createEbms3CollaborationInfo ("NewPurchaseOrder",
                                                                                                      "MyServiceTypes",
                                                                                                      "QuoteToCollect",
                                                                                                      "4321",
                                                                                                      ServletTestPMode.PMODE_CONFIG_ID_SOAP12_TEST,
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
                                                                eSetESOAPVersion)
                                            .setMustUnderstand (false);

    return aDoc.getAsSOAPDocument (aPayload);
  }

  @Nonnull
  private static Predicate <IPMode> _getFirstPModeWithID (@Nonnull final String sID)
  {
    return p -> p.getConfigID ().equals (sID);
  }
}
