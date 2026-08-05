# Bisq Node proguard file

### protobuf deps
-dontwarn java.lang.MatchException

# Keep gRPC and Netty classes
-keep class io.grpc.** { *; }
-keep class io.netty.** { *; }
-keep class io.grpc.netty.shaded.io.netty.** { *; }

# Keep all the missing classes that are referenced but not directly used
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.nano.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.core.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.lz4.**
-dontwarn net.jpountz.xxhash.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.bouncycastle.openssl.**
-dontwarn org.bouncycastle.operator.**
-dontwarn org.bouncycastle.pkcs.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.jboss.marshalling.**
-dontwarn reactor.blockhound.**
-dontwarn sun.security.x509.**

# Don't shrink/obfuscate build-time plugins
-dontwarn com.android.build.**
-dontwarn com.google.protobuf.gradle.**
-dontwarn org.codehaus.groovy.**
-dontwarn javax.inject.**
-dontwarn org.gradle.**
-dontwarn javassist.**
-dontwarn org.apache.maven.**
-dontwarn kr.motd.maven.**
-dontwarn org.eclipse.**

# Keep any native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# NOTE (issue #1680): a blanket keep for every java.io.Serializable implementor was removed - bisq2
# serializes via protobuf (fully kept below), our code via kotlinx-serialization (rules above).

# Keep classes used by androidx.datastore persistence
-keep class network.bisq.mobile.domain.data.model.** { *; }
-keep class network.bisq.mobile.domain.data.datastore.** { *; }

# Keep androidx.datastore serializer impls (the library itself ships consumer rules)
-keep class * implements androidx.datastore.core.okio.OkioSerializer { *; }
-keepclassmembers class * implements androidx.datastore.core.okio.OkioSerializer {
    public <methods>;
}

# Preserve Kotlinx Serialization serializers and entry points
-keep class **$$serializer { *; }
-keepclassmembers class **$Companion {
    public static kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class ** {
    public static kotlinx.serialization.KSerializer serializer(...);
}
-keepnames @kotlinx.serialization.Serializable class ** { *; }

# Critical R8 full mode rule for sealed class serialization
# See: https://github.com/Kotlin/kotlinx.serialization/issues/2050
# When R8 full mode is enabled, sealed classes need special handling
-if @kotlinx.serialization.Serializable class **
-keep, allowshrinking, allowoptimization, allowobfuscation, allowaccessmodification class <1>

# Keep attributes needed for polymorphic serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Explicitly keep sealed interface implementations for offer specs
-keep class network.bisq.mobile.domain.data.replicated.offer.amount.spec.** { *; }
-keep class network.bisq.mobile.domain.data.replicated.offer.price.spec.** { *; }

# Preserve Tor service classes moved to shared/domain
-keep class network.bisq.mobile.domain.service.network.** { *; }

###########################################
# Core Bisq Protobuf preservation rules
###########################################

# Keep all Bisq core classes (narrowing tracked as Phase B of issue #1680).
# NOTE: former sibling rules org.bisq.**/chat.**/network.**/bonded_roles.**/user.** were removed:
# no such top-level Java packages exist in the bisq2 jars (all core code lives under bisq.*), and
# network.** additionally swallowed ALL of our own app code (network.bisq.mobile.**), exempting it
# from shrinking - our reflective surfaces are covered by the targeted rules above.
-keep class bisq.** { *; }

# Keep all Protobuf-related classes
-keep class com.google.protobuf.** { *; }

# Keep names for Protobuf types to match type_url
-keepnames class * implements com.google.protobuf.MessageLite
-keepnames class * extends com.google.protobuf.GeneratedMessageLite
-keepnames class * extends com.google.protobuf.GeneratedMessageV3

# Keep inner builder classes and their members
-keep class **$Builder { *; }
-keepclassmembers class *$Builder { *; }

# Keep class members of any class extending GeneratedMessageLite
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}

