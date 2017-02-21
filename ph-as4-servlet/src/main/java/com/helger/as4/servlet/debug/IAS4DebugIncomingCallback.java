package com.helger.as4.servlet.debug;

import com.helger.http.HTTPHeaderMap;

public interface IAS4DebugIncomingCallback
{
  /**
   * Called before the request content is dumped.
   *
   * @param aHeaders
   *        Header map. May not be <code>null</code>.
   */
  default void onRequestBegin (final HTTPHeaderMap aHeaders)
  {}

  /**
   * A single byte was read from the HTTP input stream
   *
   * @param ret
   *        byte read
   */
  void onByteRead (int ret);

  /**
   * Called when the request content is done. This must be called in a finally
   * block once the request is finished.
   */
  default void onRequestEnd ()
  {}
}
