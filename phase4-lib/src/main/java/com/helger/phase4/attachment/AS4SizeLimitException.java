/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.attachment;

import java.io.IOException;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;

/**
 * Special exception type that is thrown, if one of the configured size limits for an incoming
 * message, an incoming attachment or the decompressed content of an incoming attachment is
 * exceeded. See issue #318.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
public class AS4SizeLimitException extends IOException
{
  public AS4SizeLimitException (@NonNull @Nonempty final String sMsg)
  {
    super (sMsg);
  }
}
