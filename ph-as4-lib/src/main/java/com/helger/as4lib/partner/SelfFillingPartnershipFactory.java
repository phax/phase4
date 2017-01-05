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
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.commons.annotation.OverrideOnDemand;

/**
 * A special in-memory partnership factory that adds a partnership if it is not
 * existing yet.
 *
 * @author Philip Helger
 */
public class SelfFillingPartnershipFactory extends AbstractPartnershipFactory
{
  /**
   * Callback method that is invoked every time a new partnership is
   * automatically added. This method is called BEFORE the main add-process is
   * started.
   *
   * @param aPartnership
   *        The partnership that will be added. Never <code>null</code>.
   */
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected void onBeforeAddPartnership (@Nonnull final Partnership aPartnership)
  {
    // Ensure the X509 key is contained for certificate store alias retrieval
    if (!aPartnership.containsSenderX509Alias ())
      aPartnership.setSenderX509Alias (aPartnership.getSenderAS2ID ());

    if (!aPartnership.containsReceiverX509Alias ())
      aPartnership.setReceiverX509Alias (aPartnership.getReceiverAS2ID ());
  }

  @Override
  @Nonnull
  public final Partnership getPartnership (@Nonnull final Partnership aPartnership)
  {
    try
    {
      return super.getPartnership (aPartnership);
    }
    catch (final PartnershipNotFoundException ex)
    {
      onBeforeAddPartnership (aPartnership);

      // Create a new one
      addPartnership (aPartnership);
      return aPartnership;
    }
  }
}
