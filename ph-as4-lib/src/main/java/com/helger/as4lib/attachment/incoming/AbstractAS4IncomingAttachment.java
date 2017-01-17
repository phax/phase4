/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4lib.attachment.incoming;

import javax.annotation.Nonnull;

import org.apache.wss4j.common.util.AttachmentUtils;

import com.helger.commons.collection.attr.MapBasedAttributeContainer;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;

/**
 * Abstract base class for incoming attachments.
 *
 * @author Philip Helger
 */
public abstract class AbstractAS4IncomingAttachment extends MapBasedAttributeContainer <String, String>
                                                    implements IAS4IncomingAttachment
{
  @Override
  @Nonnull
  public EChange setAttribute (final String sName, final String sValue)
  {
    String sRealValue;
    if (AttachmentUtils.MIME_HEADER_CONTENT_ID.equalsIgnoreCase (sName))
    {
      // Reference in header is: <ID>
      // See
      // http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
      // chapter 5.2
      sRealValue = StringHelper.trimStartAndEnd (sValue, '<', '>');
    }
    else
      sRealValue = sValue;
    return super.setAttribute (sName, sRealValue);
  }
}
