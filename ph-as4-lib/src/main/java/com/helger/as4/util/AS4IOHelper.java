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
package com.helger.as4.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.error.SingleError;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.io.file.LoggingFileOperationCallback;

@Immutable
public final class AS4IOHelper
{
  private static final FileOperationManager s_aFOM = new FileOperationManager ();
  static
  {
    s_aFOM.callbacks ().add (new LoggingFileOperationCallback ());
  }

  private AS4IOHelper ()
  {}

  @Nonnull
  public static FileOperationManager getFileOperationManager ()
  {
    return s_aFOM;
  }

  @Nonnull
  public static SingleError createError (@Nonnull final String sErrorText)
  {
    return SingleError.builderError ().setErrorText (sErrorText).build ();
  }
}
