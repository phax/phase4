package com.helger.as4server.receive.soap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsLinkedHashMap;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;

/**
 * This class manages the SOAP header element processors. This is used to
 * validate the "must understand" SOAP requirement.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SOAPHeaderElementProcessorRegistry
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SOAPHeaderElementProcessorRegistry.class);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static final ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> s_aMap = new CommonsLinkedHashMap<> ();

  private SOAPHeaderElementProcessorRegistry ()
  {}

  public static void registerHeaderElementProcessor (@Nonnull final QName aQName,
                                                     @Nonnull final ISOAPHeaderElementProcessor aProcessor)
  {
    ValueEnforcer.notNull (aQName, "QName");
    ValueEnforcer.notNull (aProcessor, "Processor");

    s_aRWLock.writeLocked ( () -> {
      if (s_aMap.containsKey (aQName))
        throw new IllegalArgumentException ("A processor for QName " + aQName.toString () + " is already registered!");
      s_aMap.put (aQName, aProcessor);
    });
    s_aLogger.info ("Successfully registered SOAP header element processor for " + aQName.toString ());
  }

  @Nullable
  public static ISOAPHeaderElementProcessor getHeaderElementProcessor (@Nullable final QName aQName)
  {
    if (aQName == null)
      return null;
    return s_aRWLock.readLocked ( () -> s_aMap.get (aQName));
  }

  @Nullable
  public static ICommonsOrderedMap <QName, ISOAPHeaderElementProcessor> getAllElementProcessors ()
  {
    return s_aRWLock.readLocked ( () -> s_aMap.getClone ());
  }
}
