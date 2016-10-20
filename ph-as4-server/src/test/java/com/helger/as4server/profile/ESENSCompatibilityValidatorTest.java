package com.helger.as4server.profile;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.Test;

import com.helger.as4lib.model.EMEP;
import com.helger.as4lib.model.pmode.PMode;
import com.helger.commons.error.list.ErrorList;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * All essentials need to be set and need to be not null since they are getting
 * checked, when a PMode is introduced into the system and these null checks
 * would be redundant in the profiles.
 *
 * @author bayerlma
 */
public class ESENSCompatibilityValidatorTest
{
  @ClassRule
  public static final PhotonBasicWebTestRule s_aRule = new PhotonBasicWebTestRule ();

  private final ESENSCompatibilityValidator aESENSCompatibilityValidator = new ESENSCompatibilityValidator ();

  @Test
  public void testValidatePModeWrongMEP ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final PMode aPMode = new PMode ("Testi");
    aPMode.setMEP (EMEP.TWO_WAY_PULL_PUSH);
    aESENSCompatibilityValidator.validatePMode (aPMode, aErrorList);

    assertTrue (aErrorList.containsAny (x -> x.getErrorText (Locale.getDefault ()).contains ("MEP")));
  }
}
