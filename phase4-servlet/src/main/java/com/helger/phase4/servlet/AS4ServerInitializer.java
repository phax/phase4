/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.servlet;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.resolve.IPModeResolver;
import com.helger.phase4.servlet.mgr.AS4DuplicateCleanupJob;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorExtractEbms3Messaging;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorRegistry;
import com.helger.phase4.servlet.soap.SOAPHeaderElementProcessorWSS4J;

/**
 * This class contains the init method for the AS4Server. Registering alle
 * Processors that are currently used
 * {@link SOAPHeaderElementProcessorExtractEbms3Messaging} and
 * {@link SOAPHeaderElementProcessorWSS4J}. Also a {@link MetaAS4Manager}
 * instance gets provided for the server to use. The duplicate cleanup job will
 * also be started.
 *
 * @author bayerlma
 * @author Philip Helger
 */
@Immutable
public final class AS4ServerInitializer
{
  private AS4ServerInitializer ()
  {}

  /**
   * Call this method in your AS4 server to initialize everything that is
   * necessary to use the {@link AS4Servlet}.
   *
   * @param aPModeResolver
   *        PMode resolver. May not be <code>null</code>.
   * @param aCryptoFactory
   *        Crypto factory to use. May not be <code>null</code>.
   */
  public static void initAS4Server (@Nonnull final IPModeResolver aPModeResolver,
                                    @Nonnull final IAS4CryptoFactory aCryptoFactory)
  {
    // Register all SOAP header element processors
    // Registration order matches execution order!
    final SOAPHeaderElementProcessorRegistry aReg = SOAPHeaderElementProcessorRegistry.getInstance ();
    if (!aReg.containsHeaderElementProcessor (SOAPHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING))
      aReg.registerHeaderElementProcessor (SOAPHeaderElementProcessorExtractEbms3Messaging.QNAME_MESSAGING,
                                           new SOAPHeaderElementProcessorExtractEbms3Messaging (aPModeResolver));

    // WSS4J must be after Ebms3Messaging handler!
    if (!aReg.containsHeaderElementProcessor (SOAPHeaderElementProcessorWSS4J.QNAME_SECURITY))
      aReg.registerHeaderElementProcessor (SOAPHeaderElementProcessorWSS4J.QNAME_SECURITY,
                                           new SOAPHeaderElementProcessorWSS4J (aCryptoFactory));

    // Ensure all managers are initialized
    MetaAS4Manager.getInstance ();

    // Schedule jobs
    AS4DuplicateCleanupJob.scheduleMe (AS4ServerConfiguration.getIncomingDuplicateDisposalMinutes ());
  }
}
