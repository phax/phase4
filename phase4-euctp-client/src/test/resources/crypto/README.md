generate testing keypair for the client 

    keytool -genkeypair -keyalg rsa -keysize 4096 -storetype PKCS12 -keystore testClient.keystore -storepass justForTesting1 -alias client -dname 'CN=phase4-euctp-client, OU=phase4, O=phax, L=Darmstadt, ST=Hessen, C=DE'

    keytool -list -keystore testClient.keystore -storepass justForTesting1


export client cert and build server truststore with it

    keytool -exportcert -alias client -keystore testClient.keystore -storepass justForTesting1 -file testClient.cert

    keytool -importcert -keystore testServer.truststore -storepass justForTesting3 -storetype PKCS12 -file testClient.cert -v -alias clientToTrust

    keytool -list -keystore testServer.truststore -storepass justForTesting3


generate testing keypair for the server 

    keytool -genkeypair -keyalg rsa -keysize 4096 -storetype PKCS12 -keystore testServer.keystore -storepass justForTesting2 -alias server -dname 'CN=phase4-euctp-servlet, OU=phase4, O=phax, L=Darmstadt, ST=Hessen, C=DE'

    keytool -list -keystore testServer.keystore -storepass justForTesting2


export server cert and build client truststore with it

    keytool -exportcert -alias server -keystore testServer.keystore -storepass justForTesting2 -file testServer.cert

    keytool -importcert -keystore testClient.truststore -storepass justForTesting4 -storetype PKCS12 -file testServer.cert -v -alias serverToTrust

    keytool -list -keystore testClient.truststore -storepass justForTesting4


import server cert into client keystore 

    keytool -importcert -keystore testClient.keystore -storepass justForTesting1 -storetype PKCS12 -file testServer.cert -v -alias serverToTrust