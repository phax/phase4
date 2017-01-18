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

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsSet;
import com.helger.commons.state.EChange;

/**
 * This partnership factory extends {@link IPartnershipFactory} by adding
 * "partners". This can be used for providing certain fixed value on a
 * per-partner basis (e.g. email address or X509 certificate alias to the
 * keystore) without having redundancy data in all partnerships.
 *
 * @author Philip Helger
 */
public interface IPartnershipFactoryWithPartners extends IPartnershipFactory
{
  /**
   * Add a partner.
   *
   * @param aNewPartner
   *        The partner data to be used. May not be <code>null</code>.
   */
  void addPartner (@Nonnull Partner aNewPartner);

  /**
   * Remove a partner.
   *
   * @param sPartnerName
   *        The name of the partner to be removed.
   * @return {@link EChange#CHANGED} if the partner was successfully removed,
   *         {@link EChange#UNCHANGED} if no such partner exists.
   */
  @Nonnull
  EChange removePartner (@Nullable String sPartnerName);

  /**
   * Get all the partner data of the partner with the given name.
   *
   * @param sPartnerName
   *        Partner name to search. May be <code>null</code>.
   * @return <code>null</code> if no such partner exists.
   */
  @Nullable
  IPartner getPartnerOfName (@Nullable String sPartnerName);

  /**
   * @return A set with all contained partner names. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsSet <String> getAllPartnerNames ();

  /**
   * @return An (unordered) list of all contained partner data.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <? extends IPartner> getAllPartners ();
}
