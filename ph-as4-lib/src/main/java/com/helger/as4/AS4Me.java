package com.helger.as4;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Defines everything that is related to AS4 for this entity acting as a sender
 * and as a receiver.
 *
 * @author Philip Helger
 */
public class AS4Me
{
  private String m_sID;

  public AS4Me ()
  {}

  public void initMe (@Nonnull @Nonempty final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    final String ret = m_sID;
    if (StringHelper.hasNoText (ret))
      throw new IllegalStateException ("AS4Me ID was not initialized!");
    return ret;
  }
}
