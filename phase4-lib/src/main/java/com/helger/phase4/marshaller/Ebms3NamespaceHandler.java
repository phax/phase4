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
package com.helger.phase4.marshaller;

import javax.annotation.Nonnull;

import com.helger.phase4.CAS4;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xsds.xlink.CXLink;

/**
 * A special XML namespace context that contains all default Ebms3 mappings.
 *
 * @author Philip Helger
 */
public class Ebms3NamespaceHandler extends MapBasedNamespaceContext
{
  private static final class SingletonHolder
  {
    static final Ebms3NamespaceHandler INSTANCE = new Ebms3NamespaceHandler ();
  }

  public Ebms3NamespaceHandler ()
  {
    addMapping ("ds", CAS4.DS_NS);
    addMapping ("dsig11", CAS4.DSISG11_NS);
    addMapping ("eb", CAS4.EBMS_NS);
    addMapping ("ebbp", CAS4.EBBP_NS);
    addMapping ("wsse", CAS4.WSSE_NS);
    addMapping ("wsu", CAS4.WSU_NS);
    addMapping ("xenc", CAS4.XENC_NS);
    addMapping ("xenc11", CAS4.XENC11_NS);
    // UBL
    addMapping ("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
    addMapping ("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
    addMapping ("ec", "http://www.w3.org/2001/10/xml-exc-c14n#");
    addMapping (CXLink.DEFAULT_PREFIX, CXLink.NAMESPACE_URI);
  }

  /**
   * @return The global instance of the namespace handler. Never
   *         <code>null</code>. Don't modify it! To modify it, please clone it
   *         and go from there.
   * @since 1.3.3
   */
  @Nonnull
  public static Ebms3NamespaceHandler getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
