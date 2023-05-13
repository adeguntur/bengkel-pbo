package com.bengkel.pbo.controller;

import com.bengkel.pbo.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lib.DataConnector;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;


public class CustomerController implements Initializable {

    @FXML private TextField id_customer_field;
    @FXML private TextField nama_customer_field;
    @FXML private TextField no_telp_field;
    @FXML private TextField merk_kendaraan_field;
    @FXML private TextField plat_nomor_field;

    @FXML private Button insertButton;
    @FXML private Button resetButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;

    @FXML private TableView<Customer> TableView;

    @FXML private TableColumn<Customer, Integer> id_customer_column;
    @FXML private TableColumn<Customer, String>  nama_customer_column;
    @FXML private TableColumn<Customer, String>  no_telp_column;
    @FXML private TableColumn<Customer, String>  merk_kendaraan_column;
    @FXML private TableColumn<Customer, String>  plat_nomor_column;

    DataConnector connector;

    @FXML
    private void insertButton() throws IOException, SQLException {
        try{
            connector.begin();

            DataConnector.RowEntry data = new DataConnector.RowEntry("customer");
            data.set("nama_customer", nama_customer_field.getText());
            data.set("no_telp", no_telp_field.getText());
            data.set("merk_kendaraan", merk_kendaraan_field.getText());
            data.set("plat_nomor", plat_nomor_field.getText());

            connector.persist(data);
            connector.commit();
        
        } catch (SQLException e) {
            connector.rollback();
            throw e;
        }
       
        showData();
    }


    @FXML
    private void updateButton() throws SQLException {

        List<DataConnector.RowEntry> res = connector.list("customer", "id = ?", id_customer_field.getText());

        try {
            connector.begin();
            res.forEach(e -> {
                e.set("nama_customer", nama_customer_field.getText());
                e.set("no_telp", no_telp_field.getText());
                e.set("merk_kendaraan", merk_kendaraan_field.getText());
                e.set("plat_nomor", plat_nomor_field.getText());
            });

            connector.persist(res);

            connector.commit();
        } catch (SQLException e) {
            connector.rollback();
            throw e;
        }

        showData();
    }

    @FXML
    private void deleteButton() throws SQLException{

        List<DataConnector.RowEntry> res = connector.list("customer", "id = ?", id_customer_field.getText());

        try {
            connector.begin();

            connector.delete(res);

            connector.commit();
        } catch (SQLException e) {
            connector.rollback();
            throw e;
        }

        showData();
    }


    @FXML
    private void resetButton() {
        id_customer_field.setText("");
        nama_customer_field.setText("");
        no_telp_field.setText("");
        merk_kendaraan_field.setText("");
        plat_nomor_field.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connector = new DataConnector("localhost", "bengkel", "root", "@B6da58c7");
            showData();
            TableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    id_customer_field.setText(String.valueOf(newValue.getId()));
                    nama_customer_field.setText(newValue.getNama_customer());
                    no_telp_field.setText(newValue.getNo_telp());
                    merk_kendaraan_field.setText(newValue.getMerk_kendaraan());
                    plat_nomor_field.setText(newValue.getPlat_nomor());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public ObservableList<Customer> getTransaksi() {

        ObservableList<Customer> bengkels = FXCollections.observableArrayList();

        List<DataConnector.RowEntry> res = null;
        try {
            res = connector.list("customer");
            for (DataConnector.RowEntry row : res) {
                int id = row.getId();
                String nama_customer = row.get("nama_customer").asString();
                String no_telp = row.get("no_telp").asString();
                String merk_kendaraan = row.get("merk_kendaraan").asString();
                String plat_nomor = row.get("plat_nomor").asString();
                bengkels.add(Customer.of(id,nama_customer,no_telp,merk_kendaraan,plat_nomor));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bengkels;
    }


    public void showData() {
        ObservableList<Customer> list = getTransaksi();

        id_customer_column.setCellValueFactory(new PropertyValueFactory<Customer,Integer>("id"));
        nama_customer_column.setCellValueFactory(new PropertyValueFactory<Customer,String>("nama_customer"));
        no_telp_column.setCellValueFactory(new PropertyValueFactory<Customer,String>("no_telp"));
        merk_kendaraan_column.setCellValueFactory(new PropertyValueFactory<Customer,String>("merk_kendaraan"));
        plat_nomor_column.setCellValueFactory(new PropertyValueFactory<Customer,String>("plat_nomor"));

        TableView.setItems(list);
    }


    public void backButton(ActionEvent actionEvent) {
        // buat objek Stage
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

        // buat objek FXMLLoader untuk home.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("home.fxml"));

        // muat scene home.fxml
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // set scene pada stage
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}