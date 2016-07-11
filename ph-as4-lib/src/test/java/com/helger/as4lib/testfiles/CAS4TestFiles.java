package com.helger.as4lib.testfiles;

import javax.annotation.Nonnull;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;

public class CAS4TestFiles
{
  public static final String TEST_FILE_PATH_SOAP_11 = "/soap11test/";
  public static final String TEST_FILE_PATH_SOAP_12 = "/soap12test/";

  private static final String [] SOAP_11_VALID_XML = new String [] { "BundledMessage.xml",
                                                                     "ErrorMessage.xml",
                                                                     "PullRequest.xml",
                                                                     "ReceiptMessage.xml",
                                                                     "UserMessage.xml",
                                                                     "UserMessageResponse.xml",
                                                                     "EmptyMessaging.xml" };
  private static final String [] SOAP_12_VALID_XML = new String [] { "PullRequest12.xml", "UserMessage12.xml" };

  private static final String [] SOAP_11_INVALID_XML = new String [] { "MessageInfoIDMissing.xml",
                                                                       "MessageInfoImaginaryTimestamp.xml",
                                                                       "MessageInfoMissing.xml",
                                                                       "NoMessaging.xml" };
  private static final String [] SOAP_12_INVALID_XML = new String [] { "" };

  @Nonnull
  public static ICommonsList <String> getTestFilesSOAP11ValidXML ()
  {
    return new CommonsArrayList<> (SOAP_11_VALID_XML);
  }

  @Nonnull
  public static ICommonsList <String> getTestFilesSOAP12ValidXML ()
  {
    return new CommonsArrayList<> (SOAP_12_VALID_XML);
  }

  @Nonnull
  public static ICommonsList <String> getTestFilesSOAP11InvalidXML ()
  {
    return new CommonsArrayList<> (SOAP_11_INVALID_XML);
  }

  @Nonnull
  public static ICommonsList <String> getTestFilesSOAP12InvalidXML ()
  {
    return new CommonsArrayList<> (SOAP_12_INVALID_XML);
  }

}
