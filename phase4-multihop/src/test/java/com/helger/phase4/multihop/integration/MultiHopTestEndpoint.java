/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.multihop.integration;

import java.nio.charset.Charset;
import java.security.KeyStore;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.io.iface.IHasInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.http.header.HttpHeaderMap;
import com.helger.mime.IMimeType;
import com.helger.phase4.attachment.IAS4IncomingAttachmentFactory;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.incoming.AS4IncomingMessageMetadata;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4IncomingReceiverConfiguration;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.IAS4ResponseAbstraction;
import com.helger.phase4.incoming.crypto.AS4IncomingSecurityConfiguration;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.multihop.AS4MultiHopConfig;
import com.helger.phase4.multihop.incoming.MultiHopPModeResolver;
import com.helger.phase4.multihop.incoming.MultiHopResponseSignalCustomizer;

/**
 * One AS4 endpoint of the integration test, identified by its own key alias.
 * <p>
 * The endpoint drives {@link AS4RequestHandler} directly instead of going through
 * {@code AS4Servlet}. The HTTP layer is deliberately out of scope here - it is unchanged by
 * multi-hop and already covered by the existing phase4-test suite. What this harness does
 * exercise is the complete multi-hop message flow: the role attribute, the routing headers, the
 * signature coverage and the byte transparent forwarding by an intermediary.
 * </p>
 *
 * @author Philip Helger
 */
public final class MultiHopTestEndpoint
{
  /** Captures whatever the request handler produces */
  public static final class Response implements IAS4ResponseAbstraction
  {
    private byte [] m_aBytes;
    private int m_nStatus = 200;

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

    /**
     * @return The raw response bytes or <code>null</code> if the response was empty.
     */
    public byte @Nullable [] getBytes ()
    {
      return m_aBytes;
    }

    public boolean hasBytes ()
    {
      return m_aBytes != null && m_aBytes.length > 0;
    }

    public int getStatus ()
    {
      return m_nStatus;
    }
  }

  private final String m_sName;
  private final IAS4CryptoFactory m_aCryptoFactory;
  private final AS4MultiHopConfig m_aConfig;
  private final boolean m_bMultiHopEnabled;

  /**
   * Constructor.
   *
   * @param sName
   *        A readable name, for assertion messages only.
   * @param aKeyStore
   *        The key store holding the endpoint's private key.
   * @param sKeyAlias
   *        The alias of this endpoint's key.
   * @param sKeyPassword
   *        The private key password.
   * @param aConfig
   *        The multi-hop configuration.
   * @param bMultiHopEnabled
   *        <code>false</code> to simulate a plain phase4 without this module (scenario S5).
   */
  public MultiHopTestEndpoint (@NonNull final String sName,
                               @NonNull final KeyStore aKeyStore,
                               @NonNull final String sKeyAlias,
                               @NonNull final String sKeyPassword,
                               @NonNull final AS4MultiHopConfig aConfig,
                               final boolean bMultiHopEnabled)
  {
    m_sName = sName;
    m_aCryptoFactory = new AS4CryptoFactoryInMemoryKeyStore (aKeyStore,
                                                              sKeyAlias,
                                                              sKeyPassword.toCharArray (),
                                                              aKeyStore);
    m_aConfig = aConfig;
    m_bMultiHopEnabled = bMultiHopEnabled;
  }

  @NonNull
  public String getName ()
  {
    return m_sName;
  }

  @NonNull
  public IAS4CryptoFactory getCryptoFactory ()
  {
    return m_aCryptoFactory;
  }

  /**
   * Receive one AS4 message, exactly as the servlet would.
   *
   * @param aRequestBytes
   *        The raw request bytes. Never re-serialized on the way in.
   * @param sContentType
   *        The Content-Type header value.
   * @return The captured response. Never <code>null</code>.
   * @throws Exception
   *         on error
   */
  @NonNull
  public Response receive (final byte @NonNull [] aRequestBytes, @NonNull final String sContentType) throws Exception
  {
    final Response aResponse = new Response ();
    try (final AS4RequestHandler aHandler = new AS4RequestHandler (AS4IncomingMessageMetadata.createForRequest ()))
    {
      final String sAS4ProfileID = AS4ProfileSelector.getDefaultAS4ProfileID ();
      aHandler.setCryptoFactory (m_aCryptoFactory);
      aHandler.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
      aHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID, true));
      aHandler.setIncomingAttachmentFactory (IAS4IncomingAttachmentFactory.DEFAULT_INSTANCE);
      aHandler.setIncomingSecurityConfiguration (AS4IncomingSecurityConfiguration.createDefaultInstance ());
      aHandler.setIncomingReceiverConfiguration (new AS4IncomingReceiverConfiguration ());

      // This is what AS4MultiHopRequestHandlerCustomizer does for a real servlet
      if (m_bMultiHopEnabled)
      {
        aHandler.setResponseSignalCustomizer (new MultiHopResponseSignalCustomizer (m_aConfig));
        aHandler.setPModeResolver (new MultiHopPModeResolver (aHandler.getPModeResolver ()));
      }

      final HttpHeaderMap aHeaders = new HttpHeaderMap ();
      aHeaders.addHeader ("Content-Type", sContentType);

      aHandler.handleRequest (new NonBlockingByteArrayInputStream (aRequestBytes), aHeaders, aResponse);
    }
    return aResponse;
  }
}
