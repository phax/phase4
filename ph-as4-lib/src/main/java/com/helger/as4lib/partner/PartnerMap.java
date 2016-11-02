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
