package com.helger.as4server.standalone;

import com.helger.photon.jetty.JettyRunner;

public class Jetty9090
{

  public static void main (final String [] args) throws Exception
  {
    final int nPort = 9090;
    final JettyRunner s_aJetty = new JettyRunner (nPort, nPort + 1000);
    s_aJetty.startServer ();
  }
}
