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
package com.helger.phase4.wss;

import javax.annotation.Nullable;

import org.apache.wss4j.dom.WsuIdAllocator;

import com.helger.commons.string.StringHelper;
import com.helger.phase4.model.message.MessageHelperMethods;

/**
 * phase4 specific implementation of {@link WsuIdAllocator}.
 *
 * @author Philip Helger
 * @since 0.10.4
 */
public class AS4WsuIdAllocator implements WsuIdAllocator
{
  public String createId (@Nullable final String sPrefix, final Object o)
  {
    return createSecureId (sPrefix, o);
  }

  public String createSecureId (final String sPrefix, final Object o)
  {
    return StringHelper.getConcatenatedOnDemand (sPrefix, "-", MessageHelperMethods.createRandomWSUID ());
  }
}
