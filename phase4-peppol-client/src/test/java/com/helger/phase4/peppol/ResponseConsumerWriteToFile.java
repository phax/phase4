/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.io.File;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.IAS4RawResponseConsumer;
import com.helger.phase4.servlet.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.servlet.mgr.AS4ServerConfiguration;
import com.helger.phase4.util.Phase4Exception;

/**
 * Example implementation of {@link IAS4RawResponseConsumer} writing to a file.
 *
 * @author Philip Helger
 */
public class ResponseConsumerWriteToFile implements IAS4RawResponseConsumer
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ResponseConsumerWriteToFile.class);

  public void handleResponse (@Nonnull final AS4ClientSentMessage <byte []> aResponseEntity) throws Phase4Exception
  {
    if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
    {
      final String sMessageID = aResponseEntity.getMessageID ();
      final String sFilename = AS4OutgoingDumperFileBased.DEFAULT_BASE_PATH +
                               PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                               "-" +
                               FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
                               "-response.xml";
      final File aResponseFile = new File (AS4ServerConfiguration.getDataPath (), sFilename);
      if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
        LOGGER.info ("Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
      else
        LOGGER.error ("Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
    }
  }
}
