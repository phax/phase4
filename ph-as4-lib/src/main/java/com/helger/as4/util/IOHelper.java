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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.SingleError;
import com.helger.commons.io.file.FileIOError;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.LoggingFileOperationCallback;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;

@Immutable
public final class IOHelper
{
  private static final FileOperationManager s_aFOM = new FileOperationManager (new LoggingFileOperationCallback ());

  private IOHelper ()
  {}

  @Nonnull
  public static FileOperationManager getFileOperationManager ()
  {
    return s_aFOM;
  }

  @Nonnegative
  public static long copy (@Nonnull @WillClose final InputStream aIS, @Nonnull @WillNotClose final OutputStream aOS)
  {
    final MutableLong aML = new MutableLong ();
    StreamHelper.copyInputStreamToOutputStream (aIS, aOS, aML);
    return aML.longValue ();
  }

  @Nonnull
  public static File getDirectoryFile (@Nonnull final String sDirectory)
  {
    final File aDir = new File (sDirectory);
    s_aFOM.createDirRecursiveIfNotExisting (aDir);
    return aDir;
  }

  @Nonnull
  @Nonempty
  public static String getTransferRate (final long nBytes, @Nonnull final StopWatch aSW)
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append (nBytes).append (" bytes in ").append (aSW.getMillis () / 1000.0).append (" seconds at ");

    final long nMillis = aSW.getMillis ();
    if (nMillis != 0)
    {
      final double dSeconds = nMillis / 1000.0;
      final long nBytesPerSecond = Math.round (nBytes / dSeconds);
      aSB.append (_getTransferRate (nBytesPerSecond));
    }
    else
    {
      aSB.append (_getTransferRate (nBytes));
    }

    return aSB.toString ();
  }

  @Nonnull
  @Nonempty
  private static String _getTransferRate (final long nBytesPerSecond)
  {
    final StringBuilder aSB = new StringBuilder ();
    if (nBytesPerSecond < CGlobal.BYTES_PER_KILOBYTE)
    {
      // < 1024
      aSB.append (nBytesPerSecond).append (" Bps");
    }
    else
    {
      final long nKBytesPerSecond = nBytesPerSecond / CGlobal.BYTES_PER_KILOBYTE;
      if (nKBytesPerSecond < CGlobal.BYTES_PER_KILOBYTE)
      {
        // < 1048576
        aSB.append (nKBytesPerSecond)
           .append ('.')
           .append (nBytesPerSecond % CGlobal.BYTES_PER_KILOBYTE)
           .append (" KBps");
      }
      else
      {
        // >= 1048576
        aSB.append (nKBytesPerSecond / CGlobal.BYTES_PER_KILOBYTE)
           .append ('.')
           .append (nKBytesPerSecond % CGlobal.BYTES_PER_KILOBYTE)
           .append (" MBps");
      }
    }
    return aSB.toString ();
  }

  @Nonnull
  public static File getUniqueFile (@Nonnull final File aDir, @Nullable final String sFilename)
  {
    final String sBaseFilename = FilenameHelper.getAsSecureValidFilename (sFilename);
    int nCounter = -1;
    while (true)
    {
      final File aTest = new File (aDir,
                                   nCounter == -1 ? sBaseFilename : sBaseFilename + "." + Integer.toString (nCounter));
      if (!aTest.exists ())
        return aTest;

      nCounter++;
    }
  }

  @Nonnull
  public static File moveFile (@Nonnull final File aSrc,
                               @Nonnull final File aDestFile,
                               final boolean bOverwrite,
                               final boolean bRename) throws IOException
  {
    File aRealDestFile = aDestFile;
    if (!bOverwrite && aRealDestFile.exists ())
    {
      if (!bRename)
        throw new IOException ("File already exists: " + aRealDestFile);
      aRealDestFile = getUniqueFile (aRealDestFile.getAbsoluteFile ().getParentFile (), aRealDestFile.getName ());
    }

    // Copy
    FileIOError aIOErr = s_aFOM.copyFile (aSrc, aRealDestFile);
    if (aIOErr.isFailure ())
      throw new IOException ("Copy failed: " + aIOErr.toString ());

    // Delete old
    aIOErr = s_aFOM.deleteFile (aSrc);
    if (aIOErr.isFailure ())
    {
      s_aFOM.deleteFile (aRealDestFile);
      throw new IOException ("Move failed, unable to delete " + aSrc + ": " + aIOErr.toString ());
    }
    return aRealDestFile;
  }

  @Nonnull
  public static String getFilenameFromMessageID (@Nonnull final String sMessageID)
  {
    // Remove angle brackets manually
    String s = StringHelper.removeAll (sMessageID, '<');
    s = StringHelper.removeAll (s, '>');
    return FilenameHelper.getAsSecureValidASCIIFilename (s);
  }

  @Nonnull
  public static SingleError createError (@Nonnull final String sErrorText)
  {
    return SingleError.builderError ().setErrorText (sErrorText).build ();
  }
}
