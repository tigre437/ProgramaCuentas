public class Movimiento {
    private final int id;
    private final String tipo;
    private final double cantidad;
    private final String descripcion;
    private final String fecha;
    private final String archivo;

    // Constructor completo
    public Movimiento(int id, String tipo, double cantidad, String descripcion, String fecha, String archivo) {
        this.id = id;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.archivo = archivo;
    }

    // Constructor sin ID (para nuevos movimientos antes de guardar en BD)
    public Movimiento(String tipo, double cantidad, String descripcion, String fecha, String archivo) {
        this(-1, tipo, cantidad, descripcion, fecha, archivo);
    }

    // Constructor sin archivo (por compatibilidad con código antiguo)
    public Movimiento(int id, String tipo, double cantidad, String descripcion, String fecha) {
        this(id, tipo, cantidad, descripcion, fecha, null);
    }

    // Getters
    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public double getCantidad() { return cantidad; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }
    public String getArchivo() { return archivo; }
}
