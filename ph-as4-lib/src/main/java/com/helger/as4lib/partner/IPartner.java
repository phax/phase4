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

import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.ICommonsIterable;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.name.IHasName;
import com.helger.photon.basic.object.IObject;

/**
 * Read-only interface for a single partner that is used in a partnership.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public interface IPartner extends IHasName, ICommonsIterable <Map.Entry <String, String>>, IObject
{
  /**
   * @return All contained attributes. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsMap <String, String> getAllAttributes ();
}
