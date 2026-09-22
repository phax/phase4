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
package com.helger.phase4.server.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.IMimeType;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.messaging.crypto.AS4Signer;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.AS4ReceiptMessage;
import com.helger.phase4.model.message.MessageHelperMethods;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.spi.MockSignalMessagePModeProviderSPI;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;

/**
 * Test class for the {@link com.helger.phase4.incoming.spi.IAS4IncomingSignalMessagePModeProviderSPI}
 * extension point (core change C3).<br>
 * Before C3 a <b>signed</b> standalone Receipt could not be processed at all, because no PMode
 * could be determined and the servlet passes a <code>null</code> fallback PMode. That produced an
 * <code>EBMS:0003</code> "other" error rather than an exception.
 *
 * @author Philip Helger
 */
public final class SignalMessagePModeProviderFuncTest extends AbstractAS4TestSetUp
{
  private static AS4ResourceHelper s_aResMgr;

  @BeforeClass
  public static void startServer () throws Exception
  {
    MockJettySetup.startServer ();
    s_aResMgr = MockJettySetup.getResourceManagerInstance ();
  }

  @AfterClass
  public static void shutDownServer () throws Exception
  {
    s_aResMgr = null;
    MockJettySetup.shutDownServer ();
  }

  private static final class MockResponse implements IAS4ResponseAbstraction
  {
    private byte [] m_aBytes;

    public void setContent (final byte @NonNull [] aBytes, @NonNull final Charset aCharset)
    {
      m_aBytes = aBytes;
    }

    public void setContent (@NonNull final HttpHeaderMap aHeaderMap, @NonNull final IHasInputStream aHasIS)
    {
      m_aBytes = StreamHelper.getAllBytes (aHasIS);
    }

    public void setMimeType (@NonNull final IMimeType aMimeType)
    {}

    public void setStatus (final int nStatusCode)
    {}

    @Nullable
    String getAsString ()
    {
      return m_aBytes == null ? null : new String (m_aBytes, StandardCharsets.UTF_8);
    }
  }

  /**
   * Create a signed standalone Receipt that references the provided message ID.
   */
  @NonNull
  private static Document _createSignedStandaloneReceipt (@NonNull final String sRefToMessageID) throws WSSecurityException
  {
    final AS4ReceiptMessage aReceipt = AS4ReceiptMessage.create (ESoapVersion.SOAP_12,
                                                                  MessageHelperMethods.createRandomMessageID (),
                                                                  null,
                                                                  null,
                                                                  false,
                                                                  sRefToMessageID);
    return AS4Signer.createSignedMessage (AS4CryptoFactoryConfiguration.getDefaultInstance (),
                                          aReceipt.getAsSoapDocument (),
                                          ESoapVersion.SOAP_12,
                                          aReceipt.getMessagingID (),
                                          null,
                                          s_aResMgr,
                                          false,
                                          AS4SigningParams.createDefault ());
  }

  @NonNull
  private static String _handle (@NonNull final Document aRequestDoc) throws Exception
  {
    final MockResponse aResponse = new MockResponse ();
    try (final AS4RequestHandler aHandler = new AS4RequestHandler (AS4IncomingMessageMetadata.createForRequest ()))
    {
      final String sAS4ProfileID = AS4ProfileSelector.getDefaultAS4ProfileID ();
      final IAS4CryptoFactory aCF = AS4CryptoFactoryConfiguration.getDefaultInstance ();
      aHandler.setCryptoFactory (aCF);
      aHandler.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
      aHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID, true));
      aHandler.setIncomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
      aHandler.setIncomingSecurityConfiguration (AS4IncomingSecurityConfiguration.createDefaultInstance ());
      aHandler.setIncomingReceiverConfiguration (new AS4IncomingReceiverConfiguration ());

      final HttpHeaderMap aHeaders = new HttpHeaderMap ();
      aHeaders.addHeader ("Content-Type", ESoapVersion.SOAP_12.getMimeType ().getAsString ());

      final byte [] aBytes = AS4XMLHelper.serializeXML (aRequestDoc).getBytes (StandardCharsets.UTF_8);
      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aBytes), aHeaders, aResponse);
    }
    final String s = aResponse.getAsString ();
    return s == null ? "" : s;
  }

  /**
   * C3 - with an SPI that knows the referenced message, the signed standalone Receipt is processed
   * without the "no PMode contained in AS4 state" failure.
   */
  @Test
  public void testKnownReceiptIsProcessed () throws Exception
  {
    final Document aDoc = _createSignedStandaloneReceipt (MockSignalMessagePModeProviderSPI.KNOWN_REF_TO_MESSAGE_ID_PREFIX +
                                                          MessageHelperMethods.createRandomMessageID ());
    final String sResponse = _handle (aDoc);

    assertFalse ("An EBMS:0003 error was returned, so the PMode was not resolved. Response: " + sResponse,
                 sResponse.contains ("EBMS:0003"));
    assertFalse ("The 'No PMode contained in AS4 state' error is still present. Response: " + sResponse,
                 sResponse.contains ("No PMode contained in AS4 state"));
  }

  /**
   * C3 - with an SPI that does NOT know the referenced message, the previous behaviour is
   * unchanged: the signed standalone Receipt cannot be processed.
   */
  @Test
  public void testUnknownReceiptBehavesAsBefore () throws Exception
  {
    final Document aDoc = _createSignedStandaloneReceipt ("phase4-mock-unknown-" +
                                                          MessageHelperMethods.createRandomMessageID ());
    final String sResponse = _handle (aDoc);

    assertNotNull (sResponse);
    assertTrue ("Expected the unchanged 'no PMode' failure for an unknown Receipt. Response: " + sResponse,
                sResponse.contains ("EBMS:0003") || sResponse.contains ("No PMode contained in AS4 state"));
  }
}
