package com.helger.as4server.attachment;

import com.helger.commons.collection.attr.MapBasedAttributeContainer;

/**
 * Abstract base class for incoming attachments.
 * 
 * @author Philip Helger
 */
public abstract class AbstractIncomingAttachment extends MapBasedAttributeContainer <String, String>
                                                 implements IIncomingAttachment
{

}
