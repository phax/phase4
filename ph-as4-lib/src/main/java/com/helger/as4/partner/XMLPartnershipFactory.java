/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as4.partner;

import java.io.File;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.util.IOHelper;
import com.helger.as4.util.IStringMap;
import com.helger.as4.util.StringMap;
import com.helger.as4.util.XMLHelper;
import com.helger.commons.collection.ext.ICommonsOrderedMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.string.StringHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * original author unknown this release added logic to store partnerships and
 * provide methods for partner/partnership command line processor
 *
 * @author joseph mcverry
 */
public class XMLPartnershipFactory extends AbstractPartnershipFactoryWithPartners
{
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_DISABLE_BACKUP = "disablebackup";

  private static final String PARTNER_NAME = Partner.ATTR_PARTNER_NAME;
  private static final String PARTNERSHIP_NAME = Partner.ATTR_PARTNER_NAME;
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLPartnershipFactory.class);

  public void setFilename (final String filename)
  {
    setAttribute (ATTR_FILENAME, filename);
  }

  public String getFilename () throws InvalidParameterException
  {
    return getAttributeAsString (ATTR_FILENAME);
  }

  public void refresh ()
  {
    try
    {
      load (FileHelper.getInputStream (getFilename ()));
    }
    catch (final Exception ex)
    {
      throw new RuntimeException (ex);
    }
  }

  protected void load (@Nullable @WillClose final InputStream aIS)
  {
    final PartnerMap aNewPartners = new PartnerMap ();
    final PartnershipMap aNewPartnerships = new PartnershipMap ();

    if (aIS != null)
    {
      final IMicroDocument aDocument = MicroReader.readMicroXML (aIS);
      final IMicroElement root = aDocument.getDocumentElement ();

      for (final IMicroElement eRootNode : root.getAllChildElements ())
      {
        final String sNodeName = eRootNode.getTagName ();

        if (sNodeName.equals ("partner"))
        {
          final Partner aNewPartner = loadPartner (eRootNode);
          aNewPartners.addPartner (aNewPartner);
        }
        else
          if (sNodeName.equals ("partnership"))
          {
            final Partnership aNewPartnership = loadPartnership (eRootNode, aNewPartners);
            if (aNewPartnerships.getPartnershipByName (aNewPartnership.getName ()) != null)
              throw new IllegalStateException ("Partnership with name '" +
                                               aNewPartnership.getName () +
                                               "' is defined more than once");
            aNewPartnerships.addPartnership (aNewPartnership);
          }
          else
            s_aLogger.warn ("Invalid element '" + sNodeName + "' in XML partnership file");
      }
    }

    setPartners (aNewPartners);
    setPartnerships (aNewPartnerships);
  }

  protected void loadPartnershipAttributes (@Nonnull final IMicroElement aNode, @Nonnull final Partnership aPartnership)
  {
    final String sNodeName = "attribute";
    final String sNodeKeyName = "name";
    final String sNodeValueName = "value";
    final ICommonsOrderedMap <String, String> aAttributes = XMLHelper.mapAttributeNodes (aNode,
                                                                                         sNodeName,
                                                                                         sNodeKeyName,
                                                                                         sNodeValueName);
    aPartnership.addAllAttributes (aAttributes);
  }

  @Nonnull
  public Partner loadPartner (@Nonnull final IMicroElement ePartner)
  {
    // Name is required
    final StringMap aAttrs = XMLHelper.getAllAttrsWithLowercaseNameWithRequired (ePartner, PARTNER_NAME);
    // TODO id should get here somehow if we decide to use partnerships
    return new Partner ("id", aAttrs);
  }

  protected void loadPartnerIDs (@Nonnull final IMicroElement ePartnership,
                                 @Nonnull final IPartnerMap aAllPartners,
                                 @Nonnull final Partnership aPartnership,
                                 final boolean bIsSender)
  {
    final String sPartnerType = bIsSender ? "sender" : "receiver";
    final IMicroElement ePartner = ePartnership.getFirstChildElement (sPartnerType);
    if (ePartner == null)
      throw new IllegalStateException ("Partnership '" +
                                       aPartnership.getName () +
                                       "' is missing '" +
                                       sPartnerType +
                                       "' child element");

    final IStringMap aPartnerAttrs = XMLHelper.getAllAttrsWithLowercaseName (ePartner);

    // check for a partner name, and look up in partners list if one is found
    final String sPartnerName = aPartnerAttrs.getAttributeAsString (PARTNER_NAME);
    if (sPartnerName != null)
    {
      // Resolve name from existing partners
      final IPartner aPartner = aAllPartners.getPartnerOfName (sPartnerName);
      if (aPartner == null)
      {
        throw new IllegalStateException ("Partnership '" +
                                         aPartnership.getName () +
                                         "' has a non-existing " +
                                         sPartnerType +
                                         " partner: '" +
                                         sPartnerName +
                                         "'");
      }

      // Set all attributes from the stored partner
      if (bIsSender)
        aPartnership.addSenderIDs (aPartner.getAllAttributes ());
      else
        aPartnership.addReceiverIDs (aPartner.getAllAttributes ());
    }

    // copy all other (existing) attributes to the partner id map - overwrite
    // the ones present in the partner element
    if (bIsSender)
      aPartnership.addSenderIDs (aPartnerAttrs.getAllAttributes ());
    else
      aPartnership.addReceiverIDs (aPartnerAttrs.getAllAttributes ());
  }

  @Nonnull
  public Partnership loadPartnership (@Nonnull final IMicroElement ePartnership,
                                      @Nonnull final IPartnerMap aAllPartners)
  {
    // Name attribute is required
    final IStringMap aPartnershipAttrs = XMLHelper.getAllAttrsWithLowercaseNameWithRequired (ePartnership,
                                                                                             PARTNERSHIP_NAME);

    final Partnership aPartnership = new Partnership (aPartnershipAttrs.getAttributeAsString (PARTNERSHIP_NAME));

    // load the sender and receiver information
    loadPartnerIDs (ePartnership, aAllPartners, aPartnership, true);
    loadPartnerIDs (ePartnership, aAllPartners, aPartnership, false);

    // read in the partnership attributes
    loadPartnershipAttributes (ePartnership, aPartnership);

    return aPartnership;
  }

  @Nonnull
  private File _getUniqueBackupFile (final String sFilename)
  {
    long nIndex = 0;
    File aBackupFile;
    do
    {
      aBackupFile = new File (sFilename + '.' + StringHelper.getLeadingZero (nIndex, 7));
      nIndex++;
    } while (aBackupFile.exists ());
    return aBackupFile;
  }

  /**
   * Store the current status of the partnerships to a file.
   *
   * @throws IllegalStateException
   *         In case of an error
   */
  public void storePartnership ()
  {
    final String sFilename = getFilename ();

    if (!containsAttribute (ATTR_DISABLE_BACKUP))
    {
      final File aBackupFile = _getUniqueBackupFile (sFilename);

      s_aLogger.info ("backing up " + sFilename + " to " + aBackupFile.getName ());

      final File aSourceFile = new File (sFilename);
      IOHelper.getFileOperationManager ().renameFile (aSourceFile, aBackupFile);
    }

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement ("partnerships");
    for (final IPartner aPartner : getAllPartners ())
    {
      final IMicroElement ePartner = eRoot.appendElement ("partner");
      for (final Map.Entry <String, String> aAttr : aPartner)
        ePartner.setAttribute (aAttr.getKey (), aAttr.getValue ());
    }

    for (final Partnership aPartnership : getAllPartnerships ())
    {
      final IMicroElement ePartnership = eRoot.appendElement ("partnership");
      ePartnership.setAttribute (PARTNERSHIP_NAME, aPartnership.getName ());

      final IMicroElement eSender = ePartnership.appendElement ("sender");
      for (final Map.Entry <String, String> aAttr : aPartnership.getAllSenderIDs ())
        eSender.setAttribute (aAttr.getKey (), aAttr.getValue ());

      final IMicroElement eReceiver = ePartnership.appendElement ("receiver");
      for (final Map.Entry <String, String> aAttr : aPartnership.getAllReceiverIDs ())
        eReceiver.setAttribute (aAttr.getKey (), aAttr.getValue ());

      for (final Map.Entry <String, String> aAttr : aPartnership.getAllAttributes ())
        ePartnership.appendElement ("attribute")
                    .setAttribute ("name", aAttr.getKey ())
                    .setAttribute ("value", aAttr.getValue ());
    }
    if (MicroWriter.writeToFile (aDoc, new File (sFilename)).isFailure ())
      throw new IllegalStateException ("Failed to write to file " + sFilename);
  }
}
