package com.bengkel.pbo.controller;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lib.DataConnector;
import java.text.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class TransactionHistoryController {

    private class HistoricalMaster {
        private int masterId;
        private Date date;
        private int customerId;
    }
    
    
    private class HistoricalDetail {
        private int itemId;
        private int qty;
    }
    
    @FXML protected TextField searchValue;
    
    @FXML private TableView<HistoricalMaster> masterHistory;
    @FXML private TableView<HistoricalDetail> detailHistory;
    
    @FXML protected TableColumn<TransactionHistoryController.HistoricalMaster, String> columnDate;
    @FXML protected TableColumn<TransactionHistoryController.HistoricalMaster, String> columnName;
    @FXML protected TableColumn<TransactionHistoryController.HistoricalMaster, String> columnTelp;
    
    @FXML protected TableColumn<TransactionHistoryController.HistoricalDetail, String> columnItem;
    @FXML protected TableColumn<TransactionHistoryController.HistoricalDetail, String> columnPrice;
    @FXML protected TableColumn<TransactionHistoryController.HistoricalDetail, String> columnQty;
    @FXML protected TableColumn<TransactionHistoryController.HistoricalDetail, String> columnTotal;
    
    private ObservableList<HistoricalMaster> historicalList  = FXCollections.observableArrayList();
    private ObservableList<HistoricalDetail> histDetailList = FXCollections.observableArrayList();
    
    private Map<Integer, DataConnector.RowEntry> customerMap = new HashMap<>();
    private Map<Integer, DataConnector.RowEntry> itemMap = new HashMap<>();
    private Map<Integer, List<DataConnector.RowEntry>> detailMap = new HashMap<>();
     
    private DataConnector connector;

    private void initDataMap() throws SQLException {
        List<DataConnector.RowEntry> detailList   = connector.list("transaksi_detail");
        List<DataConnector.RowEntry> itemList     = connector.list("barang");
        List<DataConnector.RowEntry> customerList = connector.list("customer");
        
        detailList.forEach(v -> {
            int masterId = v.get("id_transaksi").asInteger();
            detailMap.computeIfAbsent(masterId, k -> new ArrayList<DataConnector.RowEntry>()).add(v);
        });
        
        itemList.forEach(v -> itemMap.put(v.getId(), v));
        customerList.forEach(v -> customerMap.put(v.getId(), v));
    }
    
    @FXML 
    private void search() throws SQLException, ParseException {
        List<DataConnector.RowEntry> masterList = connector.list("transaksi");
        
        for(DataConnector.RowEntry v : masterList) {
            TransactionHistoryController.HistoricalMaster td = new TransactionHistoryController.HistoricalMaster();
            td.customerId = v.get("id_customer").asInteger();
            td.date = v.get("date").asDate();
            td.masterId = v.getId();
            
            historicalList.add(td);
        }
        
        masterHistory.setItems(historicalList);
        detailHistory.setItems(histDetailList);
    }
    
    @FXML
    public void initialize() throws SQLException, ParseException {
        
    
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

        columnDate.setCellValueFactory(cellData -> new SimpleStringProperty(sdf.format(cellData.getValue().date)));

        columnName.setCellValueFactory(cellData ->  
        {
            DataConnector.RowEntry customer = customerMap.get(cellData.getValue().customerId);
            return new SimpleStringProperty(customer.get("nama_customer").asString());
        });

        columnTelp.setCellValueFactory(cellData ->  
        {
            DataConnector.RowEntry customer = customerMap.get(cellData.getValue().customerId);
            return new SimpleStringProperty(customer.get("no_telp").asString());
        });

        columnItem.setCellValueFactory(cellData ->  
        {
            DataConnector.RowEntry item = itemMap.get(cellData.getValue().itemId);
            return new SimpleStringProperty(item.get("nama_barang").asString());
        });

        DecimalFormat df = new DecimalFormat("###,###.00");

        columnPrice.setCellValueFactory(cellData ->  
        {
            DataConnector.RowEntry item = itemMap.get(cellData.getValue().itemId);
            return new SimpleStringProperty(df.format(item.get("harga").asDouble()));
        });

        columnQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().qty)));

        columnTotal.setCellValueFactory(cellData ->  
        {
            DataConnector.RowEntry item = itemMap.get(cellData.getValue().itemId);
            return new SimpleStringProperty(df.format(item.get("harga").asDouble() * cellData.getValue().qty));
        });
                          
        try {
            connector = new DataConnector("localhost", "bengkel", "root", "@B6da58c7");
            initDataMap();
            
            masterHistory.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    
                    histDetailList.clear();
                         
                    List<DataConnector.RowEntry> details = detailMap.get(newValue.masterId);
                    details.forEach(v -> {
                          HistoricalDetail hist = new HistoricalDetail();
                          
                          hist.itemId = v.get("id_barang").asInteger();
                          hist.qty = v.get("quantity").asInteger();
                          
                          histDetailList.add(hist);
                        
                    });
                }
            });
        } catch (SQLException e) {
            throw e;
        }

        search();
    }
            
    public void backButton(ActionEvent actionEvent) {
        // buat objek Stage
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

        // buat objek FXMLLoader untuk home.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("transaction.fxml"));

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