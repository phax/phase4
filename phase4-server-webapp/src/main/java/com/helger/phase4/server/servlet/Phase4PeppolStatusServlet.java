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
package com.helger.phase4.server.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

import jakarta.servlet.annotation.WebServlet;

/**
 * The servlet to show the application status.
 *
 * @author Philip Helger
 */
@WebServlet (name = "peppol-status", urlPatterns = "/peppol-status")
public class Phase4PeppolStatusServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "peppol-status";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  public Phase4PeppolStatusServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new Phase4PeppolStatusXServletHandler ());
    handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}
