/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.attachment;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;

/**
 * A Callback Handler implementation for the case of signing/encrypting
 * Attachments via the SwA (SOAP with Attachments) specification or when using
 * xop:Include in the case of MTOM.
 *
 * @author Apache WSS4J
 */
public class AttachmentCallbackHandler implements CallbackHandler
{
  private final ICommonsList <Attachment> m_aOriginalRequestAttachments = new CommonsArrayList<> ();
  private final ICommonsMap <String, Attachment> m_aAttachmentMap = new CommonsHashMap<> ();
  private final ICommonsList <Attachment> m_aResponseAttachments = new CommonsArrayList<> ();

  public AttachmentCallbackHandler (@Nullable final Iterable <Attachment> aAttachments)
  {
    if (aAttachments != null)
      for (final Attachment aAttachment : aAttachments)
      {
        m_aOriginalRequestAttachments.add (aAttachment);
        m_aAttachmentMap.put (aAttachment.getId (), aAttachment);
      }
  }

  // Try to match the Attachment Id. Otherwise, add all Attachments.
  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <Attachment> _getAttachmentsToAdd (final String sID)
  {
    final ICommonsList <Attachment> attachments = new CommonsArrayList<> ();
    if (m_aAttachmentMap.containsKey (sID))
      attachments.add (m_aAttachmentMap.get (sID));
    else
      attachments.addAll (m_aOriginalRequestAttachments);
    return attachments;
  }

  public void handle (final Callback [] aCallbacks) throws IOException, UnsupportedCallbackException
  {
    for (final Callback aCallback : aCallbacks)
    {
      if (aCallback instanceof AttachmentRequestCallback)
      {
        final AttachmentRequestCallback aAttachmentRequestCallback = (AttachmentRequestCallback) aCallback;

        final String sAttachmentID = aAttachmentRequestCallback.getAttachmentId ();
        final ICommonsList <Attachment> aAttachments = _getAttachmentsToAdd (sAttachmentID);
        if (aAttachments.isEmpty ())
          throw new RuntimeException ("wrong attachment requested (ID=" + sAttachmentID + ")");

        aAttachmentRequestCallback.setAttachments (aAttachments);
      }
      else
        if (aCallback instanceof AttachmentResultCallback)
        {
          final AttachmentResultCallback aAttachmentResultCallback = (AttachmentResultCallback) aCallback;
          final Attachment aResponseAttachment = aAttachmentResultCallback.getAttachment ();
          m_aResponseAttachments.add (aResponseAttachment);
          m_aAttachmentMap.put (aResponseAttachment.getId (), aResponseAttachment);
        }
        else
        {
          throw new UnsupportedCallbackException (aCallback, "Unrecognized Callback");
        }
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Attachment> getResponseAttachments ()
  {
    return m_aResponseAttachments.getClone ();
  }
}
