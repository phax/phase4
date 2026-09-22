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
package com.helger.phase4.multihop.sender;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsList;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.messaging.mime.AS4MimeMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.model.message.EAS4MessageType;
import com.helger.phase4.multihop.soap.MultiHopSoapHelper;

/**
 * An {@link IAS4ClientBuildMessageCallback} that sets the <code>nextmsh</code> role (SOAP 1.2) or
 * actor (SOAP 1.1) attribute on the <code>eb:Messaging</code> element of an outgoing User Message -
 * R1, AS4 Profile section 4.3 <code>AddActorOrRoleAttribute</code>.
 * <p>
 * The attribute is set in {@code onAS4Message}, i.e. on the JAXB object before the SOAP document is
 * created. It is therefore part of the signed <code>eb:Messaging</code> element.
 * </p>
 * <p>
 * R2 - Pull Requests never get the attribute, because they are never routed through an I-Cloud.
 * </p>
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class MultiHopBuildMessageCallback implements IAS4ClientBuildMessageCallback
{
  private final IAS4ClientBuildMessageCallback m_aDelegate;
  private final boolean m_bActive;

  /**
   * Constructor.
   *
   * @param aDelegate
   *        An optional existing callback that is always invoked first. May be <code>null</code>.
   * @param bActive
   *        <code>true</code> to actually add the role/actor attribute.
   */
  public MultiHopBuildMessageCallback (@Nullable final IAS4ClientBuildMessageCallback aDelegate,
                                       final boolean bActive)
  {
    m_aDelegate = aDelegate;
    m_bActive = bActive;
  }

  /**
   * @return The optional delegate callback. May be <code>null</code>.
   */
  @Nullable
  public final IAS4ClientBuildMessageCallback getDelegate ()
  {
    return m_aDelegate;
  }

  /**
   * @return <code>true</code> if the role/actor attribute is added.
   */
  public final boolean isActive ()
  {
    return m_bActive;
  }

  @Override
  public void onAS4Message (@NonNull final AbstractAS4Message <?> aMsg)
  {
    if (m_aDelegate != null)
      m_aDelegate.onAS4Message (aMsg);

    // R2 - only User Messages, never Pull Requests or signal messages
    if (m_bActive && aMsg.getMessageType () == EAS4MessageType.USER_MESSAGE)
      MultiHopSoapHelper.setNextMSHRole (aMsg.getMessaging (), aMsg.getSoapVersion ());
  }

  @Override
  public void onBuiltAttachments (@NonNull @Nonempty final ICommonsList <WSS4JAttachment> aAttachments)
  {
    if (m_aDelegate != null)
      m_aDelegate.onBuiltAttachments (aAttachments);
  }

  @Override
  public void onSoapDocument (@NonNull final Document aDoc)
  {
    if (m_aDelegate != null)
      m_aDelegate.onSoapDocument (aDoc);
  }

  @Override
  public void onSignedSoapDocument (@NonNull final Document aDoc)
  {
    if (m_aDelegate != null)
      m_aDelegate.onSignedSoapDocument (aDoc);
  }

  @Override
  public void onEncryptedSoapDocument (@NonNull final Document aDoc)
  {
    if (m_aDelegate != null)
      m_aDelegate.onEncryptedSoapDocument (aDoc);
  }

  @Override
  public void onEncryptedMimeMessage (@NonNull final AS4MimeMessage aMimeMsg)
  {
    if (m_aDelegate != null)
      m_aDelegate.onEncryptedMimeMessage (aMimeMsg);
  }
}
