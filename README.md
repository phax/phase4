# ph-as4

A library to send and receive AS4 messages. 
Licensed under the Apache 2 License!

It consists of the following sub-projects:
  * **ph-as4-lib** - basic data structures for AS4 handling
  * **ph-as4-esens** - AS4 profile for eSENS as well as the PMode and the respective validation
  * **ph-as4-servlet** - AS4 servlet for integration into existing (since 0.6.0)
  * **ph-as4-server-webapp-demo** - AS4 servlet based server component to be integrated into an existing server

## News and noteworthy
-
  * v0.6.0
    * Extracted subproject `ph-as4-servlet` with only the AS4Servlet
    * Unified the namespaces across the subprojects
    * Requires ph-web 8.7.2 or higher
    * Renamed `ph-as4-server` to `ph-as4-server-webapp-demo`
  * v0.5.0 - 2017-01-18
    * Initial release
    * Has everything needs for sending and receiving using the eSENS P-Mode profiles
    * Basic compatibility with Holodeck 2.1.2 is provided
    * Supports signed messages
    * Supports encrypted messages
    * Supports compressed messages
    * Targets to be easily integrateable into existing solutions
    * Requires Java 8 for building and execution
    
## Configuration

The configuration of ph-as4 is based on 2 different files:
  * `crypto.properties` - the WSS4J configuration file - https://ws.apache.org/wss4j/config.html
  * `as4.properties` - ph-as4-server specific configuration file
  
### crypto.properties

Use the following file as a template and fill in your key structure:
```
org.apache.wss4j.crypto.provider=org.apache.wss4j.common.crypto.Merlin
org.apache.wss4j.crypto.merlin.keystore.file=keys/dummy-pw-test.jks
org.apache.wss4j.crypto.merlin.keystore.password=test
org.apache.wss4j.crypto.merlin.keystore.type=jks
org.apache.wss4j.crypto.merlin.keystore.alias=ph-as4
org.apache.wss4j.crypto.merlin.keystore.private.password=test
```
The file is a classpath relative path like `keys/dummy-pw-test.jks`. 

PEPPOL users: the key store must contain the AccessPoint private key.

### as4.properties

This AS4 server specific file contains the following properties:
```
server.debug=true
server.production=false
server.nostartupinfo=true
server.datapath=/var/www/as4/data

server.proxy.enabled=false
server.proxy.address=10.0.0.1
server.proxy.port=8080
``` 
    
## Known limitations

Per now the following known limitations exist:
  * Only one-way communication is supported - of course `ErrorMessage`s and `Receipt`s are sent.
  * Multi-hop does not work (and is imho not relevant for a usage in PEPPOL)
  * Pull requests are currently not supported
  
## Differences to Holodeck

  * This is a library and not a product
  * ph-as4 is licensed under the business friendly Apache 2 license and not under GPL/LGPL
  * This library only takes care about the effective receiving of documents, but does not provide a storage for them. You need to implement your own incoming document handler!
  * ph-as4 does not use an existing WS-Stack like Axis or Apache CXF but instead operates directly on a Servlet layer for retrieval and using Apache HttpClient for sending

## How to help

Any voluntary help on this project is welcome.
If you want to write documentation or test the solution - I'm glad for every help.
Just write me an email - see pom.xml for my email address - or tweet me @philiphelger

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
