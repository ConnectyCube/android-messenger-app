# android-messenger-app
A messenger app for Android

## Steps to use:

1. Register new account and application at <https://admin.connectycube.com>

2. Put Application credentials from Overview page + Account key from <https://admin.connectycube.com/account/settings> to **SettingsProvider** class

3. Follow to <https://admin.connectycube.com> and create users in **Users** module. Then put to the **user_config.json** file at least 2 and max 5 users with format *[\"login\":{\"password\":userId}]*, for ex. *"userchatLogin1":{"userchatPassword1":310}*.

4. Run project.