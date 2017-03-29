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
package com.helger.as4;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

/**
 * Defines everything that is related to AS4 for this entity acting as a sender
 * and as a receiver.
 *
 * @author Philip Helger
 */
public class AS4Me
{
  private String m_sID;

  public AS4Me ()
  {}

  public void initMe (@Nonnull @Nonempty final String sID)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    final String ret = m_sID;
    if (StringHelper.hasNoText (ret))
      throw new IllegalStateException ("AS4Me ID was not initialized!");
    return ret;
  }
}
