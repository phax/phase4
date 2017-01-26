package com.helger.as4.lib.client;

import java.io.File;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.helger.as4.CAS4;
import com.helger.as4.client.AS4Client;
import com.helger.as4.crypto.ECryptoAlgorithmCrypt;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.mock.MockEbmsHelper;
import com.helger.as4.mock.MockPModeGenerator;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.servlet.mgr.AS4ServerConfiguration;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.id.factory.FileIntIDFactory;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.servlet.mock.MockServletContext;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;

public class MainSendToLocalHost8080
{
  private static final AS4ResourceManager s_aResMgr = new AS4ResourceManager ();

  /**
   * A Filter that searches for the PModeID from the given MockPmode with the
   * right SOAPVersion
   *
   * @param eESOAPVersion
   *        declares which pmode should be chosen depending on the SOAP Version
   * @return a PMode
   */
  @Nonnull
  private static Predicate <IPMode> _getTestPModeFilter (@Nonnull final ESOAPVersion eESOAPVersion)
  {
    if (eESOAPVersion.equals (ESOAPVersion.SOAP_12))
      return p -> p.getConfigID ().equals (MockPModeGenerator.PMODE_CONFIG_ID_SOAP12_TEST);
    return p -> p.getConfigID ().equals (MockPModeGenerator.PMODE_CONFIG_ID_SOAP11_TEST);
  }

  /**
   * To reduce the amount of code in each test, this method sets the basic
   * attributes that are needed for a successful message to build. <br>
   * Only needed for positive messages.
   *
   * @return the AS4Client with the set attributes to continue
   */
  @Nonnull
  private static AS4Client _getMandatoryAttributesSuccessMessage ()
  {
    final AS4Client aClient = new AS4Client (s_aResMgr);
    aClient.setSOAPVersion (ESOAPVersion.AS4_DEFAULT);
    // Use a pmode that you know is currently running on the server your trying
    // to send the message too
    final IPMode aPModeID = MetaAS4Manager.getPModeMgr ().findFirst (_getTestPModeFilter (aClient.getSOAPVersion ()));
    if (aPModeID == null)
      throw new IllegalStateException ("Found no matching PMode!");

    aClient.setAction ("AnAction");
    aClient.setServiceType ("MyServiceType");
    aClient.setServiceValue ("OrderPaper");
    aClient.setConversationID ("9898");
    aClient.setAgreementRefPMode (aPModeID.getConfigID ());
    aClient.setAgreementRefValue ("http://agreements.holodeckb2b.org/examples/agreement0");
    aClient.setFromRole (CAS4.DEFAULT_ROLE);
    aClient.setFromPartyID ("MyPartyIDforSending");
    aClient.setToRole (CAS4.DEFAULT_ROLE);
    aClient.setToPartyID ("MyPartyIDforReceving");
    aClient.setEbms3Properties (MockEbmsHelper.getEBMSProperties ());

    return aClient;
  }

  /**
   * Sets the keystore attributes, it uses the dummy keystore
   * keys/dummy-pw-test.jks
   *
   * @param aClient
   *        the client on which these attributes should be set
   * @return the client to continue working with it
   */
  @Nonnull
  private static AS4Client _setKeyStoreTestData (@Nonnull final AS4Client aClient)
  {
    aClient.setKeyStoreAlias ("ph-as4");
    aClient.setKeyStorePassword ("test");
    aClient.setKeyStoreFile (new ClassPathResource ("keys/dummy-pw-test.jks").getAsFile ());
    aClient.setKeyStoreType ("jks");
    return aClient;
  }

  public static void main (final String [] args) throws Exception
  {
    WebScopeManager.onGlobalBegin (MockServletContext.create ());
    final File aSCPath = new File ("target/junittest").getAbsoluteFile ();
    WebFileIO.initPaths (new File (AS4ServerConfiguration.getDataPath ()).getAbsoluteFile (), aSCPath, false);
    GlobalIDFactory.setPersistentIntIDFactory (new FileIntIDFactory (WebFileIO.getDataIO ().getFile ("ids.dat")));
    try
    {
      AS4ServerConfiguration.reinitForTestOnly ();
      final AS4Client aClient = _getMandatoryAttributesSuccessMessage ();
      aClient.setPayload (DOMReader.readXMLDOM (new ClassPathResource ("PayloadXML.xml")));

      // Keystore
      _setKeyStoreTestData (aClient);

      // Encrypt specific
      aClient.setCryptoAlgorithmCrypt (ECryptoAlgorithmCrypt.AES_128_GCM);

      final IMicroDocument aDoc = aClient.sendMessageAndGetMicroDocument ("http://localhost:8080/as4");
      System.out.println (MicroWriter.getXMLString (aDoc));
    }
    finally
    {
      s_aResMgr.close ();
      WebScopeManager.onGlobalEnd ();
    }
  }
}
