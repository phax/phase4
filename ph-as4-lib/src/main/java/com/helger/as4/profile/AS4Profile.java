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

import com.helger.as4.model.pmode.config.IPModeConfig;
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
  private final ISupplier <? extends IAS4ProfileValidator> m_aProfileValidatorProvider;
  private final ISupplier <? extends IPModeConfig> m_aDefaultPModeConfigProvider;

  public AS4Profile (@Nonnull @Nonempty final String sID,
                     @Nonnull @Nonempty final String sDisplayName,
                     @Nonnull final ISupplier <? extends IAS4ProfileValidator> aProfileValidatorProvider,
                     @Nonnull final ISupplier <? extends IPModeConfig> aDefaultPModeConfigProvider)
  {
    m_sID = ValueEnforcer.notEmpty (sID, "ID");
    m_sDisplayName = ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    m_aProfileValidatorProvider = ValueEnforcer.notNull (aProfileValidatorProvider, "ProfileValidatorProvider");
    m_aDefaultPModeConfigProvider = ValueEnforcer.notNull (aDefaultPModeConfigProvider, "aDefaultPModeConfigProvider");
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

  @Nonnull
  public IAS4ProfileValidator getValidator ()
  {
    return m_aProfileValidatorProvider.get ();
  }

  @Nonnull
  public IPModeConfig createDefaultPModeConfig ()
  {
    return m_aDefaultPModeConfigProvider.get ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("DisplayName", m_sDisplayName)
                                       .append ("ProfileValidatorProvider", m_aProfileValidatorProvider)
                                       .append ("DefaultPModeConfigProvider", m_aDefaultPModeConfigProvider)
                                       .toString ();
  }
}
