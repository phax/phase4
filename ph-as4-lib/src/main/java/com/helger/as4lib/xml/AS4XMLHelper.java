package com.helger.as4lib.xml;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.as4lib.constants.CAS4;
import com.helger.as4lib.soap.ESOAPVersion;
import com.helger.commons.io.stream.NonBlockingStringWriter;
import com.helger.commons.string.StringHelper;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

public final class AS4XMLHelper
{
  public static final XMLWriterSettings XWS = new XMLWriterSettings ();
  static
  {
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("ds", CAS4.DS_NS);
    aNSCtx.addMapping ("eb", CAS4.EBMS_NS);
    aNSCtx.addMapping ("wsse", CAS4.WSSE_NS);
    aNSCtx.addMapping ("wsu", CAS4.WSU_NS);
    aNSCtx.addMapping ("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    aNSCtx.addMapping ("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    aNSCtx.addMapping ("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
    aNSCtx.addMapping ("ebbp", "  http://docs.oasis-open.org/ebxml-bp/2.0.4/ebbp-signals-2.0.4.xsd");
    for (final ESOAPVersion e : ESOAPVersion.values ())
      aNSCtx.addMapping (e.getNamespacePrefix (), e.getNamespaceURI ());
    XWS.setNamespaceContext (aNSCtx);
    XWS.setPutNamespaceContextPrefixesInRoot (true);
    XWS.setIndent (EXMLSerializeIndent.NONE);
  }

  private AS4XMLHelper ()
  {}

  @Nonnull
  private static String _serializePh (@Nonnull final Node aNode)
  {
    return XMLWriter.getNodeAsString (aNode, XWS);
  }

  @Nonnull
  private static String _serializeRT (@Nonnull final Node aNode) throws TransformerFactoryConfigurationError,
                                                                 TransformerException
  {
    final Transformer transformer = TransformerFactory.newInstance ().newTransformer ();
    final NonBlockingStringWriter aSW = new NonBlockingStringWriter ();
    transformer.transform (new DOMSource (aNode), new StreamResult (aSW));
    return aSW.getAsString ();
  }

  @Nonnull
  public static String serializeXML (@Nonnull final Node aNode) throws TransformerFactoryConfigurationError,
                                                                TransformerException
  {
    return true ? _serializeRT (aNode) : _serializePh (aNode);
  }

  // TODO replace with XMLHelper.getQName in ph-commons > 8.4.0
  @Nonnull
  public static QName getQName (@Nonnull final Element aElement)
  {
    final String sNamespaceURI = aElement.getNamespaceURI ();
    if (sNamespaceURI == null)
      return new QName (aElement.getTagName ());
    return new QName (sNamespaceURI,
                      aElement.getLocalName (),
                      StringHelper.getNotNull (aElement.getPrefix (), XMLConstants.DEFAULT_NS_PREFIX));
  }
}
