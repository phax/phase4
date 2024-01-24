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
package com.helger.phase4.model.pmode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringParser;
import com.helger.xml.microdom.convert.IMicroTypeConverter;

/**
 * Abstract base class with utility methods.
 *
 * @author Philip Helger
 * @param <T>
 *        Type to be converted from and to XML
 */
public abstract class AbstractPModeMicroTypeConverter <T> implements IMicroTypeConverter <T>
{
  protected AbstractPModeMicroTypeConverter ()
  {}

  @Nonnull
  public static ETriState getTriState (@Nullable final String sAttrValue, final boolean bDefault)
  {
    return sAttrValue == null ? ETriState.UNDEFINED : ETriState.valueOf (StringParser.parseBool (sAttrValue, bDefault));
  }
}
