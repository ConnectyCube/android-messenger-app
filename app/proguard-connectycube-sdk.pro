-keep class com.connectycube.auth.model.** { !transient <fields>; }

-keep class com.connectycube.chat.model.** { !transient <fields>; }

-keep class com.connectycube.core.model.** { !transient <fields>; }
-keep class com.connectycube.core.server.** { *; }
-keep class com.connectycube.core.rest.** { *; }
-keep class com.connectycube.core.error.** { *; }
-keep class com.connectycube.core.account.model.** { !transient <fields>; }

-keep class com.connectycube.customobjects.model.** { !transient <fields>; }

# uncomment if use Connectycube Extentions lib
#-keep class com.connectycube.extensions.** { *; }

-keep class com.connectycube.pushnotifications.model.** { !transient <fields>; }

-keep class com.connectycube.storage.model.** { !transient <fields>; }

-keep class com.connectycube.users.model.** { !transient <fields>; }

-keep class com.connectycube.videochat.** { *; }

-keep class org.jivesoftware.** { *; }

-dontwarn org.jivesoftware.smackx.**

-keep class org.webrtc.** { *; }