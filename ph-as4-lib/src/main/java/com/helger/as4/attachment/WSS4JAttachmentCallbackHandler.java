/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.as4.attachment;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.stream.HasInputStream;

/**
 * A Callback Handler implementation for the case of signing/encrypting
 * Attachments via the SwA (SOAP with Attachments) specification or when using
 * xop:Include in the case of MTOM.
 *
 * @author Apache WSS4J
 * @author Philip Helger
 */
public class WSS4JAttachmentCallbackHandler implements CallbackHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (WSS4JAttachmentCallbackHandler.class);

  private final ICommonsOrderedMap <String, WSS4JAttachment> m_aAttachmentMap = new CommonsLinkedHashMap <> ();
  private final AS4ResourceManager m_aResMgr;

  public WSS4JAttachmentCallbackHandler (@Nullable final Iterable <? extends WSS4JAttachment> aAttachments,
                                         @Nonnull final AS4ResourceManager aResMgr)
  {
    ValueEnforcer.notNull (aResMgr, "ResMgr");

    if (aAttachments != null)
      for (final WSS4JAttachment aAttachment : aAttachments)
        m_aAttachmentMap.put (aAttachment.getId (), aAttachment);
    m_aResMgr = aResMgr;
  }

  /**
   * @return The resource manager as passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4ResourceManager getResourceMgr ()
  {
    return m_aResMgr;
  }

  /**
   * Try to match the Attachment Id. Otherwise, add all Attachments.
   *
   * @param sID
   *        Attachment ID to search
   * @return Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <Attachment> _getAttachmentsToAdd (@Nullable final String sID)
  {
    final WSS4JAttachment aAttachment = m_aAttachmentMap.get (sID);
    if (aAttachment != null)
      return new CommonsArrayList <> (aAttachment);

    // Use all
    return new CommonsArrayList <> (m_aAttachmentMap.values ());
  }

  public void handle (@Nonnull final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof AttachmentRequestCallback)
      {
        final AttachmentRequestCallback aAttachmentRequestCallback = (AttachmentRequestCallback) aCallback;

        final String sAttachmentID = aAttachmentRequestCallback.getAttachmentId ();
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Requesting attachment ID '" + sAttachmentID + "'");

        final ICommonsList <Attachment> aAttachments = _getAttachmentsToAdd (sAttachmentID);
        if (aAttachments.isEmpty ())
          throw new IllegalStateException ("No attachments present");

        aAttachmentRequestCallback.setAttachments (aAttachments);
      }
      else
        if (aCallback instanceof AttachmentResultCallback)
        {
          final AttachmentResultCallback aAttachmentResultCallback = (AttachmentResultCallback) aCallback;
          final Attachment aResponseAttachment = aAttachmentResultCallback.getAttachment ();

          final String sAttachmentID = aAttachmentResultCallback.getAttachmentId ();
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Resulting attachment ID '" + sAttachmentID + "'");

          // Convert
          final WSS4JAttachment aRealAttachment = new WSS4JAttachment (m_aResMgr, aResponseAttachment.getMimeType ());
          aRealAttachment.setId (sAttachmentID);
          aRealAttachment.addHeaders (aResponseAttachment.getHeaders ());
          // Use supplier to ensure stream is opened only when needed
          aRealAttachment.setSourceStreamProvider (HasInputStream.once (aResponseAttachment::getSourceStream));

          m_aAttachmentMap.put (sAttachmentID, aRealAttachment);
        }
        else
        {
          throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
        }
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <WSS4JAttachment> getAllResponseAttachments ()
  {
    return m_aAttachmentMap.copyOfValues ();
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsOrderedMap <String, WSS4JAttachment> responseAttachments ()
  {
    return m_aAttachmentMap;
  }
}
