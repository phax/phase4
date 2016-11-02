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

import java.util.Iterator;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.helger.as4lib.util.IStringMap;
import com.helger.as4lib.util.StringMap;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents a single partner. A partnership consists of 2 partners
 * - a sender and a receiver.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public class Partner implements IPartner
{
  public static final String PARTNER_NAME = "name";

  private final StringMap m_aAttrs;

  public Partner (@Nonnull final IStringMap aAttrs)
  {
    m_aAttrs = new StringMap (aAttrs);
    if (!m_aAttrs.containsAttribute (PARTNER_NAME))
      throw new IllegalArgumentException ("The provided attributes are missing the required '" +
                                          PARTNER_NAME +
                                          "' attribute!");
  }

  @Nonnull
  public String getName ()
  {
    return m_aAttrs.getAttributeAsString (PARTNER_NAME);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllAttributes ()
  {
    return m_aAttrs.getAllAttributes ();
  }

  @Nonnull
  public Iterator <Entry <String, String>> iterator ()
  {
    return m_aAttrs.iterator ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final Partner rhs = (Partner) o;
    return m_aAttrs.equals (rhs.m_aAttrs);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aAttrs).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Attrs", m_aAttrs).toString ();
  }
}
