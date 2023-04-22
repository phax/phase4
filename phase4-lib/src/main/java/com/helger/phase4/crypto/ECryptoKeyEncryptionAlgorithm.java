/*
 * Copyright (C) 2023 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
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
package com.helger.phase4.crypto;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import org.apache.wss4j.common.WSS4JConstants;

import javax.annotation.Nonnull;

public enum ECryptoKeyEncryptionAlgorithm implements IHasID<String> {
    RSA_OAEP_XENC11(WSS4JConstants.KEYTRANSPORT_RSAOAEP_XENC11),

    // ECDH-ES KEYWRAP is not supported by WSS4J
    ECDH_ES_KEYWRAP_AES_128("http://www.w3.org/2001/04/xmlenc#kw-aes128"),
    ECDH_ES_KEYWRAP_AES_192("http://www.w3.org/2001/04/xmlenc#kw-aes192"),
    ECDH_ES_KEYWRAP_AES_256("http://www.w3.org/2001/04/xmlenc#kw-aes256");

    private final String m_sID;

    ECryptoKeyEncryptionAlgorithm (@Nonnull @Nonempty final String sID) {
        this.m_sID = sID;
    }

    @Override
    public String getID () {
        return m_sID;
    }
}
