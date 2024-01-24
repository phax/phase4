/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.server;

import org.eclipse.jetty.webapp.WebAppContext;

import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.jetty.JettyRunner;

/**
 * Special JettyRunne for AS4
 *
 * @author Philip Helger
 */
public class AS4JettyRunner extends JettyRunner
{
  public AS4JettyRunner ()
  {
    super ("AS4 Mock Jetty");
  }

  @Override
  protected void customizeWebAppCtx (final WebAppContext aWebAppCtx) throws Exception
  {
    // This enables GlobalDebug mode
    // GlobalDebug mode is required to use the "http" protocol in test
    aWebAppCtx.setInitParameter (WebAppListener.DEFAULT_INIT_PARAMETER_DEBUG, "true");
  }
}
