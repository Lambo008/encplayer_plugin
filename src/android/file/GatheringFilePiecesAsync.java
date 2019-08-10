/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2019 Ramesh M Nair
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ramzi.chunkproject.file;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.ramzi.chunkproject.common.CipherCommon;
import com.ramzi.chunkproject.player.MediaFileCallback;
import com.ramzi.chunkproject.player.encryptionsource.EncryptedFileDataSourceFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

public class GatheringFilePiecesAsync extends AsyncTask<Void, Void, Void>  {

    ConcatenatingMediaSource mediaSource;
    File chunkFileDir;
    String filename;
    long videoLength;
    int filecount;
    Context context;
    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;
    private MediaFileCallback mediaFileCallback;
    String secretKey;

    public GatheringFilePiecesAsync(Context context, File chunkFileDir, MediaFileCallback mediaFileCallback, long totalVideotime, String secretKey )  {
        this.chunkFileDir = chunkFileDir;
        this.context = context;
        this.mediaFileCallback = mediaFileCallback;
        this.filename = this.chunkFileDir.getName();
        this.filecount = 0;
        this.videoLength = totalVideotime;
        this.secretKey=secretKey;
    }

    @Override
    protected Void doInBackground(Void... voids) {
            try {

                    /**
                     * Setting exoplayer with cipher data for real time decryption of files
                     * */
                    //secertKey="aabcde1234567890";
                    byte[] key = CipherCommon.PBKDF2(secretKey.toCharArray(), CipherCommon.salt);
                    mSecretKeySpec = new SecretKeySpec(key, CipherCommon.AES_ALGORITHM);
                    mIvParameterSpec = new IvParameterSpec(CipherCommon.iv);
                    filename="";
                    try {
                        mCipher = Cipher.getInstance(CipherCommon.AES_TRANSFORMATION);
                        mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                    DataSource.Factory dataSourceFactory = new EncryptedFileDataSourceFactory(mCipher, mSecretKeySpec, mIvParameterSpec, bandwidthMeter);
                    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                    MediaSource mediaSourcesToLoad ;

                    Uri uri = Uri.fromFile(chunkFileDir);
                        mediaSourcesToLoad = new ExtractorMediaSource.Factory(dataSourceFactory).setExtractorsFactory(extractorsFactory)
                                .createMediaSource(uri);
                    /**
                     * concatenating media files
                     * */
                    mediaSource = new ConcatenatingMediaSource(mediaSourcesToLoad);
            } catch (Exception e) {
                    e.printStackTrace();
            }

        return null;

    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mediaSource != null) {
            mediaFileCallback.onMediaFileRecieve(mediaSource, filename, videoLength, filecount);
        } else {
            mediaFileCallback.onMediaFileRecieve(false);

        }
    }
}
