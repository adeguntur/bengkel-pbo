package com.bengkel.pbo.controller;

import com.bengkel.pbo.model.Barang;
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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;


public class BarangController implements Initializable {

    @FXML
    private TextField id_barang_field;

    @FXML
    private TextField nama_barang_field;

    @FXML
    private TextField harga_barang_field;

    @FXML
    private TextField stock_field;

    @FXML
    private Button insertButton;

    @FXML
    private Button resetButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private javafx.scene.control.TableView<Barang> TableView;

    @FXML
    private TableColumn<Barang, Integer> id_barang_column;

    @FXML
    private TableColumn<Barang, String> nama_barang_column;

    @FXML
    private TableColumn<Barang, Integer> harga_barang_column;

    @FXML
    private TableColumn<Barang, Integer> stock_column;


    DataConnector connector;

    @FXML
    private void insertButton() throws IOException, SQLException {
        connector.begin();

        DataConnector.RowEntry data = new DataConnector.RowEntry("barang");
        data.set("nama_barang", nama_barang_field.getText());
        data.set("harga", harga_barang_field.getText());
        data.set("stock", stock_field.getText());

        connector.persist(data);
        connector.commit();

        showData();
    }


    @FXML
    private void updateButton() throws SQLException {

        List<DataConnector.RowEntry> res = connector.list("barang", "id = ?", id_barang_field.getText());

        try {
            connector.begin();
            res.forEach(e -> {
                e.set("nama_barang", nama_barang_field.getText());
                e.set("harga", harga_barang_field.getText());
                e.set("stock", stock_field.getText());
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

        List<DataConnector.RowEntry> res = connector.list("barang", "id = ?", id_barang_field.getText());

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
        id_barang_field.setText("");
        nama_barang_field.setText("");
        harga_barang_field.setText("");
        stock_field.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connector = new DataConnector("localhost", "bengkel", "root", "password");
            showData();
            TableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    id_barang_field.setText(String.valueOf(newValue.getId()));
                    nama_barang_field.setText(newValue.getNama_barang());
                    harga_barang_field.setText(String.valueOf(newValue.getHarga_barang()));
                    stock_field.setText(String.valueOf(newValue.getStock()));
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public ObservableList<Barang> getTransaksi() {

        ObservableList<Barang> bengkels = FXCollections.observableArrayList();

        List<DataConnector.RowEntry> res = null;
        try {
            res = connector.list("barang");
            for (DataConnector.RowEntry row : res) {
                int id = row.getId();
                String nama_barang = row.get("nama_barang").asString();
                int harga = row.get("harga").asInteger();
                int stock = row.get("stock").asInteger();
                bengkels.add(Barang.of(id,nama_barang,harga,stock));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bengkels;
    }


    public void showData() {
        ObservableList<Barang> list = getTransaksi();

        id_barang_column.setCellValueFactory(new PropertyValueFactory<Barang,Integer>("id"));
        nama_barang_column.setCellValueFactory(new PropertyValueFactory<Barang,String>("nama_barang"));
        harga_barang_column.setCellValueFactory(new PropertyValueFactory<Barang,Integer>("harga_barang"));
        stock_column.setCellValueFactory(new PropertyValueFactory<Barang,Integer>("stock"));


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