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
package com.helger.as4.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BadRequestException extends RuntimeException
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BadRequestException.class);

  public BadRequestException (final String sMsg)
  {
    super (sMsg);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("BadRequest: " + sMsg);
  }

  public BadRequestException (final String sMsg, final Throwable t)
  {
    super (sMsg + "; Technical details [" + t.getClass ().getName () + "]: " + t.getMessage ());
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("BadRequest: " + sMsg, t);
  }
}
