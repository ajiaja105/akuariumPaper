# Gentlefin 3D - Live Wallpaper (starting point)

Ini KERANGKA AWAL, bukan produk jadi. Dibuat sebagai starting point
sesuai permintaan - skrip Gfv9.html dibungkus WebView di dalam
WallpaperService Android.

## Sebelum build
1. Tambahkan semua file di ASET_YANG_HARUS_DITAMBAH.md ke
   app/src/main/assets/
2. Ganti app/src/main/res/drawable/ic_wallpaper_icon.xml dengan
   screenshot akuarium asli (format PNG, taruh di res/drawable/,
   update referensi di AndroidManifest.xml & wallpaper_config.xml)

## Cara build
- **Kalau pakai Android Studio (PC/laptop):** buka folder ini sebagai
  proyek, biarkan Gradle sync, lalu Run ke device/emulator.
- **Kalau pakai "Code on the Go" (HP):** import folder ini sebagai
  proyek existing. Saya belum pernah coba tool ini langsung, jadi
  langkah persisnya mungkin beda - ikuti dokumentasi appdevforall.

## Urutan testing yang disarankan
1. **Build & install dulu, buka sebagai wallpaper di HP.**
2. Kalau muncul layar HITAM/KOSONG saja → kemungkinan besar ini
   pertanda WebGL gagal bikin context (masalah yang sudah diprediksi
   di komentar AquariumWallpaperService.kt). Cek Logcat, cari baris
   yang mengandung "WebGL" atau "GL Error".
3. Kalau itu terjadi, itu bukan bug di skrip akuarium kamu - itu
   keterbatasan pendekatan WebView-di-memori yang dipakai file ini.
   Opsi lanjutannya ada di komentar dalam
   AquariumWallpaperService.kt (teknik VirtualDisplay, atau turun ke
   Canvas2D).
4. Kalau muncul GAMBAR TAPI IKAN TIDAK GERAK / patah-patah parah →
   kemungkinan frameIntervalMs (30fps loop gambar manual) kurang
   cocok, atau device kamu terlalu berat untuk render ganda
   (WebView render + manual draw loop) - ini juga baru ketahuan
   setelah dicoba langsung.
5. Kalau JALAN LANCAR → lanjut sambungkan tombol "Feed Fish" (test
   tap di layar home), lalu baru pikirkan UI utk ganti-ganti setting
   (butuh Activity konfigurasi terpisah, belum ada di kerangka ini).

## Yang BELUM ada di kerangka ini (perlu kamu kembangkan)
- Activity "Configure" (buka saat user long-press wallpaper > Settings)
  utk expose slider2 warna/kecepatan dari HTML ke pengaturan native
- Icon & preview thumbnail asli (masih placeholder)
- Penanganan kalau WebGL gagal (fallback ke pesan error yg rapi,
  bukan layar hitam kosong)
- Testing baterai/panas HP - render 3D terus-menerus sbg wallpaper
  bisa boros baterai, ini co
mmon complaint di app 3D live wallpaper
