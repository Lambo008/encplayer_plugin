package com.ramzi.chunkproject.player;

import com.google.android.exoplayer2.source.ConcatenatingMediaSource;

public interface MediaFileCallback {

     void onMediaFileRecieve(ConcatenatingMediaSource mediaSource,String filename,long totalTIme,int totalIndex);
     void onMediaFileRecieve(boolean status);

}
