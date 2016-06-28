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
package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.name.IHasName;
import com.helger.commons.state.EMandatory;
import com.helger.commons.state.IMandatoryIndicator;
import com.helger.commons.string.StringHelper;

/**
 * A payload part is a data structure that consists of five properties: name (or
 * Content-ID) that is the part identifier, and can be used as an index in the
 * notation PayloadProfile[]; MIME data type (<code>text/xml</code>,
 * <code>application/pdf</code>, etc.); name of the applicable XML Schema file
 * if the MIME data type is text/xml; maximum size in kilobytes; and a Boolean
 * value indicating whether the part is expected or optional, within the User
 * message. The message payload(s) must match this profile.
 *
 * @author Philip Helger
 */
public class PModePayloadProfile implements IHasName, IMandatoryIndicator
{
  private final String m_sName;
  private final IMimeType m_aMimeType;
  private final String m_sXSDFilename;
  private final Integer m_aMaxSizeKB;
  private final EMandatory m_eMandatory;

  public PModePayloadProfile (@Nonnull @Nonempty final String sName,
                              @Nonnull final IMimeType aMimeType,
                              @Nullable final String sXSDFilename,
                              @Nullable final Integer aMaxSizeKB,
                              @Nonnull final EMandatory eMandatory)
  {
    m_sName = ValueEnforcer.notEmpty (sName, "Name");
    m_aMimeType = ValueEnforcer.notNull (aMimeType, "MimeType");
    m_sXSDFilename = sXSDFilename;
    m_aMaxSizeKB = aMaxSizeKB;
    m_eMandatory = ValueEnforcer.notNull (eMandatory, "Mandatory");
  }

  @Nonnull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  @Nullable
  public String getXSDFilename ()
  {
    return m_sXSDFilename;
  }

  public boolean hasXSDFilename ()
  {
    return StringHelper.hasText (m_sXSDFilename);
  }

  @Nullable
  public Integer getMaxSizeKB ()
  {
    return m_aMaxSizeKB;
  }

  public boolean hasMaxSizeKB ()
  {
    return m_aMaxSizeKB != null;
  }

  public boolean isMandatory ()
  {
    return m_eMandatory.isMandatory ();
  }

  public boolean isOptional ()
  {
    return m_eMandatory.isOptional ();
  }
}
