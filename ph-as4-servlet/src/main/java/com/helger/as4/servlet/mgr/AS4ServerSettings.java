/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.servlet.mgr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.as4.model.pmode.resolve.IPModeResolver;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

@NotThreadSafe
public final class AS4ServerSettings
{
  private static final String DEFAULT_RESPONDER_ID = "default";

  private static String s_sResponderID = DEFAULT_RESPONDER_ID;
  private static IIncomingAttachmentFactory s_aIncomingAttachmentFactory = WSS4JAttachment::createIncomingFileAttachment;
  private static IPModeResolver s_aPModeResolver = new DefaultPModeResolver (false);
  private static AS4CryptoFactory s_aAS4CryptoFactory = AS4CryptoFactory.DEFAULT_INSTANCE;

  private AS4ServerSettings ()
  {}

  @Nonnull
  @Nonempty
  public static String getDefaultResponderID ()
  {
    return s_sResponderID;
  }

  public static void setDefaultResponderID (@Nonnull @Nonempty final String sResponderID)
  {
    ValueEnforcer.notEmpty (sResponderID, "ResponderID");
    s_sResponderID = sResponderID;
  }

  @Nonnull
  public static IIncomingAttachmentFactory getIncomingAttachmentFactory ()
  {
    return s_aIncomingAttachmentFactory;
  }

  public static void setIncomingAttachmentFactory (@Nonnull final IIncomingAttachmentFactory aIncomingAttachmentFactory)
  {
    ValueEnforcer.notNull (aIncomingAttachmentFactory, "IncomingAttachmentFactory");
    s_aIncomingAttachmentFactory = aIncomingAttachmentFactory;
  }

  @Nonnull
  public static IPModeResolver getPModeResolver ()
  {
    return s_aPModeResolver;
  }

  public static void setPModeResolver (@Nonnull final IPModeResolver aPModeResolver)
  {
    ValueEnforcer.notNull (aPModeResolver, "PModeResolver");
    s_aPModeResolver = aPModeResolver;
  }

  @Nonnull
  public static AS4CryptoFactory getAS4CryptoFactory ()
  {
    return s_aAS4CryptoFactory;
  }

  public static void setAS4CryptoFactory (@Nonnull final AS4CryptoFactory aAS4CryptoFactory)
  {
    ValueEnforcer.notNull (aAS4CryptoFactory, "AS4CryptoFactory");
    s_aAS4CryptoFactory = aAS4CryptoFactory;
  }
}
