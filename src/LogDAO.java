import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    public static void agregarLog(String accion) {
        String sql = "INSERT INTO logs(accion, fecha_hora) VALUES (?, ?)";
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accion);
            pstmt.setString(2, fechaHora);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Log> listarLogs() {
        List<Log> lista = new ArrayList<>();
        String sql = "SELECT * FROM logs ORDER BY fecha_hora DESC";

        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Log(
                        rs.getInt("id"),
                        rs.getString("accion"),
                        rs.getString("fecha_hora")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }
}
