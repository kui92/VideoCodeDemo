package com.linglong.videocode;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.yanzhenjie.permission.AndPermission;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void click(View view){
        switch (view.getId()){
            case R.id.btnPermission:
                checkCameraPermation();
                break;
            case R.id.btnCamera:
                startActivity(new Intent(this,CameraActivity.class));
                break;
        }
    }


    private void checkCameraPermation(){
        String [] permissions = {Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO};
        AndPermission.with(this)
                .runtime()
                .permission(permissions)
                .start();
    }


}
