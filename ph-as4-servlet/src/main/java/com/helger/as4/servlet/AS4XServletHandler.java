/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.servlet;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.http.EHttpMethod;
import com.helger.http.EHttpVersion;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * AS4 receiving servlet.<br>
 * Use a configuration like the following in your <code>WEB-INF/web.xm</code>
 * file:
 *
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.helger.as4.servlet.AS4Servlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;AS4Servlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Martin Bayerl
 * @author Philip Helger
 */
public final class AS4XServletHandler implements IXServletSimpleHandler
{
  public AS4XServletHandler ()
  {}

  @Nonnull
  @Override
  public AS4Response createUnifiedResponse (@Nonnull final EHttpVersion eHTTPVersion,
                                            @Nonnull final EHttpMethod eHTTPMethod,
                                            @Nonnull final HttpServletRequest aHttpRequest,
                                            @Nonnull final IRequestWebScope aRequestScope)
  {
    return new AS4Response (eHTTPVersion, eHTTPMethod, aHttpRequest);
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final AS4Response aHttpResponse = (AS4Response) aUnifiedResponse;

    try (final AS4Handler aHandler = new AS4Handler ())
    {
      aHandler.handleRequest (aRequestScope, aHttpResponse);
    }
    catch (final BadRequestException ex)
    {
      // Logged inside
      aHttpResponse.setResponseError (HttpServletResponse.SC_BAD_REQUEST,
                                      "Bad Request: " + ex.getMessage (),
                                      ex.getCause ());
    }
    catch (final Throwable t)
    {
      // Logged inside
      aHttpResponse.setResponseError (HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                      "Internal error processing AS4 request",
                                      t);
    }
  }
}
