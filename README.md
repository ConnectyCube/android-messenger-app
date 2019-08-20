# A chat messenger and video calling demo app for Android using ConnectyCube platform

This project contains a chat messenger & video calling open source demo app for Android written on Kotlin and based on [ConnectyCube](https://connectycube.com/) communication platform.

## How to run:

1. Register new account and application at <https://admin.connectycube.com>

2. Put Application credentials from Overview page + Account key from <https://admin.connectycube.com/account/settings> to **app/src/main/java/com/connectycube/messenger/utilities/SettingsProvider.kt** class

3. Follow to <https://admin.connectycube.com> and create users in **Users** module. Then put to the **user_configiration.json** file at least 2 and max 5 users with format *[\"login\":{\"password\":userId}]*, for ex. *"userchatLogin1":{"userchatPassword1":310}*.

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

