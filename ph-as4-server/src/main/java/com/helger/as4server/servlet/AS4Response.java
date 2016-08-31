package com.helger.as4server.servlet;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.charset.CCharset;
import com.helger.commons.mime.CMimeType;
import com.helger.web.servlet.response.UnifiedResponse;

public class AS4Response extends UnifiedResponse
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS4Response.class);

  public AS4Response (@Nonnull final HttpServletRequest aHttpServletRequest)
  {
    super (aHttpServletRequest);
    disableCaching ();
    setAllowContentOnStatusCode (true);
  }

  public void setResponseError (@Nonnegative final int nStatusCode,
                                @Nonnull final String sMsg,
                                @Nullable final Throwable t)
  {
    s_aLogger.error (sMsg, t);
    setContentAndCharset (sMsg, CCharset.CHARSET_UTF_8_OBJ);
    setMimeType (CMimeType.TEXT_PLAIN);
    setStatus (nStatusCode);
  }

  public void setBadRequest (@Nonnull final String sMsg)
  {
    setResponseError (HttpServletResponse.SC_BAD_REQUEST, sMsg, null);
  }
}
