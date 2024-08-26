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
package com.helger.phase4.server.storage;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.string.StringHelper;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.photon.io.WebFileIO;

/**
 * Central storage helper
 *
 * @author Philip Helger
 */
@Immutable
public final class StorageHelper
{
  // In memory counter
  private static final AtomicInteger FILE_SEQ_COUNTER = new AtomicInteger (0);

  private StorageHelper ()
  {}

  @Nonnull
  private static File _getStorageFile (@Nonnull final OffsetDateTime aLDT, @Nonnull final String sFilenameExt)
  {
    final String sYear = StringHelper.getLeadingZero (aLDT.getYear (), 4);
    final String sMonth = StringHelper.getLeadingZero (aLDT.getMonthValue (), 2);
    final String sDay = StringHelper.getLeadingZero (aLDT.getDayOfMonth (), 2);
    final String sFilename = FilenameHelper.getAsSecureValidFilename (PDTIOHelper.getTimeForFilename (aLDT.toLocalTime ()) +
                                                                      "-" +
                                                                      FILE_SEQ_COUNTER.incrementAndGet () +
                                                                      "-" +
                                                                      sFilenameExt);
    return WebFileIO.getDataIO ().getFile ("as4dump/" + sYear + "/" + sMonth + "/" + sDay + "/" + sFilename);
  }

  @Nonnull
  public static File getStorageFile (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                     @Nonnull final String sFilenameExt)
  {
    ValueEnforcer.notNull (aMessageMetadata, "MessageMetadata");
    ValueEnforcer.notEmpty (sFilenameExt, "Ext");
    ValueEnforcer.isTrue (sFilenameExt.contains ("."), "Extension must contain a dot");

    return _getStorageFile (aMessageMetadata.getIncomingDT (), aMessageMetadata.getIncomingUniqueID () + sFilenameExt);
  }

  @Nonnull
  public static File getStorageFile (@Nonnull @Nonempty final String sMessageID,
                                     @Nonnegative final int nTry,
                                     @Nonnull final String sFilenameExt)
  {
    ValueEnforcer.notEmpty (sMessageID, "MessageID");
    ValueEnforcer.notEmpty (sFilenameExt, "Ext");
    ValueEnforcer.isTrue (sFilenameExt.contains ("."), "Extension must contain a dot");

    return _getStorageFile (MetaAS4Manager.getTimestampMgr ().getCurrentDateTime (), sMessageID + "-" + nTry + sFilenameExt);
  }
}