# Keep all protobuf resolver classes and their names - CRITICAL FOR PROTOBUF DESERIALIZATION
-keep class bisq.common.proto.ProtoResolver { *; }
-keep class bisq.common.proto.PersistableProtoResolverMap { *; }
-keep class bisq.common.proto.NetworkStorageWhiteList { *; }
-keep class * implements bisq.common.proto.PersistableProtoResolver { *; }

# Keep all static initializers (needed for PersistableProtoResolverMap)
-keepclassmembers class * {
    static <clinit>();
}

# Keep annotations used for proto type resolving
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes *Annotation*

# Keep fields and methods used for reflection
-keepclassmembers class * {
    @com.google.protobuf.* *;
}

# Keep resolver and registration methods
-keepclassmembers class * {
    static *** register*(...);
    public static void register(...);
    public static * fromAny(...);
    public static bisq.common.proto.ProtoResolver getResolver();
    public static bisq.common.proto.ProtoResolver getNetworkMessageResolver();
    public static * fromProto(*);
}

# Keep protobuf internal cached size fields and related synthetic methods
-keepclassmembers class * {
    int memoizedSerializedSize;
    int memoizedSize;
    int memoizedHashCode;
    synthetic <methods>;
}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    public int getSerializedSize();
}

# Keep statics and enums used in Protobuf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep anything under .proto. packages if they exist
-keep class **.proto.** { *; }
-keepnames class **.proto.**

# Keep persistence store classes
-keep class bisq.persistence.** { *; }
-keep class bisq.network.identity.** { *; }

# Keep all Bouncy Castle classes
-keep class org.bouncycastle.** { *; }

# Keep all Tor-related classes
-keep class org.torproject.** { *; }

# Ignore missing Java desktop/server classes
-dontwarn com.sun.net.httpserver.**
-dontwarn jakarta.servlet.**
-dontwarn java.awt.**
-dontwarn java.awt.image.**
-dontwarn javax.servlet.**
-dontwarn javax.**
-dontwarn jakarta.**

# Keep Logback and SLF4J classes
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**

# Keep Bisq logging classes specifically
-keep class bisq.common.logging.** { *; }

# Keep logback configuration classes that use reflection
-keep class ch.qos.logback.core.rolling.** { *; }
-keep class ch.qos.logback.classic.** { *; }

## General Android/Kotlin/Compose

# Keep Kotlin Metadata
-keepattributes KotlinMetadata
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# NOTE (issue #1680): blanket keeps for kotlinx.**, androidx.compose.**, @Composable classes,
# ComposableSingletons/LiveLiterals, org.koin.** and kotlin.Metadata were removed - all of these
# libraries ship their own consumer proguard rules, and Koin resolves via constructor references
# in module DSLs (no reflection). KotlinMetadata is preserved via -keepattributes above.

# Comprehensive -dontwarn section (consolidated)
-dontwarn com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector
-dontwarn com.sun.xml.fastinfoset.sax.AttributesHolder
-dontwarn com.sun.jdi.**
-dontwarn com.sun.xml.fastinfoset.stax.**
-dontwarn groovy.lang.**
-dontwarn groovy.transform.Generated
-dontwarn java.beans.**
-dontwarn java.lang.Module
-dontwarn java.lang.management.**
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.imageio.**
-dontwarn javax.mail.**
-dontwarn javax.management.**
-dontwarn javax.naming.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.avalon.framework.logger.Logger
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.logging.impl.Log4JLogger
-dontwarn org.apache.maven.**
-dontwarn org.brotli.dec.BrotliInputStream
-dontwarn org.codehaus.groovy.**
-dontwarn org.codehaus.janino.**
-dontwarn org.codehaus.plexus.component.annotations.Component
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.eclipse.ui.IStartup
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.hibernate.validator.HibernateValidator
-dontwarn org.ietf.jgss.**
-dontwarn org.jvnet.**
-dontwarn org.osgi.framework.**
-dontwarn sun.reflect.Reflection

# Keep specific classes that need explicit preservation
-keep class org.apache.commons.logging.impl.Log4JLogger { *; }

