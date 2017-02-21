package com.helger.as4.servlet.debug;

import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.DevelopersNote;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.http.HTTPHeaderMap;

@Deprecated
@DevelopersNote ("Using System.out is evil but simple :)")
public final class AS4SystemOutIncomingCallback implements IAS4DebugIncomingCallback
{
  public void onRequestBegin (@Nonnull final HTTPHeaderMap aHeaders)
  {
    System.out.println ("Incoming AS4 request");
    for (final Map.Entry <String, ICommonsList <String>> aEntry : aHeaders)
      for (final String sValue : aEntry.getValue ())
        System.out.println ("  " + aEntry.getKey () + "=" + sValue);
    System.out.println ();
  }

  public void onByteRead (final int ret)
  {
    System.out.print ((char) (ret & 0xff));
  }

  public void onRequestEnd ()
  {
    System.out.println ();
    System.out.println ();
  }
}
