package com.helger.as4lib.marshaller;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.xml.namespace.MapBasedNamespaceContext;

public class Ebms3NamespaceHandler extends MapBasedNamespaceContext
{
  public Ebms3NamespaceHandler ()
  {
    addMapping ("ds", CAS4.DS_NS);
    addMapping ("eb", CAS4.EBMS_NS);
    addMapping ("wsse", CAS4.WSSE_NS);
    addMapping ("wsu", CAS4.WSU_NS);
    addMapping ("ebbp", CAS4.EBBP_NS);
    addMapping ("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    addMapping ("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    addMapping ("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
    addMapping ("xlink", "http://www.w3.org/1999/xlink");
    for (final ESOAPVersion e : ESOAPVersion.values ())
      addMapping (e.getNamespacePrefix (), e.getNamespaceURI ());
  }
}
