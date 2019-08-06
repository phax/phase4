package com.helger.as4.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

public class MultiOutputStream extends OutputStream
{
  private final OutputStream [] m_aOSs;

  public MultiOutputStream (@Nonnull @Nonempty final OutputStream... aOSs)
  {
    ValueEnforcer.notEmptyNoNullValue (aOSs, "OutputStreams");
    m_aOSs = aOSs;
  }

  @Override
  public void write (final int b) throws IOException
  {
    for (final OutputStream aOS : m_aOSs)
      aOS.write (b);
  }

  @Override
  public void write (final byte [] b) throws IOException
  {
    write (b, 0, b.length);
  }

  @Override
  public void write (final byte [] aBuf, final int nOfs, final int nLen) throws IOException
  {
    for (final OutputStream aOS : m_aOSs)
      aOS.write (aBuf, nOfs, nLen);
  }

  @Override
  public void flush () throws IOException
  {
    for (final OutputStream aOS : m_aOSs)
      aOS.flush ();
  }

  @Override
  public void close () throws IOException
  {
    for (final OutputStream aOS : m_aOSs)
      aOS.close ();
  }
}
