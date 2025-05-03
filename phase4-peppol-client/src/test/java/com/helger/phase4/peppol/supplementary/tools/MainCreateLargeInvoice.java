/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.supplementary.tools;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.helger.commons.CGlobal;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.misc.SizeHelper;

public class MainCreateLargeInvoice
{
  public static void main (final String [] args) throws Exception
  {
    final String sSourceInvoice = SimpleFileIO.getFileAsString (new File ("src/test/resources/external/examples/base-example.xml"),
                                                                StandardCharsets.UTF_8);
    final int nLastLineStart = sSourceInvoice.lastIndexOf ("<cac:InvoiceLine>");
    final String sEnd = "</cac:InvoiceLine>";
    final int nLastLineEnd = sSourceInvoice.lastIndexOf (sEnd) + sEnd.length ();

    final String sFirstPart = sSourceInvoice.substring (0, nLastLineStart);
    final String sRepeatablePart = sSourceInvoice.substring (nLastLineStart, nLastLineEnd);
    final String sLastPart = sSourceInvoice.substring (nLastLineEnd);
    if (!sSourceInvoice.equals (sFirstPart + sRepeatablePart + sLastPart))
      throw new IllegalStateException ("Source invoice is not valid!");

    long nChars = sFirstPart.length () + sLastPart.length ();
    // The target size of the invoice in characters
    final long nTargetChars = CGlobal.BYTES_PER_GIGABYTE * 1;

    final SizeHelper aSH = new SizeHelper (Locale.ENGLISH);
    int nLineID = 2;
    final File fTarget = new File ("src/test/resources/external/examples/large-files/base-example-large-" +
                                   aSH.getAsMatching (nTargetChars).toLowerCase (Locale.ROOT) +
                                   ".xml");
    System.out.println ("Creating file " +
                        fTarget.getName () +
                        " with ~" +
                        (nTargetChars / sRepeatablePart.length ()) +
                        " lines");
    try (final OutputStream aOS = FileHelper.getBufferedOutputStream (fTarget))
    {
      // Write the first part
      aOS.write (sFirstPart.getBytes (StandardCharsets.UTF_8));

      while (nChars < nTargetChars)
      {
        // Write the repeatable part
        final String sNewLine = sRepeatablePart.replace ("<cbc:ID>2</cbc:ID>", "<cbc:ID>" + nLineID + "</cbc:ID>");
        nChars += sNewLine.length ();

        aOS.write (sNewLine.getBytes (StandardCharsets.UTF_8));
        nLineID++;

        if (nLineID % 10_000 == 0)
          System.out.println ("Written " + nLineID + " lines");
      }

      // Write the last part
      aOS.write (sLastPart.getBytes (StandardCharsets.UTF_8));
    }
    System.out.println ("Done with " + nChars + " characters");
  }
}
