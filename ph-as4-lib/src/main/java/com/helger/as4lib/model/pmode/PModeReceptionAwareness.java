package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.ETriState;

public class PModeReceptionAwareness
{
  public static final boolean DEFAULT_RECEPTION_AWARENESS = true;
  public static final boolean DEFAULT_RETRY = true;
  public static final boolean DEFAULT_DUPLICATE_DETECTION = true;

  private ETriState m_eReceptionAwareness;

  private ETriState m_eRetry;

  private ETriState m_eDuplicateDetection;

  public PModeReceptionAwareness (@Nonnull final ETriState eReceptionAwareness,
                                  @Nonnull final ETriState eRetry,
                                  @Nonnull final ETriState eDuplicateDetection)
  {
    setReceptionAwareness (eReceptionAwareness);
    setRetry (eRetry);
    setDuplicateDetection (eDuplicateDetection);
  }

  public boolean isReceptionAwarenessDefined ()
  {
    return m_eReceptionAwareness.isDefined ();
  }

  @Nonnull
  public boolean isReceptionAwareness ()
  {
    return m_eReceptionAwareness.getAsBooleanValue (DEFAULT_RECEPTION_AWARENESS);
  }

  public void setReceptionAwareness (final boolean bReceptionAwareness)
  {
    setReceptionAwareness (ETriState.valueOf (bReceptionAwareness));
  }

  public void setReceptionAwareness (@Nonnull final ETriState eReceptionAwareness)
  {
    ValueEnforcer.notNull (eReceptionAwareness, "ReceptionAwareness");
    m_eReceptionAwareness = eReceptionAwareness;
  }

  public boolean isRetryDefined ()
  {
    return m_eRetry.isDefined ();
  }

  @Nonnull
  public boolean isRetry ()
  {
    return m_eRetry.getAsBooleanValue (DEFAULT_RETRY);
  }

  public void setRetry (final boolean bRetry)
  {
    setRetry (ETriState.valueOf (bRetry));
  }

  public void setRetry (@Nonnull final ETriState eRetry)
  {
    ValueEnforcer.notNull (eRetry, "Retry");
    m_eRetry = eRetry;
  }

  public boolean isDuplicateDetectionDefined ()
  {
    return m_eDuplicateDetection.isDefined ();
  }

  @Nonnull
  public boolean isDuplicateDetection ()
  {
    return m_eDuplicateDetection.getAsBooleanValue (DEFAULT_DUPLICATE_DETECTION);
  }

  public void setDuplicateDetection (final boolean bDuplicateDetection)
  {
    setDuplicateDetection (ETriState.valueOf (bDuplicateDetection));
  }

  public void setDuplicateDetection (@Nonnull final ETriState eDuplicateDetection)
  {
    ValueEnforcer.notNull (eDuplicateDetection, "DuplicateDetection");
    m_eDuplicateDetection = eDuplicateDetection;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeReceptionAwareness rhs = (PModeReceptionAwareness) o;
    return m_eReceptionAwareness.equals (rhs.m_eReceptionAwareness) &&
           m_eRetry.equals (rhs.m_eRetry) &&
           m_eDuplicateDetection.equals (rhs.m_eDuplicateDetection);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eReceptionAwareness)
                                       .append (m_eRetry)
                                       .append (m_eDuplicateDetection)
                                       .getHashCode ();
  }
}
