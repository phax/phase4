package com.helger.phase4.profile.euctp;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * This enum contains all the EU CTP AS4 services.
 *
 * @author Jon Rios
 * @author Philip Helger
 */
public enum EEuCtpService
{
  TRADER_TO_CUSTOMS ("eu_ics2_t2c"),
  CUSTOMS_TO_TRADER ("eu_ics2_c2t");

  private final String m_sValue;

  EEuCtpService (@Nonnull @Nonempty final String sValue)
  {
    m_sValue = sValue;
  }

  @Nonnull
  @Nonempty
  public String getValue ()
  {
    return m_sValue;
  }
}
