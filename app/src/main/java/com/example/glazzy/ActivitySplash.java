package com.example.glazzy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class ActivitySplash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logo = findViewById(R.id.splashLogo);
        TextView tagline = findViewById(R.id.splashTagline);
        ProgressBar progress = findViewById(R.id.splashProgress);

        // Animasi logo dari bawah + fade in
        logo.setTranslationY(100f);
        logo.setAlpha(0f);
        logo.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(200)
                .start();

        // Animasi tagline dari bawah + fade in
        tagline.setTranslationY(60f);
        tagline.setAlpha(0f);
        tagline.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(600)
                .start();

        // Animasi progress fade in lalu pindah ke MainActivity
        progress.setAlpha(0f);
        progress.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(900)
                .withEndAction(() ->
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(ActivitySplash.this, MainActivity.class);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }, 1200)
                ).start();
    }
}