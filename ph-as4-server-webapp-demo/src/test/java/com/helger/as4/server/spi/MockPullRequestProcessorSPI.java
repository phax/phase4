package com.helger.as4.server.spi;

import javax.annotation.Nonnull;

import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.model.EMEPBinding;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.servlet.spi.IAS4ServletPullRequestProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.commons.annotation.IsSPIImplementation;

@IsSPIImplementation
public class MockPullRequestProcessorSPI implements IAS4ServletPullRequestProcessorSPI
{
  public PMode processAS4UserMessage (@Nonnull final Ebms3SignalMessage aSignalMessage)
  {
    final PMode aPMode = ESENSPMode.createESENSPMode ("pullinitiator",
                                                      "pullresponder",
                                                      AS4ServerConfiguration.getSettings ()
                                                                            .getAsString ("server.address",
                                                                                          "http://localhost:8080/as4"),
                                                      (i, r) -> "PullPMode");
    aPMode.setMEPBinding (EMEPBinding.PULL);
    return aPMode;
  }
}
