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
package com.helger.phase4.server.standalone;

import java.io.IOException;

import com.helger.commons.io.resource.FileSystemResource;
import com.helger.phase4.ScopedAS4Configuration;
import com.helger.photon.jetty.JettyRunner;
import com.helger.photon.jetty.JettyStopper;

/**
 * Run this AS4 server locally using Jetty on port 9090 in / context.
 *
 * @author Philip Helger
 */
public final class RunInJettyAS4TEST9090
{
  private static final int PORT = 9090;
  private static final int STOP_PORT = PORT + 1000;

  private static ScopedAS4Configuration s_aSC;

  public static void startNinetyServer () throws Exception
  {
    s_aSC = ScopedAS4Configuration.create (new FileSystemResource ("src/test/resources/test-phase4-9090.properties"));
    final JettyRunner aJetty = new JettyRunner ();
    aJetty.setPort (PORT).setStopPort (STOP_PORT).setAllowAnnotationBasedConfig (false);
    aJetty.startServer ();
  }

  public static void stopNinetyServer () throws IOException
  {
    new JettyStopper ().setStopPort (STOP_PORT).run ();
    if (s_aSC != null)
      s_aSC.close ();
  }

  public static void main (final String [] args) throws Exception
  {
    startNinetyServer ();
  }
}
