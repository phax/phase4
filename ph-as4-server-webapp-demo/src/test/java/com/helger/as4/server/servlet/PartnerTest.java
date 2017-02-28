/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.server.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.entity.StringEntity;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.as4.error.EEbmsError;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.partner.IPartner;
import com.helger.as4.partner.Partner;
import com.helger.as4.partner.PartnerManager;
import com.helger.as4.server.MockPModeGenerator;
import com.helger.as4.server.message.AbstractUserMessageTestSetUp;
import com.helger.as4.util.AS4XMLHelper;
import com.helger.as4.util.StringMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.security.certificate.CertificateHelper;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author bayerlma
 */
public class PartnerTest extends AbstractUserMessageTestSetUpExt
{
  private final PartnerManager m_aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
  private static final String PARTNER_ID = "testpartner";

  @BeforeClass
  public static void startServer () throws Exception
  {
    AbstractUserMessageTestSetUp.startServer ();
    final StringMap aStringMap = new StringMap ();
    aStringMap.setAttribute (Partner.ATTR_PARTNER_NAME, PARTNER_ID);
    final byte [] aCertBytes = StreamHelper.getAllBytes (new ClassPathResource ("partner-cert.txt"));
    final X509Certificate aUsedCertificate = CertificateHelper.convertByteArrayToCertficate (aCertBytes);
    aStringMap.setAttribute (Partner.ATTR_CERT, CertificateHelper.getPEMEncodedCertificate (aUsedCertificate));
    final PartnerManager aPartnerMgr = MetaAS4Manager.getPartnerMgr ();
    aPartnerMgr.createOrUpdatePartner (PARTNER_ID, aStringMap);
  }

  @Test
  public void testPartnerCertificateSavedSuccess () throws CertificateException
  {
    final IPartner aPartner = m_aPartnerMgr.getPartnerOfID (PARTNER_ID);
    assertNotNull (aPartner);

    final String sCertificate = aPartner.getAllAttributes ().get (Partner.ATTR_CERT);
    assertTrue (StringHelper.hasText (sCertificate));

    final X509Certificate aCert = CertificateHelper.convertStringToCertficate (sCertificate);
    assertNotNull (aCert);
    aCert.checkValidity ();
  }

  @Test
  public void testAddingNewUnkownPartnerInitiator () throws Exception
  {
    final String sPartnerID = "TestPartnerUnkown";

    final Document aDoc = _modifyUserMessage (MockPModeGenerator.PMODE_CONFIG_ID_SOAP12_TEST,
                                              sPartnerID,
                                              null,
                                              _defaultProperties ());
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
    final IPartner aPartner = m_aPartnerMgr.getPartnerOfID (sPartnerID);
    assertNotNull (aPartner);

    m_aPartnerMgr.deletePartner (aPartner.getID ());
  }

  @Test
  public void testAddingNewUnkownPartnerResponder () throws Exception
  {
    final String sPartnerID = "random_party_id121";

    final Document aDoc = _modifyUserMessage (MockPModeGenerator.PMODE_CONFIG_ID_SOAP12_TEST,
                                              null,
                                              sPartnerID,
                                              _defaultProperties ());
    assertNotNull (aDoc);

    final String sResponse = sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);

    // Afterwards something should be present
    assertTrue (m_aPartnerMgr.getCount () > 0);

    final IPartner aPartner = m_aPartnerMgr.getPartnerOfID (sPartnerID);
    assertNotNull (sResponse, aPartner);

    m_aPartnerMgr.deletePartner (aPartner.getID ());
  }

  // Currently Default PMode is not supported / wanted
  @Ignore
  @Test
  public void testPartnersExistShouldGetDefaultConfig () throws Exception
  {

    final Document aDoc = _modifyUserMessage (null, null, null, _defaultProperties ());
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)), true, null);
  }

  @Test
  public void testPartnersExistShouldThrowFaultSinceNewPModeConfig () throws Exception
  {

    final Document aDoc = _modifyUserMessage ("testfaultconfig", null, null, _defaultProperties ());
    assertNotNull (aDoc);

    sendPlainMessage (new StringEntity (AS4XMLHelper.serializeXML (aDoc)),
                      false,
                      EEbmsError.EBMS_PROCESSING_MODE_MISMATCH.getErrorCode ());
  }
}
