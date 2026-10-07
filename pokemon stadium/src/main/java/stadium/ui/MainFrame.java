package stadium.ui;

import stadium.api.PokeApiClient;
import stadium.battle.Battle;
import stadium.battle.BattleListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Locale;

/**
 * Ventana principal. Implementa BattleListener: el estado del combate (HP, log, ganador)
 * se refleja en pantalla SOLO a partir de los eventos del Battle.
 */
public class MainFrame extends JFrame implements BattleListener {
    private final PokemonPanel panelA;
    private final PokemonPanel panelB;
    private final JButton fightButton = new JButton("Fight!");
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");

    private Battle currentBattle;

    public MainFrame() {
        super("Pokémon Stadium");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        setContentPane(root);

        PokeApiClient client = new PokeApiClient();
        panelA = new PokemonPanel("Jugador 1", Theme.P1, client, this::showError, this::onPokemonLoaded);
        panelB = new PokemonPanel("Jugador 2", Theme.P2, client, this::showError, this::onPokemonLoaded);

        // Centro: VS + botón Fight!
        JLabel vs = new JLabel("VS", SwingConstants.CENTER);
        vs.setFont(Theme.BOLD.deriveFont(28f));
        vs.setForeground(Theme.TEXT);
        vs.setAlignmentX(Component.CENTER_ALIGNMENT);

        fightButton.setEnabled(false); // deshabilitado hasta que ambos estén cargados
        Theme.styleButton(fightButton, Theme.FIGHT);
        fightButton.setFont(Theme.BOLD.deriveFont(18f));
        Dimension fightSize = new Dimension(120, 48);
        fightButton.setPreferredSize(fightSize);
        fightButton.setMaximumSize(fightSize);
        fightButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        fightButton.addActionListener(e -> startFight());

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(Box.createVerticalGlue());
        center.add(vs);
        center.add(Box.createVerticalStrut(12));
        center.add(fightButton);
        center.add(Box.createVerticalGlue());

        // Fichas a los lados; se estiran con la ventana
        JPanel arena = new JPanel(new GridBagLayout());
        arena.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridx = 0; gbc.weightx = 1; gbc.insets = new Insets(0, 0, 0, 10);
        arena.add(panelA.getMainPanel(), gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        arena.add(center, gbc);
        gbc.gridx = 2; gbc.weightx = 1; gbc.insets = new Insets(0, 0, 0, 0);
        arena.add(panelB.getMainPanel(), gbc);

        // Log desplazable + barra de estado
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(Theme.TEXT);
        logArea.setMargin(new Insets(4, 8, 4, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(100, 110));
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1, true), " Log de batalla ",
                TitledBorder.LEFT, TitledBorder.TOP, Theme.BOLD.deriveFont(12f), Theme.LABEL));

        statusLabel.setFont(Theme.BOLD);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        setStatus("Carga dos Pokémon para comenzar.", Theme.INFO);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.add(logScroll, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);

        root.add(arena, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        // Tamaño ajustado a la pantalla (también en portátiles pequeños); la ventana se puede redimensionar
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(940, (int) (screen.width * 0.92)), Math.min(720, (int) (screen.height * 0.88)));
        setMinimumSize(new Dimension(780, 540));
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- acciones (siempre en el EDT)

    private void onPokemonLoaded() {
        setStatus("Pokémon cargado correctamente.", Theme.OK);
        log("Cargado: " + lastLoadedInfo());
        fightButton.setEnabled(panelA.getPokemon() != null && panelB.getPokemon() != null);
    }

    private String lastLoadedInfo() {
        String a = panelA.getPokemon() == null ? "?" : panelA.getPokemon().getName();
        String b = panelB.getPokemon() == null ? "?" : panelB.getPokemon().getName();
        return "J1 = " + a + " | J2 = " + b;
    }

    private void startFight() {
        fightButton.setEnabled(false);
        panelA.setLocked(true);
        panelB.setLocked(true);
        logArea.setText("");
        setStatus("¡Combate en curso!", Theme.WIN);

        currentBattle = new Battle(panelA.getPokemon(), panelB.getPokemon(), this);
        log("=== " + currentBattle.getLabelA() + " vs " + currentBattle.getLabelB() + " ===");
        currentBattle.start();
    }

    private void showError(String message) {
        setStatus("⚠ " + message, Theme.BAD);
        log("[ERROR] " + message);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }

    private void log(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // auto-scroll
    }

    // ---------------------------------------------------------------- BattleListener
    // Llegan desde el hilo del combate -> se pasan al EDT con invokeLater.

    @Override
    public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(attacker).append(" ataca a ").append(defender)
              .append(" y causa ").append(damage).append(" de daño");
            if (critical) sb.append(" ¡CRÍTICO!");
            if (modifier > 1.0) sb.append(String.format(Locale.US, " (súper efectivo x%.1f)", modifier));
            else if (modifier < 1.0) sb.append(String.format(Locale.US, " (poco efectivo x%.1f)", modifier));
            log(sb.toString());
        });
    }

    @Override
    public void onHpChanged(String pokemon, int hpActual) {
        SwingUtilities.invokeLater(() -> {
            if (pokemon.equals(currentBattle.getLabelA())) panelA.updateHp(hpActual);
            else if (pokemon.equals(currentBattle.getLabelB())) panelB.updateHp(hpActual);
            log("   HP de " + pokemon + ": " + hpActual);
        });
    }

    @Override
    public void onBattleEnded(String winner) {
        SwingUtilities.invokeLater(() -> {
            log("*** ¡" + winner + " gana el combate! ***");
            setStatus("Ganador: " + winner, Theme.WIN);
            panelA.setLocked(false);
            panelB.setLocked(false);
            fightButton.setEnabled(true); // permite revancha (el HP se restaura al iniciar)
        });
    }
}
