package com.linglong.videocode;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class PlayVideoActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {

    public static final String PATH = "Path";

    private SurfaceView surface;
    private Button button;
    private SurfaceHolder holder;
    private String path = "";
    private VideoDecoder decoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        surface = findViewById(R.id.surface);
        button = findViewById(R.id.btn);
        button.setText("播放");
        Intent intent = getIntent();
        if (intent!=null){
            path = intent.getStringExtra(PATH);
        }
        button.setOnClickListener(this);
        decoder = new VideoDecoder();
        holder = surface.getHolder();
        holder.addCallback(this);
    }

    @Override
    public void onClick(View v) {
        decoder.start(path,holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        decoder.stop();
    }
}
