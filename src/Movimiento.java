public class Movimiento {
    private final int id;
    private final String tipo;
    private final double cantidad;
    private final String descripcion;
    private final String fecha;

    public Movimiento(int id, String tipo, double cantidad, String descripcion, String fecha) {
        this.id = id;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public Movimiento(String tipo, double cantidad, String descripcion, String fecha) {
        this(-1, tipo, cantidad, descripcion, fecha);
    }

    // Getters
    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public double getCantidad() { return cantidad; }
    public String getDescripcion() { return descripcion; }
    public String getFecha() { return fecha; }
}
