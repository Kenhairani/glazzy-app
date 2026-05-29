package com.example.glazzy;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Kelas custom untuk menampung Drawable sementara (placeholder) saat gambar belum dimuat
public class CustomDrawablePlaceHolder extends Drawable {
    private Drawable drawable;

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (drawable != null) drawable.draw(canvas); // Gambar drawable jika sudah di-set
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {}
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSPARENT; }
}