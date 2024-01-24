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
package com.helger.phase4.profile;

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;

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
  private final Supplier <? extends IAS4ProfileValidator> m_aProfileValidatorProvider;
  private final IAS4ProfilePModeProvider m_aDefaultPModeProvider;
  private final IPModeIDProvider m_aPModeIDProvider;
  private final boolean m_bDeprecated;
  private final boolean m_bInvokeSPIForPingMessage;

  /**
   * Constructor
   *
   * @param sID
   *        Profile ID. May neither be <code>null</code> nor empty.
   * @param sDisplayName
   *        Profile display name. May neither be <code>null</code> nor empty.
   * @param aProfileValidatorProvider
   *        Profile validator supplier. May not be <code>null</code>. The
   *        supplier may supply <code>null</code> values.
   * @param aDefaultPModeProvider
   *        Default PMode supplier. May not be <code>null</code>.
   * @param aPModeIDProvider
   *        PMode ID provider. May not be <code>null</code>.
   * @param bDeprecated
   *        <code>true</code> if the profile is deprecated, <code>false</code>
   *        if not.
   * @param bInvokeSPIForPingMessage
   *        <code>true</code> to invoke the custom SPI handler for received
   */
  public AS4Profile (@Nonnull @Nonempty final String sID,
                     @Nonnull @Nonempty final String sDisplayName,
                     @Nonnull final Supplier <? extends IAS4ProfileValidator> aProfileValidatorProvider,
                     @Nonnull final IAS4ProfilePModeProvider aDefaultPModeProvider,
                     @Nonnull final IPModeIDProvider aPModeIDProvider,
                     final boolean bDeprecated,
                     final boolean bInvokeSPIForPingMessage)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    ValueEnforcer.notNull (aProfileValidatorProvider, "ProfileValidatorProvider");
    ValueEnforcer.notNull (aDefaultPModeProvider, "aDefaultPModeProvider");
    ValueEnforcer.notNull (aPModeIDProvider, "PModeIDProvider");

    m_sID = sID;
    m_sDisplayName = sDisplayName;
    m_aProfileValidatorProvider = aProfileValidatorProvider;
    m_aDefaultPModeProvider = aDefaultPModeProvider;
    m_aPModeIDProvider = aPModeIDProvider;
    m_bDeprecated = bDeprecated;
    m_bInvokeSPIForPingMessage = bInvokeSPIForPingMessage;
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

  @Nullable
  public IAS4ProfileValidator getValidator ()
  {
    return m_aProfileValidatorProvider.get ();
  }

  @Nonnull
  public PMode createPModeTemplate (@Nonnull @Nonempty final String sInitiatorID,
                                    @Nonnull @Nonempty final String sResponderID,
                                    @Nullable final String sAddress)
  {
    return m_aDefaultPModeProvider.getOrCreatePMode (sInitiatorID, sResponderID, sAddress);
  }

  @Nonnull
  public IPModeIDProvider getPModeIDProvider ()
  {
    return m_aPModeIDProvider;
  }

  public boolean isDeprecated ()
  {
    return m_bDeprecated;
  }

  public boolean isInvokeSPIForPingMessage ()
  {
    return m_bInvokeSPIForPingMessage;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final AS4Profile rhs = (AS4Profile) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ID", m_sID)
                                       .append ("DisplayName", m_sDisplayName)
                                       .append ("ProfileValidatorProvider", m_aProfileValidatorProvider)
                                       .append ("DefaultPModeProvider", m_aDefaultPModeProvider)
                                       .append ("PModeIDProvider", m_aPModeIDProvider)
                                       .append ("Deprecated", m_bDeprecated)
                                       .append ("InvokeSPIForPingMessage", m_bInvokeSPIForPingMessage)
                                       .getToString ();
  }
}
