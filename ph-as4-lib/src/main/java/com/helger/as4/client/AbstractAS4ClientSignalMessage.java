/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.as4.client;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

public abstract class AbstractAS4ClientSignalMessage extends AbstractAS4Client
{
  private final ICommonsList <Object> m_aAny = new CommonsArrayList <> ();

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Object> getAllAny ()
  {
    return m_aAny.getClone ();
  }

  public void setAny (@Nonnull final Iterable <Object> aAny)
  {
    ValueEnforcer.notNull (aAny, "Any");
    m_aAny.setAll (aAny);
  }
}
