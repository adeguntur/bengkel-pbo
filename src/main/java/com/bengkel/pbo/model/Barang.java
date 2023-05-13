package com.bengkel.pbo.model;

public class Barang {

    private int id;
    private String nama_barang;
    private int harga_barang;
    private int stock;

    public static Barang of(int id, String nama_barang, int harga_barang, int stock) {
        Barang data = new Barang();
        data.setId(id);
        data.setNama_barang(nama_barang);
        data.setHarga_barang(harga_barang);
        data.setStock(stock);
        return data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama_barang() {
        return nama_barang;
    }

    public void setNama_barang(String nama_barang) {
        this.nama_barang = nama_barang;
    }

    public int getHarga_barang() {
        return harga_barang;
    }

    public void setHarga_barang(int harga_barang) {
        this.harga_barang = harga_barang;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
