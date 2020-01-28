package com.helger.phase4.messaging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Incoming message mode
 *
 * @author Philip Helger
 * @since 0.9.8
 */
public enum EAS4IncomingMessageMode implements IHasID <String>
{
  REQUEST ("request"),
  RESPONSE ("response");

  private final String m_sID;

  private EAS4IncomingMessageMode (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static EAS4IncomingMessageMode getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4IncomingMessageMode.class, sID);
  }
}
