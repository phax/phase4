package com.helger.as4server.client;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import com.helger.as4server.message.MessageHelperMethods;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public class SerializerXML
{
  public static final XMLWriterSettings XWS = new XMLWriterSettings ();
  static
  {
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("ds", MessageHelperMethods.DS_NS);
    aNSCtx.addMapping ("eb", MessageHelperMethods.EBMS_NS);
    aNSCtx.addMapping ("wsse", MessageHelperMethods.WSSE_NS);
    aNSCtx.addMapping ("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
    aNSCtx.addMapping ("S11", "http://schemas.xmlsoap.org/soap/envelope/");
    aNSCtx.addMapping ("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    aNSCtx.addMapping ("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    aNSCtx.addMapping ("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
    XWS.setNamespaceContext (aNSCtx);
    XWS.setPutNamespaceContextPrefixesInRoot (true);
    XWS.setIndent (EXMLSerializeIndent.NONE);
  }

  private static String _serializeXMLMy (final Node aNode)
  {
    return XMLWriter.getNodeAsString (aNode, XWS);
  }

  private static String _serializeXMLRT (final Node aNode) throws TransformerFactoryConfigurationError,
                                                           TransformerException
  {
    if (false)
      return XMLWriter.getNodeAsString (aNode, XWS);

    final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
    final NonBlockingStringWriter aSW = new NonBlockingStringWriter ();
    transformer.transform (new DOMSource (aNode), new StreamResult (aSW));
    return aSW.getAsString ();
  }

  public static String serializeXML (final Node aNode) throws TransformerFactoryConfigurationError,
                                                        TransformerException
  {
    return true ? _serializeXMLRT (aNode) : _serializeXMLMy (aNode);
  }
}
