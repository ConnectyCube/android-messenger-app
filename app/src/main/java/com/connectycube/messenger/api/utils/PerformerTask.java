package com.connectycube.messenger.api.utils;

import android.os.Bundle;

import com.connectycube.chat.ConnectycubeRestChatService;
import com.connectycube.chat.model.ConnectycubeChatDialog;
import com.connectycube.core.ConnectycubeProgressCallback;
import com.connectycube.core.EntityCallback;
import com.connectycube.core.exception.ResponseException;
import com.connectycube.core.server.Performer;
import com.connectycube.storage.ConnectycubeStorage;
import com.connectycube.storage.model.ConnectycubeFile;
import com.connectycube.users.ConnectycubeUsers;
import com.connectycube.users.model.ConnectycubeUser;

import java.io.File;

public class PerformerTask {

    public static Performer<ConnectycubeUser> uploadUserAvatarTask(final int userId, final File file, final boolean publicAccess, final ConnectycubeProgressCallback progressCallback) {
        return new PerformerImpl<ConnectycubeUser>() {
            @Override
            public void performAsync(final EntityCallback<ConnectycubeUser> entityCallback) {
                ConnectycubeStorage.uploadFileTask(file, publicAccess, progressCallback).performAsync(new EntityCallback<ConnectycubeFile>() {

                    @Override
                    public void onSuccess(ConnectycubeFile connectycubeFile, Bundle bundle) {
                        ConnectycubeUser user = new ConnectycubeUser(userId);
                        user.setAvatar(connectycubeFile.getPublicUrl());
                        ConnectycubeUsers.updateUser(user).performAsync(new EntityCallback<ConnectycubeUser>() {
                            @Override
                            public void onSuccess(ConnectycubeUser newUser, Bundle bundle) {
                                entityCallback.onSuccess(newUser, bundle);
                            }

                            @Override
                            public void onError(ResponseException e) {
                                entityCallback.onError(e);
                            }
                        });
                    }

                    @Override
                    public void onError(ResponseException e) {
                        entityCallback.onError(e);
                    }
                });
            }
        };
    }

    public static Performer<ConnectycubeChatDialog> uploadDialogPhotoTask(final String dialogId, final File file, final boolean publicAccess, final ConnectycubeProgressCallback progressCallback) {
        return new PerformerImpl<ConnectycubeChatDialog>() {
            @Override
            public void performAsync(final EntityCallback<ConnectycubeChatDialog> entityCallback) {
                ConnectycubeStorage.uploadFileTask(file, publicAccess, progressCallback).performAsync(new EntityCallback<ConnectycubeFile>() {

                    @Override
                    public void onSuccess(ConnectycubeFile connectycubeFile, Bundle bundle) {
                        ConnectycubeChatDialog dialog = new ConnectycubeChatDialog(dialogId);
                        dialog.setPhoto(connectycubeFile.getPublicUrl());
                        ConnectycubeRestChatService.updateChatDialog(dialog, null).performAsync(new EntityCallback<ConnectycubeChatDialog>() {
                            @Override
                            public void onSuccess(ConnectycubeChatDialog newDialog, Bundle bundle) {
                                entityCallback.onSuccess(newDialog, bundle);
                            }

                            @Override
                            public void onError(ResponseException e) {
                                entityCallback.onError(e);
                            }
                        });
                    }

                    @Override
                    public void onError(ResponseException e) {
                        entityCallback.onError(e);
                    }
                });
            }
        };
    }
}