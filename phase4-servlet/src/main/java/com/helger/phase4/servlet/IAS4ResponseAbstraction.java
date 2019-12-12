package com.helger.phase4.servlet;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IHasInputStream;
import com.helger.commons.mime.IMimeType;

/**
 * A synthetic wrapper for an AS4 HTTP response. By default
 * {@link AS4UnifiedResponse} is the logical implementation, but the return
 * types are different.
 *
 * @author Philip Helger
 * @since 0.9.6
 */
public interface IAS4ResponseAbstraction
{
  void addCustomResponseHeaders (@Nonnull HttpHeaderMap aHeaderMap);

  void setCharset (@Nonnull Charset aCharset);

  void setContent (@Nonnull byte [] aBytes);

  void setContent (@Nonnull IHasInputStream aHasIS);

  void setMimeType (@Nonnull IMimeType aMimeType);

  void setStatus (int nStatusCode);

  @Nonnull
  static IAS4ResponseAbstraction wrap (@Nonnull final AS4UnifiedResponse aHttpResponse)
  {
    return new IAS4ResponseAbstraction ()
    {
      public void addCustomResponseHeaders (@Nonnull final HttpHeaderMap aHeaderMap)
      {
        aHttpResponse.addCustomResponseHeaders (aHeaderMap);
      }

      public void setCharset (@Nonnull final Charset aCharset)
      {
        aHttpResponse.setCharset (aCharset);
      }

      public void setContent (@Nonnull final byte [] aBytes)
      {
        aHttpResponse.setContent (aBytes);
      }

      public void setContent (@Nonnull final IHasInputStream aHasIS)
      {
        aHttpResponse.setContent (aHasIS);
      }

      public void setMimeType (@Nonnull final IMimeType aMimeType)
      {
        aHttpResponse.setMimeType (aMimeType);
      }

      public void setStatus (final int nStatusCode)
      {
        aHttpResponse.setStatus (nStatusCode);
      }
    };
  }
}
