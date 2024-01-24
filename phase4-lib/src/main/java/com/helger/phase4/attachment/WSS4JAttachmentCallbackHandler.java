/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.attachment;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.stream.HasInputStream;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.util.AS4ResourceHelper;

/**
 * A Callback Handler implementation for the case of signing/encrypting
 * Attachments via the SwA (SOAP with Attachments) specification or when using
 * xop:Include in the case of MTOM. This class is used both in
 * signing/encryption and in verification/decryption
 *
 * @author Apache WSS4J
 * @author Philip Helger
 */
public class WSS4JAttachmentCallbackHandler implements CallbackHandler
{
  public static final String ATTACHMENT_ID_ATTACHMENTS = "Attachments";

  private static final Logger LOGGER = LoggerFactory.getLogger (WSS4JAttachmentCallbackHandler.class);

  private final ICommonsOrderedMap <String, WSS4JAttachment> m_aAttachmentMap = new CommonsLinkedHashMap <> ();
  private final AS4ResourceHelper m_aResHelper;

  public WSS4JAttachmentCallbackHandler (@Nullable final Iterable <? extends WSS4JAttachment> aSrcAttachments,
                                         @Nonnull @WillNotClose final AS4ResourceHelper aResHelper)
  {
    ValueEnforcer.notNull (aResHelper, "ResHelper");

    if (aSrcAttachments != null)
      for (final WSS4JAttachment aAttachment : aSrcAttachments)
      {
        final String sID = aAttachment.getId ();
        if (m_aAttachmentMap.containsKey (sID))
        {
          LOGGER.error ("Another attachment with ID '" +
                        sID +
                        "' is already contained in the Map. The latter one will override the previous object");
        }
        m_aAttachmentMap.put (sID, aAttachment);
      }
    m_aResHelper = aResHelper;
  }

  /**
   * @return The resource manager as passed in the constructor. Never
   *         <code>null</code>.
   */
  @Nonnull
  public final AS4ResourceHelper getResourceHelper ()
  {
    return m_aResHelper;
  }

  /**
   * Try to match the Attachment Id. Otherwise, add all Attachments if the ID
   * "Attachments" is used.
   *
   * @param sAttachmentID
   *        Attachment ID to search
   * @return Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <Attachment> _getAttachmentsToAdd (@Nullable final String sAttachmentID)
  {
    // Check for direct match
    final WSS4JAttachment aAttachment = m_aAttachmentMap.get (sAttachmentID);
    if (aAttachment != null)
      return new CommonsArrayList <> (aAttachment);

    // Use all (stripped from cid:Attachments)
    if (ATTACHMENT_ID_ATTACHMENTS.equals (sAttachmentID))
      return new CommonsArrayList <> (m_aAttachmentMap.values ());

    throw new IllegalStateException ("Failed to resolve attachment with ID '" +
                                     sAttachmentID +
                                     "' in " +
                                     m_aAttachmentMap.keySet ());
  }

  public void handle (@Nonnull final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof AttachmentRequestCallback)
      {
        final AttachmentRequestCallback aAttachmentRequestCallback = (AttachmentRequestCallback) aCallback;

        // Get attachment ID
        final String sAttachmentID = aAttachmentRequestCallback.getAttachmentId ();
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Requesting attachment ID '" + sAttachmentID + "'");

        // Get attachments
        final ICommonsList <Attachment> aAttachments = _getAttachmentsToAdd (sAttachmentID);
        if (aAttachments.isEmpty ())
          throw new IllegalStateException ("No attachments present for ID '" +
                                           sAttachmentID +
                                           "' in " +
                                           m_aAttachmentMap.keySet ());

        // Invoke callback
        aAttachmentRequestCallback.setAttachments (aAttachments);
      }
      else
        if (aCallback instanceof AttachmentResultCallback)
        {
          final AttachmentResultCallback aAttachmentResultCallback = (AttachmentResultCallback) aCallback;

          // Attachment ID
          final String sAttachmentID = aAttachmentResultCallback.getAttachmentId ();
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Resulting attachment ID '" + sAttachmentID + "'");

          // Source attachment
          final WSS4JAttachment aSrcAttachment = m_aAttachmentMap.get (sAttachmentID);
          if (aSrcAttachment == null)
            throw new IllegalStateException ("Failed to resolve source attachment with ID '" +
                                             sAttachmentID +
                                             "' in " +
                                             m_aAttachmentMap.keySet ());

          // Decrypted attachment
          final Attachment aAttachmentResult = aAttachmentResultCallback.getAttachment ();

          // Convert
          final WSS4JAttachment aEffectiveResultAttachment = new WSS4JAttachment (m_aResHelper,
                                                                                  aAttachmentResult.getMimeType ());
          aEffectiveResultAttachment.setId (sAttachmentID);
          aEffectiveResultAttachment.addHeaders (aAttachmentResult.getHeaders ());
          // This property is only in WSS4JAttachment so we need to copy it
          // separately
          aEffectiveResultAttachment.setCharset (aSrcAttachment.getCharsetOrDefault (null));
          // Use supplier to ensure stream is opened only when needed
          aEffectiveResultAttachment.setSourceStreamProvider (HasInputStream.once (aAttachmentResult::getSourceStream));

          // Overwrite decrypted attachment in the Map
          m_aAttachmentMap.put (sAttachmentID, aEffectiveResultAttachment);
        }
        else
        {
          throw new UnsupportedCallbackException (aCallback,
                                                  "Unrecognized Callback of class " + aCallback.getClass ().getName ());
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("AttachmentMap", m_aAttachmentMap)
                                       .append ("ResHelper", m_aResHelper)
                                       .getToString ();
  }
}
