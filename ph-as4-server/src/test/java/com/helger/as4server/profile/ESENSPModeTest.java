package com.helger.as4server.profile;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

public class ESENSPModeTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  @Test
  public void testESENSPMode ()
  {
    ESENSPMode.getESENSPMode ();
  }
}
