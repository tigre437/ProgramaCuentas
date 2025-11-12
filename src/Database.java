import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:cuentas.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void crearTablas() {
        String tablaMovimientos = """
        CREATE TABLE IF NOT EXISTS movimientos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            tipo TEXT NOT NULL,
            cantidad REAL NOT NULL,
            descripcion TEXT,
            fecha TEXT NOT NULL
        );
    """;

        String tablaLogs = """
        CREATE TABLE IF NOT EXISTS logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            accion TEXT NOT NULL,
            fecha_hora TEXT NOT NULL
        );
    """;

        String tablaSaldo = """
        CREATE TABLE IF NOT EXISTS saldo (
            id INTEGER PRIMARY KEY CHECK(id = 1),
            valor REAL NOT NULL
        );
    """;

        String inicializarSaldo = """
        INSERT OR IGNORE INTO saldo(id, valor) VALUES(1, 0);
    """;

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(tablaMovimientos);
            stmt.execute(tablaLogs);
            stmt.execute(tablaSaldo);
            stmt.execute(inicializarSaldo);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
