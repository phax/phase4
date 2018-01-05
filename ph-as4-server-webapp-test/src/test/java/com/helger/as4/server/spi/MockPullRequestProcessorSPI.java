/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
