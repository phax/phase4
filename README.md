# phase4 - AS4 client and server

![Logo](https://github.com/phax/phase4/blob/master/docs/logo/phase4-logo-653-180.png)

A library to send and receive AS4 messages. 
Licensed under the Apache 2 License!

It consists of the following sub-projects:
  * **phase4-lib** - basic data structures for AS4 handling
  * **phase4-profile-cef** - AS4 profile for CEF/eSENS as well as the PMode and the respective validation
  * **phase4-profile-peppol** - AS4 profile for Peppol as well as the PMode and the respective validation (since v0.9.1)
  * **phase4-servlet** - AS4 servlet for integration into existing
  * **phase4-test** - integration test project
  * **phase4-server-webapp** - Standalone AS4 server for **demo** purposes
  * **phase4-peppol-client** - a specific client to send messages to Peppol (since v0.9.3)
  * **phase4-peppol-servlet** - a specific servlet that can be used to receive messages from Peppol (since v0.9.7)
  
This solution is CEF compliant. See the test report at https://ec.europa.eu/cefdigital/wiki/download/attachments/82773297/phase4%20AS4%20test%20runs.zip?version=1&modificationDate=1565683321725&api=v2

This solution is Peppol compliant. See the test report at https://github.com/phax/phase4/blob/master/docs/Peppol/TestBedReport-POP000306-20190906T103327.pdf
    
# Configuration

The configuration of phase4 is based on 2 different files:
  * `crypto.properties` - the WSS4J configuration file - https://ws.apache.org/wss4j/config.html
  * `phase4.properties` - phase4-servlet specific configuration file (was called `as4.properties` before v0.9.0)

Additionally some phase4 specific system properties are available.
  
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

Peppol users: the key store must contain the AccessPoint private key and the truststore must contain the Peppol truststore.

**Note:** since v0.9.6 the configuration of the keystore and truststore can be done in the code (using `AS4CryptoProperties`) and this configuration file becomes optional.

**Note:** since v0.9.7 the whole crypto configuration can be done in-memory when using `AS4CryptoFactoryInMemoryKeyStore`.

### phase4.properties

This property file must be provided, when the `phase4-servlet` submodule for receiving is used.
If you are only using `phase4-lib` for sending, than this file is not of interest.

This file contains the following properties:

```ini
#server.profile=peppol
server.debug=false
server.production=false
server.nostartupinfo=true
server.datapath=/var/www/as4/data
#server.incoming.duplicatedisposal.minutes=10
#server.address=
```

The file is searched in the locations specified as follows:
* A path denoted by the environment variable `PHASE4_SERVER_CONFIG`
* A path denoted by the system property `phase4.server.configfile`
* A path denoted by the system property `as4.server.configfile` (for legacy reasons)
* A file named `private-phase4.properties` within your classpath
* A file named `phase4.properties` within your classpath
* A file named `private-as4.properties` within your classpath (for legacy reasons)
* A file named `as4.properties` within your classpath (for legacy reasons)

The properties have the following meaning
* **`server.profile`**: a specific AS4 profile ID that can be used to validate incoming messages. Only needed in specific circumstances. Not present by default.
* **`server.debug`**: enable or disable the global debugging mode in the system. It is recommended to have this always set to `false` except you are developing with the components. Valid values are `true` and `false`.
* **`server.production`**: enable or disable the global production mode in the system. It is recommended to have this set to `true` when running an instance in a production like environment to improve performance and limit internal checks. Valid values are `true` and `false`.
* **`server.nostartupinfo`**: disable the logging of certain internals upon server startup when set to `true`. Valid values are `true` and `false`.
* **`server.datapath`**: the writable directory where the server stores data. It is recommended to be an absolute path (starting with `/`). The default value is the relative directory `conf`.
* **`server.incoming.duplicatedisposal.minutes`**: the number of minutes a message is kept for duplication check. After that time, the same message can be retrieved again. Valid values are integer numbers &ge; 0. The default value is `10`. 
* **`server.address`**: the public URL of this AS4 server to send responses to. This value is optional.

### System properties

The following special system properties are supported:
* **`phase4.manager.inmemory`** (since v0.9.6): if set to `true` the system will not try to store data in the file system. By default this is `false`.

# Different configurations

To handle common parts of AS4 PModes this project uses so called "profiles". Currently two default profiles are available:

* CEF with ID `cef` in submodule `phase4-profile-cef` - see https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/eDelivery+AS4+-+1.14 for the full specification
* Peppol with ID `peppol` in submodule `phase4-profile-peppol` - see https://docs.peppol.eu/edelivery/as4/specification/ for the full specification

To use one of these profiles, the respective Maven artifacts must be added as dependencies to your project as in

```xml
    <dependency>
      <groupId>com.helger</groupId>
      <artifactId>phase4-profile-peppol</artifactId>
      <version>x.y.z</version>
    </dependency>
```

If you want to create your own profile, you need to provide an [SPI](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) implementation of the `phase4-lib` interface `com.helger.phase4.profile.IAS4ProfileRegistrarSPI`. See the above mentioned submodules as examples on how to do that.


# Peppol handling

Peppol is an international eDelivery network. Read more on https://peppol.eu

## Subproject phase4-peppol-client

The contained project contains a class called `Phase4PeppolSender.Builder` - it contains all the parameters with some example values so that you can start easily. Alternatively the class `Phase4PeppolSender.SBDHBuilder` offers a build class where you can add your pre-build StandardBusinessDocument, which implies that no implicit validation of the business document takes place. This class contains utility methods to explicitly validate the payload.

As a prerequisite, the files `phase4.properties` and `crypto.properties` must be filled out correctly and your Peppol AP certificate must be provided (the default configured name is `test-ap.p12`).

See the folder https://github.com/phax/phase4/tree/master/phase4-peppol-client/src/test/java/com/helger/phase4/peppol for different examples on how to send messages via the Peppol AS4 client. 

## Subproject phase4-peppol-servlet

Available from v0.9.7 onwards.
Register the Servlet `com.helger.phase4.peppol.servlet.Phase4PeppolServlet` in your application.
Than implement the SPI interface `com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI` to handle incoming Peppol messages.

Sample setup for `WEB-INF/web.xml`:

```xml
<servlet>
  <servlet-name>Phase4PeppolServlet</servlet-name>
  <servlet-class>com.helger.phase4.peppol.servlet.Phase4PeppolServlet</servlet-class>
</servlet>
<servlet-mapping>
  <servlet-name>Phase4PeppolServlet</servlet-name>
  <url-pattern>/as4</url-pattern>
</servlet-mapping>
```

By default the "receiver checks" are enabled. They are checking if the incoming message is targeted for the correct Access Point. That is done by performing an SMP lookup on the receiver/document type/process ID and check if the resulting values match the preconfigured values. That of course requires that the preconfigured values need to be set, before a message can be received. That needs to be done via the static methods in class `Phase4PeppolServletConfiguration`. Alternatively you can disable the receiver checks using the `setReceiverCheckEnabled` method in said class.

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

If you like the project, a star on GitHub is always appreciated.

# News and noteworthy

* v0.9.8 - work in progress
    * Added possibility to use external message ID in Peppol client
* v0.9.7 - 2020-01-20
    * Removed the default configuration files from `phase4-peppol-client`
    * Added the new submodule `phase4-peppol-servlet` with the Peppol specific receiving stuff
    * Extracted interface `IAS4Attachment` from `WSS4JAttachment` for read-only access
    * Fixed NPE when receiving an attachment without a "Content-ID"
    * Removed all deprecated and unused methods from previous versions
    * Extracted `IAS4CryptoFactory` interface for broader usage
    * Added possibility to use a preconfigured receiver AP certificate and endpoint URL for the Peppol client
    * Changed `IPhase4PeppolValidatonResultHandler` to be an empty interface and `Phase4PeppolValidatonResultHandler` is the default implementation
    * The base class of `Phase4PeppolException` changed from `Exception` to `Phase4Exception`
    * Incoming messages are checked via against the values configured in class `Phase4PeppolServletConfiguration`
    * For security reasons the dependency to the XML pull parser "woodstox" was removed
    * For security reasons the dependency to the DNS library "dnsjava" was removed
    * Added the new class `AS4CryptoFactoryInMemoryKeyStore` that takes an in-memory key store and trust store (see [issue #28](https://github.com/phax/phase4/issues/28))
    * Updated to peppol-commons 7.0.6 with more flexible SMP client API
    * `SOAPHeaderElementProcessorRegistry` is no longer a singleton
    * The Peppol client can now handle Receipts that are MIME encoded
    * The Peppol client now verifies the signatures of the response messages
    * The Peppol client now honours the "incoming dumper" for the response messages
* v0.9.6 - 2019-12-12
    * Removed the "ExceptionCallback" from `Phase4PeppolSender`
    * Changed the data types of "ResponseConsumer" and "SignalMsgConsumer" from `Phase4PeppolSender` to be able to throw exception (binary incompatible change)
    * Added the possibility to configure the keystore without the need of having the `crypto.properties` file
    * Extracted interface `IMPCManager` from `MPCManager` and using it internally
    * Extracted interface `IPModeManager` from `PModeManager` and using it internally
    * The method `IPModeManager.validatePMode` now throws a checked `PModeValidationException` exception (incompatible change)
    * Added the possibility to customize the outgoing dumper in class `Phase4PeppolSender`
    * Added specific `Phase4PeppolSMPException` for SMP lookup errors (incompatible change)
    * Extracted interface `IAS4DuplicateManager` from `AS4DuplicateManager` and using it internally
    * Added the possibility to send pre-build SBDH messages (see [issue #22](https://github.com/phax/phase4/issues/22)) (binary incompatible change)
    * Added support for creating in-memory managers only, using the system property `phase4.manager.inmemory`
    * Parameter type of `IAS4IncomingDumper.onNewRequest` changed to `HttpHeaderMap` (incompatible change)
    * Made `AS4RequestHandler` usage more flexible to not solely rely on the Servlet API
    * New logo thanks to Maria Petritsopoulou - http://stirringpixels.com/
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
    * Added new subproject `phase4-peppol-client` to easily send AS4 messages to Peppol
    * Fixed default initiator URL (see [issue #18](https://github.com/phax/phase4/issues/18))
* v0.9.2 - 2019-10-07
    * Fixed an invalid assumption in the Peppol PMode validator.
* v0.9.1 - 2019-09-06 - Peppol conformant
    * Ignored WSS4J dependency "ehcache" to create smaller deployments
    * Added new subproject `phase4-profile-peppol` for the Peppol AS4 profile
    * From Party ID type and To Party ID type can now be set in the client
    * The service type can now be set in a PMode
    * Requires ph-commons 9.3.6
    * Requires ph-web 9.1.3
    * This is the first version passing the Peppol Testbed v1
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
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)