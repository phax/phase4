package com.helger.as4.servlet.debug;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import com.helger.commons.io.stream.WrappedInputStream;

public class AS4DebugInputStream extends WrappedInputStream
{
  private final IAS4DebugIncomingCallback m_aCB;

  public AS4DebugInputStream (@Nonnull final InputStream aWrappedIS, @Nonnull final IAS4DebugIncomingCallback aCB)
  {
    super (aWrappedIS);
    m_aCB = aCB;
  }

  @Override
  public int read () throws IOException
  {
    // Read from HTTP input stream
    final int ret = super.read ();
    m_aCB.onByteRead (ret);
    return ret;
  }

  @Override
  public int read (final byte [] aBuf, final int nOfs, final int nLen) throws IOException
  {
    // Read from HTTP input stream
    final int ret = super.read (aBuf, nOfs, nLen);
    for (int i = 0; i < ret; ++i)
      m_aCB.onByteRead (aBuf[nOfs + i]);
    return ret;
  }
}
