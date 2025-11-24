/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.messaging.http;

import java.io.IOException;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.http.CHttp;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.phase4.config.AS4Configuration;
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
  public HttpResponseData handleResponse (@NonNull final ClassicHttpResponse aHttpResponse) throws @NonNull ExtendedHttpResponseException,
                                                                                            IOException
  {
    final StatusLine aStatusLine = new StatusLine (aHttpResponse);
    final HttpEntity aEntity = aHttpResponse.getEntity ();

    if (AS4Configuration.isHttpResponseAcceptAllStatusCodes ())
    {
      // continue - new way since 4.1.1
    }
    else
    {
      // Only continue if status code is 2xx (default in phase4 <= 4.1.0)
      if (aStatusLine.getStatusCode () >= CHttp.HTTP_MULTIPLE_CHOICES)
      {
        // Consume entity and throw
        throw ExtendedHttpResponseException.create (aStatusLine, aHttpResponse, aEntity);
      }
    }

    return new HttpResponseData (aStatusLine, aEntity);
  }

  @NonNull
  public static HttpClientResponseHandler <byte []> getHandlerByteArray ()
  {
    return aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = INSTANCE.handleResponse (aHttpResponse);
      return EntityUtils.toByteArray (aResponseData.entity);
    };
  }

  @NonNull
  public static HttpClientResponseHandler <IMicroDocument> getHandlerMicroDom ()
  {
    return aHttpResponse -> {
      // Accepts all response codes
      final HttpResponseData aResponseData = INSTANCE.handleResponse (aHttpResponse);
      final byte [] aXMLBytes = EntityUtils.toByteArray (aResponseData.entity);
      return MicroReader.readMicroXML (aXMLBytes);
    };
  }
}
