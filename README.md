[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://stand-with-ukraine.pp.ua)

# Chat and voice / video calling app using ConnectyCube

This project contains a chat and voice / video calling app for Android written in Kotlin and based on [ConnectyCube](https://connectycube.com/) communication platform.

<img src="https://developers.connectycube.com/images/code_samples/android_codesample_messenger_demo1.jpg" width=180 />&nbsp;&nbsp;&nbsp;
<img src="https://developers.connectycube.com/images/code_samples/android_codesample_messenger_demo2.jpg" width=180 />&nbsp;&nbsp;&nbsp;
<img src="https://developers.connectycube.com/images/code_samples/android_codesample_messenger_demo3.jpg" width=180 />

## Features 
- User authorization 
- User profile and avatar
- Chat dialogs (private and group)
- Group chat: edit group name, description; add/remove participants; add/remove admins
- Group chat info
- Send messages
- File attachments (only Image)
- Sent/Delivered/Read messages statuses
- ‘Is typing’ statuses
- Video and Audio calls (p2p and group)

## Technical specification:
- Language - [Kotlin](https://kotlinlang.org) (with using coroutines)
- Support library - [androidx](https://developer.android.com/jetpack/androidx)

**Used Android Architecture Components:**
- [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) - Notify views when underlying database changes
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Manage UI-related data in a lifecycle-conscious way
- [Room](https://developer.android.com/topic/libraries/architecture/room) - Fluent SQLite database access
- [Paging](https://developer.android.com/topic/libraries/architecture/paging) - Gradually load information on demand from data source
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Manage Android background jobs

## How to run:

1. Register new account and application at <https://admin.connectycube.com>

2. Put Application credentials from Overview page from <https://admin.connectycube.com/account/settings> to **app/src/main/java/com/connectycube/messenger/utilities/SettingsProvider.kt** class

3. Follow to <https://admin.connectycube.com> and create users in **Users** module. Then put to the **app/src/main/assets/user_configiration.json** file at least 2 and max 5 users with format *[\"login\":{\"password\":userId}]*, for ex. *"userchatLogin1":{"userchatPassword1":310}*.

4. To setup push notifications, do the following:
- get and set Server Key [FCM](https://developers.connectycube.com/android/push-notifications?id=configure-firebase-project-and-api-key) in ConnectyCube Dashboard
- define **sender_id** (your sender id from google console) in string resource and put your **google-services.json** to module package
- uncomment *apply plugin: 'com.google.gms.google-services'* line in app module **build.gradle** file. 

For more information look at <https://developers.connectycube.com/android/push-notifications>

5. Run project.

## Documentation

- [Android SDK documentation](https://developers.connectycube.com/android/)
- [Authentication and Users](https://developers.connectycube.com/android/authentication-and-users)
- [Chat API](https://developers.connectycube.com/android/messaging)
- [Video Chat API](https://developers.connectycube.com/android/videocalling)
- [Push Notifications API](https://developers.connectycube.com/android/push-notifications)

## Have an issue?

Got troubles with integration? Create an issue at [Issues page](https://github.com/ConnectyCube/android-messenger-app/issues)

**Want to support our team**:<br>
<a href="https://www.buymeacoffee.com/connectycube" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>

## License

See [LICENSE](LICENSE)

