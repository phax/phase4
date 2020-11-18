# phase4 - AS4 client and server

![Logo](https://github.com/phax/phase4/blob/master/docs/logo/phase4-logo-653-180.png)

A library to send and receive AS4 messages. 
Licensed under the Apache 2 License!

It consists of the following sub-projects:
  * **phase4-lib** - basic data structures for AS4 handling, sending and receiving
  * **phase4-profile-cef** - AS4 profile for CEF/eSENS as well as the PMode and the respective validation
  * **phase4-profile-peppol** - AS4 profile for Peppol as well as the PMode and the respective validation (since v0.9.1)
  * **phase4-test** - integration test project
  * **phase4-server-webapp** - Standalone AS4 server for **demo** purposes
  * **phase4-dynamic-discovery** - a shared library that contains common stuff for dynamic discovery using SML and SMP (since 0.10.6)
  * **phase4-peppol-client** - a specific client to send messages to Peppol (since v0.9.3)
  * **phase4-peppol-servlet** - a specific servlet that can be used to receive messages from Peppol (since v0.9.7)
  * **phase4-peppol-server-webapp** - a simple standalone Peppol AS4 server for **demo** purposes (since v0.9.9) 
  * **phase4-cef-client** - a specific client to send messages using the CEF profile (since v0.9.15)
  
This solution is CEF compliant. See the test report at https://ec.europa.eu/cefdigital/wiki/download/attachments/82773297/phase4%20AS4%20test%20runs.zip?version=1&modificationDate=1565683321725&api=v2

This solution is Peppol compliant. See the test report at https://github.com/phax/phase4/blob/master/docs/Peppol/TestBedReport-POP000306-20190906T103327.pdf

## Known users

Some known users of phase4 - mostly in the context of Peppol - are (in alphabetical order):

* Bundesrechenzentrum / Federal Computing Center (AT) - https://www.brz.gv.at/
* ecosio GmbH (AT) - https://ecosio.com/
* Fitek AS (EE) - https://fitek.com/
* GHX LLC (UK) - https://www.ghx.com/
* Qvalia Group AB (SE) - https://qvalia.com/
* Storecove (Global) - https://www.storecove.com/
* Strands (ES) - https://strands.com/
* T-Systems Multimedia Solutions GmbH (DE) - https://www.t-systems-mms.com
* Telema AS (EE) - https://telema.com/
* TOOP4EU (EU project) - http://toop.eu/
* unifiedpost group (BE) - https://www.unifiedpost.com/

If you are a phase4 user and want to be listed here, write me an email to phase4[at]helger[dot]com
    
# Configuration

The configuration part was reworked for 0.11.0 version.

The primary configuration file for phase4 is called `phase4.properties`.
It contains both the phase4 specific configuration items as well as the WSS4J ones (see https://ws.apache.org/wss4j/config.html).
The resolution of the configuration properties is not bound to the configuration file - system properties and environment variables can also be used. See https://github.com/phax/ph-commons#ph-config for details.
Upon resolution of configuration values, Java system properties have the highest priority (400), before environment variables (300), the file `phase4.properties` (203), the file `private-application.json` (195), the file `private-application.properties` (190), the file `application.json` (185), the file `application.properties` (180) and finally the file `reference.properties` (1).

Note: prior to v0.11.0 a file called `crypto.properties` may have contained the keystore and truststore parameters used by phase4. Since v0.11.0 all these parameters are now exclusively read from `phase4.properties`.

Note: programmatic access to the configuration is solely achieved via class `com.helger.phase4.config.AS4Configuration`.

## WSS4J properties

Note: the descriptions and the default values are taken from [WSS4J](https://ws.apache.org/wss4j/config.html).

* **`org.apache.wss4j.crypto.provider`**: WSS4J specific provider used to create Crypto instances. Defaults to `org.apache.wss4j.common.crypto.Merlin`.
* **`org.apache.wss4j.crypto.merlin.keystore.type`**: the keystore type. Usually one of `JKS` or `PKCS12`. Defaults to `java.security.KeyStore.getDefaultType()`.
* **`org.apache.wss4j.crypto.merlin.keystore.file`**: the path to the keystore. Can be an entry in the class path, a URL or an absolute file path.
* **`org.apache.wss4j.crypto.merlin.keystore.password`**: the password to the whole keystore.
* **`org.apache.wss4j.crypto.merlin.keystore.alias`**: the alias of the key to be used inside the keystore. **Hint** case sensitivity may be important here.
* **`org.apache.wss4j.crypto.merlin.keystore.private.password`**: the password to access the key only. May be different from the keystore password.

* **`org.apache.wss4j.crypto.merlin.load.cacerts`**: Whether or not to load the CA certificates in `${java.home}/lib/security/cacerts` (default is `false`).
* **`org.apache.wss4j.crypto.merlin.truststore.provider`**: The provider used to load truststores. By default it’s the same as the keystore provider. Set to an empty value to force use of the JRE’s default provider.
* **`org.apache.wss4j.crypto.merlin.truststore.type`**: The truststore type. Usually one of `JKS` or `PKCS12`. Defaults to `java.security.KeyStore.getDefaultType()`.
* **`org.apache.wss4j.crypto.merlin.truststore.file`**: The location of the truststore. Can be an entry in the class path, a URL or an absolute file path.
* **`org.apache.wss4j.crypto.merlin.truststore.password`**: The truststore password. Defaults to `changeit`.

Note: for Peppol users the key store must contain the AccessPoint private key and the truststore must contain the Peppol truststore.

## phase4 properties

The properties have the following meaning
* **`global.debug`**: enable or disable the global debugging mode in the system. It is recommended to have this always set to `false` except you are developing with the components. Valid values are `true` and `false` (prior 0.11.0 this property was called `server.debug`).
* **`global.production`**: enable or disable the global production mode in the system. It is recommended to have this set to `true` when running an instance in a production like environment to improve performance and limit internal checks. Valid values are `true` and `false` (prior 0.11.0 this property was called `server.production`).
* **`global.nostartupinfo`**: disable the logging of certain internals upon server startup when set to `true`. Valid values are `true` and `false` (prior 0.11.0 this property was called `server.nostartupinfo`).
* **`global.datapath`**: the writable directory where the server stores data. It is recommended to be an absolute path (starting with `/`). The default value is the relative directory `conf` (prior 0.11.0 this property was called `server.datapath`).

* **`phase4.manager.inmemory`** (since 0.11.0): if this property is set to `true` than phase4 will not create persistent data for PModes ands other domain objects. Since 0.11.0 the default value is `true` (prior versions used `false` as the default)
* **`phase4.wss4j.syncsecurity`** (since 0.11.0): if this property is set to `true` all signing, encryption, signature verification and decryption is linearized in an artificial lock. This should help working around the https://issues.apache.org/jira/browse/WSS-660 bug if one Java runtime needs to contain multiple instances of phase4. Note: this flag is still experimental. Note: this is only a work-around if only phase4 based applications run in the same Java runtime - if other WSS4J applications (like e.g. Oxalis) are also run, this switch does not solve the issue. Defaults to `false`.
* **`phase4.profile`**: a specific AS4 profile ID that can be used to validate incoming messages. Only needed in specific circumstances. Not present by default (prior 0.11.0 this property was called `server.profile`).
* **`phase4.incoming.duplicatedisposal.minutes`**: the number of minutes a message is kept for duplication check. After that time, the same message can be retrieved again. Valid values are integer numbers &ge; 0. The default value is `10` (prior 0.11.0 this property was called `server.incoming.duplicatedisposal.minutes`).
* **`phase4.dump.path`** (since 0.11.0): the base path where dumps of incoming and outgoing files should be created, if the respective dumpers are activated. The default value is `phase4-dumps` relative to the current working directory.
* **`phase4.endpoint.address`**: the public URL of this AS4 server to send responses to. This value is optional. (prior 0.11.0 this property was called `server.address`).

# Profiles vs. PModes

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

## Peppol Prerequisites

To perform testing with Peppol you **MUST** have a valid Peppol certificate.
Testing with a self-signed certificate does **not** work.
Only certificates that are based on the Peppol AP PKI will be accepted.
You may read https://peppol.helger.com/public/locale-en_US/menuitem-docs-peppol-pki for more information on the Peppol PKI.
To retrieve a Peppol certificate, you must be a member of OpenPEPPOL AISBL - see https://peppol.eu/get-involved/join-openpeppol/ for details.

## Peppol AS4 specifics

OASIS AS4 is a profile of OASIS EBMS v3. CEF AS4 is a profile of OASIS AS4. Peppol AS4 is a profile of CEF AS4. Find the Peppol specification document at https://docs.peppol.eu/edelivery/as4/specification/

Peppol has a very limited use of AS4. Some highlights are:
* It uses only one-way push
* TLS certificates must have SSL labs test grade "A" - that means e.g. no TLS 1.0 or 1.1 support
* Signing and encryption rules follow the CEF AS4 profile requirements (AES 128 CGM, SHA-256)
* It allows only for one payload
* You have to use MIME encoding for the payload - and are not allowed to add it into the SOAP body
* The payload is always an SBD envelope (Standard Business Document; mostly wrongly addressed as SBDH - Standard Business Document Header) - same as for Peppol AS2
* Compression must be supported but can be chosen on the senders discretion

## Subproject phase4-peppol-client

This subproject is your entry point for **sending** messages into the Peppol eDelivery network.

The contained project contains a class called `Phase4PeppolSender.Builder` (accessible via factory method `Phase4PeppolSender.builder()`) - it contains all the parameters with some example values so that you can start easily. Alternatively the class `Phase4PeppolSender.SBDHBuilder` (accessible via factory method `Phase4PeppolSender.sbdhBuilder()`) offers a build class where you can add your pre-build StandardBusinessDocument, which implies that no implicit validation of the business document takes place. This class contains utility methods to explicitly validate the payload.

As a prerequisite, the file `phase4.properties` must be filled out correctly and your Peppol AP certificate must be provided (the default configured name is `test-ap.p12`).

See the folder https://github.com/phax/phase4/tree/master/phase4-peppol-client/src/test/java/com/helger/phase4/peppol for different examples on how to send messages via the Peppol AS4 client.

The client side validation of outgoing business documents is implemented using the [Business Document Validation Engine](https://github.com/phax/ph-bdve/) (BDVE).

## Subproject phase4-peppol-servlet

This subproject is your entry point for **receiving** messages from the Peppol eDelivery network.

It assumes you are running an Application server like [Apache Tomcat](https://tomcat.apache.org/) or [Eclipse Jetty](https://www.eclipse.org/jetty/) to handle incoming connections.

Available from v0.9.7 onwards.
Register the Servlet `com.helger.phase4.peppol.servlet.Phase4PeppolServlet` in your application.
Then implement the SPI interface `com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI` to handle incoming Peppol messages. See [Introduction to the Service Providers Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) if you are not familiar with the Java concept of SPI.

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

Additionally before you can start, an `IAS4CryptoFactory` MUST be set. An implementation of this interface provides the keystore as well as the private key for doing signing and/or encryption services in phase4. Default implementations shipping with phase4 are `AS4CryptoFactoryPropertiesFile` and `AS4CryptoFactoryInMemoryKeyStore`. To change that configuration use the extended constructor of `AS4XServletHandler` that itself is instantiated in the `Phase4PeppolServlet` - therefore a custom Servlet class is required, where `Phase4PeppolServlet` should be used as the "copy-paste template" (and don't forget to reference the new servlet class from the `WEB-INF/web.xml` mentioned above). 

**Note:** in v0.9.8 the receiving SPI method was heavily extended to be able to retrieve more information elements directly, without needing to dive deeper into the code.

## Subproject phase4-peppol-server-webapp

This subproject shows how to a simple standalone Peppol AS4 server could look like.
It is a **demo** implementation and does not do anything with the payload except storing it on disk.
Use this as the basis for implementing your own solution - don't take it "as is".
It takes incoming requests via HTTP POST at the URL `/as4`.

Upon startup it checks that a valid Peppol Access Point (AP) certificate is installed.

It stores all incoming requests on disk based on the incoming date time.
* The full incoming message is stored with extension `.as4in`
* The SOAP document is stored with extension `.soap`
* The (decrypted) Peppol payload (SBD Document) is stored with extension `.sbd`
* The returned receipt is stored with extension `.response`

To configure your certificate, modify the file `phase4.properties`. Usually there is no need to alter the truststore - it's the Peppol default truststore and considered to be constant.

Note: this application uses the property `smp.url` in configuration file `phase4.properties` to locate it's home SMP for cross checking if the incoming request is targeted for itself.

To start it from within your IDE you may run the test class `com.helger.phase4.peppol.server.standalone.RunInJettyPHASE4PEPPOL` - it will spawn on http://localhost:8080`.
For IntelliJ users: make sure the folder `phase4-peppol-server-webapp` is the startup directory.

# Usage with Maven

If you want to use phase4 with Maven I suggest the following way:

1. add the BOM into your `<dependencyManagement>` section and
2. add the main artefacts without version in the `<dependency>` block to have a consistent versioning:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
...
  <dependencyManagement>
    <dependencies>
...
      <!-- step 1 -->
      <dependency>
        <groupId>com.helger.phase4</groupId>
        <artifactId>phase4-parent-pom</artifactId>
        <version>x.y.z</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
...
    </dependencies>
  </dependencyManagement>
...
  <dependencies>
...
    <!-- step 2 -->
    <dependency>
      <groupId>com.helger.phase4</groupId>
      <artifactId>phase4-lib</artifactId>
    </dependency>
    <dependency>
      <groupId>com.helger.phase4</groupId>
      <artifactId>phase4-profile-peppol</artifactId>
    </dependency>
...
  </dependencies>
...
</project>
```

Note: prior to v0.9.17 the Maven groupId was `com.helger`.

# Building from source

Apache Maven needed 3.6 or later and Java JDK 8 or later is required.

To build the whole package on the commandline use `mvn clean install -U`.

If you are importing this into your IDE and you get build errors, it maybe necessary to run `mvn process-sources` once in the `phase4-lib` subproject. Afterwards the folder `target/generated-sources/xjc` must be added to the source build path. When building only on the commandline, this is done automatically.

# Known limitations

Per now the following known limitations exist:
* phase4 is not a standalone project but a library that you need to manually integrate into your system 
* Multi-hop does not work

# How to help

Any voluntary help on this project is welcome.
If you want to write documentation or test the solution - I'm glad for every help.
Just write me an email - see pom.xml for my email address

If you like the project, a star on GitHub is always appreciated.

# News and noteworthy

* v0.12.4 - 2020-11-18
   * Remembering the original compression state of incoming attachments
   * Updated to ph-bdve-rules 1.0.14 including Peppol Fall 2020 release corrigendum
* v0.12.3 - 2020-11-06
   * The `phase4-server-webapp` project now also stores all incoming messages to the dump path
   * Ensure the incoming dumper `AS4IncomingDumperFileBased` creates a unique filename by default
   * Allow an empty AS4 Conversation ID in a UserMessage
   * Ensuring that outgoing messages can be dumped, even if retries is set to 0 (see [issue #43](https://github.com/phax/phase4/issues/43))
* v0.12.2 - 2020-10-05
    * Extended the `IPhase4PeppolIncomingSBDHandlerSPI` interface to be able to reject messages on the AS4 layer
    * Updated to ph-bdve-rules 1.0.8
* v0.12.1 - 2020-09-28
    * Updated to peppol-commons 8.2.4
    * Made the value checks when reading Peppol SBDH documents customizable via `Phase4PeppolServletConfiguration.setPerformSBDHValueChecks`
    * Extended the Peppol client sender API to easily send special binary and text payload
* v0.12.0 - 2020-09-22
    * Extended the `IPModeResolver` to also contain the agreementRef value (for ENTSOG) - backwards incompatible change
    * Added support for custom "Part properties" in `IAS4Attachment` (for ENTSOG)
    * The sending date and time of the AS4 message can now be configured in the client
    * Made class `PMode` more static (see [issue #41](https://github.com/phax/phase4/issues/41))
    * `PModeValidationException` is now a subclass of `Phase4Exception`
    * Added setters to some PMode related domain classes
    * A default serialization of the PMode objects as JSON is available (see [issue #40](https://github.com/phax/phase4/issues/40))
    * The internal interface `IAS4MessageState` is now standalone
    * Made the incoming message metadata in class `AS4XServletHandler` easily customizable.
    * Made truststore accessible through `IAS4CryptoFactory`
    * Added new interface `IAS4UserMessageConsumer`
    * Extended API to make PullRequest sending simpler
    * Moved shared fields from `AbstractAS4UserMessageBuilder` to `AbstractAS4MessageBuilder`
    * Added new sanity builder for AS4 Pull Requests using `Phase4Sender.builderPullRequest()`
    * Changed `PMode IAS4ServletPullRequestProcessorSPI.processAS4UserMessage` to `IPMode IAS4ServletPullRequestProcessorSPI.findPMode`
* v0.11.1 - 2020-09-17
    * Updated to Jakarta JAXB 2.3.3
    * Updated to ph-sbdh 4.1.1
    * Updated to peppol-commons 8.2.2
* v0.11.0 - 2020-09-08
    * Extracted new enum `ECryptoKeyIdentifierType` to make the key information type customizable
    * Reworked the configuration so that system properties and environment variables can also be used
    * The class `AS4Configuration` is now the primary source for configuration stuff
    * Class `AS4ServerConfiguration` was deleted
    * Extracted the class `AS4CryptoFactoryProperties` as the base class for `AS4CryptoFactoryPropertiesFile`
    * Deprecated class `AS4CryptoFactoryPropertiesFile` in favour of `AS4CryptoFactoryProperties`
    * The file `crypto.properties` is considered deprecated. All values should be placed now in `phase4.properties`.
    * By default the "in memory" managers are enabled. To disable this, add `phase4.manager.inmemory=false` in your configuration.
    * Dumping interfaces no longer implement `Serializable`
    * Added missing `onEndRequest` call to the outgoing dumper when sending responses
* v0.10.6 - 2020-09-03
    * The CEF client now has support for OASIS BDXR SMP v2
    * The signature canonicalization method can now be customized
    * Created new submodule `phase4-dynamic-discovery` that contains the shared parts used for dynamic discovery with SML and SMP
    * `phase4-peppol-client` and `phase4-cef-client` use the classes from `phase4-dynamic-discovery` - backwards incompatible change
* v0.10.5 - 2020-08-30
    * Updated to ph-commons 9.4.7
    * Updated to ph-oton 8.2.6
    * Updated to peppol-commons 8.1.7
    * Replaced `AS4WorkerPool` with `PhotonWorkerPool`
    * Improved validation of Peppol requirements for incoming messages, if the correct AS4 Profile "peppol" is selected
    * Using Java 8 date and time classes for JAXB created classes
* v0.10.4 - 2020-07-22
    * Extracted `IAS4ProfileManager` interface
    * Added profile manager to the `IManagerFactory` interface
    * Reworked the WSS4J initialization code to try to avoid the WSS-660 issue
* v0.10.3 - 2020-07-15
    * Updated to ph-commons 9.4.6
    * Added `AS4ServerInitializer.shutdownAS4Server` to gracefully unschedule all jobs
    * Improved customizability of the `Phase4CEFSender` to define if the `@type` attribute should be emitted or not
    * Fixed an invalid `Content-Type` parsing issue, if an empty parameter is contained
* v0.10.2 - 2020-07-07
    * Fixed an UnsupportedOperationException when AS4 HTTP Debugging was enabled AND an outgoing dumper was registered (see [issue #39](https://github.com/phax/phase4/issues/39))
    * Extended Peppol SBDH based builder to set the identifiers from the SBDH (see [issue #22](https://github.com/phax/phase4/issues/22))
    * Moved the `HttpClientFactory` setting one class up from `AbstractAS4UserMessageBuilder` to `AbstractAS4MessageBuilder`
    * Improved the configurability of the dumpers   
* v0.10.1 - 2020-06-24
    * Added the possibility to provide a custom VESRegistry to the Peppol client to provide additional validation rules
    * Changed the method `IAS4DuplicateManager` method `findFirst` to `getItemOfMessageID` to be implementable in different ways
    * Updated to WSS4J 2.3.0 and XMLSec 2.2.0
    * Using `ph-xsds-xlink` and `ph-xsds-xml` for a shared "XLink" JAXB artefact
* v0.10.0 - 2020-06-08
    * Updated to ph-bdve 6.0.0
    * Merged `phase4-servlet` into `phase4-lib`; therefore dropped `phase4-servlet` submodule
    * Moved internal classes to new packages: `BasicHttpPoster`, `AS4BidirectionalClientHelper`
    * Added a new class `Phase4Sender` that does offer sending capabilities with the builder pattern
    * All the client builders were unified - that creates incompatible name changes to `Phase4PeppolSender` (as in `setSenderPartyID` &rarr; `senderPartyID`)
    * Extracted `IAS4TimestampManager` to be able to provide custom timestamps 
* v0.9.17 - 2020-05-27
    * Changed Maven groupId to `com.helger.phase4`
    * Updated to ph-commons 9.4.4
* v0.9.16 - 2020-05-20
    * Becoming more specific in thrown exceptions. Avoiding all "throws Exception"
    * Fixed a potential concurrency error in `IPModeManager` implementations when calling "createOrUpdatePMode"
    * Fixed a potential concurrency error in `AS4CryptoFactoryPropertiesFile.getDefaultInstance()`
    * Added new class `Phase4OutgoingAttachment` for easier creation of outgoing attachments
    * Extended the `Phase4CEFSender` to handle multiple attachments. 
    * Extended the `Phase4CEFSender` to allow overriding "Action" and "Service" 
* v0.9.15 - 2020-05-19
    * Increased customizability of `AS4XServletHandler`
    * Added a new submodule `phase4-cef-client` for easy sending using the CEF profile
    * Note: this version had a problem when deploying to Maven Central - **so it's binary representation is broken**
* v0.9.14 - 2020-04-28
    * Updated to WSS4J 2.2.5
    * Updated to ph-commons 9.4.1
    * Improved configurability of `MetaAS4Manager`
    * Moved callback interface `IPhase4PeppolResponseConsumer` to `IAS4RawResponseConsumer` in `phase4-lib` 
    * Moved callback interface `IPhase4PeppolSignalMessageConsumer` to `IAS4SignalMessageConsumer` in `phase4-lib`
    * Moved `Phase4PeppolSender.parseSignalMessage` to class `AS4IncomingHandler` in `phase4-servlet`
    * Removed the check for the `refToMessageInError` attribute when receiving "Error SignalMessages"
* v0.9.13 - 2020-03-17
    * Moved `originalSender` and `finalRecipient` tests to the CEF and Peppol profiles (see [issue #33](https://github.com/phax/phase4/issues/33))
    * Added new class `AS4ProfileSelector` for more flexible profile selection
    * Added possibility for dumping the created SBDH in `Phase4PeppolSender.Builder` (see [issue #34](https://github.com/phax/phase4/issues/34))
    * Made the setter of `Phase4PeppolServletMessageProcessorSPI` chainable
    * Extracted class `Phase4PeppolReceiverCheckData` to make the consistency check more flexible.
* v0.9.12 - 2020-03-09
    * Fixed potential NPE in error case (see [issue #32](https://github.com/phax/phase4/issues/32))
    * Fixed the setting of the `originalSender` and the `finalRecipient` message properties for Peppol. The `type` attribute must contain the identifier scheme.
* v0.9.11 - 2020-03-03
    * Updated to ph-web 9.1.10
    * Propagating processing errors to the client (see [issue #30](https://github.com/phax/phase4/issues/30)) - thanks to https://github.com/RovoMe
    * Replaced the unchecked `AS4BadRequestException` with the checked `Phase4Exception` (backwards incompatible change)
* v0.9.10 - 2020-02-16
    * Fixed a stupid error in the demo code that prohibits the correct receiver check activation - see https://github.com/phax/phase4/commit/796c054d972562d31fe33597b8f7938081b8183e for the resolution
    * Invoking the `AS4RequestHandler` error consumer also on asynchronous processing
    * Extended the error consumer interface of `AS4RequestHandler` from `Consumer` to `IAS4RequestHandlerErrorConsumer` (backwards incompatible change)
    * Extended the message metadata class `AS4IncomingMessageMetadata`
    * Updated to ph-web 9.1.9
* v0.9.9 - 2020-02-09
    * Removed the methods deprecated in v0.9.8
    * Updated to peppol-commons 8.x
    * Extended `Phase4PeppolEndpointDetailProviderSMP` API
    * Added new subproject `phase4-peppol-server-webapp` with a demo server for receiving messages via Peppol
    * Extended `IAS4IncomingDumper` API with an "end request" notifier
    * The asynchronous response now also uses the outgoing dumper
    * Merged two methods in class `IAS4ResponseAbstraction` into one (backwards incompatible change)
    * Invoking the outgoing dumper also for responses sent for incoming messages
* v0.9.8 - 2020-01-29
    * Added possibility to use external message ID in Peppol client
    * Added new classes `AS4IncomingDumperSingleUse` and `AS4OutgoingDumperSingleUse` for easier per-call dumping
    * Peppol client now has an additional callback to retrieve the AS4 URL where the message is send to
    * No longer throwing an exception if `phase4.properties` is not available. Changed to a warning.
    * Added new class `AS4IncomingMessageMetadata` to hold metadata for each incoming message
    * The `IAS4ServletMessageProcessorSPI` API was modified to now include `IAS4IncomingMessageMetadata` (backwards incompatible change)  
    * The `IPhase4PeppolIncomingSBDHandlerSPI` API was modified to now include `IAS4IncomingMessageMetadata` as well as `PeppolSBDHDocument`, `Ebms3UserMessage` and `IAS4MessageState` (backwards incompatible change)  
    * The `IAS4IncomingDumper` API was modified to now include `IAS4IncomingMessageMetadata` (backwards incompatible change)  
    * Added the original (potentially encrypted) SOAP document into `IAS4MessageState`
    * Renamed type `ESOAPVersion` to `ESoapVersion` (backwards incompatible change)
    * Method names in `IAS4ClientBuildMessageCallback` changed to use `Soap` instead of `SOAP`
    * Extended `IAS4ServletMessageProcessorSPI` with a possibility to process the response message send out
    * Renamed `AS4CryptoFactory` to `AS4CryptoFactoryPropertiesFile` (backwards incompatible change)
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