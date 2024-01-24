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
package com.helger.phase4.peppol.servlet;

import javax.annotation.Nonnull;

/**
 * Special client exception that always translates to an AS4 error message send
 * back to C2. Compared to the default Exception handling in
 * {@link Phase4PeppolServletMessageProcessorSPI} the returned error message in
 * the AS4 ErrorMessage contains no custom prefix. So it's all caller induced.
 *
 * @author Philip Helger
 * @since 1.4.3
 */
public class Phase4PeppolClientException extends RuntimeException
{
  public Phase4PeppolClientException (@Nonnull final String sMsg)
  {
    super (sMsg);
  }
}
