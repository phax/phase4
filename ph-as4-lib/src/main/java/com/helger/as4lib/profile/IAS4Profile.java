package com.helger.as4lib.profile;

import java.io.Serializable;

import com.helger.as4lib.model.profile.IAS4ProfileValidator;
import com.helger.as4lib.validator.IAS4SignalMessageValidator;
import com.helger.as4lib.validator.IAS4UserMessageValidator;
import com.helger.commons.id.IHasID;
import com.helger.commons.name.IHasDisplayName;

public interface IAS4Profile extends
                             IHasID <String>,
                             IHasDisplayName,
                             IAS4ProfileValidator,
                             IAS4UserMessageValidator,
                             IAS4SignalMessageValidator,
                             Serializable
{
  /**
   * @return <code>true</code> if this is the default profile.
   */
  default boolean isDefaultProfile ()
  {
    return false;
  }
}