# Obfuscation is OFF deliberately and stays off (issue #1680): Bisq is AGPL open source, so renaming
# buys no secrecy, and readable production stack traces matter more than the Play advisory item -
# we run no mapping-upload pipeline. Optimization however is ON: everything reflection-sensitive
# (protobuf, resolvers, bisq core) is explicitly kept below, which is what makes it safe.
-dontobfuscate

# Keep all protobuf and resolver classes completely intact
-keep class bisq.common.proto.** { *; }
-keep class bisq.persistence.** { *; }
-keep class bisq.network.p2p.services.data.storage.** { *; }
-keep class bisq.network.p2p.message.** { *; }

# Keep all protobuf generated classes
-keep class com.google.protobuf.** { *; }
-keep class **.protobuf.** { *; }

# Keep all store classes and their methods
-keep class **.*Store { *; }
-keep class * implements bisq.persistence.PersistableStore { *; }

# Keep resolver registration
-keep class bisq.application.ResolverConfig { *; }


# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# NOTE (issue #1680): a previous rule here kept every class OUTSIDE the bisq/protobuf/netty/... list
# ("-keep class !bisq.**,...") - despite its comment it PREVENTED all external-library shrinking.
# Removed; third-party libraries rely on their bundled consumer rules plus the explicit keeps above.

# Allow removal of unused external library methods and debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove Kermit debug/info/verbose logs in release builds
-assumenosideeffects class co.touchlab.kermit.Logger {
    public *** d(...);
    public *** v(...);
    public *** i(...);
}

# Remove debug log calls from our logging interface
-assumenosideeffects class * implements network.bisq.mobile.domain.utils.Logging {
    *** log.d(...);
    *** log.v(...);
    *** log.i(...);
}

# NOTE: earlier revisions stripped System.out/println and protobuf getSerializedSize() calls here via
# -assumenosideeffects, as a workaround for bisq2 core log noise. Removed (issue #1680): those rules were
# dormant while -dontoptimize was set and would have activated for the first time with optimization on -
# risky around SystemOutFilter's stream capture - and they are redundant since #767 silences the core's
# logback (root OFF) and hard-blocks stdout in release builds at runtime.

## Tink (com.google.crypto.tink) — used transitively for EncryptedSharedPreferences /
## push-notification-key encryption. Tink ships an unused KeysDownloader utility that
## references google-http-client + joda-time; those are not on our classpath because
## we never call KeysDownloader. R8 fails the build on the unresolved references unless
## we explicitly tell it to ignore them.
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn org.joda.time.Instant

## bisq2 2.1.11 transitive deps (Apache HttpClient5, Apache Commons, Jersey, Swagger,
## gRPC-Netty-shaded) reference JDK-only and optional classes that don't exist on
## Android. None are reachable at runtime on the node app:
##  - MethodHandleProxies / AnnotatedParameterizedType / InaccessibleObjectException
##    — JDK-only reflection APIs used by Apache Commons Lang3 dynamic proxies and
##    Jersey/Swagger WADL generators. The node app doesn't expose REST surfaces, so
##    none of those code paths run.
##  - jdk.net.ExtendedSocketOptions / jdk.net.Sockets — Sun-internal socket option
##    APIs used by HttpClient5; the library falls back gracefully when absent.
##  - org.apache.commons.compress.compressors.* — optional content-codec for
##    HttpClient5; falls back to gzip/deflate when commons-compress isn't on the
##    classpath. We don't depend on it transitively.
##  - org.osgi.annotation.bundle.Export — compile-time annotation only, never
##    referenced at runtime. Pulled in by shaded gRPC-Netty's JCTools package-info.
## Rules mirror the R8-generated missing_rules.txt verbatim.
-dontwarn java.lang.invoke.MethodHandleProxies
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.InaccessibleObjectException
-dontwarn jdk.net.ExtendedSocketOptions
-dontwarn jdk.net.Sockets
-dontwarn org.apache.commons.compress.compressors.CompressorException
-dontwarn org.apache.commons.compress.compressors.CompressorInputStream
-dontwarn org.apache.commons.compress.compressors.CompressorOutputStream
-dontwarn org.apache.commons.compress.compressors.CompressorStreamFactory
-dontwarn org.osgi.annotation.bundle.Export
