package com.example.myapplication.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamNotInitializedException;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamServer;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TorrentStreamManager implements TorrentServerListener {
    private static final String TAG = "TorrentStreamManager";
    private static final String STREAM_CHANNEL_ID = "torrent_stream_channel";
    private static final int STREAM_NOTIFICATION_ID = 1001;

    private final Context context;
    private TorrentStreamServer torrentStreamServer;
    private TorrentStreamListener listener;

    public interface TorrentStreamListener {
        void onProgressUpdate(int progress);
        void onStreamReady();
        void onStreamError(String error);
    }

    public TorrentStreamManager(Context context, TorrentStreamListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        createNotificationChannel();
        initializeServer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    STREAM_CHANNEL_ID,
                    "Stream Status",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows streaming preparation progress");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void initializeServer() {
        System.setProperty("nanohttpd.mimetypes", "mimetypes.properties");

        TorrentOptions torrentOptions = new TorrentOptions.Builder()
                .saveLocation(context.getExternalCacheDir())
                .prepareSize(100L * 1024 * 1024)
                .removeFilesAfterStop(true)
                .build();

        torrentStreamServer = TorrentStreamServer.getInstance();
        torrentStreamServer.setTorrentOptions(torrentOptions);
        torrentStreamServer.setServerHost("0.0.0.0");
        torrentStreamServer.setServerPort(8080);
        torrentStreamServer.startTorrentStream();
        torrentStreamServer.addListener(this);
    }

    public void startStream(String magnetUrl) {
        try {
            torrentStreamServer.startStream(magnetUrl);
        } catch (IOException | TorrentStreamNotInitializedException e) {
            listener.onStreamError(e.getMessage());
            Log.e(TAG, "Stream start error", e);
        }
    }

    public void stopStream() {
        if (torrentStreamServer != null) {
            torrentStreamServer.stopStream();
            deleteFiles();
            cancelNotification();
        }
    }

    private void deleteFiles() {
        try {
            Torrent currentTorrent = torrentStreamServer.getCurrentTorrent();
            if (currentTorrent != null) {
                File videoFile = currentTorrent.getVideoFile();
                File torrentDir = videoFile.getParentFile();

                if (videoFile.exists() && !videoFile.delete()) {
                    Log.e(TAG, "Failed to delete video file");
                }

                if (torrentDir != null && torrentDir.exists() && !deleteRecursive(torrentDir)) {
                    Log.e(TAG, "Failed to delete torrent directory");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error", e);
        }
    }

    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        Log.d(TAG, "Stream prepared");
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        Log.d(TAG, "Stream started");
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        listener.onStreamError(e.getMessage());
        deleteFiles();
        showErrorNotification(e.getMessage());
    }

    private void showErrorNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STREAM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle("Stream Error")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(STREAM_NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        listener.onStreamReady();
        showStreamReadyNotification();
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        if (status.bufferProgress < 100) {
            int progress = (int) status.bufferProgress;
            Log.d(TAG, "Progress: " + progress + "%");
            listener.onProgressUpdate(progress);
            showStreamNotification(progress);
        }
    }

    private void showStreamNotification(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STREAM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stream)
                .setContentTitle("Preparing Stream")
                .setContentText("Buffering... " + progress + "%")
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(STREAM_NOTIFICATION_ID, builder.build());
    }

    private void showStreamReadyNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, STREAM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stream_ready)
                .setContentTitle("Stream Ready")
                .setContentText("Tap to open player")
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(STREAM_NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancel(STREAM_NOTIFICATION_ID);
    }

    @Override
    public void onStreamStopped() {
        deleteFiles();
        cancelNotification();
    }

    @Override
    public void onServerReady(String url) {
        Log.d(TAG, "onServerReady: " + url);
        try {
            Uri originalUri = Uri.parse(url);
            Uri streamUri = originalUri.buildUpon()
                    .scheme("http")
                    .encodedAuthority("127.0.0.1:" + originalUri.getPort())
                    .build();

            new Thread(() -> verifyServerConnection(streamUri)).start();
        } catch (Exception e) {
            listener.onStreamError(e.getMessage());
        }
    }

    private void verifyServerConnection(Uri streamUri) {
        try {
            URL testUrl = new URL(streamUri.toString());
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Thread.sleep(500);
                launchVlcPlayer(streamUri);
            } else {
                listener.onStreamError("Server error: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection verification failed", e);
            listener.onStreamError("Connection failed: " + e.getMessage());
        }
    }

    private void launchVlcPlayer(Uri streamUri) {
        try {
            Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
            vlcIntent.setDataAndType(streamUri, "video/*");
            vlcIntent.setPackage("org.videolan.vlc");
            vlcIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (vlcIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(vlcIntent);
            } else {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=org.videolan.vlc")));
            }
        } catch (Exception e) {
            listener.onStreamError("Player launch error: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (torrentStreamServer != null) {
            stopStream();
            torrentStreamServer.stopTorrentStream();
            torrentStreamServer = null;
        }
    }

    // Unused TorrentServerListener methods
    public void onServerReady(Torrent torrent) {}
}