package com.helger.as4server.settings;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public class AS4ServerSettings
{
  private final static String DEFAULT_RESPONDER_ID = "default";

  private static String m_sResponderID = DEFAULT_RESPONDER_ID;

  private AS4ServerSettings ()
  {}

  public static String getDefaultResponderID ()
  {
    return m_sResponderID;
  }

  public static void setDefaultResponderID (@Nonnull @Nonempty final String sResponderID)
  {
    ValueEnforcer.notEmpty (sResponderID, "ResponderID");
    m_sResponderID = sResponderID;
  }
}
