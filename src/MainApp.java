import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class MainApp extends Application {

    private TableView<Movimiento> tablaMovimientos;
    private TableView<Log> tablaLogs;
    private Label lblSaldo;
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private ObservableList<Movimiento> movimientos;

    @Override
    public void start(Stage stage) {
        Database.crearTablas();


        // === ENCABEZADO ===
        Label encabezado = new Label("Gestión de Cuenta");
        encabezado.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        encabezado.setAlignment(Pos.CENTER);

        // === FORMULARIO SUPERIOR ===
        ComboBox<String> tipoBox = new ComboBox<>();
        tipoBox.getItems().addAll("Ingreso", "Gasto");
        tipoBox.setValue("Ingreso");
        tipoBox.setPrefSize(155, 50);
        tipoBox.setStyle("-fx-font-size: 20px;");

        TextField cantidadField = new TextField();
        cantidadField.setPromptText("Cantidad (€)");
        cantidadField.setPrefSize(140, 50);
        cantidadField.setStyle("-fx-font-size: 20px;");

        TextField descripcionField = new TextField();
        descripcionField.setPromptText("Descripción");
        descripcionField.setPrefSize(200, 50);
        descripcionField.setStyle("-fx-font-size: 20px;");

        Button btnAdjuntar = new Button("Adjuntar archivo");
        btnAdjuntar.setPrefSize(200, 50);
        btnAdjuntar.setStyle("-fx-font-size: 20px;");


        final File[] archivoSeleccionado = {null};

        btnAdjuntar.setOnAction(_ -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar archivo");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                    new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"),
                    new FileChooser.ExtensionFilter("PDF", "*.pdf")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                archivoSeleccionado[0] = file;
            }
        });



        Button btnAgregar = new Button("Añadir");
        btnAgregar.setDefaultButton(true);
        btnAgregar.setPrefSize(140, 50);
        btnAgregar.setStyle("-fx-font-size: 20px;");
        btnAgregar.setOnAction(_ -> {
            try {
                String tipo = tipoBox.getValue();
                double cantidad = parseCantidad(cantidadField.getText());
                String desc = descripcionField.getText();
                String fecha = LocalDate.now().toString();


                String archivoNombre = (archivoSeleccionado[0] != null) ? archivoSeleccionado[0].getName() : null;
                Movimiento m = new Movimiento(tipo, cantidad, desc, fecha, archivoNombre);
                MovimientoDAO.agregarMovimiento(m);


                // Si el usuario adjuntó un archivo
                if (archivoSeleccionado[0] != null) {
                    try {
                        // Crear carpeta si no existe
                        Path carpetaAdjuntos = Paths.get("data/adjuntos");
                        if (!Files.exists(carpetaAdjuntos)) {
                            Files.createDirectories(carpetaAdjuntos);
                        }

                        // Nombre de archivo único con el ID del movimiento (una vez insertado)
                        int idMovimiento = MovimientoDAO.obtenerUltimoId(); // deberás tener este método en el DAO
                        String nombreArchivo = idMovimiento + "_" + archivoSeleccionado[0].getName();

                        Path destino = carpetaAdjuntos.resolve(nombreArchivo);
                        Files.copy(archivoSeleccionado[0].toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

                        MovimientoDAO.actualizarArchivo(idMovimiento, nombreArchivo);
                        archivoSeleccionado[0] = null;
                    } catch (IOException e) {
                        showError("Error al guardar el archivo adjunto");
                    }
                }


                // Actualizar saldo acumulado
                double saldoActual = MovimientoDAO.obtenerSaldo();
                if (tipo.equals("Ingreso")) saldoActual += cantidad;
                else if (tipo.equals("Gasto")) saldoActual -= cantidad;
                MovimientoDAO.actualizarSaldo(saldoActual); // Método que guarda el saldo en BD

                actualizarTablas();
                cantidadField.clear();
                descripcionField.clear();
            } catch (NumberFormatException | ParseException ex) {
                showError("Cantidad no válida");
            }
        });


        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setAlignment(Pos.CENTER);
        formGrid.addRow(0, tipoBox, cantidadField, descripcionField, btnAdjuntar, btnAgregar);

        // === BOTONES DE ACCIÓN ===
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        Button btnAjustar = new Button("Ajustar saldo");

        Button[] botones = {btnEditar, btnEliminar, btnAjustar};
        for (Button b : botones) {
            b.setPrefSize(140, 50);
            b.setStyle("-fx-font-size: 16px;");
        }

        btnEditar.setOnAction(_ -> editarMovimiento());
        btnEliminar.setOnAction(_ -> eliminarMovimiento());
        btnAjustar.setOnAction(_ -> ajustarSaldo());

        HBox acciones = new HBox(15, btnEditar, btnEliminar, btnAjustar);
        acciones.setAlignment(Pos.CENTER);
        acciones.setPadding(new Insets(10, 0, 20, 0));

        // === SALDO ===
        lblSaldo = new Label("Saldo: 0,00 €");
        lblSaldo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        HBox saldoBox = new HBox(lblSaldo);
        saldoBox.setAlignment(Pos.CENTER);
        saldoBox.setPadding(new Insets(10, 0, 10, 0));

        VBox parteSuperior = new VBox(10, encabezado, formGrid, acciones, saldoBox);
        parteSuperior.setAlignment(Pos.CENTER);
        parteSuperior.setPadding(new Insets(10));
        parteSuperior.setStyle("-fx-border-color: gray; -fx-border-width: 0 0 2 0;");

        // === CONTROLES DE FILTRO ===
        ComboBox<String> tipoFiltro = new ComboBox<>();
        tipoFiltro.getItems().addAll("Todos", "Ingreso", "Gasto", "Ajuste");
        tipoFiltro.setValue("Todos");


        TextField descripcionFiltro = new TextField();
        descripcionFiltro.setPromptText("Descripción");

        TextField minCantidadFiltro = new TextField();
        minCantidadFiltro.setPromptText("Mínimo");

        TextField maxCantidadFiltro = new TextField();
        maxCantidadFiltro.setPromptText("Máximo");

        DatePicker fechaDesdeFiltro = new DatePicker();
        fechaDesdeFiltro.setPromptText("Desde");

        DatePicker fechaHastaFiltro = new DatePicker();
        fechaHastaFiltro.setPromptText("Hasta");

        HBox filtros = new HBox(10, tipoFiltro, descripcionFiltro, minCantidadFiltro, maxCantidadFiltro, fechaDesdeFiltro, fechaHastaFiltro);
        filtros.setPadding(new Insets(10));
        filtros.setAlignment(Pos.CENTER);
        filtros.setStyle("-fx-border-color: gray; -fx-border-radius: 5; -fx-padding: 5;");

        // === TABLA DE MOVIMIENTOS ===
        tablaMovimientos = new TableView<>();
        tablaMovimientos.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<Movimiento, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTipo()));
        colTipo.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.10));

        TableColumn<Movimiento, String> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(nf.format(d.getValue().getCantidad())));
        colCantidad.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.15));

        TableColumn<Movimiento, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDescripcion()));
        colDescripcion.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.45));

        TableColumn<Movimiento, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFecha()));
        colFecha.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.15));

        TableColumn<Movimiento, Void> colArchivo = new TableColumn<>("Archivo");
        colArchivo.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.20)); // algo más ancho

        colArchivo.setCellFactory(col -> new TableCell<>() {
            private final Button btnAbrir = new Button("\uD83D\uDD0D");
            private final Button btnCambiar = new Button("📎");
            private final Button btnBorrar = new Button("❌");
            private final HBox contenedor = new HBox(5, btnAbrir, btnCambiar, btnBorrar);

            {
                contenedor.setAlignment(Pos.CENTER);

                // === ABRIR ===
                btnAbrir.setOnAction(_ -> {
                    Movimiento mov = getTableView().getItems().get(getIndex());
                    if (mov.getArchivo() == null || mov.getArchivo().isEmpty()) {
                        showError("Este movimiento no tiene archivo adjunto.");
                        return;
                    }

                    Path archivo = Paths.get("data/adjuntos", mov.getArchivo());
                    if (!Files.exists(archivo)) {
                        showError("El archivo no existe: " + archivo);
                        return;
                    }

                    try {
                        java.awt.Desktop.getDesktop().open(archivo.toFile());
                    } catch (IOException e) {
                        showError("No se pudo abrir el archivo.");
                    }
                });

                // === CAMBIAR ===
                btnCambiar.setOnAction(_ -> {
                    Movimiento mov = getTableView().getItems().get(getIndex());

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Seleccionar nuevo archivo");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                            new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"),
                            new FileChooser.ExtensionFilter("PDF", "*.pdf")
                    );
                    File nuevoArchivo = fileChooser.showOpenDialog(getScene().getWindow());
                    if (nuevoArchivo != null) {
                        try {
                            Path carpetaAdjuntos = Paths.get("data/adjuntos");
                            if (!Files.exists(carpetaAdjuntos)) {
                                Files.createDirectories(carpetaAdjuntos);
                            }

                            String nombreArchivoNuevo = mov.getId() + "_" + nuevoArchivo.getName();
                            Path destino = carpetaAdjuntos.resolve(nombreArchivoNuevo);
                            Files.copy(nuevoArchivo.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

                            MovimientoDAO.actualizarArchivo(mov.getId(), nombreArchivoNuevo);
                            actualizarTablas();

                        } catch (IOException e) {
                            showError("Error al cambiar el archivo adjunto.");
                        }
                    }
                });

                // === BORRAR ===
                btnBorrar.setOnAction(_ -> {
                    Movimiento mov = getTableView().getItems().get(getIndex());
                    if (mov.getArchivo() == null || mov.getArchivo().isEmpty()) {
                        showError("No hay archivo que borrar.");
                        return;
                    }

                    Path archivo = Paths.get("data/adjuntos", mov.getArchivo());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "¿Borrar el archivo adjunto "+ mov.getArchivo() +" de este movimiento?",
                            ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> res = confirm.showAndWait();

                    if (res.isPresent() && res.get() == ButtonType.YES) {
                        try {
                            Files.deleteIfExists(archivo);
                            MovimientoDAO.actualizarArchivo(mov.getId(), null);
                            actualizarTablas();
                        } catch (IOException e) {
                            showError("Error al borrar el archivo.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(contenedor);
                }
            }
        });
        colArchivo.prefWidthProperty().bind(tablaMovimientos.widthProperty().multiply(0.15));


        tablaMovimientos.getColumns().addAll(colTipo, colCantidad, colDescripcion, colFecha, colArchivo);


// === TABLA DE LOGS ===
        tablaLogs = new TableView<>();
        tablaLogs.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<Log, String> colAccion = new TableColumn<>("Acción");
        colAccion.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getAccion()));

        TableColumn<Log, String> colFechaHora = new TableColumn<>("Fecha/Hora");
        colFechaHora.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFechaHora()));
        // Asignar porcentaje del ancho
        colAccion.prefWidthProperty().bind(tablaLogs.widthProperty().multiply(0.75));
        colFechaHora.prefWidthProperty().bind(tablaLogs.widthProperty().multiply(0.25));

        tablaLogs.getColumns().addAll(colAccion, colFechaHora);

