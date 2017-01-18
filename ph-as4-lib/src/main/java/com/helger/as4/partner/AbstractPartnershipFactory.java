/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as4.partner;

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
