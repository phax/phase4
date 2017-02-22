package com.helger.as4.messaging.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the meta message types.
 *
 * @author Philip Helger
 */
public enum EAS4MessageType implements IHasID <String>
{
  ERROR_MESSAGE ("errormsg"),
  PULL_REQUEST ("pullreq"),
  RECEIPT ("receipt"),
  USER_MESSAGE ("usermsg");

  private final String m_sID;

  private EAS4MessageType (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isUserMessage ()
  {
    return this == USER_MESSAGE;
  }

  public boolean isSignalMessage ()
  {
    return this != USER_MESSAGE;
  }

  public boolean isReceiptOrError ()
  {
    return this == RECEIPT || this == ERROR_MESSAGE;
  }

  @Nullable
  public static EAS4MessageType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EAS4MessageType.class, sID);
  }
}
