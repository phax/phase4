package com.helger.as4lib.model.mpc;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.type.ObjectType;
import com.helger.photon.basic.object.AbstractObject;
import com.helger.photon.security.object.StubObject;

public class MPC extends AbstractObject implements IMPC
{
  public static final ObjectType OT = new ObjectType ("as4.mpc");

  public MPC (@Nonnull @Nonempty final String sID)
  {
    this (StubObject.createForCurrentUserAndID (sID));
  }

  MPC (@Nonnull final StubObject aStubObject)
  {
    super (aStubObject);
  }

  @Nonnull
  public ObjectType getObjectType ()
  {
    return OT;
  }

}
