package com.example.perpustakaanflora

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DataPeminjaman(
    val nama: String,
    val gender: String,
    val universitas: String,
    val buku: String,
    val tanggalPinjam: String,
    val tanggalRencana: String,
    var status: String = "Dipinjam"
)

class MainActivity : AppCompatActivity() {

    private val daftarBuku = mutableListOf(
        "Algoritma dan Struktur Data",
        "Pemrograman Java untuk Pemula",
        "Belajar Bahasa Jepang"
    )

    private val riwayat = mutableListOf<DataPeminjaman>()

    private val prefs by lazy {
        getSharedPreferences("PerpustakaanFlora", MODE_PRIVATE)
    }

    private val formatTanggal =
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private fun tanggalHariIni(): String {
        return formatTanggal.format(Calendar.getInstance().time)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        muatData()

        findViewById<Button>(R.id.btnPinjam).setOnClickListener {
            pinjamBuku()
        }

        findViewById<Button>(R.id.btnKembali).setOnClickListener {
            kembalikanBuku()
        }

        findViewById<Button>(R.id.btnDaftar).setOnClickListener {
            tampilkanDaftarBuku()
        }

        findViewById<Button>(R.id.btnRiwayat).setOnClickListener {
            tampilkanRiwayat()
        }
    }

    // MENYIMPAN DATA
    private fun simpanData() {
        val bukuJson = JSONArray()
        daftarBuku.forEach {
            bukuJson.put(it)
        }

        val riwayatJson = JSONArray()

        riwayat.forEach { data ->
            val obj = JSONObject()
            obj.put("nama", data.nama)
            obj.put("gender", data.gender)
            obj.put("universitas", data.universitas)
            obj.put("buku", data.buku)
            obj.put("tanggalPinjam", data.tanggalPinjam)
            obj.put("tanggalRencana", data.tanggalRencana)
            obj.put("status", data.status)

            riwayatJson.put(obj)
        }

        prefs.edit()
            .putString("daftarBuku", bukuJson.toString())
            .putString("riwayat", riwayatJson.toString())
            .apply()
    }

