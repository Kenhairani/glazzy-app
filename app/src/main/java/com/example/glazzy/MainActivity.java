package com.example.glazzy;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    HomeFragment homeFragment                   = new HomeFragment();
    BookmarkFragment bookmarkFragment           = new BookmarkFragment();
    HistoryFragment historyFragment             = new HistoryFragment();
    AboutFragment aboutFragment                 = new AboutFragment();
    FragmentNotifications notificationsFragment = new FragmentNotifications();
    Fragment activeFragment;
    FragmentManager fm;
    BottomNavigationView bottomNav;
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fm             = getSupportFragmentManager();
        activeFragment = homeFragment;

        // Daftarkan semua fragment ke container
        fm.beginTransaction()
                .add(R.id.fragmentContainer, aboutFragment).hide(aboutFragment)
                .add(R.id.fragmentContainer, notificationsFragment).hide(notificationsFragment)
                .add(R.id.fragmentContainer, historyFragment).hide(historyFragment)
                .add(R.id.fragmentContainer, bookmarkFragment).hide(bookmarkFragment)
                .add(R.id.fragmentContainer, homeFragment)
                .commit();

        // Delay 3 detik, lalu bersihkan artikel yang tidak valid
        new Handler().postDelayed(() ->
                ArticleValidator.validateAndClean(this, null), 3000);

        // Listener untuk perpindahan tab bawah
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(homeFragment);
            } else if (id == R.id.nav_bookmark) {
                switchFragment(bookmarkFragment);
            } else if (id == R.id.nav_history) {
                switchFragment(historyFragment);
            }
            return true;
        });

        // Logika tombol kembali:
        // • Di notif/about  → tutup fragment tsb
        // • Di selain home  → kembali ke home
        // • Di home         → tampilkan dialog konfirmasi keluar
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeFragment == notificationsFragment) {
                    closeNotifications();
                    return;
                }
                if (activeFragment == aboutFragment) {
                    closeAbout();
                    return;
                }
                if (activeFragment != homeFragment) {
                    switchFragment(homeFragment);
                    bottomNav.setSelectedItemId(R.id.nav_home);
                    return;
                }
                // Tampilkan dialog "Yakin mau keluar?"
                Dialog exitDialog = new Dialog(MainActivity.this);
                exitDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                exitDialog.setContentView(R.layout.dialog_exit);
                if (exitDialog.getWindow() != null) {
                    exitDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    exitDialog.getWindow().setLayout(
                            (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                }
                exitDialog.findViewById(R.id.btnExitConfirm).setOnClickListener(v -> finish());
                exitDialog.findViewById(R.id.btnExitCancel).setOnClickListener(v -> exitDialog.dismiss());
                exitDialog.show();
            }
        });

        // Mulai pengecekan otomatis tiap 15 detik
        startPeriodicCheck();
    }

    // Jalankan checkNewPosts() tiap 15 detik (mulai setelah 5 detik)
    void startPeriodicCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (homeFragment != null && homeFragment.isAdded()) {
                    homeFragment.checkNewPosts();
                }
                handler.postDelayed(this, 15 * 1000); // tiap 15 detik
            }
        }, 5000); // mulai 5 detik setelah app buka
    }

    // Pindah antar fragment dengan animasi fade
    void switchFragment(Fragment fragment) {
        fm.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
    }

    // Buka halaman About (sembunyikan checklist nav bawah)
    public void openAbout() {
        switchFragment(aboutFragment);
        bottomNav.getMenu().setGroupCheckable(0, false, true);
    }

    // Tutup About, kembali ke Home
    public void closeAbout() {
        switchFragment(homeFragment);
        bottomNav.getMenu().setGroupCheckable(0, true, true);
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    // Buka notifikasi:
    // • Sembunyikan bottom nav
    // • Validasi artikel dulu, lalu load notifikasi
    public void openNotifications() {
        bottomNav.setVisibility(View.GONE);
        switchFragment(notificationsFragment);
        bottomNav.getMenu().setGroupCheckable(0, false, true);

        ArticleValidator.validateAndClean(this, () ->
                runOnUiThread(() -> notificationsFragment.loadNotifications())
        );
    }

    // Tutup notifikasi:
    // • Tandai sudah dibaca
    // • Tampilkan kembali bottom nav
    // • Update badge notifikasi di HomeFragment
    public void closeNotifications() {
        // Tandai sudah dibaca
        NotificationHelper.markAsRead(this);

        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.getMenu().setGroupCheckable(0, true, true);
        bottomNav.setSelectedItemId(R.id.nav_home);
        switchFragment(homeFragment);

        // Update badge setelah fragment switch selesai
        handler.postDelayed(this::updateNotifBadge, 100);
    }

    // Toggle visibility bottom nav dari fragment lain
    public void setBottomNavVisible(boolean visible) {
        bottomNav.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // Update badge notifikasi di HomeFragment (jika sudah attached)
    public void updateNotifBadge() {
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            homeFragment.updateBadge();
        }
    }

    // Bersihkan semua handler saat activity dihancurkan (hindari memory leak)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // polling berhenti
    }
}