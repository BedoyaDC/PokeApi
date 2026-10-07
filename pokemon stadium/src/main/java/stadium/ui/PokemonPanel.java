package stadium.ui;

import stadium.api.PokeApiClient;
import stadium.api.PokeApiException;
import stadium.model.Pokemon;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Ficha ("Pokédex") de un jugador. La estructura se diseña en PokemonPanel.form
 * (GUI Designer de IntelliJ); los campos de abajo están ENLAZADOS por nombre a ese formulario,
 * e IntelliJ los inicializa automáticamente al construir el objeto. El aspecto (colores,
 * fuentes, bordes) se aplica por código con {@link Theme}.
 *
 * Las consultas a la API se hacen en un SwingWorker para no congelar la UI.
 */
public class PokemonPanel {
    @FunctionalInterface
    private interface Fetcher {
        Pokemon get() throws PokeApiException;
    }

    // ---- Componentes enlazados con PokemonPanel.form (no los inicialices con new) ----
    private JPanel mainPanel;
    private JButton randomButton;
    private JTextField searchField;
    private JButton loadButton;
    private JLabel spriteLabel;
    private JTextField idField;
    private JTextField nameField;
    private JTextField weightField;
    private JTextField heightField;
    private JTextField typesField;
    private JTextField hpField;
    private JTextField attackField;
    private JTextField defenseField;
    private JTextField speedField;
    private JProgressBar hpBar;

    private final PokeApiClient client;
    private final Consumer<String> errorSink;
    private final Runnable onLoaded;
    private final Color accent;

    private Pokemon pokemon;
    private boolean loading = false;
    private boolean locked = false;

