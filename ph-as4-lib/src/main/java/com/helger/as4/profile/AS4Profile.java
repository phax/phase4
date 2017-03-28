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
package com.helger.as4.profile;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.as4.model.pmode.PMode;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.function.ISupplier;
import com.helger.commons.string.ToStringGenerator;

/**
 * Default implementation of {@link IAS4Profile}.
 *
 * @author Philip Helger
 */
@Immutable
public class AS4Profile implements IAS4Profile
{
  private final String m_sID;
  private final String m_sDisplayName;
  private boolean m_bDynamicID;
  private final ISupplier <? extends IAS4ProfileValidator> m_aProfileValidatorProvider;
  private final ISupplier <? extends PMode> m_aDefaultPModeProvider;

  public AS4Profile (@Nonnull @Nonempty final String sID,
                     @Nonnull @Nonempty final String sDisplayName,
                     final boolean bDynamicID,
                     @Nonnull final ISupplier <? extends IAS4ProfileValidator> aProfileValidatorProvider,
                     @Nonnull final ISupplier <? extends PMode> aDefaultPModeProvider)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_sDisplayName = ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    m_bDynamicID = bDynamicID;
    m_aProfileValidatorProvider = ValueEnforcer.notNull (aProfileValidatorProvider, "ProfileValidatorProvider");
    m_aDefaultPModeProvider = ValueEnforcer.notNull (aDefaultPModeProvider, "aDefaultPModeProvider");
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return m_sDisplayName;
  }

  public boolean isDynamicID ()
  {
    return m_bDynamicID;
  }

  /**
   * @param bDynamicID
   *        True if dynamic ID should be used looks like this:
   *        InitiatorID-ResponderID <br>
   *        False if you want to set your own id.
   */
  public void setDynamicID (final boolean bDynamicID)
  {
    m_bDynamicID = bDynamicID;
  }

  @Nonnull
  public IAS4ProfileValidator getValidator ()
  {
    return m_aProfileValidatorProvider.get ();
  }

  @Nonnull
  public PMode createPModeTemplate ()
  {
    return m_aDefaultPModeProvider.get ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("DisplayName", m_sDisplayName)
                                       .append ("DynamicID", m_bDynamicID)
                                       .append ("ProfileValidatorProvider", m_aProfileValidatorProvider)
                                       .append ("DefaultPModeProvider", m_aDefaultPModeProvider)
                                       .getToString ();
  }
}
