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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4lib.mgr.MetaAS4Manager;
import com.helger.as4lib.partner.IPartner;
import com.helger.as4lib.partner.Partner;
import com.helger.as4lib.partner.PartnerManager;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.security.certificate.CertificateHelper;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author bayerlma
 */
public class PartnerTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  private static final Locale LOCALE = Locale.US;

  @Test
  public void tesPartnerCertificateSavedSuccess () throws CertificateException
  {
    final PartnerManager aPM = MetaAS4Manager.getPartnerMgr ();
    final IPartner aPartner = aPM.getPartnerOfID ("APP_1000000101");
    final X509Certificate aCert = CertificateHelper.convertByteArrayToCertficate (CertificateHelper.convertCertificateStringToByteArray (aPartner.getAllAttributes ()
                                                                                                                                                 .get (Partner.ATTR_CERT)));
    aCert.checkValidity ();
  }
}
