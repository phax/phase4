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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.IMimeType;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4SigningParams;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.crypto.IWSSecSignatureCustomizer;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.IAS4ResponseSignalCustomizer;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.model.ESoapVersion;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.server.AbstractAS4TestSetUp;
import com.helger.phase4.server.MockJettySetup;
import com.helger.phase4.server.message.MockMessages;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.AS4XMLHelper;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for the {@link IAS4ResponseSignalCustomizer} extension point of
 * {@link AS4RequestHandler} (core change C1).<br>
 * The handler is driven directly instead of through the servlet, because the customizer is set per
 * {@link AS4RequestHandler} instance.
 *
 * @author Philip Helger
 */
public final class ResponseSignalCustomizerFuncTest extends AbstractAS4TestSetUp
{
  private static final String TEST_NS = "urn:phase4:test:c1";

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

  /**
   * Captures whatever the request handler produces as the HTTP response.
   */
  private static final class MockResponse implements IAS4ResponseAbstraction
  {
    private byte [] m_aBytes;
    private int m_nStatus = -1;

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
    {
      m_nStatus = nStatusCode;
    }

    @Nullable
    Document getAsDocument ()
    {
      return m_aBytes == null ? null : DOMReader.readXMLDOM (m_aBytes);
    }
  }

  /**
   * A customizer that appends one element to the SOAP header and - if the response is signed - adds
   * that element to the list of signed parts.
   */
  private static final class MockCustomizer implements IAS4ResponseSignalCustomizer
  {
    private final AtomicInteger m_aInvocationCount = new AtomicInteger (0);
    private EAS4MessageType m_eLastType;

    public void customizeResponseSignal (@NonNull final IAS4IncomingMessageState aIncomingState,
                                         @NonNull final EAS4MessageType eResponseType,
                                         @NonNull final ESoapVersion eResponseSoapVersion,
                                         @NonNull final Document aUnsignedResponseDoc,
                                         @Nullable final AS4SigningParams aResponseSigningParams)
    {
      m_aInvocationCount.incrementAndGet ();
      m_eLastType = eResponseType;

      // Find the SOAP Header
      final Element aHeader = XMLHelper.getFirstChildElementOfName (aUnsignedResponseDoc.getDocumentElement (),
                                                                    eResponseSoapVersion.getNamespaceURI (),
                                                                    eResponseSoapVersion.getHeaderElementName ());
      assertNotNull ("No SOAP Header in the response", aHeader);

      // Append a custom header element with a wsu:Id
      final String sID = "phase4-test-" + UUID.randomUUID ().toString ();
      final Element aNew = aUnsignedResponseDoc.createElementNS (TEST_NS, "t:TestHeader");
      aNew.setAttributeNS (CAS4.WSU_NS, "wsu:Id", sID);
      aNew.setTextContent ("hello");
      aHeader.appendChild (aNew);

      if (aResponseSigningParams != null)
      {
        // Make sure the new element is covered by the signature as well
        final IWSSecSignatureCustomizer aOld = aResponseSigningParams.getWSSecSignatureCustomizer ();
        aResponseSigningParams.setWSSecSignatureCustomizer (new IWSSecSignatureCustomizer ()
        {
          @NonNull
          public WSSecSignature createWSSecSignature (@NonNull final WSSecHeader aSecHeader)
          {
            return aOld != null ? aOld.createWSSecSignature (aSecHeader) : new WSSecSignature (aSecHeader);
          }

          public void customize (@NonNull final WSSecSignature aWSSecSignature)
          {
            if (aOld != null)
              aOld.customize (aWSSecSignature);
            aWSSecSignature.getParts ().add (new WSEncryptionPart (sID, "Content"));
          }
        });
      }
    }
  }

  @NonNull
  private static MockResponse _handle (@NonNull final Document aRequestDoc,
                                       @Nullable final IAS4ResponseSignalCustomizer aCustomizer) throws Exception
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
      aHandler.setResponseSignalCustomizer (aCustomizer);

      final HttpHeaderMap aHeaders = new HttpHeaderMap ();
      aHeaders.addHeader ("Content-Type", ESoapVersion.SOAP_12.getMimeType ().getAsString ());

