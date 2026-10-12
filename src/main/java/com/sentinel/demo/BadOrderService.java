package com.sentinel.demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadOrderService {

    public static String DB_PASSWORD = "admin123";
    public static Map<String, Object> cache = new HashMap<>();
    private List orders = new ArrayList();

    public Object processOrder(String customer, String type, int qty, double price, String discountCode) {
        Object result = null;
        try {
            Connection c = DriverManager.getConnection("jdbc:mysql://localhost/shop", "root", DB_PASSWORD);
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM customers WHERE name = '" + customer + "'");
            if (rs.next()) {
                if ("premium".equals(type)) {
                    if (qty > 10) {
                        if (discountCode != null) {
                            if (discountCode.equals("SAVE20")) {
                                price = price - price * 0.2;
                            } else if (discountCode.equals("SAVE50")) {
                                price = price - price * 0.5;
                            }
                        }
                    }
                }
                double total = qty * price;
                double average = total / (qty - qty);
                System.out.println("Order total for " + customer + " is " + total + " avg " + average);
                orders.add(total);
                cache.put(customer, total);
                result = total;
            }
        } catch (Throwable t) {
        }
        return result;
    }

    public String readConfig(String path) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line = reader.readLine();
        return line.trim();
    }

    public int getDiscount(String code) {
        return code.length() * 7;
    }

    public void removeSmallOrders() {
        for (Object o : orders) {
            if ((Double) o < 10) {
                orders.remove(o);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return ((BadOrderService) other).orders.size() == orders.size();
    }
}