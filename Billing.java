/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */


import javax.swing.*;                
import java.awt.*; 
import java.sql.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;

public class Billing extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Billing.class.getName());
    private JTable billTable;
    
    public Billing() {
        setTitle("ElectroMart Billing");
    setSize(800, 600);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setLayout(new BorderLayout());

    // Call all the panel initializer methods
    initSelectionPanel();       
    //initBillTablePanel();      
    initButtonPanel();          
    initBillTablePanel();
    loadSavedBills(); 
    

    }

    
    private void initSelectionPanel() {
    JPanel selectionPanel = new JPanel();
    selectionPanel.setBackground(new Color(230, 255, 250)); 
    selectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10));

    JLabel lblBuyer = new JLabel("Buyer:");
    JLabel lblProduct = new JLabel("Product:");
    JLabel lblQuantity = new JLabel("Quantity:");

    JComboBox<String> cbBuyers = new JComboBox<>();
    JComboBox<String> cbProducts = new JComboBox<>();
    JTextField tfQuantity = new JTextField(5);

    // ✨ Styling
    Font labelFont = new Font("SansSerif", Font.BOLD, 14);
    for (JLabel label : new JLabel[]{lblBuyer, lblProduct, lblQuantity}) {
        label.setFont(labelFont);
    }

    // 📥 Load buyers from DB
    try {
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT name FROM buyers");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            cbBuyers.addItem(rs.getString("name"));
        }
        con.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

    // 📥 Load products from DB
    try {
        Connection con = DBConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT name FROM products");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            cbProducts.addItem(rs.getString("name"));
        }
        con.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

    //  Add components to panel
    selectionPanel.add(lblBuyer);
    selectionPanel.add(cbBuyers);
    selectionPanel.add(lblProduct);
    selectionPanel.add(cbProducts);
    selectionPanel.add(lblQuantity);
    selectionPanel.add(tfQuantity);

    add(selectionPanel, BorderLayout.NORTH);
}
    
    