// === LISTA FILTRADA Y ORDENADA ===
        movimientos = FXCollections.observableArrayList(MovimientoDAO.listarMovimientos());
        FilteredList<Movimiento> filtrados = new FilteredList<>(movimientos, _ -> true);

        SortedList<Movimiento> ordenados = new SortedList<>(filtrados, (m1, m2) -> {
            int cmp = LocalDate.parse(m2.getFecha()).compareTo(LocalDate.parse(m1.getFecha())); // fecha descendente
            if (cmp == 0) return Integer.compare(m2.getId(), m1.getId()); // id descendente
            return cmp;
        });

        tablaMovimientos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Movimiento item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle(""); // fila vacía o nula, sin estilo
                } else {
                    // Colores según tipo de movimiento
                    switch (item.getTipo()) {
                        case "Ingreso" -> setStyle("-fx-background-color: #C8E6C9;");  // verde claro
                        case "Gasto" -> setStyle("-fx-background-color: #FFCDD2;");    // rojo claro
                        case "Ajuste" -> setStyle("-fx-background-color: #FFF9C4;");   // amarillo claro
                        default -> setStyle("");
                    }
                }
            }
        });

        tablaMovimientos.setItems(ordenados);
        colFecha.setSortType(TableColumn.SortType.ASCENDING);
        tablaMovimientos.getSortOrder().add(colFecha);
        tablaMovimientos.sort();


        Runnable actualizarFiltro = () -> filtrados.setPredicate(m -> {
            if (!tipoFiltro.getValue().equals("Todos") && !m.getTipo().equals(tipoFiltro.getValue()))
                return false;

            if (!descripcionFiltro.getText().isEmpty() &&
                    !m.getDescripcion().toLowerCase().contains(descripcionFiltro.getText().toLowerCase()))
                return false;

            double min, max;
            try {
                min = minCantidadFiltro.getText().isEmpty() ? Double.MIN_VALUE : Double.parseDouble(minCantidadFiltro.getText());
            } catch (NumberFormatException e) {
                min = Double.MIN_VALUE;
            }

            try {
                max = maxCantidadFiltro.getText().isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxCantidadFiltro.getText());
            } catch (NumberFormatException e) {
                max = Double.MAX_VALUE;
            }

            if (m.getCantidad() < min || m.getCantidad() > max)
                return false;

            LocalDate fechaDesde = fechaDesdeFiltro.getValue();
            LocalDate fechaHasta = fechaHastaFiltro.getValue();
            LocalDate fechaMovimiento = LocalDate.parse(m.getFecha());

            if (fechaDesde != null && fechaMovimiento.isBefore(fechaDesde))
                return false;
            if (fechaHasta != null && fechaMovimiento.isAfter(fechaHasta))
                return false;

            return true;
        });


        tipoFiltro.valueProperty().addListener((_, _, _) -> actualizarFiltro.run());
        descripcionFiltro.textProperty().addListener((_, _, _) -> actualizarFiltro.run());
        minCantidadFiltro.textProperty().addListener((_, _, _) -> actualizarFiltro.run());
        maxCantidadFiltro.textProperty().addListener((_, _, _) -> actualizarFiltro.run());
        fechaDesdeFiltro.valueProperty().addListener((_, _, _) -> actualizarFiltro.run());
        fechaHastaFiltro.valueProperty().addListener((_, _, _) -> actualizarFiltro.run());

        // === SPLITPANE PARA LAS TABLAS ===
        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(tablaMovimientos, tablaLogs);
        split.setDividerPositions(0.5); // 50%-50%

        // === LAYOUT PRINCIPAL ===
        VBox layoutCentro = new VBox(10, parteSuperior, filtros, split);
        layoutCentro.setPadding(new Insets(15));
        layoutCentro.setAlignment(Pos.TOP_CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(layoutCentro);

        Scene scene = new Scene(root, 1200, 900);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("estilo.css")).toExternalForm());
        stage.setTitle("Gestión de Cuenta");
        stage.setScene(scene);
        stage.show();

        actualizarTablas();
    }

    private void actualizarTablas() {
        movimientos.setAll(MovimientoDAO.listarMovimientos()); // recarga desde DB
        tablaLogs.getItems().setAll(LogDAO.listarLogs());
        lblSaldo.setText("Saldo: " + nf.format(MovimientoDAO.obtenerSaldo()));

        // recalcular saldo leyendo todos los movimientos
        double saldo = MovimientoDAO.obtenerSaldo();
        lblSaldo.setText("Saldo: " + nf.format(saldo));

        // color según positivo/negativo
        if (saldo < 0) {
            lblSaldo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: red;");
        } else {
            lblSaldo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: black;");
        }
    }


    private double parseCantidad(String text) throws ParseException {
        text = text.trim().replace(',', '.');
        return Double.parseDouble(text);
    }

    private void showError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.show();
    }

    // ---- Métodos de acción ----
    // EDITAR movimiento seleccionado
    private void editarMovimiento() {
        Movimiento seleccionado = tablaMovimientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            showError("Seleccione un movimiento para editar");
            return;
        }

        if ("Ajuste".equals(seleccionado.getTipo()) && MovimientoDAO.hayMovimientosPosteriores(seleccionado)) {
            showError("No se puede editar un ajuste si hay movimientos posteriores");
            return;
        }

        // Dialog personalizado
        Dialog<Movimiento> dialog = new Dialog<>();
        dialog.setTitle("Editar movimiento");
        dialog.setHeaderText("Modifique los valores y pulse Aceptar");

        ButtonType aceptarButtonType = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(aceptarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<String> tipoField = new ComboBox<>();
        if ("Ajuste".equals(seleccionado.getTipo())) {
            // Si es un ajuste, solo puede seguir siendo ajuste y no se puede cambiar
            tipoField.getItems().add("Ajuste");
            tipoField.setValue("Ajuste");
            tipoField.setDisable(true);
        } else {
            // Si no es un ajuste, solo puede ser Ingreso o Gasto
            tipoField.getItems().addAll("Ingreso", "Gasto");
            tipoField.setValue(seleccionado.getTipo());
        }


        TextField cantidadField = new TextField(String.valueOf(seleccionado.getCantidad()));
        TextField descField = new TextField(seleccionado.getDescripcion());

        grid.addRow(0, new Label("Tipo:"), tipoField);
        grid.addRow(1, new Label("Cantidad (€):"), cantidadField);
        grid.addRow(2, new Label("Descripción:"), descField);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == aceptarButtonType) {
                try {
                    double cantidad = parseCantidad(cantidadField.getText());
                    if (cantidad <= 0) {
                        showError("La cantidad debe ser mayor que 0");
                        return null;
                    }
                    return new Movimiento(seleccionado.getId(), tipoField.getValue(), cantidad, descField.getText(), seleccionado.getFecha(), seleccionado.getArchivo());
                } catch (ParseException e) {
                    showError("Cantidad no válida");
                    return null;
                }
            }
            return null;
        });

        Optional<Movimiento> result = dialog.showAndWait();
        result.ifPresent(m -> {
            // Recalcular saldo
            double saldoActual = MovimientoDAO.obtenerSaldo();

            // Quitar efecto del movimiento original
            if (seleccionado.getTipo().equals("Ingreso")) saldoActual -= seleccionado.getCantidad();
            else if (seleccionado.getTipo().equals("Gasto")) saldoActual += seleccionado.getCantidad();

            // Aplicar efecto del nuevo movimiento
            if (m.getTipo().equals("Ingreso")) saldoActual += m.getCantidad();
            else if (m.getTipo().equals("Gasto")) saldoActual -= m.getCantidad();

            MovimientoDAO.actualizarMovimiento(m.getId(), m.getTipo(), m.getCantidad(), m.getDescripcion());
            MovimientoDAO.actualizarSaldo(saldoActual);

            // Registrar la edición
            String logMsg = String.format("Movimiento editado: ID %d, Tipo %s, Cantidad %s, Descripción %s",
                    m.getId(),
                    m.getTipo(),
                    nf.format(m.getCantidad()),
                    m.getDescripcion());
            LogDAO.agregarLog(logMsg);

            actualizarTablas();
        });
    }

    // ELIMINAR movimiento seleccionado
    private void eliminarMovimiento() {
        Movimiento seleccionado = tablaMovimientos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            showError("Seleccione un movimiento para eliminar");
            return;
        }

        if (seleccionado.getTipo().equals("Ajuste") && MovimientoDAO.hayMovimientosPosteriores(seleccionado)) {
            showError("No se puede eliminar un ajuste que tiene movimientos posteriores.");
            return;
        }

        String mensaje = "¿Eliminar movimiento seleccionado?\n\n"
                + "Tipo: " + seleccionado.getTipo() + "\n"
                + "Cantidad: " + nf.format(seleccionado.getCantidad()) + "\n"
                + "Descripción: " + seleccionado.getDescripcion();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            MovimientoDAO.eliminarMovimiento(seleccionado.getId());
            actualizarTablas();
        }
    }



    // AJUSTAR saldo usando ajustarSaldoDirecto
    private void ajustarSaldo() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Ajustar saldo");
        dialog.setContentText("Ingrese el saldo real actual (€):");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            double saldoDeseado = parseCantidad(result.get());
            MovimientoDAO.ajustarSaldoDirecto(saldoDeseado); // llama al método estático
            actualizarTablas();
        } catch (ParseException e) {
            showError("Cantidad no válida");
        }
    }



    public static void main(String[] args) {
        launch(args);
    }
}
