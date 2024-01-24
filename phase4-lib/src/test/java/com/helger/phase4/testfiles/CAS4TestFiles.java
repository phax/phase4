/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.phase4.testfiles;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Constants for test files
 *
 * @author Philip Helger
 */
public final class CAS4TestFiles
{
  public static final String TEST_FILE_PATH_SOAP_11 = "external/soap11test/";
  public static final String TEST_FILE_PATH_SOAP_12 = "external/soap12test/";

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

  private CAS4TestFiles ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <String> getTestFilesSoap11ValidXML ()
  {
    return new CommonsArrayList <> (SOAP_11_VALID_XML);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <String> getTestFilesSoap12ValidXML ()
  {
    return new CommonsArrayList <> (SOAP_12_VALID_XML);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <String> getTestFilesSoap11InvalidXML ()
  {
    return new CommonsArrayList <> (SOAP_11_INVALID_XML);
  }

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <String> getTestFilesSoap12InvalidXML ()
  {
    return new CommonsArrayList <> (SOAP_12_INVALID_XML);
  }
}
