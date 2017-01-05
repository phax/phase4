/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as4lib.util.IStringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.collection.ext.ICommonsOrderedSet;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;

/**
 * The default implementation of {@link IPartnershipMap}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PartnershipMap implements IPartnershipMap
{
  private final ICommonsOrderedMap <String, Partnership> m_aMap = new CommonsLinkedHashMap<> ();

  public PartnershipMap ()
  {}

  /**
   * Set all partnerships from the passed map. All existing partnerships are
   * removed.
   *
   * @param aPartnerships
   *        The partnerships to be set. May not be <code>null</code>.
   */
  public void setPartnerships (@Nonnull final PartnershipMap aPartnerships)
  {
    ValueEnforcer.notNull (aPartnerships, "Partnerships");
    m_aMap.setAll (aPartnerships.m_aMap);
  }

  /**
   * Add a new partnership.
   *
   * @param aPartnership
   *        The partnership to be added. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if adding was successfully,
   *         {@link EChange#UNCHANGED} if a partnership with the given name is
   *         already present and nothing changed.
   */
  @Nonnull
  public EChange addPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    final String sName = aPartnership.getName ();
    if (m_aMap.containsKey (sName))
      return EChange.UNCHANGED;
    m_aMap.put (sName, aPartnership);
    return EChange.CHANGED;
  }

  /**
   * Overwrite an existing partnership.
   *
   * @param aPartnership
   *        The partnership to be set (and potentially overwritten). May not be
   *        <code>null</code>.
   */
  public void setPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    // overwrite if already present
    m_aMap.put (aPartnership.getName (), aPartnership);
  }

  /**
   * Remove the provided partnership.
   *
   * @param aPartnership
   *        The partnership to be removed. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if removal was successful,
   *         {@link EChange#UNCHANGED} if no such partnership is contained.
   */
  @Nonnull
  public EChange removePartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    return EChange.valueOf (m_aMap.remove (aPartnership.getName ()) != null);
  }

  @Nullable
  public Partnership getPartnershipByName (@Nullable final String sName)
  {
    return m_aMap.get (sName);
  }

  /**
   * @param aSearchIDs
   *        Search IDs. May not be <code>null</code>.
   * @param aPartnerIDs
   *        Partner IDs. May not be <code>null</code>.
   * @return <code>true</code> if searchIds is not empty and if all values in
   *         searchIds match values in partnerIds. This means that partnerIds
   *         can contain more elements than searchIds
   */
  private static boolean _arePartnerIDsPresent (@Nonnull final IStringMap aSearchIDs,
                                                @Nonnull final IStringMap aPartnerIDs)
  {
    if (aSearchIDs.isEmpty ())
      return false;

    for (final Map.Entry <String, String> aSearchEntry : aSearchIDs)
    {
      final String sSearchValue = aSearchEntry.getValue ();
      final String sPartnerValue = aPartnerIDs.getAttributeAsString (aSearchEntry.getKey ());
      if (!EqualsHelper.equals (sSearchValue, sPartnerValue))
        return false;
    }
    return true;
  }

  @Nullable
  public Partnership getPartnershipByID (@Nonnull final IStringMap aSenderIDs, @Nonnull final IStringMap aReceiverIDs)
  {
    // For all partnerships
    for (final Partnership aPartnership : m_aMap.values ())
    {
      // Get all sender attributes of the current partnership
      final IStringMap aCurrentSenderIDs = aPartnership.getAllSenderIDs ();
      // Do the sender attributes of the current partnership match?
      if (_arePartnerIDsPresent (aSenderIDs, aCurrentSenderIDs))
      {
        // Get the receiver attributes of the current partnership
        final IStringMap aCurrentReceiverIDs = aPartnership.getAllReceiverIDs ();
        // Do the sender attributes of the current partnership match?
        if (_arePartnerIDsPresent (aReceiverIDs, aCurrentReceiverIDs))
        {
          // We take the first match :)
          return aPartnership;
        }
      }
    }

    return null;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedSet <String> getAllPartnershipNames ()
  {
    return m_aMap.copyOfKeySet ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Partnership> getAllPartnerships ()
  {
    return m_aMap.copyOfValues ();
  }
}
