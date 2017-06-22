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

import javax.annotation.concurrent.Immutable;
import javax.xml.namespace.QName;

import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.servlet.mgr.AS4DuplicateCleanupJob;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.as4.servlet.soap.SOAPHeaderElementProcessorWSS4J;

@Immutable
public final class AS4ServerInitializer
{
  private static final QName QNAME_MESSAGING = new QName ("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/",
                                                          "Messaging");
  private static final QName QNAME_SECURITY = new QName ("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                                                         "Security");

  private AS4ServerInitializer ()
  {}

  /**
   * Call this method in your AS4 server to initialize everything that is
   * necessary to use the {@link AS4Servlet}.
   */
  public static void initAS4Server ()
  {
    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry aReg = SOAPHeaderElementProcessorRegistry.getInstance ();
    if (!aReg.containsHeaderElementProcessor (QNAME_MESSAGING))
      aReg.registerHeaderElementProcessor (QNAME_MESSAGING, new SOAPHeaderElementProcessorExtractEbms3Messaging ());
    // WSS4J must be after Ebms3Messaging handler!
    if (!aReg.containsHeaderElementProcessor (QNAME_SECURITY))
      aReg.registerHeaderElementProcessor (QNAME_SECURITY, new SOAPHeaderElementProcessorWSS4J ());

    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();

    // Schedule jobs
    AS4DuplicateCleanupJob.scheduleMe (AS4ServerConfiguration.getIncomingDuplicateDisposalMinutes ());
  }
}
