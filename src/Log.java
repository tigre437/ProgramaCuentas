public class Log {
    private final int id;
    private final String accion;
    private final String fechaHora;

    public Log(int id, String accion, String fechaHora) {
        this.id = id;
        this.accion = accion;
        this.fechaHora = fechaHora;
    }

    public int getId() { return id; }
    public String getAccion() { return accion; }
    public String getFechaHora() { return fechaHora; }
}
