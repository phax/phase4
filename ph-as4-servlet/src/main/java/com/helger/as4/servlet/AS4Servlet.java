/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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

import com.helger.as4.attachment.IIncomingAttachmentFactory;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

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
public class AS4Servlet extends AbstractXServlet
{
  public AS4Servlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);
    final AS4ResourceManager aResMgr = new AS4ResourceManager ();
    final AS4CryptoFactory aCryptoFactory = AS4ServerSettings.getAS4CryptoFactory ();
    final IIncomingAttachmentFactory aIAF = AS4ServerSettings.getIncomingAttachmentFactory ();
    handlerRegistry ().registerHandler (EHttpMethod.POST, new AS4XServletHandler (aResMgr, aCryptoFactory, aIAF));
  }
}
