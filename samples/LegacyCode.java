import java.sql.*;

public class LegacyCode {
    public String findUser(String name) {
        try {
            Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/app", "admin", "admin123");
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
            if (rs.next()) {
                if (rs.getString("role") != null) {
                    if (rs.getString("role").equals("ADMIN")) {
                        return "admin:" + rs.getString("name");
                    } else {
                        return "user:" + rs.getString("name");
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
}