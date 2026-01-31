/*
 * Copyright (C) 2025-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance.server.api;

import org.jspecify.annotations.NonNull;

import com.helger.dbnalliance.commons.EDBNAllianceStage;
import com.helger.phase4.dbnalliance.server.APConfig;
import com.helger.photon.api.APIDescriptor;
import com.helger.photon.api.APIPath;
import com.helger.photon.api.IAPIExceptionMapper;
import com.helger.photon.api.IAPIRegistry;

/**
 * This class contains the API registration and global parameters.
 *
 * @author Philip Helger
 */
public final class Phase4API
{
  public static final String PARAM_SENDER_ID = "senderId";
  public static final String PARAM_RECEIVER_ID = "receiverId";
  public static final String PARAM_DOC_TYPE_ID = "docTypeId";
  public static final String PARAM_PROCESS_ID = "processId";

  private Phase4API ()
  {}

  public static void init (@NonNull final IAPIRegistry aAPIRegistry)
  {
    final IAPIExceptionMapper aExceptionMapper = new APIExceptionMapper ();

    final EDBNAllianceStage eStage = APConfig.getStage ();

    {
      final APIDescriptor aSendAS4 = new APIDescriptor (APIPath.post ("/sendas4/{" +
                                                                      PARAM_SENDER_ID +
                                                                      "}/{" +
                                                                      PARAM_RECEIVER_ID +
                                                                      "}/{" +
                                                                      PARAM_DOC_TYPE_ID +
                                                                      "}/{" +
                                                                      PARAM_PROCESS_ID +
                                                                      "}"), new APIPostSendDocument (eStage));
      aSendAS4.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aSendAS4);
    }
  }
}
