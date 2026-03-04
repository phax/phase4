/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.phase4.peppol;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.security.certificate.CertificateDecodeHelper;

/**
 * Base class to add a single sanity method
 *
 * @author Philip Helger
 */
public abstract class AbstractPhase4Sender
{
  @Nullable
  protected static X509Certificate pem2cert (@NonNull final String sCert)
  {
    return new CertificateDecodeHelper ().source (sCert).pemEncoded (true).getDecodedOrNull ();
  }

  @NonNull
  protected static X509Certificate pem2certPHG3 ()
  {
    // Predefined G3 certs
    return pem2cert ("-----BEGIN CERTIFICATE-----\n" +
                     "MIIFsDCCA5igAwIBAgIUF5iu6+gA+IBNSaWYX4mmzDxrQskwDQYJKoZIhvcNAQEL\n" +
                     "BQAwazELMAkGA1UEBhMCQkUxGTAXBgNVBAoTEE9wZW5QRVBQT0wgQUlTQkwxFjAU\n" +
                     "BgNVBAsTDUZPUiBURVNUIE9OTFkxKTAnBgNVBAMTIFBFUFBPTCBBQ0NFU1MgUE9J\n" +
                     "TlQgVEVTVCBDQSAtIEczMB4XDTI1MDkwOTAwMDAwMFoXDTI3MDgyOTIzNTk1OVow\n" +
                     "XjELMAkGA1UEBhMCQVQxIjAgBgNVBAoMGUhlbGdlciBJVCBDb25zdWx0aW5nIEdt\n" +
                     "YkgxFzAVBgNVBAsMDlBFUFBPTCBURVNUIEFQMRIwEAYDVQQDDAlQT1AwMDAzMDYw\n" +
                     "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC9hHOrfLXSMBe6VXXU1dBX\n" +
                     "6oqnRvx3IUwrBIn+NGuYl04FOm7nm7tZ6QIM2KTzuz7v9HFIgamHlxfHHk4GHYOL\n" +
                     "ye6xQfEEK51WxUt6N/erpttJ1dnr7X2kr2qbMTmWzPmW0puKi+ugFaLgMrkxu3yS\n" +
                     "GVKYRWnv1xGsukcgNeLQbOFwmMf47NfkiRNz9NymYTSc0/dgN6HCl1DHB1UtfoH1\n" +
                     "qWTnKkEK8B0puX59k8KqC8Qt/zbgyrC9n12V+b6GhixvS8ngmTp2AcBGcRaydL/G\n" +
                     "nyT+MTx463Ia5AUYhy+KmYh40hljXwFUl2j5pn36IvK0mQgNxPcurmAvhmcmjY/f\n" +
                     "AgMBAAGjggFXMIIBUzAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBS8P6H20f6L7fgp\n" +
                     "9wPp3pDKTaAGhTAfBgNVHSMEGDAWgBSzzETvdq+Byd/zX6WeiHGtn6D3cDAOBgNV\n" +
                     "HQ8BAf8EBAMCBLAwFgYDVR0lAQH/BAwwCgYIKwYBBQUHAwIwgYoGCCsGAQUFBwEB\n" +
                     "BH4wfDArBggrBgEFBQcwAYYfaHR0cDovL29jc3Aub25lLm5sLmRpZ2ljZXJ0LmNv\n" +
                     "bTBNBggrBgEFBQcwAoZBaHR0cDovL2NhY2VydHMub25lLm5sLmRpZ2ljZXJ0LmNv\n" +
                     "bS9QRVBQT0xBQ0NFU1NQT0lOVFRFU1RDQS1HMy5jcnQwTgYDVR0fBEcwRTBDoEGg\n" +
                     "P4Y9aHR0cDovL2NybC5vbmUubmwuZGlnaWNlcnQuY29tL1BFUFBPTEFDQ0VTU1BP\n" +
                     "SU5UVEVTVENBLUczLmNybDANBgkqhkiG9w0BAQsFAAOCAgEAXbHWJXYKfhIin2T1\n" +
                     "o/RciJeAXEPbyh/lEqvw1TaSWxz0swo0HhVxC9XAA2ufZaZjOgps4tZTmxG9IxkX\n" +
                     "XO9B3nngMU3DOOfW73cYA0mFrDHLnaALaxgenCVgLyN/f22/8cbHYoUDNoOSBVCZ\n" +
                     "j0TFNpnMQvVXvzvr8lNjpf6hTevPvZEa+8Vy5aBlwbbGh1L8cgc1WToJ9OX7n7zz\n" +
                     "1waKwqyPukTjsNLiz7Nc6nOtBXUCgoTY+9G24FEN0k11kkM3cnhTODeQFZiRGHKv\n" +
                     "RdaZpBODyuVm/U8d5ewAFvpW1N+wTOVkB372Ctg0mMtN+6xd5/VJdoTYOFkjQ/Fa\n" +
                     "m32bZVJ2z8O5meGqWoDHLKoZfMYj6VMiwjTtsF1MK0qgj9AKvCcE6NGn5R8hZikq\n" +
                     "tp/Kvx4I8X94dK+jZr7AKZlJsqaQGhEo1BX0LN2JcnwAc3oNHOyzj3exfkPspUjf\n" +
                     "9DgwLZ+PNrSSLk+tJsMxEJTSIntSSpZOwhGtws5ZtQX3UEp2SwSUrnylCkgXq7yq\n" +
                     "qA3lX/8chHDoI+A1VNR1VOhO2cJN3I+mj16GoEFGxqroB9dcI2ARA9qpKXFX6P1g\n" +
                     "MPg8omdlrplC5QrXgIF23tXDeOmK/ezvlp2kP2g/KiCbVqvt8l2wwaTRqzNnSC0T\n" +
                     "RJ8ByMAV0Fj5XGS1Kl5UGv/BqGY=\n" +
                     "-----END CERTIFICATE-----\n");
  }
}
