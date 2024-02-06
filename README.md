# A chat messenger and video calling demo app for Android using ConnectyCube platform

This project contains a chat messenger & video calling open source demo app for Android written on Kotlin and based on [ConnectyCube](https://connectycube.com/) communication platform.

<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_messenger_demo1.jpg" width=180 />&nbsp;&nbsp;&nbsp;
<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_messenger_demo2.jpg" width=180 />&nbsp;&nbsp;&nbsp;
<img src="https://developers.connectycube.com/docs/_images/code_samples/android_codesample_messenger_demo3.jpg" width=180 />

## Features 
- Chat 
- Video chat

## Technical specification:
- Language - [Kotlin](https://kotlinlang.org) (with using coroutines)
- Support library - [androidx](https://developer.android.com/jetpack/androidx)

## Used Android Architecture Components:
- [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) - Notify views when underlying database changes
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Manage UI-related data in a lifecycle-conscious way
- [Room](https://developer.android.com/topic/libraries/architecture/room) - Fluent SQLite database access
- [Paging](https://developer.android.com/topic/libraries/architecture/paging) - Gradually load information on demand from data source
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - Manage Android background jobs

## Project contains the following features implemented:
- Edit username and avatar
- Chat dialogs creation (private and group)
- Group chat: edit group name, description; add/remove participants; add/remove admins
- Group chat info
- Send messages
- File attachments (only Image)
- Sent/Delivered/Read messages statuses
- ‘Is typing’ statuses
- Video and Audio calls (p2p and group)


## How to run:

1. Register new account and application at <https://admin.connectycube.com>

2. Put Application credentials from Overview page + Account key from <https://admin.connectycube.com/account/settings> to **app/src/main/java/com/connectycube/messenger/utilities/SettingsProvider.kt** class

3. Follow to <https://admin.connectycube.com> and create users in **Users** module. Then put to the **app/src/main/assets/user_configiration.json** file at least 2 and max 5 users with format *[\"login\":{\"password\":userId}]*, for ex. *"userchatLogin1":{"userchatPassword1":310}*.

4. Run project.

## Documentation

All the samples use ConnectyCube SDK. The following tech integration documentation is available:

- [Android SDK documentation](https://developers.connectycube.com/android/)
- [Chat API](https://developers.connectycube.com/android/messaging)
- [Video Chat API](https://developers.connectycube.com/android/videocalling)
- [Authentication and Users](https://developers.connectycube.com/android/authentication-and-users)

## Have an issue?

Got troubles with integration? Just create an issue at [Issues page](https://github.com/ConnectyCube/android-messenger-app/issues)

## License

See [LICENSE](LICENSE)

