package com.helger.as4server.attachment;

import java.io.InputStream;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.string.ToStringGenerator;

/**
 * In memory incoming attachment.
 * 
 * @author Philip Helger
 */
public class IncomingInMemoryAttachment extends AbstractIncomingAttachment
{
  private final byte [] m_aData;

  public IncomingInMemoryAttachment (@Nonnull final byte [] aData)
  {
    m_aData = ValueEnforcer.notNull (aData, "Data");
  }

  @Nonnull
  public InputStream getInputStream ()
  {
    return new NonBlockingByteArrayInputStream (m_aData);
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("Data#", m_aData.length).toString ();
  }
}
