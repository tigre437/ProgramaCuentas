import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MovimientoDAO {
    public static void agregarMovimiento(Movimiento m) {
        String sql = "INSERT INTO movimientos(tipo, cantidad, descripcion, fecha, archivo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, m.getTipo());
            pstmt.setDouble(2, m.getCantidad());
            pstmt.setString(3, m.getDescripcion());
            pstmt.setString(4, m.getFecha());
            pstmt.setString(5, m.getArchivo()); // ✅ ahora guarda el nombre del archivo
            pstmt.executeUpdate();

            LogDAO.agregarLog("Movimiento Nuevo: " + m.getTipo() + " " + m.getCantidad());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void actualizarMovimiento(int id, String tipo, double cantidad, String descripcion) {
        String sql = "UPDATE movimientos SET tipo=?, cantidad=?, descripcion=? WHERE id=?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tipo);
            pstmt.setDouble(2, cantidad);
            pstmt.setString(3, descripcion);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void eliminarMovimiento(int id) {
        String sql = "DELETE FROM movimientos WHERE id=?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Movimiento> listarMovimientos() {
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT id, tipo, cantidad, descripcion, fecha, archivo FROM movimientos ORDER BY fecha DESC";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(new Movimiento(
                        rs.getInt("id"),
                        rs.getString("tipo"),
                        rs.getDouble("cantidad"),
                        rs.getString("descripcion"),
                        rs.getString("fecha"),
                        rs.getString("archivo") // ✅ ahora lo carga correctamente
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }


    public static double obtenerSaldo() {
        String sql = "SELECT valor FROM saldo WHERE id = 1";
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("valor");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void actualizarSaldo(double nuevoSaldo) {
        String sql = "UPDATE saldo SET valor = ? WHERE id = 1";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, nuevoSaldo);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void ajustarSaldoDirecto(double saldoDeseado) {
        try (Connection conn = Database.connect()) {
            double saldoActual = obtenerSaldo();

            // Guardar log del ajuste
            String logMensaje = "Saldo ajustado: anterior "
                    + NumberFormat.getCurrencyInstance(Locale.GERMANY).format(saldoActual)
                    + ", nuevo "
                    + NumberFormat.getCurrencyInstance(Locale.GERMANY).format(saldoDeseado);
            LogDAO.agregarLog(logMensaje);

            // Actualizar tabla de saldo
            actualizarSaldo(saldoDeseado);

            // Registrar movimiento "Ajuste" (solo informativo)
            String sqlInsertar = "INSERT INTO movimientos(tipo, cantidad, descripcion, fecha) VALUES(?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertar)) {
                pstmt.setString(1, "Ajuste");
                pstmt.setDouble(2, Math.abs(saldoDeseado));
                pstmt.setString(3, "Ajuste manual de saldo");
                pstmt.setString(4, LocalDate.now().toString());
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean hayMovimientosPosteriores(Movimiento m) {
        String sql = "SELECT COUNT(*) FROM movimientos WHERE " +
                "(fecha > ?) OR (fecha = ? AND id > ?)";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getFecha());
            ps.setString(2, m.getFecha());
            ps.setInt(3, m.getId());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void actualizarArchivo(int id, String archivo) {
        String sql = "UPDATE movimientos SET archivo = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, archivo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int obtenerUltimoId() {
        String sql = "SELECT MAX(id) FROM movimientos";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }




}
