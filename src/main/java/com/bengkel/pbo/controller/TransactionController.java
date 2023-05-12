package com.bengkel.pbo.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bengkel.pbo.App;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.stage.Stage;
import lib.DataConnector;

public class TransactionController {

    class TemporaryData {
        private String columnCode;
        private String columnName;
        private String columnPrice;
        private String columnQty;
        private String columnTotal;

        /**
         * @return the columnCode
         */
        public String getColumnCode() {
            return columnCode;
        }

        /**
         * @param columnCode the columnCode to set
         */
        public void setColumnCode(String columnCode) {
            this.columnCode = columnCode;
        }

        /**
         * @return the columnName
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * @param columnName the columnName to set
         */
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        /**
         * @return the columnPrice
         */
        public String getColumnPrice() {
            return columnPrice;
        }

        /**
         * @param columnPrice the columnPrice to set
         */
        public void setColumnPrice(String columnPrice) {
            this.columnPrice = columnPrice;
        }

        /**
         * @return the columnQty
         */
        public String getColumnQty() {
            return columnQty;
        }

        /**
         * @param columnQty the columnQty to set
         */
        public void setColumnQty(String columnQty) {
            this.columnQty = columnQty;
        }

        /**
         * @return the columnTotal
         */
        public String getColumnTotal() {
            return columnTotal;
        }

        /**
         * @param columnTotal the columnTotal to set
         */
        public void setColumnTotal(String columnTotal) {
            this.columnTotal = columnTotal;
        }
    }
    
    private final DecimalFormat df = new DecimalFormat("###,###.00");
    
    @FXML protected ComboBox<String> ItemList;
    @FXML protected ComboBox<String> customerName;

    @FXML protected TextField priceLabel;
    @FXML protected TextField amountField;
    @FXML protected TextField changeLabel;
    @FXML protected TextField textQty;
    
    @FXML protected Label totalLabel;
    @FXML protected Label totalQty;
    @FXML protected Label todayLabel;
    
    @FXML protected TableView transactionItems;
    
    @FXML protected TableColumn<TemporaryData, String> columnCode;
    @FXML protected TableColumn<TemporaryData, String> columnName;
    @FXML protected TableColumn<TemporaryData, String> columnPrice;
    @FXML protected TableColumn<TemporaryData, String> columnQty;
    @FXML protected TableColumn<TemporaryData, String> columnTotal;
    
    @FXML protected Button btnCancel;

    public Button backButton;

    private ObservableList<String> serviceList = FXCollections.observableArrayList();
    private Map<String, DataConnector.RowEntry> serviceMap = new HashMap<>();
    
    private ObservableList<String> customerList = FXCollections.observableArrayList();
    private Map<String, DataConnector.RowEntry> customerMap = new HashMap<>();
    
    private ObservableList<TemporaryData> tmpData = FXCollections.observableArrayList();   
    
    private List<TemporaryData> checkoutItems = new ArrayList<>();
    
    DataConnector connector;
    private void mock() throws SQLException{
        List<DataConnector.RowEntry> entries = connector.list("barang");
        
        entries.sort((v1,v2) -> v1.get("nama_barang").asString().compareTo(v2.get("nama_barang").asString()));
        entries.forEach(entry -> {
            serviceList.add(entry.get("nama_barang").asString());
            serviceMap.put(entry.get("nama_barang").asString(), entry);
        });
        
        List<DataConnector.RowEntry> customers = connector.list("customer");
        customers.sort((v1,v2) -> v1.get("nama_customer").asString().compareTo(v2.get("nama_customer").asString()));
        customers.forEach(customer -> {
            customerList.add(customer.get("nama_customer").asString());
            customerMap.put(customer.get("nama_customer").asString(), customer);
        });
    }
   
    @FXML
    protected void initialize() throws SQLException {
        connector = new DataConnector("127.0.0.1", "bengkel", "root", "password");
        
        mock();
        ItemList.setItems(serviceList);
        customerName.setItems(customerList);
         
        columnCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColumnCode()));
        columnName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColumnName()));
        columnPrice.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColumnPrice()));
        columnQty.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColumnQty()));
        columnTotal.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getColumnTotal()));
        
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        todayLabel.setText((sdf.format(new Date())));
        
        resetParameter();
    }
    
    @FXML
    private void btnCancel_click() throws IOException {
        tmpData.clear();
        resetParameter();
        totalPrice = 0.0;
        totalLabel.setText("0.00");
    }
    
    @FXML
    private void itemSelected() throws IOException {
        if(ItemList.getValue() == null){
            return;
        }
        
        DataConnector.RowEntry data = serviceMap.get(ItemList.getValue());
        
        priceLabel.setText(df.format(data.get("harga").asDouble()));
        totalQty.setText("/ " + String.valueOf(data.get("stock")));
        textQty.setText("1");
       
    }
    
    private double totalPrice = 0.0;
    
    @FXML
    private void proceed() throws IOException, SQLException {
        
        DataConnector.RowEntry customer = customerMap.get(customerName.getValue());
        Date d = new Date();
        
        try{
            connector.begin();
            
            DataConnector.RowEntry data = new DataConnector.RowEntry("transaksi");
            data.set("id_customer", customer.getId());
            data.set("date", d);
            
            connector.persist(data);
            
            customerList.add(customer.get("nama_customer").asString());
            customerMap.put(customer.get("nama_customer").asString(), customer);

            connector.commit();            
        }catch(Exception e){
            connector.rollback();
        }
        
        try{
            connector.begin();
            
            List<DataConnector.RowEntry> data = connector.list("transaksi", "date=? and id_customer=?", 
                    d, customer.getId());
            
            data.sort((v1,v2) -> v2.getId() < v1.getId() ? -1 : 1);
            
            
            for(TemporaryData v : tmpData) {
                DataConnector.RowEntry detail = new DataConnector.RowEntry("transaksi_detail");
                detail.set("id_transaksi", data.get(0).getId());
                detail.set("id_barang", v.columnCode);
                detail.set("quantity", v.getColumnQty());
                connector.persist(detail);
            }     
            
            connector.commit();            
        }catch(Exception e){
            connector.rollback();
        }
        
        resetParameter();
        
        tmpData.clear();
        totalPrice = 0.0;
        totalLabel.setText("0.00");
    }
    
    @FXML
    private void showHistory() throws IOException {
        App.setRoot("transactionHistory");
    }
    
    @FXML
    private void addItem() throws IOException {
        
        DataConnector.RowEntry data = serviceMap.get(ItemList.getValue());
        
        TemporaryData td = new TemporaryData();
        td.setColumnCode(String.valueOf(data.getId()));
        td.setColumnName(ItemList.getValue());
        td.setColumnPrice(df.format(data.get("harga").asDouble()));
        td.setColumnQty(textQty.getText());
        td.setColumnTotal(df.format(data.get("harga").asDouble()* Double.parseDouble(textQty.getText())));
        
        totalPrice += data.get("harga").asDouble()* Double.parseDouble(textQty.getText());
        totalLabel.setText(df.format(totalPrice));

        tmpData.add(td);
        transactionItems.setItems(tmpData);

        
        resetParameter();  
    }
    
    private void resetParameter() {
        ItemList.setValue(null);
        priceLabel.setText("");
        textQty.setText("");
        totalQty.setText("/ ~");
        amountField.setText("0.00");
        changeLabel.setText("0.00");
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
