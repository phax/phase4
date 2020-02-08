/**
 * Copyright (C) 2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.server.storage;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.photon.app.io.WebFileIO;

/**
 * Central storage helper
 * 
 * @author Philip Helger
 */
public final class StorageHelper
{
  // In memory counter
  private static final AtomicInteger FILE_SEQ_COUNTER = new AtomicInteger (0);

  private StorageHelper ()
  {}

  @Nonnull
  public static File getStorageFile (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                     @Nonnull final String sExt)
  {
    ValueEnforcer.notNull (aMessageMetadata, "MessageMetadata");
    ValueEnforcer.notEmpty (sExt, "Ext");
    ValueEnforcer.isTrue (sExt.startsWith ("."), "Extension must start with a dot");

    final LocalDateTime aLDT = aMessageMetadata.getIncomingDT ();
    final String sYear = StringHelper.getLeadingZero (aLDT.getYear (), 4);
    final String sMonth = StringHelper.getLeadingZero (aLDT.getMonthValue (), 2);
    final String sDay = StringHelper.getLeadingZero (aLDT.getDayOfMonth (), 2);
    final String sFilename = FilenameHelper.getAsSecureValidFilename (PDTIOHelper.getTimeForFilename (aLDT.toLocalTime ()) +
                                                                      "-" +
                                                                      FILE_SEQ_COUNTER.incrementAndGet () +
                                                                      "-" +
                                                                      aMessageMetadata.getIncomingUniqueID () +
                                                                      sExt);
    return WebFileIO.getDataIO ().getFile ("as4dump/" + sYear + "/" + sMonth + "/" + sDay + "/" + sFilename);
  }
}
