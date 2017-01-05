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
package com.helger.as4lib.partner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsSet;
import com.helger.commons.state.EChange;

/**
 * Abstract {@link IPartnershipFactoryWithPartners} implementation based on
 * {@link AbstractPartnershipFactory} using {@link PartnerMap} as the underlying
 * data storage object for the partners.
 *
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractPartnershipFactoryWithPartners extends AbstractPartnershipFactory
                                                             implements IPartnershipFactoryWithPartners
{
  private final PartnerMap m_aPartners = new PartnerMap ();

  protected final void setPartners (@Nonnull final PartnerMap aPartners)
  {
    m_aRWLock.writeLockedThrowing ( () -> {
      m_aPartners.setPartners (aPartners);
      markAsChanged ();
    });
  }

  public void addPartner (@Nonnull final Partner aNewPartner)
  {
    m_aRWLock.writeLockedThrowing ( () -> {
      m_aPartners.addPartner (aNewPartner);
      markAsChanged ();
    });
  }

  @Nonnull
  public EChange removePartner (@Nullable final String sPartnerName)
  {
    return m_aRWLock.writeLockedThrowing ( () -> {
      if (m_aPartners.removePartner (sPartnerName).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    });
  }

  @Nullable
  public Partner getPartnerOfName (@Nullable final String sPartnerName)
  {
    return m_aRWLock.readLocked ( () -> m_aPartners.getPartnerOfName (sPartnerName));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllPartnerNames ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartners.getAllPartnerNames ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Partner> getAllPartners ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartners.getAllPartners ());
  }

  @Nonnull
  public IPartnerMap getPartnerMap ()
  {
    return m_aRWLock.readLocked ( () -> m_aPartners);
  }
}