    public PokemonPanel(String title, Color accent, PokeApiClient client,
                        Consumer<String> errorSink, Runnable onLoaded) {
        this.client = client;
        this.errorSink = errorSink;
        this.onLoaded = onLoaded;
        this.accent = accent;

        if (mainPanel == null) {
            throw new IllegalStateException("El formulario PokemonPanel.form no fue inicializado. "
                    + "Ejecuta desde IntelliJ (Build and run using: IntelliJ IDEA) y revisa "
                    + "Settings > Editor > GUI Designer.");
        }

        applyTheme(title);

        // ---- ActionListeners ----
        randomButton.addActionListener(e -> load(client::fetchRandom));
        loadButton.addActionListener(e -> load(() -> client.fetchByName(searchField.getText())));
        searchField.addActionListener(e -> load(() -> client.fetchByName(searchField.getText()))); // Enter

        // El sprite se reescala cuando cambia el tamaño disponible (ventana redimensionada)
        spriteLabel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { renderSprite(); }
        });
    }

    public JPanel getMainPanel() { return mainPanel; }

    // ---------------------------------------------------------------- aspecto

    private void applyTheme(String title) {
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Theme.CARD);
        TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(accent, 2, true), " " + title + " ",
                TitledBorder.CENTER, TitledBorder.TOP, Theme.BOLD.deriveFont(14f), accent);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                titled, BorderFactory.createEmptyBorder(0, 6, 6, 6)));

        // Etiquetas fijas ("ID", "Nombre", ...)
        for (Component c : mainPanel.getComponents()) {
            if (c instanceof JLabel && c != spriteLabel) {
                c.setForeground(Theme.LABEL);
                c.setFont(Theme.BOLD.deriveFont(12f));
            }
        }

        // Campos de datos (solo lectura, centrados)
        for (JTextField f : new JTextField[]{idField, nameField, weightField, heightField,
                typesField, hpField, attackField, defenseField, speedField}) {
            Theme.styleField(f, Theme.FIELD_BORDER);
            f.setEditable(false);
            f.setHorizontalAlignment(SwingConstants.CENTER);
        }
        nameField.setFont(Theme.BOLD.deriveFont(14f));
        nameField.setForeground(accent);

        // Campo de búsqueda y botones con el color del jugador
        Theme.styleField(searchField, accent);
        searchField.setHorizontalAlignment(SwingConstants.CENTER);
        searchField.setToolTipText("Nombre del Pokémon (ej. pikachu) y Enter");
        Theme.styleButton(randomButton, accent);
        Theme.styleButton(loadButton, accent);

        // Sprite
        spriteLabel.setOpaque(true);
        spriteLabel.setBackground(Color.WHITE);
        spriteLabel.setForeground(Theme.LABEL);
        spriteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        spriteLabel.setBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1, true));
        spriteLabel.setText("Elige un Pokémon");

        // Barra de HP (UI básica para que respete los colores)
        hpBar.setUI(new BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return new Color(30, 30, 45); } // texto sobre la parte vacía
            @Override protected Color getSelectionForeground() { return Color.WHITE; }          // texto sobre la parte llena
        });
        hpBar.setBackground(new Color(214, 219, 234));
        hpBar.setBorder(BorderFactory.createLineBorder(new Color(150, 158, 190), 1, true));
        hpBar.setFont(Theme.BOLD.deriveFont(14f));
        hpBar.setStringPainted(true);
        hpBar.setValue(0);
        hpBar.setString("HP");
    }

    // ---------------------------------------------------------------- carga

    /** Consulta la API en un hilo de fondo y actualiza la UI al terminar. */
    private void load(Fetcher fetcher) {
        setLoading(true);
        new SwingWorker<Pokemon, Void>() {
            @Override
            protected Pokemon doInBackground() throws Exception {
                return fetcher.get(); // hilo de fondo
            }

            @Override
            protected void done() { // de vuelta en el EDT
                try {
                    showPokemon(get());
                    onLoaded.run();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    errorSink.accept(cause instanceof PokeApiException
                            ? cause.getMessage() : "Error inesperado: " + cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    setLoading(false);
                }
            }
        }.execute();
    }

    private Image spriteSource; // imagen original (96x96 aprox.) para reescalar sin perder nitidez

    /** Muestra un Pokémon recién cargado (HP completo). */
    private void showPokemon(Pokemon p) {
        this.pokemon = p;
        this.spriteSource = p.getSprite();
        if (spriteSource != null) {
            spriteLabel.setText(null);
            renderSprite();
        } else {
            spriteLabel.setIcon(null);
            spriteLabel.setText("(sin sprite)");
        }
        idField.setText("#" + p.getId());
        nameField.setText(p.getName().toUpperCase(Locale.ROOT));
        weightField.setText(String.format(Locale.US, "%.1f", p.getWeightKg()));
        heightField.setText(String.format(Locale.US, "%.1f", p.getHeightM()));
        typesField.setText(String.join(" / ", p.getTypes()).toUpperCase(Locale.ROOT));
        Color typeColor = Theme.typeColor(p.getPrimaryType()); // el tipo se pinta con su color
        typesField.setBackground(typeColor);
        typesField.setForeground(Theme.contrast(typeColor));
        hpField.setText(String.valueOf(p.getMaxHp()));
        attackField.setText(String.valueOf(p.getAttack()));
        defenseField.setText(String.valueOf(p.getDefense()));
        speedField.setText(String.valueOf(p.getSpeed()));
        resetHpBar(p.getMaxHp());
    }

    /** Dibuja el sprite del tamaño del espacio disponible, con escalado "pixel perfect". */
    private void renderSprite() {
        if (spriteSource == null) return;
        int w = spriteSource.getWidth(null);
        int h = spriteSource.getHeight(null);
        if (w <= 0 || h <= 0) return;

        int box = Math.min(spriteLabel.getWidth(), spriteLabel.getHeight()) - 12;
        if (box < 64) box = 160;          // antes del primer layout
        box = Math.min(box, 320);         // tope razonable
        double scale = Math.min((double) box / w, (double) box / h);
        int nw = Math.max(1, (int) (w * scale));
        int nh = Math.max(1, (int) (h * scale));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(spriteSource, 0, 0, nw, nh, null);
        g.dispose();
        spriteLabel.setIcon(new ImageIcon(out));
    }

    private javax.swing.Timer hpTimer;  // animación de la barra
    private int hpShown;                // valor que se está mostrando ahora mismo

    /** Pone la barra directamente al HP completo (Pokémon recién cargado o inicio de combate). */
    private void resetHpBar(int max) {
        if (hpTimer != null) hpTimer.stop();
        hpBar.setMaximum(max);
        paintHp(max);
    }

    /**
     * Actualiza la barra de HP (la llama la ventana al recibir onHpChanged).
     * Si la vida baja, la barra se desliza suavemente hasta el nuevo valor.
     */
    public void updateHp(int hp) {
        if (pokemon == null) return;
        int max = pokemon.getMaxHp();
        hpBar.setMaximum(max);
        if (hpTimer != null) hpTimer.stop();

        final int target = Math.max(0, Math.min(hp, max));
        if (target >= hpShown) {          // curación / reinicio: sin animación
            paintHp(target);
            return;
        }
        final int step = Math.max(1, (int) Math.ceil((hpShown - target) / 12.0));
        hpTimer = new javax.swing.Timer(35, e -> {
            int next = Math.max(target, hpShown - step);
            paintHp(next);
            if (next == target) ((javax.swing.Timer) e.getSource()).stop();
        });
        hpTimer.start();
    }

    /** Dibuja la barra con un valor concreto y el color según el porcentaje de vida. */
    private void paintHp(int value) {
        hpShown = value;
        int max = hpBar.getMaximum();
        hpBar.setValue(value);
        hpBar.setString(value + " / " + max + " HP");
        double ratio = max == 0 ? 0 : (double) value / max;
        hpBar.setForeground(ratio > 0.5 ? new Color(67, 190, 85)
                : ratio > 0.2 ? new Color(235, 180, 30) : new Color(220, 55, 55));
    }

    private void setLoading(boolean value) {
        loading = value;
        refreshEnabled();
    }

    /** Bloquea los controles mientras hay un combate en curso. */
    public void setLocked(boolean value) {
        locked = value;
        refreshEnabled();
    }

    private void refreshEnabled() {
        boolean enabled = !loading && !locked;
        randomButton.setEnabled(enabled);
        loadButton.setEnabled(enabled);
        searchField.setEnabled(enabled);
    }

    public Pokemon getPokemon() { return pokemon; }
}
