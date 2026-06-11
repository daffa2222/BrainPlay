# BrainPlay
### Tugas Final Lab Mobile 2026 - Aplikasi Game Kuis BrainPlay

---

## 📱 Deskripsi Aplikasi
BrainPlay adalah aplikasi Android bertema Hiburan yang menghadirkan pengalaman kuis pilihan ganda yang seru dan menantang. Setiap sesi permainan disajikan dengan bank soal berkualitas yang ditarik langsung dari OpenTDB API, sehingga pertanyaan yang muncul selalu beragam dan tidak monoton. Ketika koneksi internet tidak tersedia, aplikasi secara otomatis beralih ke mode offline dan langsung mengambil soal dari database lokal di perangkat jadi kuis tetap bisa dinikmati kapan saja dan di mana saja. Setiap hasil permainan dicatat secara permanen beserta tanggal dan skor akhirnya, memungkinkan pengguna memantau perkembangan mereka dari waktu ke waktu. Tampilan aplikasi pun sepenuhnya adaptif, mendukung Light Mode dan Dark Mode sesuai preferensi pengguna.

---

## 🎮 Cara Penggunaan

### 1. Menu Utama (MenuFragment)
Saat aplikasi dibuka, pengguna disambut oleh halaman beranda yang menyediakan:
* Tombol Mulai Kuis untuk masuk ke sesi permainan.
* Tombol navigasi menuju halaman Riwayat.
* Switch Toggle untuk beralih antara Dark Theme dan Light Theme secara langsung.

### 2. Mengikuti Kuis (QuizActivity)
* Tekan tombol Mulai Kuis — sistem akan memuat 5 pertanyaan pilihan ganda.
* Pilih salah satu jawaban (A, B, C, atau D).
    * Benar → tombol berubah menjadi 🟢 Hijau.
    * Salah → tombol berubah menjadi 🔴 Merah.
* Umpan balik warna ditampilkan selama 1,5 detik sebelum soal berikutnya muncul.
* Jika tombol back fisik ditekan saat kuis berlangsung, dialog konfirmasi akan muncul untuk mencegah keluar tidak sengaja.

### 3. Hasil & Riwayat (HistoryFragment)
* Setelah soal terakhir dijawab, dialog skor akhir kustom akan tampil.
* Tekan Selesai — skor otomatis tersimpan dan pengguna dapat melihat daftar riwayat nilai pada halaman riwayat.

### 4. Kondisi Offline
* Jika internet terputus saat kuis dimulai, sistem menampilkan notifikasi dan langsung mengambil soal dari database lokal.
* Jika terjadi kegagalan total, tombol Refresh akan muncul untuk memicu ulang koneksi ke API.

---

## ⚙️ Implementasi Teknis
Aplikasi ini memenuhi 7 Spesifikasi Teknis yang diwajibkan:

### 1. Activity Layout

| Komponen | Peran |
| :--- | :--- |
| **MainActivity.java** | Activity utama, dikonfigurasi sebagai Launcher di AndroidManifest.xml |
| **QuizActivity.java** | Activity mandiri yang mengelola siklus hidup, logika permainan, dan validasi jawaban |

### 2. Mekanisme Intent
Perpindahan layar dari halaman utama menuju QuizActivity diimplementasikan secara asinkronus menggunakan objek Intent. Penutupan Activity menggunakan finish() untuk memastikan backstack kembali dengan aman.

### 3. RecyclerView
RecyclerView di dalam HistoryFragment merender daftar riwayat skor secara vertikal menggunakan LinearLayoutManager, ditenagai oleh kelas adapter kustom HistoryAdapter.java.

### 4. Fragment & Jetpack Navigation Component
Halaman utama dibagi menjadi dua fragmen mandiri:
* MenuFragment
* HistoryFragment

Seluruh alur navigasi, ID tujuan, dan aksi perpindahan antar-fragmen dikendalikan secara terpusat melalui berkas nav_graph.xml.

### 5. Background Thread (Handler)
Penundaan waktu saat pergantian soal diimplementasikan menggunakan android.os.Handler().postDelayed() selama 1500ms di dalam method checkAnswer(). Proses ini diisolasi pada Background Thread agar UI Main Thread tidak membeku (freeze) saat memberikan umpan balik warna kepada pengguna.

### 6. Networking (Retrofit)
* Koneksi HTTP dibangun menggunakan library Retrofit dengan baseUrl("https://opentdb.com/").
* Respons JSON diurai secara asinkronus melalui enqueue() ke dalam model QuizResponse.
* Komponen btnRefresh tersedia untuk memicu ulang fungsi muatUlangKuis() ketika terjadi network error.

### 7. Persistensi Data Lokal & Dynamic Theme
#### SQLite Database
* Logika penyimpanan dikapsulasi di dalam kelas QuizDbHelper.java.
* Saat Retrofit berhasil mengambil data online, soal langsung disalin ke SQLite melalui simpanSemuaSoalKeLokal().
* Jika offline terdeteksi, method handleOfflineCondition() memotong jalur internet dan menarik data langsung dari SQLite.

#### Dual Theme Layout
* Deteksi tema menggunakan Configuration.UI_MODE_NIGHT_MASK.
* Dialog kustom menyesuaikan warna latar secara otomatis:
    * 🌙 Dark Mode → Abu-abu Material #333333
    * ☀️ Light Mode → Krem Susu #EFE5DB

---

## ⭐ Fitur Inovatif
### Anti-Accidental Exit
Mengimplementasikan arsitektur modern OnBackPressedDispatcher di dalam onCreate() untuk mengintersep tombol back fisik smartphone. Setiap tindakan pengguna disaring melalui pop-up dialog konfirmasi kustom sebelum kuis dihentikan, mencegah kehilangan progres secara tidak sengaja.