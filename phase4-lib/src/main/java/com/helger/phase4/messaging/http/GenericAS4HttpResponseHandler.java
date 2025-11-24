package com.helger.phase4.messaging.http;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.phase4.messaging.http.GenericAS4HttpResponseHandler.HttpResponseData;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * This is a specific HTTP client response handler. Compared to the default ones I usually use, it
 * can pass through content even if an HTTP error occurred.
 *
 * @author Philip Helger
 * @since 4.1.1
 */
public class GenericAS4HttpResponseHandler implements HttpClientResponseHandler <HttpResponseData>
{
  public static record HttpResponseData (@NonNull StatusLine statusLine, @NonNull HttpEntity entity)
  {
    @Nonnegative
    public int getStatusCode ()
    {
      return statusLine.getStatusCode ();
    }
  }

  public static final GenericAS4HttpResponseHandler INSTANCE = new GenericAS4HttpResponseHandler ();

  private GenericAS4HttpResponseHandler ()
  {}

  @NonNull
  public HttpResponseData handleResponse (@NonNull final ClassicHttpResponse aHttpResponse)
  {
    final StatusLine aStatusLine = new StatusLine (aHttpResponse);
    final HttpEntity aEntity = aHttpResponse.getEntity ();
    return new HttpResponseData (aStatusLine, aEntity);
  }

  @NonNull
  public static HttpClientResponseHandler <byte []> getHandlerByteArray ()
  {
    return aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = GenericAS4HttpResponseHandler.INSTANCE.handleResponse (aHttpResponse);
      return EntityUtils.toByteArray (aResponseData.entity);
    };
  }

  @NonNull
  public static HttpClientResponseHandler <IMicroDocument> getHandlerMicroDom ()
  {
    return aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = GenericAS4HttpResponseHandler.INSTANCE.handleResponse (aHttpResponse);
      final byte [] aXMLBytes = EntityUtils.toByteArray (aResponseData.entity);
      return MicroReader.readMicroXML (aXMLBytes);
    };
  }
}
