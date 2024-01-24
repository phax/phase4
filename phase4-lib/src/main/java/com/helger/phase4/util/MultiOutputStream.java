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
package com.helger.phase4.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

/**
 * An output stream that writes to multiple output streams.
 * 
 * @author Philip Helger
 */
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
