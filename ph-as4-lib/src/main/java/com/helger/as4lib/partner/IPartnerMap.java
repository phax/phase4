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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as4lib.util.StringMap;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsOrderedSet;

/**
 * Read-only interface to manage the mapping from name to partner (represented
 * by a {@link StringMap}).
 *
 * @author Philip Helger
 */
public interface IPartnerMap
{
  @Nullable
  IPartner getPartnerOfName (@Nullable String sPartnerName);

  @Nonnull
  @ReturnsMutableCopy
  ICommonsOrderedSet <String> getAllPartnerNames ();

  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <? extends IPartner> getAllPartners ();
}