      // Use the canonicalization aware serialization, otherwise the signature breaks
      final byte [] aBytes = AS4XMLHelper.serializeXML (aRequestDoc).getBytes (StandardCharsets.UTF_8);
      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aBytes), aHeaders, aResponse);
    }
    return aResponse;
  }

  @Nullable
  private static Element _findTestHeader (@Nullable final Document aDoc)
  {
    if (aDoc == null)
      return null;
    final Element aHeader = XMLHelper.getFirstChildElementOfName (aDoc.getDocumentElement (),
                                                                  ESoapVersion.SOAP_12.getNamespaceURI (),
                                                                  ESoapVersion.SOAP_12.getHeaderElementName ());
    if (aHeader == null)
      return null;
    return XMLHelper.getFirstChildElementOfName (aHeader, TEST_NS, "TestHeader");
  }

  @NonNull
  private static ICommonsList <String> _getAllSignedReferenceURIs (@NonNull final Document aDoc)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    final NodeList aRefs = aDoc.getElementsByTagNameNS (WSConstants.SIG_NS, "Reference");
    for (int i = 0; i < aRefs.getLength (); ++i)
      ret.add (((Element) aRefs.item (i)).getAttribute ("URI"));
    return ret;
  }

  /**
   * C1 - without a customizer the response must be exactly what it was before, i.e. no additional
   * SOAP header element shows up.
   */
  @Test
  public void testNoCustomizerMeansNoChange () throws Exception
  {
    final Document aRequest = MockMessages.createUserMessageSigned (ESoapVersion.SOAP_12, null, null, s_aResMgr);
    final MockResponse aResponse = _handle (aRequest, null);

    final Document aResponseDoc = aResponse.getAsDocument ();
    assertNotNull ("No response received", aResponseDoc);
    assertEquals ("A header was added although no customizer was set", null, _findTestHeader (aResponseDoc));
  }

  /**
   * C1 - for a Receipt the customizer must be invoked, its header must be present in the response,
   * and the header must be covered by the signature.
   */
  @Test
  public void testReceiptCustomizerIsInvokedAndSigned () throws Exception
  {
    final Document aRequest = MockMessages.createUserMessageSigned (ESoapVersion.SOAP_12, null, null, s_aResMgr);

    final MockCustomizer aCustomizer = new MockCustomizer ();
    final MockResponse aResponse = _handle (aRequest, aCustomizer);

    assertEquals ("Customizer was not invoked exactly once", 1, aCustomizer.m_aInvocationCount.get ());
    assertEquals (EAS4MessageType.RECEIPT, aCustomizer.m_eLastType);

    final Document aResponseDoc = aResponse.getAsDocument ();
    assertNotNull ("No response received", aResponseDoc);

    final Element aTestHeader = _findTestHeader (aResponseDoc);
    assertNotNull ("The customizer added header is not present in the response", aTestHeader);

    final String sID = aTestHeader.getAttributeNS (CAS4.WSU_NS, "Id");
    assertTrue ("The added header has no wsu:Id", sID.length () > 0);

    final ICommonsList <String> aRefs = _getAllSignedReferenceURIs (aResponseDoc);
    assertTrue ("The response is not signed at all", aRefs.isNotEmpty ());
    assertTrue ("The customizer added header is NOT covered by the signature. References: " + aRefs,
                aRefs.contains ("#" + sID));
  }

  /**
   * C1 - the same for an Error response. A message that is not PMode conform produces an Error.
   */
  @Test
  public void testErrorCustomizerIsInvoked () throws Exception
  {
    final Document aRequest = MockMessages.testUserMessageNotSignedNotPModeConform (ESoapVersion.SOAP_12, null, null);

    final MockCustomizer aCustomizer = new MockCustomizer ();
    final MockResponse aResponse = _handle (aRequest, aCustomizer);

    assertEquals ("Customizer was not invoked exactly once", 1, aCustomizer.m_aInvocationCount.get ());
    assertEquals (EAS4MessageType.ERROR_MESSAGE, aCustomizer.m_eLastType);

    final Document aResponseDoc = aResponse.getAsDocument ();
    assertNotNull ("No response received", aResponseDoc);
    assertNotNull ("The customizer added header is not present in the Error response",
                   _findTestHeader (aResponseDoc));
  }
}
