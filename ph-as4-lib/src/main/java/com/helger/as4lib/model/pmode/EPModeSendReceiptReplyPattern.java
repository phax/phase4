package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the different sendReceipt reply-patterns
 * 
 * @author bayerlma
 */
public enum EPModeSendReceiptReplyPattern implements IHasID <String>
{
  RESPONSE ("response"),
  CALLBACK ("callback");

  private final String m_sID;

  private EPModeSendReceiptReplyPattern (@Nonnull @Nonempty final String sID)
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
  public static EPModeSendReceiptReplyPattern getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPModeSendReceiptReplyPattern.class, sID);
  }
}
