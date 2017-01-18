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
package com.helger.as4.servlet.spi;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents the result of a message processor SPI implementation.
 * 
 * @author Philip Helger
 */
public class AS4MessageProcessorResult implements ISuccessIndicator, Serializable
{
  private final ESuccess m_eSuccess;

  public AS4MessageProcessorResult (@Nonnull final ESuccess eSuccess)
  {
    m_eSuccess = ValueEnforcer.notNull (eSuccess, "Success");
  }

  public boolean isSuccess ()
  {
    return m_eSuccess.isSuccess ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Success", m_eSuccess).toString ();
  }
}
