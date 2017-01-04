package com.helger.as4server.settings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4lib.model.pmode.config.DefaultPModeConfigResolver;
import com.helger.as4lib.model.pmode.config.IPModeConfigResolver;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

@NotThreadSafe
public class AS4ServerSettings
{
  private final static String DEFAULT_RESPONDER_ID = "default";

  private static String m_sResponderID = DEFAULT_RESPONDER_ID;
  private static IPModeConfigResolver s_aPModeConfigResolver = new DefaultPModeConfigResolver ();

  private AS4ServerSettings ()
  {}

  @Nonnull
  @Nonempty
  public static String getDefaultResponderID ()
  {
    return m_sResponderID;
  }

  public static void setDefaultResponderID (@Nonnull @Nonempty final String sResponderID)
  {
    ValueEnforcer.notEmpty (sResponderID, "ResponderID");
    m_sResponderID = sResponderID;
  }

  @Nonnull
  public static IPModeConfigResolver getPModeConfigResolver ()
  {
    return s_aPModeConfigResolver;
  }

  public static void setPModeConfigResolver (@Nonnull final IPModeConfigResolver aPModeConfigResolver)
  {
    ValueEnforcer.notNull (aPModeConfigResolver, "PModeConfigResolver");
    s_aPModeConfigResolver = aPModeConfigResolver;
  }
}
