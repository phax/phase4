package com.helger.as4lib.model.pmode;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.ETriState;

public class PModeReceptionAwareness
{

  public static final boolean DEFAULT_RECEPTION_AWARENESS = true;
  public static final boolean DEFAULT_RETRY = true;
  public static final boolean DEFAULT_DUPLICATE_DETECTION = true;

  private ETriState m_bReceptionAwareness;

  private ETriState m_bRetry;

  private ETriState m_bDuplicateDetection;

  public PModeReceptionAwareness (@Nonnull final ETriState bReceptionAwareness,
                                  @Nonnull final ETriState bRetry,
                                  @Nonnull final ETriState bDuplicateDetection)
  {
    m_bReceptionAwareness = bReceptionAwareness;
    m_bRetry = bRetry;
    m_bDuplicateDetection = bDuplicateDetection;
  }

  public boolean isReceptionAwarenessDefined ()
  {
    return m_bReceptionAwareness.isDefined ();
  }

  @Nonnull
  public boolean isReceptionAwareness ()
  {
    return m_bReceptionAwareness.getAsBooleanValue (DEFAULT_RECEPTION_AWARENESS);
  }

  public void setReceptionAwareness (final boolean bReceptionAwareness)
  {
    setReceptionAwareness (ETriState.valueOf (bReceptionAwareness));
  }

  public void setReceptionAwareness (@Nonnull final ETriState eReceptionAwareness)
  {
    ValueEnforcer.notNull (eReceptionAwareness, "ReceptionAwareness");
    m_bReceptionAwareness = eReceptionAwareness;
  }

  public boolean isRetryDefined ()
  {
    return m_bRetry.isDefined ();
  }

  @Nonnull
  public boolean isRetry ()
  {
    return m_bRetry.getAsBooleanValue (DEFAULT_RETRY);
  }

  public void setRetry (final boolean bRetry)
  {
    setRetry (ETriState.valueOf (bRetry));
  }

  public void setRetry (@Nonnull final ETriState eRetry)
  {
    ValueEnforcer.notNull (eRetry, "Retry");
    m_bRetry = eRetry;
  }

  public boolean isDuplicateDetectionDefined ()
  {
    return m_bDuplicateDetection.isDefined ();
  }

  @Nonnull
  public boolean isDuplicateDetection ()
  {
    return m_bDuplicateDetection.getAsBooleanValue (DEFAULT_DUPLICATE_DETECTION);
  }

  public void setDuplicateDetection (final boolean bDuplicateDetection)
  {
    setDuplicateDetection (ETriState.valueOf (bDuplicateDetection));
  }

  public void setDuplicateDetection (@Nonnull final ETriState eDuplicateDetection)
  {
    ValueEnforcer.notNull (eDuplicateDetection, "DuplicateDetection");
    m_bDuplicateDetection = eDuplicateDetection;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeReceptionAwareness rhs = (PModeReceptionAwareness) o;
    return m_bReceptionAwareness.equals (rhs.m_bReceptionAwareness) &&
           EqualsHelper.equals (m_bRetry, rhs.m_bRetry) &&
           EqualsHelper.equals (m_bDuplicateDetection, rhs.m_bDuplicateDetection);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_bReceptionAwareness)
                                       .append (m_bRetry)
                                       .append (m_bDuplicateDetection)
                                       .getHashCode ();
  }
}
