/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as4lib.partner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.attr.MapBasedAttributeContainerAny;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;

/**
 * Abstract {@link IPartnershipFactory} implementation using
 * {@link PartnershipMap} as the underlying data storage object.
 *
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractPartnershipFactory extends MapBasedAttributeContainerAny <String>
                                                 implements IPartnershipFactory
{
  @SuppressWarnings ("unused")
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractPartnershipFactory.class);

  protected final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final PartnershipMap m_aPartnerships = new PartnershipMap ();

  /**
   * Callback method that is invoked, when this object is modified. This method
   * must be overridden to do something useful. A use case scenario could e.g.
   * be automatic storage of changes. @ In case anything goes wrong
   */
  @OverrideOnDemand
  @IsLocked (ELockType.WRITE)
  protected void markAsChanged ()
  {}

  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public Partnership getPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");

    return m_aRWLock.readLockedThrowing ( () -> {
      Partnership aRealPartnership = m_aPartnerships.getPartnershipByName (aPartnership.getName ());
      if (aRealPartnership == null)
      {
        // Found no partnership by name
        aRealPartnership = m_aPartnerships.getPartnershipByID (aPartnership.getAllSenderIDs (),
                                                               aPartnership.getAllReceiverIDs ());
      }

      if (aRealPartnership == null)
        throw new PartnershipNotFoundException (aPartnership);
      return aRealPartnership;
    });
  }

  @Nullable
  public Partnership getPartnershipByName (@Nullable final String sName)
  {
    return m_aRWLock.readLocked ( () -> m_aPartnerships.getPartnershipByName (sName));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllPartnershipNames ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartnerships.getAllPartnershipNames ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Partnership> getAllPartnerships ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartnerships.getAllPartnerships ());
  }

  @Nonnull
  public IPartnershipMap getPartnershipMap ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartnerships);
  }

  protected final void setPartnerships (@Nonnull final PartnershipMap aPartnerships)
  {
    m_aRWLock.writeLockedThrowing ( () -> {
      m_aPartnerships.setPartnerships (aPartnerships);
      markAsChanged ();
    });
  }

  @Nonnull
  public final EChange addPartnership (@Nonnull final Partnership aPartnership)
  {
    return m_aRWLock.writeLockedThrowing ( () -> {
      if (m_aPartnerships.addPartnership (aPartnership).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    });
  }

  @Nonnull
  public final EChange removePartnership (@Nonnull final Partnership aPartnership)
  {
    return m_aRWLock.writeLockedThrowing ( () -> {
      if (m_aPartnerships.removePartnership (aPartnership).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    });
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("Partnerships", m_aPartnerships).toString ();
  }
}
