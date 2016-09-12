package com.helger.as4lib.model.pmode;

import javax.annotation.Nullable;

import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;

public class PModeManager extends AbstractMapBasedWALDAO <IPMode, PMode>
{
  public PModeManager (@Nullable final String sFilename) throws DAOException
  {
    super (PMode.class, sFilename);
  }

  @Nullable
  public IPMode getPModeOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
