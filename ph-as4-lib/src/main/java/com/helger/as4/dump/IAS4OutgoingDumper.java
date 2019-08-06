package com.helger.as4.dump;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Interface for dumping outgoing requests
 *
 * @author Philip Helger
 * @since 0.9.0
 */
public interface IAS4OutgoingDumper
{
  /**
   * Called for new requests.
   *
   * @param sMessageID
   *        The message ID of the outgoing message. Neither <code>null</code>
   *        nor empty.
   * @param Custom
   *        headers to be added to the HTTP entity. May be <code>null</code>.
   * @return If <code>null</code> is returned, nothing is dumped, else each byte
   *         written to the target stream is also written to that output stream.
   * @throws IOException
   *         in case of an error
   */
  @Nullable
  OutputStream onNewRequest (@Nonnull @Nonempty String sMessageID,
                             @Nullable HttpHeaderMap aCustomHeaders) throws IOException;
}
