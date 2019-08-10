/*
 The MIT License (MIT)

 Copyright (c) 2017 Nedim Cholich

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package com.ramzi.chunkproject;

import org.apache.cordova.PermissionHelper;

import android.Manifest;
import android.content.Intent;
import android.net.*;
import android.view.ViewGroup;
import org.apache.cordova.*;
import org.json.*;

public class EncPlayerPlugin extends CordovaPlugin {
    private static final int PLAY_RESULT_ID = 11110;     // Constant for player result


    // private Player player;

    @Override
    public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) throws JSONException {
        try {
            final EncPlayerPlugin self = this;
            
            if (action.equals("startPlayer")) {
                
                boolean needExternalStoragePermission =
                        !PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

                boolean needExternalWStoragePermission =
                        !PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (needExternalStoragePermission || needExternalWStoragePermission) {
                    if(needExternalWStoragePermission && needExternalStoragePermission ){
                        PermissionHelper.requestPermissions(this, PLAY_RESULT_ID, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    } else if (needExternalStoragePermission) {
                        PermissionHelper.requestPermission(this, PLAY_RESULT_ID, Manifest.permission.READ_EXTERNAL_STORAGE);
                    } else {
                        PermissionHelper.requestPermission(this, PLAY_RESULT_ID, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            
                            Intent intent = new Intent(self.cordova.getActivity(), ChunkMainActivity.class);
                            self.cordova.startActivityForResult(self, intent, PLAY_RESULT_ID);

                            new CallbackResponse(callbackContext).send(PluginResult.Status.NO_RESULT, true);
                        }
                    });
                }
                return true;
            }

            else if (action.equals("show")) {
                // cordova.getActivity().runOnUiThread(new Runnable() {
                //     public void run() {
                //         if (self.player != null) {
                //             self.player.close();
                //         }
                //         JSONObject params = data.optJSONObject(0);
                //         self.player = new Player(new Configuration(params), cordova.getActivity(), callbackContext, webView);
                //         self.player.createPlayer();
                //         new CallbackResponse(callbackContext).send(PluginResult.Status.NO_RESULT, true);
                //     }
                // });
                return true;
            }
            else {
                new CallbackResponse(callbackContext).send(PluginResult.Status.INVALID_ACTION, false);
                return false;
            }
        }
        catch (Exception e) {
            new CallbackResponse(callbackContext).send(PluginResult.Status.JSON_EXCEPTION, false);
            return false;
        }
    }
}