private void initBillTablePanel() {
    String[] columns = {"Product Name", "Quantity", "Unit Price", "Total"};
    DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
    billTable = new JTable(tableModel);

    billTable.setRowHeight(24);
    billTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
    billTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 15));
    billTable.getTableHeader().setBackground(new Color(200, 255, 200));
    billTable.getTableHeader().setForeground(Color.BLACK);

    JScrollPane scrollPane = new JScrollPane(billTable);
    add(scrollPane, BorderLayout.CENTER);
}


  private void initButtonPanel() {
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(new Color(230, 250, 255));
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

    JButton btnMakeBill = new JButton("Add a Bill");
    JButton btnPrint = new JButton("Print Bill");
    JButton btnPreview = new JButton("Preview Bill");

    btnMakeBill.setFont(new Font("SansSerif", Font.BOLD, 14));
    btnPrint.setFont(new Font("SansSerif", Font.BOLD, 14));
    btnPreview.setFont(new Font("SansSerif", Font.BOLD, 14));
    
    btnPreview.addActionListener(e -> {
    int selectedRow = billTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select one bill row to preview.");
        return;
    }

    String product = billTable.getValueAt(selectedRow, 0).toString();
    int qty = Integer.parseInt(billTable.getValueAt(selectedRow, 1).toString());
    double price = Double.parseDouble(billTable.getValueAt(selectedRow, 2).toString());
    double total = Double.parseDouble(billTable.getValueAt(selectedRow, 3).toString());

    StringBuilder previewText = new StringBuilder();
    previewText.append(" ElectroMart Bill \n\n");
    previewText.append("Date: ").append(java.time.LocalDate.now()).append("\n\n");

    previewText.append("Product\tQty\tUnit Price\tTotal\n");
    previewText.append("------------------------------------------\n");
    previewText.append(String.format("%-8s\t%-3d\t₹%-10.2f\t₹%.2f\n", product, qty, price, total));

    previewText.append("\nTotal Payable: ₹").append(total);

    JTextArea area = new JTextArea(previewText.toString());
    area.setEditable(false);
    area.setFont(new Font("Monospaced", Font.PLAIN, 14));
    JScrollPane pane = new JScrollPane(area);

    JFrame previewFrame = new JFrame("Bill Preview");
    previewFrame.setSize(500, 400);
    previewFrame.add(pane);
    previewFrame.setLocationRelativeTo(this);
    previewFrame.setVisible(true);
});
    

    btnMakeBill.addActionListener(e -> {
    try {
        // Get selected buyer and product
        JComboBox<String> cbBuyers = null;
        JComboBox<String> cbProducts = null;
        JTextField tfQuantity = null;

        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (Component inner : panel.getComponents()) {
                    if (inner instanceof JComboBox<?>) {
                        if (cbBuyers == null) cbBuyers = (JComboBox<String>) inner;
                        else cbProducts = (JComboBox<String>) inner;
                    }
                    if (inner instanceof JTextField) tfQuantity = (JTextField) inner;
                }
            }
        }

        if (cbBuyers == null || cbProducts == null || tfQuantity == null) {
            JOptionPane.showMessageDialog(this, "Missing input fields.");
            return;
        }

        String buyerName = (String) cbBuyers.getSelectedItem();
        String productName = (String) cbProducts.getSelectedItem();
        int quantity = Integer.parseInt(tfQuantity.getText().trim());

        // ✅ Connect once and reuse
        Connection con = DBConnection.getConnection();

        // Check stock
        PreparedStatement checkStock = con.prepareStatement("SELECT quantity, price FROM products WHERE name = ?");
        checkStock.setString(1, productName);
        ResultSet rs = checkStock.executeQuery();

        if (rs.next()) {
            int availableQty = rs.getInt("quantity");
            double unitPrice = rs.getDouble("price");

            if (quantity > availableQty) {
                JOptionPane.showMessageDialog(this,
                    "❌ Sorry, only (" + availableQty + ") stocks are available for " + productName + " •︵•");

                con.close();
                return;
            }

            double total = unitPrice * quantity;
            java.sql.Date today = new java.sql.Date(System.currentTimeMillis());

            // ✅ Insert bill into BillsSaved
            Connection con2 = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/BillsSaved", "root", ""
            );

            PreparedStatement insert = con2.prepareStatement(
                "INSERT INTO bills (buyer_name, product_name, quantity, price, total, date) VALUES (?, ?, ?, ?, ?, ?)"
            );
            insert.setString(1, buyerName);
            insert.setString(2, productName);
            insert.setInt(3, quantity);
            insert.setDouble(4, unitPrice);
            insert.setDouble(5, total);
            insert.setDate(6, today);
            insert.executeUpdate();

            con2.close();

            // 🔻Reduce stock
            PreparedStatement updateStock = con.prepareStatement(
                "UPDATE products SET quantity = quantity - ? WHERE name = ?"
            );
            updateStock.setInt(1, quantity);
            updateStock.setString(2, productName);
            updateStock.executeUpdate();

            con.close();

            // Show in UI table
            DefaultTableModel model = (DefaultTableModel) billTable.getModel();
            model.addRow(new Object[]{productName, quantity, unitPrice, total});
            tfQuantity.setText("");

            JOptionPane.showMessageDialog(this, "✅ Item added and stock updated!");
        } else {
            con.close();
            JOptionPane.showMessageDialog(this, "Product not found in database.");
        }

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error generating bill:\n" + ex.getMessage());
    }
});

   btnPrint.addActionListener(e -> {
    int selectedRow = billTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a bill to print.");
        return;
    }

    String product = billTable.getValueAt(selectedRow, 0).toString();
    int qty = Integer.parseInt(billTable.getValueAt(selectedRow, 1).toString());
    double price = Double.parseDouble(billTable.getValueAt(selectedRow, 2).toString());
    double total = Double.parseDouble(billTable.getValueAt(selectedRow, 3).toString());

    String receiptText = String.format(
        "         ElectroMart\n" +
        "       🛍️ Official Receipt 🛍️\n\n" +
        "Date: %s\n\n" +
        "Item        Qty   Unit Price   Total\n" +
        "--------------------------------------\n" +
        "%-10s  %-4d  ₹%-11.2f  ₹%.2f\n\n" +
        "Grand Total: ₹%.2f\n" +
        "--------------------------------------\n" +
        "Thank you for shopping with us!\n",
        java.time.LocalDate.now(), product, qty, price, total, total
    );

    JTextArea textArea = new JTextArea(receiptText);
    textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

    try {
        boolean printed = textArea.print();
        if (printed) {
            JOptionPane.showMessageDialog(this, "🖨️ Receipt sent to printer!");
        } else {
            JOptionPane.showMessageDialog(this, "❌ Printing was cancelled.");
        }
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error printing receipt:\n" + ex.getMessage());
    }
});

    buttonPanel.add(btnMakeBill);
    buttonPanel.add(btnPrint);
    buttonPanel.add(btnPreview);
    add(buttonPanel, BorderLayout.SOUTH);
}
  
  
  private void loadSavedBills() {
    try {
        Connection con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/BillsSaved", "root", ""
        );
        PreparedStatement ps = con.prepareStatement("SELECT product_name, quantity, price, total FROM bills");
        ResultSet rs = ps.executeQuery();

        DefaultTableModel model = (DefaultTableModel) billTable.getModel();
        while (rs.next()) {
            String product = rs.getString("product_name");
            int qty = rs.getInt("quantity");
            double price = rs.getDouble("price");
            double total = rs.getDouble("total");
            model.addRow(new Object[]{product, qty, price, total});
        }

        con.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    @SuppressWarnings("unchecked")
   
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * @param args the command line arguments
     */

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new Billing().setVisible(true));
}
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
