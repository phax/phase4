/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.xservlet.AbstractXServlet;

/**
 * AS4 receiving servlet.<br>
 * Use a configuration like the following in your <code>WEB-INF/web.xm</code>
 * file:
 *
 * <pre>
&lt;servlet&gt;
  &lt;servlet-name&gt;Phase4PeppolServlet&lt;/servlet-name&gt;
  &lt;servlet-class&gt;com.helger.phase4.peppol.servlet.Phase4PeppolServlet&lt;/servlet-class&gt;
&lt;/servlet&gt;
&lt;servlet-mapping&gt;
  &lt;servlet-name&gt;Phase4PeppolServlet&lt;/servlet-name&gt;
  &lt;url-pattern&gt;/as4&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Philip Helger
 */
public class Phase4PeppolServlet extends AbstractXServlet
{
  /**
   * Default constructor using default crypto factory.
   */
  public Phase4PeppolServlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);
    final AS4XServletHandler aHdl = new AS4XServletHandler ();
    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST, aHdl);
  }
}
