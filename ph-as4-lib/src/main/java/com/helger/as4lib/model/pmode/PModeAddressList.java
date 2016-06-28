/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4lib.model.pmode;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;

public class PModeAddressList
{
  public static final char ADDRESS_SEPARATOR = ',';

  private final ICommonsList <String> m_aAddresses = new CommonsArrayList<> ();

  public PModeAddressList ()
  {}

  public PModeAddressList (@Nullable final String... aAddresses)
  {
    if (aAddresses != null)
      for (final String sAddress : aAddresses)
        addAddress (sAddress);
  }

  public PModeAddressList (@Nullable final Collection <String> aAddresses)
  {
    if (aAddresses != null)
      m_aAddresses.addAll (aAddresses);
  }

  public void addAddress (@Nonnull @Nonempty final String sAddress)
  {
    ValueEnforcer.notEmpty (sAddress, "Address");
    m_aAddresses.add (sAddress);
  }

  public void removeAddress (@Nullable final String sAddress)
  {
    m_aAddresses.remove (sAddress);
  }

  public void removeAllAddresses ()
  {
    m_aAddresses.clear ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllAddresses ()
  {
    return m_aAddresses.getClone ();
  }

  @Nonnegative
  public int getAddressCount ()
  {
    return m_aAddresses.size ();
  }

  public boolean isEmpty ()
  {
    return m_aAddresses.isEmpty ();
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
  public static PModeAddressList createFromString (@Nullable final String sAddressString)
  {
    final ICommonsList <String> aAddresses = StringHelper.getExploded (ADDRESS_SEPARATOR, sAddressString);
    return new PModeAddressList (aAddresses);
  }
}
