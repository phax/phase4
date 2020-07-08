package com.helger.phase4.peppol.utils;

import java.io.File;

import com.helger.commons.base64.Base64;
import com.helger.commons.io.file.SimpleFileIO;

public class MainBase64
{
  public static void main (final String [] args)
  {
    final File f = new File ("src/test/resources/test-ap.p12");
    final byte [] b = SimpleFileIO.getAllFileBytes (f);
    SimpleFileIO.writeFile (new File (f.getAbsolutePath () + ".b64"), Base64.safeEncodeBytesToBytes (b));
  }
}
