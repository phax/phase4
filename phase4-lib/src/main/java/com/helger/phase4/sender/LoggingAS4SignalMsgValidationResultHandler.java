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
package com.helger.phase4.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logging implementation of
 * {@link IAS4SignalMessageValidationResultHandler}.
 *
 * @author Philip Helger
 */
public class LoggingAS4SignalMsgValidationResultHandler implements IAS4SignalMessageValidationResultHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingAS4SignalMsgValidationResultHandler.class);

  public void onSuccess ()
  {
    LOGGER.info ("All sent DSig references were contained in the AS4 Receipt message - good.");
  }

  public void onError (final String sErrorMsg)
  {
    LOGGER.error (sErrorMsg);
  }

  public void onNotApplicable ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("The DSig references were not compared, because either the sent and/or the received message did no contain the necessary data elements");
  }
}
