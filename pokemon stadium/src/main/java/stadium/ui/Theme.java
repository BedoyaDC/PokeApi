package stadium.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Colores y estilos sencillos compartidos por la interfaz (tema claro con acentos de color). */
final class Theme {
    private Theme() {}

    static final Color BG = new Color(232, 237, 247);          // fondo de la ventana
    static final Color CARD = new Color(250, 251, 255);        // fondo de cada ficha
    static final Color LABEL = new Color(70, 80, 115);         // etiquetas ("ID", "Nombre"...)
    static final Color TEXT = new Color(35, 42, 70);
    static final Color FIELD_BG = new Color(238, 241, 249);
    static final Color FIELD_BORDER = new Color(190, 198, 222);
    static final Color P1 = new Color(211, 47, 47);            // jugador 1: rojo
    static final Color P2 = new Color(25, 118, 210);           // jugador 2: azul
    static final Color FIGHT = new Color(245, 124, 0);         // botón Fight!
    static final Color DISABLED = new Color(176, 182, 200);
    static final Color OK = new Color(46, 125, 50);
    static final Color BAD = new Color(198, 40, 40);
    static final Color INFO = new Color(21, 101, 192);
    static final Color WIN = new Color(230, 126, 0);

    static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    // Colores aproximados de cada tipo de Pokémon
    private static final Map<String, Color> TYPE_COLORS = new HashMap<>();
    static {
        String[][] t = {
                {"normal", "A8A878"}, {"fire", "F08030"}, {"water", "6890F0"}, {"grass", "78C850"},
                {"electric", "F8D030"}, {"ice", "98D8D8"}, {"fighting", "C03028"}, {"poison", "A040A0"},
                {"ground", "E0C068"}, {"flying", "A890F0"}, {"psychic", "F85888"}, {"bug", "A8B820"},
                {"rock", "B8A038"}, {"ghost", "705898"}, {"dragon", "7038F8"}, {"dark", "705848"},
                {"steel", "B8B8D0"}, {"fairy", "EE99AC"}
        };
        for (String[] e : t) TYPE_COLORS.put(e[0], Color.decode("#" + e[1]));
    }

    static Color typeColor(String type) {
        return TYPE_COLORS.getOrDefault(type == null ? "" : type.toLowerCase(Locale.ROOT), FIELD_BG);
    }

    /** Texto blanco u oscuro, según cuál se lea mejor sobre el fondo dado. */
    static Color contrast(Color bg) {
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return lum > 0.6 ? new Color(30, 30, 40) : Color.WHITE;
    }

    /** Botón de color con efecto hover y estado deshabilitado. Llamar DESPUÉS de fijar enabled inicial. */
    static void styleButton(JButton b, Color base) {
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(BOLD);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(b.isEnabled() ? base : DISABLED);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(base.brighter()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(b.isEnabled() ? base : DISABLED); }
        });
        b.addPropertyChangeListener("enabled", e -> b.setBackground(b.isEnabled() ? base : DISABLED));
    }

    static void styleField(JTextField f, Color borderColor) {
        f.setOpaque(true);
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT);
        f.setDisabledTextColor(LABEL);
        f.setFont(BOLD);
        f.setBorder(fieldBorder(borderColor));
    }

    static Border fieldBorder(Color c) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(c, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6));
    }
}
