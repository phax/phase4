/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * List of addresses in a PMode
 *
 * @author Philip Helger
 */
@NotThreadSafe
@MustImplementEqualsAndHashcode
public class PModeAddressList implements Serializable, ICloneable <PModeAddressList>
{
  public static final char ADDRESS_SEPARATOR = ',';

  private final ICommonsList <String> m_aAddresses = new CommonsArrayList <> ();

  public PModeAddressList ()
  {}

  public PModeAddressList (@Nullable final String... aAddresses)
  {
    if (aAddresses != null)
      addresses ().addAll (aAddresses);
  }

  public PModeAddressList (@Nullable final Iterable <? extends String> aAddresses)
  {
    if (aAddresses != null)
      addresses ().addAll (aAddresses);
  }

  /**
   * @return A mutable list of all contained addresses. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public final ICommonsList <String> addresses ()
  {
    return m_aAddresses;
  }

  /**
   * @return A copy of all contained addresses. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsList <String> getAllAddresses ()
  {
    return m_aAddresses.getClone ();
  }

  /**
   * @return All addresses as a single string, separated by a "comma char".
   *         Never <code>null</code>.
   */
  @Nonnull
  public String getAsString ()
  {
    return StringHelper.getImploded (ADDRESS_SEPARATOR, m_aAddresses);
  }

  @Nonnull
  @ReturnsMutableCopy
  public PModeAddressList getClone ()
  {
    return new PModeAddressList (m_aAddresses);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PModeAddressList rhs = (PModeAddressList) o;
    return m_aAddresses.equals (rhs.m_aAddresses);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aAddresses).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Addresses", m_aAddresses).getToString ();
  }

  /**
   * Create a new {@link PModeAddressList} based on a comma separated String.
   *
   * @param sAddressString
   *        The address string to be split. May be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list.
   */
  @Nonnull
  public static PModeAddressList createFromString (@Nullable final String sAddressString)
  {
    final ICommonsList <String> aAddresses = StringHelper.getExploded (ADDRESS_SEPARATOR, sAddressString);
    return new PModeAddressList (aAddresses);
  }
}
