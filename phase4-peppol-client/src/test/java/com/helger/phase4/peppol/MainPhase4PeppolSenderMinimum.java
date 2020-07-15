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
package com.helger.phase4.peppol;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.bdve.peppol.PeppolValidation3_10_1;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.serialize.read.DOMReader;

/**
 * The main class that requires manual configuration before it can be run. This
 * is a dummy and needs to be adopted to your needs. It just does the absolute
 * minimum, but stores data in the file system.
 *
 * @author Philip Helger
 */
public final class MainPhase4PeppolSenderMinimum
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainPhase4PeppolSenderMinimum.class);

  public static void main (final String [] args)
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());

    // Set data IO base directory
    final File aSCPath = new File (AS4ServerConfiguration.getDataPath ()).getAbsoluteFile ();
    WebFileIO.initPaths (aSCPath, aSCPath.getAbsolutePath (), false);

    try (final WebScoped w = new WebScoped ())
    {
      final Element aPayloadElement = DOMReader.readXMLDOM (new File ("src/test/resources/examples/base-example.xml"))
                                               .getDocumentElement ();
      if (aPayloadElement == null)
        throw new IllegalStateException ();

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9958:peppol-development-governikus-01");
      if (Phase4PeppolSender.builder ()
                            .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"))
                            .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme ("urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                            .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme ("9915:phase4-test-sender"))
                            .receiverParticipantID (aReceiverID)
                            .senderPartyID ("POP000306")
                            .payload (aPayloadElement)
                            .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER, aReceiverID, ESML.DIGIT_TEST))
                            .validationConfiguration (PeppolValidation3_10_1.VID_OPENPEPPOL_INVOICE_V3,
                                                      new Phase4PeppolValidatonResultHandler ())
                            .sendMessage ()
                            .isSuccess ())
      {
        LOGGER.info ("Successfully sent Peppol message via AS4");
      }
      else
      {
        LOGGER.error ("Failed to send Peppol message via AS4");
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
    finally
    {
      WebScopeManager.onGlobalEnd ();
    }
  }
}
