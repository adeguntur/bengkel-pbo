package com.bengkel.pbo.model;

public class Customer {
    private int id;
    private String nama_customer;
    private String no_telp;
    private String merk_kendaraan;
    private String plat_nomor;

    public static Customer of(int id, String nama_customer, String no_telp, String merk_kendaraan, String plat_nomor) {
        Customer data = new Customer();
        data.setId(id);
        data.setNama_customer(nama_customer);
        data.setNo_telp(no_telp);
        data.setMerk_kendaraan(merk_kendaraan);
        data.setPlat_nomor(plat_nomor);
        return data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama_customer() {
        return nama_customer;
    }

    public void setNama_customer(String nama_customer) {
        this.nama_customer = nama_customer;
    }

    public String getNo_telp() {
        return no_telp;
    }

    public void setNo_telp(String no_telp) {
        this.no_telp = no_telp;
    }

    public String getMerk_kendaraan() {
        return merk_kendaraan;
    }

    public void setMerk_kendaraan(String merk_kendaraan) {
        this.merk_kendaraan = merk_kendaraan;
    }

    public String getPlat_nomor() {
        return plat_nomor;
    }

    public void setPlat_nomor(String plat_nomor) {
        this.plat_nomor = plat_nomor;
    }

}
