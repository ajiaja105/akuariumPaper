# File yang WAJIB kamu tambahkan manual ke app/src/main/assets/

Skrip index.html (hasil copy dari Gfv9.html) mereferensikan file-file
ini secara relatif. Cuma index.html sendiri yang saya punya - file di
bawah ini HARUS kamu copy manual dari paket netlify-demo-package kamu
ke folder app/src/main/assets/ (sejajar dengan index.html):

- akuarium.glb
- ikan-dasar.glb
- ikan-slot1.glb
- umpan.glb
- batu.glb
- pohon.glb
- rumput.glb
- dinding.jpg
- manifest.json (opsional - ini PWA manifest, tidak wajib utk WebView app)

## Slot ikan 2-5
Kalau nanti ada model tambahan yang diisi ke slot 2-5, itu juga perlu
ditambahkan ke assets/ dengan nama sesuai yang dipakai di kode
(ikan-slot2.glb, ikan-slot3.glb, dst).

## PENTING - proteksi model dihapus
Ingat: di versi Netlify, ikan-dasar.glb & ikan-slot1.glb dimuat LANGSUNG
(tanpa guardedGlbUrl), jadi tidak perlu diubah. Tapi kalau kamu nanti
gabungkan versi yang SUDAH pakai proteksi get-model.js/guardedGlbUrl(),
baris itu harus diubah dulu jadi path assets biasa, misalnya:

    // SEBELUM (versi Netlify dgn proteksi):
    new GLTFLoader().load(guardedGlbUrl('ikan-slot1.glb'), ...)

    // SESUDAH (versi Android, tanpa proteksi):
    new GLTFLoader().load('ikan-slot1.glb', ...)

Karena tidak ada server Netlify di app Android, guardedGlbUrl() akan
selalu gagal kalau tidak diganti.
