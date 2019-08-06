package com.helger.as4.servlet.dump;

import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

/**
 * Interface for dumping incoming requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4IncomingDumper
{
  /**
   * Called for new requests.
   *
   * @param aHttpServletRequest
   *        The current servlet request.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         read from the source stream is written to that output stream.
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull HttpServletRequest aHttpServletRequest);
}
