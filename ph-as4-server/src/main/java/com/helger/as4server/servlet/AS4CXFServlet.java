/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.as4server.servlet;

import javax.servlet.ServletConfig;

import org.apache.cxf.BusFactory;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import com.helger.as4server.ws.msh.MSHWebservice;

public class AS4CXFServlet extends CXFNonSpringServlet
{
  @Override
  public void loadBus (final ServletConfig aServletConfig)
  {
    super.loadBus (aServletConfig);
    BusFactory.setDefaultBus (bus);

    {
      final LoggingFeature aLF = new LoggingFeature ();
      aLF.setPrettyLogging (true);
      bus.getFeatures ().add (aLF);
    }

    // Called at startup time to register this web service.
    final JaxWsServerFactoryBean aSrvFactory = new JaxWsServerFactoryBean ();
    aSrvFactory.setAddress ("/msh");
    aSrvFactory.setServiceBean (new MSHWebservice ());
    if (false)
    {
      aSrvFactory.getInInterceptors ().add (new LoggingInInterceptor ());
      aSrvFactory.getOutInterceptors ().add (new LoggingOutInterceptor ());
    }
    aSrvFactory.getProperties (true).put (Message.FAULT_STACKTRACE_ENABLED, "true");
    aSrvFactory.getProperties (true).put (Message.EXCEPTION_MESSAGE_CAUSE_ENABLED, "true");
    aSrvFactory.create ();
  }
}
