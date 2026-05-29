# Glazzy - Eyewear Review Android Application

Glazzy adalah aplikasi Android native yang dirancang khusus sebagai platform katalog, review, dan rekomendasi kacamata. Aplikasi ini dibangun menggunakan Android Studio dengan bahasa pemrograman Java dan desain antarmuka berbasis XML Layout. Data artikel pada aplikasi ini diambil secara dinamis via REST API yang terintegrasi langsung dengan platform utama kami di glazzy.web.id.


## 🚀 Fitur Utama

Aplikasi Glazzy dilengkapi dengan berbagai fitur interaktif untuk memudahkan pengguna:
*   **Splash Screen** – Halaman pembuka (onboarding singkat) saat aplikasi pertama kali dijalankan.
*   **Home & Kategori Dinamis** – Menampilkan daftar artikel seputar kacamata yang dilengkapi dengan filter kategori interaktif (bisa digeser/diklik) untuk menyaring artikel sesuai kebutuhan.
*   **Fitur Search** – Memudahkan pengguna menemukan artikel kacamata spesifik secara cepat di halaman Home.
*   **Sistem Notifikasi** – Memberikan pemberitahuan secara *real-time* kepada pengguna apabila terdapat artikel baru yang dipublikasikan.
*   **Bookmark** – Menyimpan artikel favorit ke penyimpanan lokal agar bisa dibaca kembali di lain waktu tanpa kehilangan data.
*   **Riwayat (History)** – Mencatat riwayat artikel terakhir yang telah selesai dibaca oleh pengguna.
*   **Fitur Share** – Memudahkan pengguna untuk membagikan tautan artikel kacamata kepada orang lain melalui aplikasi pihak ketiga.
*   **Halaman About Glazzy** – Informasi mengenai aplikasi yang dapat diakses secara unik hanya dengan menekan logo Glazzy di halaman Home.


## 🛠️ Tech Stack & Komponen Android

Aplikasi ini mengimplementasikan konsep-konsep fundamental dan lanjutan dalam pengembangan aplikasi Android, meliputi:

*   **Bahasa Pemrograman:** Java
*   **User Interface (UI):** XML Layout, Vector Asset
*   **Arsitektur & Navigasi:** Fragment, Navigation Component (Single Activity Architecture)
*   **Koneksi Jaringan:** Pengambilan data artikel secara dinamis dari REST API
*   **Penyimpanan Lokal:** Manajemen data lokal (Local Storage) untuk menangani fitur Bookmark dan Riwayat membaca pengguna.


## ℹ️ Catatan Arsitektur & Aksesibilitas Data

Aplikasi Glazzy awalnya dirancang untuk mengonsumsi data artikel secara *real-time* dari REST API platform utama di **glazzy.web.id**. Namun, untuk menjamin keberlanjutan portofolio ini dalam jangka panjang (antisipasi jika masa aktif *hosting* web utama berakhir), sistem telah dilengkapi dengan mekanisme **Local Fallback Data**.

Jika koneksi ke server REST API mengalami kendala atau *offline*, aplikasi secara otomatis akan mengalihkan sumber data ke penyimpanan cadangan lokal (`assets/artikel_backup.json`). Hal ini memastikan seluruh komponen UI seperti halaman Home, filter kategori, dan fitur Search tetap dapat diuji dan berfungsi dengan normal selamanya.


## 📸 Tampilan Aplikasi (Screenshots)

<p align="center">
  <img src="glazzy-showcase.jpg" width="800" alt="Glazzy Application Showcase">
</p>


## 👨‍💻 Kontributor

Proyek ini dikembangkan sebagai Project Akhir oleh:
*   **Kenhairani Baeha**
*   **T. Syarifah Gita Azzahra**
