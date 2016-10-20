package com.helger.as4lib.model.profile;

import java.io.Serializable;

import javax.annotation.Nullable;

import com.helger.commons.id.IHasID;
import com.helger.commons.name.IHasDisplayName;

public interface IAS4Profile extends IHasID <String>, IHasDisplayName, Serializable
{
  @Nullable
  IAS4ProfileValidator getValidator ();
}
