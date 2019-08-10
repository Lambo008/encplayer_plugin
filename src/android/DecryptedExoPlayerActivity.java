package com.ramzi.chunkproject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.*;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.IllegalSeekPositionException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.ramzi.chunkproject.file.GatheringFilePiecesAsync;
import com.ramzi.chunkproject.player.MediaFileCallback;
import com.ramzi.chunkproject.player.animation.PlayIconDrawable;
import com.ramzi.chunkproject.player.gestures.GestureListener;
import com.ramzi.chunkproject.player.utils.AudioReactor;
import com.ramzi.chunkproject.utils.AppPreferences;
import com.ramzi.chunkproject.utils.Constants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import static com.ramzi.chunkproject.player.animation.AnimationUtils.Type.SCALE_AND_ALPHA;
import static com.ramzi.chunkproject.player.animation.AnimationUtils.animateView;

public class DecryptedExoPlayerActivity extends AppCompatActivity implements Player.EventListener, MediaFileCallback {
    SimpleExoPlayer player;
    private PlayerView mExoPlayerView;
    ConstraintLayout mainContainer;
    AppCompatSeekBar seekBar;
    TextView timeText, seekStatus;
    ImageView playPauseImageView;
    PlayIconDrawable play;
    RelativeLayout playerControlLayer;
    LinearLayout toolbarLayer;
    ProgressBar loadingProgressbar;
    long totalLength;
    boolean isPlayer = false;
    boolean isSeeking = false;
    String totalTimeStamp;
    int screenWidth;
    int screenHeigth;
    boolean gestureSeek = false;
    int gestureSeekIndex = 0;
    long gestureSeekPosition = 0;
    private boolean isresume = false;
    ConcatenatingMediaSource mediaMergeSource;
    RelativeLayout brView, volumeView;
    ImageView brIV;
    ProgressBar brPG;
    ProgressBar vPG;
    ImageView vIV;
    int maxGestureLength;
    public static String TAG = "chunk";
    AppPreferences playerPrefrence;
    AudioReactor audioReactor;
    int maxVolume = 0;
    final Handler hideControl = new Handler();
    ProgressDialog progress;
    public static final int DEFAULT_CONTROLS_DURATION = 300; // 300 millis
    private final float MAX_GESTURE_LENGTH = 0.75f;
    int pauseIndex;
    long pausePosition;
    String secretKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            InputStream is = getAssets().open("configStartKey.cfg");
            int size = is.available();

            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Convert the buffer into a string.
            secretKey = new String(buffer);
            Log.d("text", secretKey);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        playerPrefrence = new AppPreferences(getApplicationContext(), Constants.BASE_PREF);
        if (playerPrefrence.getFloat(Constants.BRIGHTNESS_FLOAT_PREF) != -100f) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = playerPrefrence.getFloat(Constants.BRIGHTNESS_FLOAT_PREF);
            getWindow().setAttributes(lp);
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        hideSystemUi();
        setContentView(getResources().getIdentifier("exoplayer_activity", "layout", getPackageName()));
        seekBar = findViewById(getResources().getIdentifier("seek", "id", getPackageName()));
        timeText = findViewById(getResources().getIdentifier("time_text", "id", getPackageName()));
        seekStatus = findViewById(getResources().getIdentifier("seek_ststus", "id", getPackageName()));
        mExoPlayerView = findViewById(getResources().getIdentifier("exoplayer", "id", getPackageName()));
        playerControlLayer = findViewById(getResources().getIdentifier("player_control", "id", getPackageName()));
        playPauseImageView = findViewById(getResources().getIdentifier("pause_play_button", "id", getPackageName()));
        mainContainer = findViewById(getResources().getIdentifier("main_container", "id", getPackageName()));
        brView = findViewById(getResources().getIdentifier("brightnessRelativeLayout", "id", getPackageName()));
        brIV = findViewById(getResources().getIdentifier("brightnessImageView", "id", getPackageName()));
        brPG = findViewById(getResources().getIdentifier("brightnessProgressBar", "id", getPackageName()));
        volumeView = findViewById(getResources().getIdentifier("volumeRelativeLayout", "id", getPackageName()));
        vPG = findViewById(getResources().getIdentifier("volumeProgressBar", "id", getPackageName()));
        vIV = findViewById(getResources().getIdentifier("volumeImageView", "id", getPackageName()));
        toolbarLayer = findViewById(getResources().getIdentifier("toolbarLayer", "id", getPackageName()));
        loadingProgressbar = findViewById(getResources().getIdentifier("progressBar_cyclic", "id", getPackageName()));
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isSeeking = true;
                    seekToPart(progress);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                isSeeking = false;
                if (hideControl != null) {
                    callDelay();
                }
            }

        });
        setUpGestureControls();
        initExoPlayer();

        mExoPlayerView.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            if (l != ol || t != ot || r != or || b != ob) {
                int width = r - l, height = b - t;
                maxGestureLength = (int) (Math.min(width, height) * MAX_GESTURE_LENGTH);
                brPG.setMax(maxGestureLength);
                vPG.setMax(maxGestureLength);


                setInitialGestureValues();
            }
        });

        File chunkFileDirectory = new File(getIntent().getExtras().getString("file_dir"));
        String videofilename=chunkFileDirectory.getName();
        String[] splitString = videofilename.split("video");
        int length = splitString.length;
        String timeline = splitString[length - 1].replace(".enc", "");
        long videototaltime=Long.parseLong(timeline);
        progress = new ProgressDialog(this);
        progress.setMessage("Gathering file please wait....");
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);
        progress.show();
        new GatheringFilePiecesAsync(getApplicationContext(), chunkFileDirectory, DecryptedExoPlayerActivity.this, videototaltime, secretKey).execute();
        play = PlayIconDrawable.builder()
                .withColor(Color.WHITE)
                .withInterpolator(new FastOutSlowInInterpolator())
                .withDuration(300)
                .withInitialState(PlayIconDrawable.IconState.PAUSE)
                .withAnimatorListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        Log.d("Animation", "animationFinished");
                    }
                })
                .withStateListener(new PlayIconDrawable.StateListener() {
                    @Override
                    public void onStateChanged(PlayIconDrawable.IconState state) {
                        Log.d("IconState", "onStateChanged: " + state);
                    }
                })
                .into(playPauseImageView);

        playPauseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (play.getIconState() == PlayIconDrawable.IconState.PAUSE) {
                    pausePlayer();
                } else if (play.getIconState() == PlayIconDrawable.IconState.PLAY) {
                    startPlayer();
                }
            }
        });
    }

    private void initExoPlayer() {
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        audioReactor = new AudioReactor(getApplicationContext(), player);
        maxVolume = audioReactor.getMaxVolume();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isresume) {
            // isresume=false;
            player.stop();
            player.clearVideoSurface();
            // isInPlayer = false;
            initExoPlayer();
            loadPlayer();
        } else {
            loadPlayer();
        }
    }

    private void setUpGestureControls() {
        mExoPlayerView.setOnTouchListener(new ExVidPlayerGestureListener(DecryptedExoPlayerActivity.this, mExoPlayerView));
        mExoPlayerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mExoPlayerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mExoPlayerView.getHeight(); //height is ready
                screenWidth = mExoPlayerView.getWidth();
                screenHeigth = mExoPlayerView.getHeight();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isresume = true;
        pauseIndex = player.getCurrentPeriodIndex();
        pausePosition = player.getCurrentPosition();
        player.stop();
        player.release();
    }

    @Override
    public void onMediaFileRecieve(ConcatenatingMediaSource mediaSource, String filename, long totalTime, int totalIndex) {
        if (progress != null && !isFinishing()) {
            progress.dismiss();
        }
        if (mediaSource != null) {
            if (mExoPlayerView != null && player != null) {
                totalLength = totalTime;
                totalTimeStamp = String.format("%02d", ((totalLength / 1000) / 60) / 60) + ":" + String.format("%02d", ((totalLength / 1000) / 60) % 60) + ":" + String.format("%02d", (totalLength / 1000) % 60);
                mediaMergeSource = mediaSource;
                loadPlayer();
                if (hideControl != null) {
                    callDelay();
                }
            }
        }

    }

    private void callDelay() {
        hideControl.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isSeeking) {
                    hideControl();
                    hideSystemUi();
                }
            }
        }, 2000);
    }

    @Override
    public void onMediaFileRecieve(boolean status) {
        if (progress != null && !isFinishing()) {
            progress.dismiss();
        }
        Toast.makeText(getApplicationContext(), "Gathering files failed", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void seekToTime() {
        int index = player.getCurrentWindowIndex();
        long currentIndexSeekValue = 0;
        if (index == 0) {
            currentIndexSeekValue = player.getContentPosition();
        } else {
            currentIndexSeekValue = ((6000000 * index) + player.getContentPosition());
        }
        changeTimeTextView(currentIndexSeekValue);
    }

    public void seekToPart(int currentProgress) {
        seekToTime();
        long currentSeekingTime = (long) (currentProgress / 100.0f * totalLength);
        int index = (int) Math.floor(currentSeekingTime / 6000000);
        long currentIndexSeekValue = 0;
        if (index == 0) {
            currentIndexSeekValue = currentSeekingTime;
        } else {
            currentIndexSeekValue = currentSeekingTime - (6000000 * index);
        }
        Log.d("Value>>>>", currentProgress + " datatata \nTotalTime :" + totalLength + "\n Current Time : "
                + currentSeekingTime + "\n index:" + index + "-----" + "indexseek " + currentIndexSeekValue);
        try {
            player.seekTo(index, currentIndexSeekValue);
        } catch (IllegalSeekPositionException e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (player != null) {
                player.setPlayWhenReady(false);
                player.stop();
            }
            if (mHandler != null) {
                mHandler.removeCallbacks(updateProgressAction);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d("Ststs", playbackState + ">>>");
        isPlayer = playWhenReady;
        if (playWhenReady && playbackState == Player.STATE_READY) {
            mHandler = new Handler();
            mHandler.post(updateProgressAction);
            if (loadingProgressbar != null) {
                loadingProgressbar.setVisibility(View.GONE);
            }
        } else if (playbackState == Player.STATE_ENDED) {
            if (loadingProgressbar != null) {
                loadingProgressbar.setVisibility(View.GONE);
            }
            if (play != null) {
                play.animateToState(PlayIconDrawable.IconState.PLAY);
            }
            try {
                player.seekTo(0, 0);
            } catch (IllegalSeekPositionException e) {

            }
            player.setPlayWhenReady(false);

            seekBar.setProgress(0);
            if (mHandler != null) {

                mHandler.removeCallbacks(updateProgressAction);
            }
        } else if (playbackState == Player.STATE_BUFFERING) {
            if (loadingProgressbar != null) {
                loadingProgressbar.setVisibility(View.VISIBLE);
            }
        }
        else if (playbackState == Player.STATE_READY) {
            if (loadingProgressbar != null) {
                loadingProgressbar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
        }
    }

    private Handler mHandler;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            Log.d("getcurrent", "lalu out>>" + player.getCurrentPosition());
            updateProgress();
        }
    };

    public void updateProgress() {
        seekToTime();
        if (isPlayer) {
            long delayMs = TimeUnit.SECONDS.toMillis(1);
            mHandler.postDelayed(updateProgressAction, delayMs);
            if (!isSeeking) {
                if (player.getCurrentWindowIndex() == 0) {
                    seekBar.setProgress((int) (player.getCurrentPosition() * 1.0f / totalLength * 100));
                } else {
                    long currentposition = player.getCurrentPosition() + (6000000 * player.getCurrentWindowIndex());
                    seekBar.setProgress((int) (currentposition * 1.0f / totalLength * 100));
                }
            }
        }
    }

    public void changeTimeTextView(long milliSec) {
        timeText.setText(String.format("%02d", ((milliSec / 1000) / 60) / 60) + ":" + String.format("%02d", ((milliSec / 1000) / 60) % 60) + ":" + String.format("%02d", (milliSec / 1000) % 60) + "/" + totalTimeStamp);
    }

    int currentIndex = 0;

    public void back(View view) {
        finish();
    }

    private class ExVidPlayerGestureListener extends GestureListener {
        ExVidPlayerGestureListener(Context ctx, View rootview) {
            super(ctx, rootview);
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (gestureSeek) {
                seekStatus.setVisibility(View.GONE);
                if (motionEvent.getAction() == 0) {
                    Log.d(TAG, "yes got new index" + currentIndex);
                } else if (motionEvent.getAction() == 1) {
                    if (player != null) {
                        try {
                            Log.d(TAG, gestureSeekPosition + "yes got ne2222w index" + gestureSeekIndex);

                            player.seekTo(gestureSeekIndex, gestureSeekPosition);
                        } catch (Exception e) {
                            Log.d(TAG, "yes error");
                        }
                    }
                    play.animateToState(PlayIconDrawable.IconState.PAUSE);
                    player.setPlayWhenReady(true);
                    gestureSeek = false;
                }
            }
            return super.onTouch(view, motionEvent);
        }

        @Override
        public void onTap() {
            if (playerControlLayer.getVisibility() == View.VISIBLE) {
                hideControl();
                hideSystemUi();
                try {
                    hideControl.removeCallbacksAndMessages(null);
                } catch (Exception e) {
                }
            } else {
                showControl();
                showSystemUi();
                callDelay();
            }
            showControl();
        }

        @Override
        public void onHorizontalScroll(MotionEvent event, float delta) {
            seekStatus.setVisibility(View.VISIBLE);
            gestureSeek = true;
            currentIndex = player.getCurrentWindowIndex();
            pausePlayer();
            Log.d(TAG, screenWidth + "swiping horizontaly" + delta + ">>>>motion" + event.getAction());
            long perscreen = 6000000 / screenWidth;
            if (delta * perscreen < 0) {
                seekStatus.setText("-" + TimeUnit.MILLISECONDS.toSeconds((long) (0 - (delta * perscreen))));
                Log.d(TAG, delta * perscreen + "Scroll to incrise= Minus" + (6000000 + (delta * perscreen)));
                seekToGesture((long) (0 - (delta/100 * perscreen)), false);
            } else {
                Log.d(TAG, perscreen + "Scroll to incrise= Adding " + delta * perscreen);
                seekStatus.setText("+" + TimeUnit.MILLISECONDS.toSeconds((long) (delta * perscreen)));
                seekToGesture((long) (delta/100 * perscreen), true);
            }
        }

        private void seekToGesture(long seekValue, boolean forward) {
            Log.d(TAG, "/////////////////////////////////////start////////////////////////////");
            long currentPosition;
            if (currentIndex == 0) {
                currentPosition = player.getCurrentPosition();
            } else {
                currentPosition = player.getCurrentPosition() + (6000000 * currentIndex);
            }
            if (forward) {
                currentPosition = currentPosition + seekValue;
            } else {
                currentPosition = currentPosition - seekValue;
            }
            int index = (int) Math.floor(currentPosition / 6000000);
            long newIndexSeekValue;
            if (index == 0) {
                newIndexSeekValue = currentPosition;
            } else {
                newIndexSeekValue = currentPosition - (6000000 * index);
            }
            gestureSeekIndex = index;
            gestureSeekPosition = newIndexSeekValue;
        }

        @Override
        public void onSwipeRight() {
            Log.d(TAG, "Swipe right");
        }

        @Override
        public void onSwipeLeft() {
            Log.d(TAG, "Swipe left");
        }

        @Override
        public void onSwipeBottom() {
            Log.d(TAG, "Swipe left");
        }

        @Override
        public void onSwipeTop() {
            Log.d(TAG, "Swipe left");
        }

        @Override
        public void brightness(int value) {
            Log.d("Brigthnesss", value + ">>>");
            if (!gestureSeek) {
                brPG.incrementProgressBy(value);
                float currentProgressPercent =
                        (float) brPG.getProgress() / maxGestureLength;
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = currentProgressPercent;
                getWindow().setAttributes(layoutParams);
                if (playerPrefrence != null) {
                    playerPrefrence.saveFloatData(Constants.BRIGHTNESS_FLOAT_PREF, currentProgressPercent);
                }

                final int resId =
                        currentProgressPercent < 0.25 ? getResources().getIdentifier("ic_brightness_low_white_72dp", "drawable", getPackageName())
                                : currentProgressPercent < 0.75 ? getResources().getIdentifier("ic_brightness_medium_white_72dp", "drawable", getPackageName())
                                : getResources().getIdentifier("ic_brightness_high_white_72dp", "drawable", getPackageName());
                brIV.setImageDrawable(
                        AppCompatResources.getDrawable(getApplicationContext(), resId)
                );
                if (brView.getVisibility() != View.VISIBLE) {
                    animateView(brView, SCALE_AND_ALPHA, true, 200);
                }
            }
        }

        @Override
        public void volume(int value) {
            if (!gestureSeek) {
                vPG.incrementProgressBy(value);
                float currentProgressPercent =
                        (float) vPG.getProgress() / maxGestureLength;
                int currentVolume = (int) (maxVolume * currentProgressPercent);
                if (audioReactor != null) {
                    Log.d(TAG, currentVolume + ">>>>chappa" + maxVolume);
                    audioReactor.setVolume(currentVolume);
                }
                final int resId =
                        currentProgressPercent <= 0 ? getResources().getIdentifier("ic_volume_off_white_72dp", "drawable", getPackageName())
                                : currentProgressPercent < 0.25 ? getResources().getIdentifier("ic_volume_mute_white_72dp", "drawable", getPackageName())
                                : currentProgressPercent < 0.75 ? getResources().getIdentifier("ic_volume_down_white_72dp", "drawable", getPackageName())
                                : getResources().getIdentifier("ic_volume_up_white_72dp", "drawable", getPackageName());
                vIV.setImageDrawable(
                        AppCompatResources.getDrawable(getApplicationContext(), resId)
                );

                if (volumeView.getVisibility() != View.VISIBLE) {
                    animateView(volumeView, SCALE_AND_ALPHA, true, 200);
                }
                if (brView.getVisibility() == View.VISIBLE) {
                    brView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onScrollEnd() {
            onScrollOver();
        }
    }

    private void showSystemUi() {
        final int visibility;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            visibility = View.STATUS_BAR_VISIBLE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorInt final int systenUiColor =
                    ActivityCompat.getColor(getApplicationContext(), getResources().getIdentifier("video_overlay_color", "color", getPackageName()));
            getWindow().setStatusBarColor(systenUiColor);
            getWindow().setNavigationBarColor(systenUiColor);
        }
        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /*Hide the system ui*/
    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            getWindow().getDecorView().setSystemUiVisibility(visibility);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /*Pause the player*/
    private void pausePlayer() {
        if (player != null) {
            if (play != null) {
                play.animateToState(PlayIconDrawable.IconState.PLAY);
            }
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    /*Start the player*/
    private void startPlayer() {
        if (player != null) {
            if (play != null) {
                play.animateToState(PlayIconDrawable.IconState.PAUSE);
            }
            player.setPlayWhenReady(true);
            player.getPlaybackState();
        }
    }

    /*Load the exo player*/
    private void loadPlayer() {
        if (mediaMergeSource != null) {
            player.addListener(this);
            player.prepare(mediaMergeSource);
            mExoPlayerView.setUseController(false);
            mExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(true);
            play.animateToState(PlayIconDrawable.IconState.PAUSE);
            mExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

            if (isresume) {
                try {

                    player.seekTo(pauseIndex, pausePosition);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                isresume = false;
            }
        }
    }

    /*Hide the volume and brightness view after in active touch*/
    private void onScrollOver() {
        if (volumeView.getVisibility() == View.VISIBLE) {
            animateView(volumeView, SCALE_AND_ALPHA, false, 200, 200);
        }
        if (brView.getVisibility() == View.VISIBLE) {
            animateView(brView, SCALE_AND_ALPHA, false, 200, 200);
        }
    }

    /*Initiate the gesture value of volume and brightness if player have history*/
    private void setInitialGestureValues() {
        if (audioReactor != null) {
            final float currentVolumeNormalized = (float) audioReactor.getVolume() / audioReactor.getMaxVolume();
            vPG.setProgress((int) (vPG.getMax() * currentVolumeNormalized));
        }
        if (playerPrefrence != null) {
            if (playerPrefrence.getFloat(Constants.BRIGHTNESS_FLOAT_PREF) != -100f) {
                brPG.setProgress((int) (brPG.getMax() * playerPrefrence.getFloat(Constants.BRIGHTNESS_FLOAT_PREF)));
            }
        }
    }

    /*Animation to hide the show views*/
    public void showControl() {
        if (playerControlLayer != null && toolbarLayer != null) {
            animateView(playerControlLayer, true, DEFAULT_CONTROLS_DURATION);
            animateView(toolbarLayer, true, DEFAULT_CONTROLS_DURATION);
        }
    }

    /*Animation to hide the control views*/
    public void hideControl() {
        if (playerControlLayer != null && toolbarLayer != null) {
            animateView(playerControlLayer, false, DEFAULT_CONTROLS_DURATION, 0);
            animateView(toolbarLayer, false, DEFAULT_CONTROLS_DURATION, 0);
        }
    }
}
