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
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.collection.ext.ICommonsOrderedSet;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;

/**
 * Defines a map with all known partners.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PartnerMap implements IPartnerMap
{
  private final ICommonsOrderedMap <String, Partner> m_aMap = new CommonsLinkedHashMap<> ();

  public PartnerMap ()
  {}

  public void addPartner (@Nonnull final Partner aNewPartner)
  {
    ValueEnforcer.notNull (aNewPartner, "NewPartner");

    final String sName = aNewPartner.getName ();
    if (m_aMap.containsKey (sName))
      throw new IllegalStateException ("Partner is defined more than once: '" + sName + "'");

    m_aMap.put (sName, aNewPartner);
  }

  public void setPartners (@Nonnull final PartnerMap aPartners)
  {
    ValueEnforcer.notNull (aPartners, "Partners");
    m_aMap.setAll (aPartners.m_aMap);
  }

  @Nonnull
  public EChange removePartner (@Nullable final String sPartnerName)
  {
    return m_aMap.removeObject (sPartnerName);
  }

  @Nullable
  public Partner getPartnerOfName (@Nullable final String sPartnerName)
  {
    return m_aMap.get (sPartnerName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedSet <String> getAllPartnerNames ()
  {
    return m_aMap.copyOfKeySet ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Partner> getAllPartners ()
  {
    return m_aMap.copyOfValues ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Map", m_aMap).toString ();
  }
}
