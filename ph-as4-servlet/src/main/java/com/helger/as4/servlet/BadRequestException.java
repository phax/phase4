package com.helger.as4.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BadRequestException extends RuntimeException
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (BadRequestException.class);

  public BadRequestException (final String sMsg)
  {
    super (sMsg);
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BadRequest: " + sMsg);
  }

  public BadRequestException (final String sMsg, final Throwable t)
  {
    super (sMsg + "; Technical details [" + t.getClass ().getName () + "]: " + t.getMessage ());
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BadRequest: " + sMsg, t);
  }
}
