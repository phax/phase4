package com.helger.as4lib.constants;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class CAS4
{
  public static final String PATH_SCHEMATA = "/schemas/";
  public static final String XSD_EBMS_HEADER = PATH_SCHEMATA + "ebms-header-3_0-200704.xsd";
  public static final String XSD_SOAP11 = PATH_SCHEMATA + "soap11.xsd";
  public static final String XSD_SOAP12 = PATH_SCHEMATA + "soap12.xsd";
  public static final String XSD_XML = PATH_SCHEMATA + "xml.xsd";

  private CAS4 ()
  {}
}
