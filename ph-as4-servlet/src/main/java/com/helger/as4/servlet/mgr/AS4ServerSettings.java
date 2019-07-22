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
package com.helger.as4.servlet.mgr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.commons.ValueEnforcer;

@NotThreadSafe
public final class AS4ServerSettings
{
  private static IIncomingAttachmentFactory s_aIncomingAttachmentFactory = WSS4JAttachment::createIncomingFileAttachment;

  private AS4ServerSettings ()
  {}

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
}
