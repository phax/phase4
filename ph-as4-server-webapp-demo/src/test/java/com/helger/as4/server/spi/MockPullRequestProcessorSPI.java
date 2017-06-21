package com.helger.as4.server.spi;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

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
                                                      AS4ServerConfiguration.getServerAddress (),
                                                      (i, r) -> "PullPMode");
    if (aSignalMessage.getPullRequest () != null)
    {
      if (!aSignalMessage.getAny ().isEmpty ())
      {
        final Element aElement = (Element) aSignalMessage.getAnyAtIndex (0);
        if (aElement.getTextContent ().contains ("pushPull"))
        {
          aPMode.setMEPBinding (EMEPBinding.PUSH_PULL);
        }
        else
          if (aElement.getTextContent ().contains ("pullPush"))
          {
            aPMode.setMEPBinding (EMEPBinding.PULL_PUSH);
          }
      }
      else
      {
        aPMode.setMEPBinding (EMEPBinding.PULL);
      }
    }
    return aPMode;
  }
}