    // MEMUAT DATA
    private fun muatData() {
        try {
            val bukuJson = prefs.getString("daftarBuku", null)

            if (bukuJson != null) {
                daftarBuku.clear()
                val array = JSONArray(bukuJson)

                for (i in 0 until array.length()) {
                    daftarBuku.add(array.getString(i))
                }
            }

            val riwayatJson = prefs.getString("riwayat", null)

            if (riwayatJson != null) {
                riwayat.clear()
                val array = JSONArray(riwayatJson)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    riwayat.add(
                        DataPeminjaman(
                            nama = obj.optString("nama"),
                            gender = obj.optString("gender"),
                            universitas = obj.optString("universitas"),
                            buku = obj.optString("buku"),
                            tanggalPinjam = obj.optString("tanggalPinjam"),
                            tanggalRencana = obj.optString(
                                "tanggalRencana",
                                obj.optString("tanggalKembali", "")
                            ),
                            status = obj.optString("status", "Dipinjam")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Gagal memuat data",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // INPUT TANGGAL
    private fun pilihTanggal(target: EditText) {
        val kalender = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, tahun, bulan, hari ->
                target.setText(
                    String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%04d",
                        hari,
                        bulan + 1,
                        tahun
                    )
                )
            },
            kalender.get(Calendar.YEAR),
            kalender.get(Calendar.MONTH),
            kalender.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun buatTanggalInput(): EditText {
        return EditText(this).apply {
            hint = "Pilih tanggal"
            isFocusable = false
            isClickable = true
            setOnClickListener {
                pilihTanggal(this)
            }
        }
    }

    private fun buatLabel(teks: String): TextView {
        return TextView(this).apply {
            text = teks
            textSize = 15f
            setPadding(0, 12, 0, 4)
        }
    }

    // DAFTAR BUKU
    private fun tampilkanDaftarBuku() {
        val daftar = daftarBuku.map { buku ->
            val sedangDipinjam = riwayat.any {
                it.buku == buku && it.status == "Dipinjam"
            }

            "$buku - ${if (sedangDipinjam) "Dipinjam" else "Tersedia"}"
        }

        AlertDialog.Builder(this)
            .setTitle("Daftar Koleksi Buku")
            .setItems(daftar.toTypedArray(), null)
            .setPositiveButton("Tutup", null)
            .show()
    }

    // PEMINJAMAN BUKU
    private fun pinjamBuku() {

        val tersedia = daftarBuku.filter { buku ->
            riwayat.none {
                it.buku == buku && it.status == "Dipinjam"

            }
        }

        if (tersedia.isEmpty()) {
            Toast.makeText(
                this,
                "Tidak ada buku yang tersedia",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nama = EditText(this).apply {
            hint = "Masukkan nama lengkap"
        }

        val universitas = EditText(this).apply {
            hint = "Masukkan nama universitas"
        }

        val gender = Spinner(this)
        val pilihanGender = listOf("Perempuan", "Laki-laki")

        gender.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            pilihanGender
        )

        val buku = Spinner(this)
        buku.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tersedia
        )

        val tanggalPinjam = buatTanggalInput().apply {
            setText(tanggalHariIni())
        }

        val tanggalRencana = buatTanggalInput()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 10)

            addView(buatLabel("Nama Peminjam"))
            addView(nama)

            addView(buatLabel("Jenis Kelamin"))
            addView(gender)

            addView(buatLabel("Universitas"))
            addView(universitas)

            addView(buatLabel("Judul Buku"))
            addView(buku)

            addView(buatLabel("Tanggal Pinjam"))
            addView(tanggalPinjam)

            addView(buatLabel("Tanggal Kembali Rencana"))
            addView(tanggalRencana)
        }

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }


        val dialog = AlertDialog.Builder(this)
            .setTitle("Form Peminjaman Buku")
            .setView(scrollView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val namaText = nama.text.toString().trim()
                    val universitasText =
                        universitas.text.toString().trim()
                    val pinjamText =
                        tanggalPinjam.text.toString()
                    val kembaliText =
                        tanggalRencana.text.toString()

                    if (namaText.isEmpty() ||
                        universitasText.isEmpty() ||
                        kembaliText.isEmpty()
                    ) {
                        Toast.makeText(
                            this,
                            "Lengkapi semua data terlebih dahulu",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    riwayat.add(
                        DataPeminjaman(
                            nama = namaText,
                            gender = gender.selectedItem.toString(),
                            universitas = universitasText,
                            buku = buku.selectedItem.toString(),
                            tanggalPinjam = pinjamText,
                            tanggalRencana = kembaliText
                        )
                    )

                    simpanData()

                    Toast.makeText(
                        this,
                        "Peminjaman berhasil disimpan",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    // PENGEMBALIAN BUKU
    private fun kembalikanBuku() {

        val bukuDipinjam = riwayat.filter {
            it.status == "Dipinjam"
        }

        if (bukuDipinjam.isEmpty()) {
            Toast.makeText(
                this,
                "Tidak ada buku yang sedang dipinjam",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val pilihan = bukuDipinjam.map {
            "${it.nama} - ${it.buku}"
        }

        AlertDialog.Builder(this)
            .setTitle("Pilih Buku yang Dikembalikan")
            .setItems(pilihan.toTypedArray()) { _, posisi ->

                val data = bukuDipinjam[posisi]

                AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Pengembalian")
                    .setMessage(
                        "Buku ${data.buku} atas nama ${data.nama} akan dikembalikan?"
                    )
                    .setPositiveButton("Ya, Kembalikan") { _, _ ->

                        data.status = "Dikembalikan"
                        simpanData()

                        Toast.makeText(
                            this,
                            "Buku berhasil dikembalikan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // RIWAYAT PEMINJAMAN
    private fun tampilkanRiwayat() {
        val daftarRiwayat = riwayat.mapIndexed { index, data ->
            """
        ${index + 1}. ${data.nama}
        Jenis Kelamin: ${data.gender}
        Universitas: ${data.universitas}
        Buku: ${data.buku}
        Tanggal Pinjam: ${data.tanggalPinjam}
        Tanggal Kembali Rencana: ${data.tanggalRencana}
        Status: ${data.status}
        """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("Riwayat Peminjaman")
            .setItems(daftarRiwayat.toTypedArray(), null)
            .setPositiveButton("Tutup", null)
            .setNegativeButton("Hapus Riwayat") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Konfirmasi")
                    .setMessage("Yakin ingin menghapus semua riwayat?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Hapus") { _, _ ->
                        riwayat.clear()
                        simpanData()

                        Toast.makeText(
                            this,
                            "Riwayat berhasil dihapus",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .show()
            }
            .show()
    }

}