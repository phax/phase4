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
package com.helger.as4.servlet;

import java.security.cert.X509Certificate;

import javax.annotation.concurrent.Immutable;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.CryptoType.TYPE;
import org.apache.wss4j.common.ext.WSSecurityException;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.servlet.mgr.AS4DuplicateCleanupJob;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorWSS4J;
import com.helger.commons.collection.ArrayHelper;

@Immutable
public final class AS4ServerInitializer
{
  private AS4ServerInitializer ()
  {}

  private static void _getMyAPP ()
  {
    AS4ServerSettings.getDefaultResponderID ();
  }

  private static void _getMyKeyAlias ()
  {
    AS4ServerSettings.getAS4CryptoFactory ().getCryptoProperties ().getKeyAlias ();
  }

  private static void _getMyCert () throws WSSecurityException
  {
    final CryptoType aCT = new CryptoType (TYPE.ALIAS);
    aCT.setAlias (AS4ServerSettings.getAS4CryptoFactory ().getCryptoProperties ().getKeyAlias ());
    final X509Certificate [] aCertList = AS4ServerSettings.getAS4CryptoFactory ()
                                                          .getCrypto ()
                                                          .getX509Certificates (aCT);
    if (ArrayHelper.isEmpty (aCertList))
      throw new IllegalStateException ("Failed to find default partner certificate from alias '" +
                                       aCT.getAlias () +
                                       "'");
    final X509Certificate aCert = aCertList[0];
  }

  /**
   * Call this method in your AS4 server to initialize everything that is
   * necessary to use the {@link AS4Servlet}.
   */
  public static void initAS4Server ()
  {
    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry aReg = SOAPHeaderElementProcessorRegistry.getInstance ();
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
                                                    "Messaging"),
                                         new SOAPHeaderElementProcessorExtractEbms3Messaging ());
    // WSS4J must be after Ebms3Messaging handler!
    aReg.registerHeaderElementProcessor (new QName ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                                                    "Security"),
                                         new SOAPHeaderElementProcessorWSS4J ());

    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();

    // Schedule jobs
    AS4DuplicateCleanupJob.scheduleMe (AS4ServerConfiguration.getIncomingDuplicateDisposalMinutes ());
  }
}
