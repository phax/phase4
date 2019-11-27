# phase4 - AS4 client and server

![Logo](https://github.com/phax/phase4/blob/master/docs/logo/phase4-logo-152-100.png)

A library to send and receive AS4 messages. 
Licensed under the Apache 2 License!

It consists of the following sub-projects:
  * **phase4-lib** - basic data structures for AS4 handling
  * **phase4-profile-cef** - AS4 profile for CEF/eSENS as well as the PMode and the respective validation
  * **phase4-profile-peppol** - AS4 profile for PEPPOL as well as the PMode and the respective validation (since v0.9.1)
  * **phase4-servlet** - AS4 servlet for integration into existing
  * **phase4-test** - integration test project
  * **phase4-server-webapp** - Standalone AS4 server for **demo** purposes
  * **phase4-peppol-client** - a specific client to send messages to PEPPOL (since v0.9.3)  
  
This solution is CEF compliant. See the test report at https://ec.europa.eu/cefdigital/wiki/download/attachments/82773297/phase4%20AS4%20test%20runs.zip?version=1&modificationDate=1565683321725&api=v2

This solution is Peppol compliant. See the test report at https://github.com/phax/phase4/blob/master/docs/PEPPOL/TestBedReport-POP000306-20190906T103327.pdf
    
# Configuration

The configuration of phase4 is based on 2 different files:
  * `crypto.properties` - the WSS4J configuration file - https://ws.apache.org/wss4j/config.html
  * `phase4.properties` - ph-as4-server specific configuration file (was called `as4.properties` before v0.9.0)
  
### crypto.properties

Use the following file as a template and fill in your key structure:

```ini
org.apache.wss4j.crypto.provider=org.apache.wss4j.common.crypto.Merlin
org.apache.wss4j.crypto.merlin.keystore.file=keys/dummy-pw-test.jks
org.apache.wss4j.crypto.merlin.keystore.password=test
org.apache.wss4j.crypto.merlin.keystore.type=jks
org.apache.wss4j.crypto.merlin.keystore.alias=ph-as4
org.apache.wss4j.crypto.merlin.keystore.private.password=test
```

The file is a classpath relative path like `keys/dummy-pw-test.jks`. 

PEPPOL users: the key store must contain the AccessPoint private key and the truststore must contain the PEPPOL truststore.

### phase4.properties

This AS4 server specific file contains the following properties:

```ini
#server.profile=
server.debug=false
server.production=false
server.nostartupinfo=true
server.datapath=/var/www/as4/data
#server.incoming.duplicatedisposal.minutes=10
#server.address=
```

The file is searched in the locations specified as follows:
* A path denoted by the environment variable `AS4_SERVER_CONFIG`
* A path denoted by the system property `phase4.server.configfile`
* A path denoted by the system property `as4.server.configfile`
* A file named `private-phase4.properties` within your classpath
* A file named `phase4.properties` within your classpath
* A file named `private-as4.properties` within your classpath
* A file named `as4.properties` within your classpath

The properties have the following meaning
* **`server.profile`**: a specific AS4 profile ID that can be used to validate incoming messages. Only needed in specific circumstances. Not present by default.
* **`server.debug`**: enable or disable the global debugging mode in the system. It is recommended to have this always set to `false` except you are developing with the components. Valid values are `true` and `false`.
* **`server.production`**: enable or disable the global production mode in the system. It is recommended to have this set to `true` when running an instance in a production like environment to improve performance and limit internal checks. Valid values are `true` and `false`.
* **`server.nostartupinfo`**: disable the logging of certain internals upon server startup when set to `true`. Valid values are `true` and `false`.
* **`server.datapath`**: the writable directory where the server stores data. It is recommended to be an absolute path (starting with `/`). The default value is the relative directory `conf`.
* **`server.incoming.duplicatedisposal.minutes`**: the number of minutes a message is kept for duplication check. After that time, the same message can be retrieved again. Valid values are integer numbers &ge; 0. The default value is `10`. 
* **`server.address`**: the public URL of this AS4 server to send responses to. This value is optional.

# Peppol handling

## Subproject phase4-peppol-client

The contained project contains a class called `Phase4PeppolSender.Builder` - it contains all the parameters with some example values so that you can start easily. As a prerequisite, the files `phase4.properties` and `crypto.properties` must be filled out correctly and your Peppol AP certificate must be provided (the default configured name is `test-ap.p12`).

## Subproject phase4-peppol-servlet

**The Peppol specific receiving part is work in progress and will be available soon**

# Building from source

Apache Maven needed 3.6 or later and Java JDK 8 or later is required.

To build the whole package on the commandline use `mvn clean install`.

If you are importing this into your IDE and you get build errors, it maybe necessary to run `mvn process-sources` once in the `phase4-lib` subproject. Afterwards the folder `target/generated-sources/xjc` must be added to the source build path. When building only on the commandline, this is done automatically.

# Known limitations

Per now the following known limitations exist:
* Multi-hop does not work
* phase4 is not a standalone project but a library that you need to manually integrate into your system 

# How to help

Any voluntary help on this project is welcome.
If you want to write documentation or test the solution - I'm glad for every help.
Just write me an email - see pom.xml for my email address

# News and noteworthy

* v0.9.6 - work in progress
    * Removed the "ExceptionCallback" from `Phase4PeppolSender`
* v0.9.5 - 2019-11-27
    * Enforcing the usage of `Phase4PeppolSender.builder()` by making the main sending method private
    * Updated to peppol-commons 7.0.4 (moved classes `PeppolCerticateChecker` and `EPeppolCertificateCheckResult` there) (incompatible change)
    * Replaced the Peppol client "certificate consumer" type to be `IPhase4PeppolCertificateCheckResultHandler` (incompatible change)
* v0.9.4 - 2019-11-20
    * Updated to ph-commons 9.3.8
    * Added OCSP/CLR check for Peppol certificates
    * Added support for validation of outgoing Peppol messages using the default Peppol Schematrons
    * Extended the Peppol client API a bit for client side validation (see [issue #19](https://github.com/phax/phase4/issues/19))
    * Outgoing messages now have the User-Agent HTTP header set (see [issue #20](https://github.com/phax/phase4/issues/20))
    * Fixed a typo in the short name of `EBMS_FAILED_DECRYPTION` (see [issue #21](https://github.com/phax/phase4/issues/21))
    * Added a new `Builder` class for the Peppol AS4 client - use `Phase4PeppolSender.builder()` to get started
* v0.9.3 - 2019-11-05
    * Updated to peppol-commons 7.0.3
    * Added new subproject `phase4-peppol-client` to easily send AS4 messages to PEPPOL
    * Fixed default initiator URL (see [issue #18](https://github.com/phax/phase4/issues/18))
* v0.9.2 - 2019-10-07
    * Fixed an invalid assumption in the PEPPOL PMode validator.
* v0.9.1 - 2019-09-06 - PEPPOL conformant
    * Ignored WSS4J dependency "ehcache" to create smaller deployments
    * Added new subproject `phase4-profile-peppol` for the PEPPOL AS4 profile
    * From Party ID type and To Party ID type can now be set in the client
    * The service type can now be set in a PMode
    * Requires ph-commons 9.3.6
    * Requires ph-web 9.1.3
    * This is the first version passing the PEPPOL Testbed v1
* v0.9.0 - 2019-08-08 - CEF conformant
    * The GitHub repository was officially renamed to **phase4**
    * All Maven artifact IDs were renamed from `ph-as4-*` to `phase4-*`
    * The package names changes from `com.helger.as4.*` to `com.helger.phase4.*`
    * Updated to WSS4J 2.2.4
    * Updated to ph-oton 8.2.0
    * Updated to peppol-commons 7.0.0
    * Updated to ph-commons 9.3.5
    * The submodule `ph-as4-esens` was renamed to `phase4-profile-cef`
    * The AS4 message handler now have a chance to access the received HTTP headers
    * Renamed `ph-as4-server-webapp-test` to `phase4-test`
    * Improved Crypto stuff configurability
    * Renamed `AS4ResourceManager` to `AS4ResourceHelper`
    * Renamed `AS4Handler` to `AS4RequestHandler`
    * Reworked client API so that it can be used chainable
    * Added retry support to clients
    * Added possibility to dump incoming and outgoing requests using `AS4DumpManager`
    * This version passes the CEF "AS4 Basic Connectivity Tests"
    * This version passes the CEF "AS4 Common Profile Test Assertions"
    * This version passes the CEF "AS4 Four Corner Profile Enhancement Test Assertions" 
* v0.8.2 - 2019-02-27
    * Adoptions for integration into TOOP
* v0.8.1 - 2018-11-26
    * The web application now uses LOG4J 2.x
    * Requires at least ph-commons 9.2.0
    * Added `@type`-fix from https://issues.oasis-open.org/projects/EBXMLMSG/issues/EBXMLMSG-2
* v0.8.0 - 2018-06-21
    * Updated to ph-commons 9.1.2
    * Updated to BouncyCastle 1.59
    * Updated to WSS4J 2.2.2
    * Successfully send test messages to AS4.NET and Holodeck 3.x
* v0.7.0 - 2017-07-24
    * Added HTTP retry for client
    * Added server duplicate message detection for incoming messages
    * `MessageInfo/Timestamp` uses UTC - thanks Sander
    * Added two-way handling
    * Fixed bug that Receipt is not signed (if desired)
    * Removed `PModeConfig` in favor of redundant `PMode` objects
    * Removed partner handling - not needed anymore 
    * To be on the safe side, delete all previously created `as4-*.xml` files as there were incompatible changes.
    * Added a second webapp - one for demo, one for testing
* v0.6.0 - 2017-01-26
    * Extracted subproject `ph-as4-servlet` with only the AS4Servlet
    * Unified the namespaces across the sub-projects
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

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
