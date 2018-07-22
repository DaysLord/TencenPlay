package com.tencentplay.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.tencentplay.demo.player.SuperPlayerActivity;
/**
 * Title:
 * Description:
 * Copyright:Copyright(c)2018
 * Company:
 *
 * @author: SweatLau
 * @date: 2018/7/21 00:56
 */
public class MainActivity extends AppCompatActivity {
    boolean iswatch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startActivity(new Intent(MainActivity.this,SuperPlayerActivity.class));
                iswatch = true;
            }
        }).start();
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(iswatch){
                    startActivity(new Intent(MainActivity.this,SuperPlayerActivity.class));
                }
            }
        });
    }
}
