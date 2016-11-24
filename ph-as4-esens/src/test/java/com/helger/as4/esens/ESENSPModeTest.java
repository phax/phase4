package com.helger.as4.esens;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4.esens.ESENSPMode;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

public class ESENSPModeTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  @Test
  public void testESENSPMode ()
  {
    ESENSPMode.createESENSPMode ();
  }
}
