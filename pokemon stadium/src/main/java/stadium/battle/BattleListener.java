package stadium.battle;

/**
 * Eventos del combate. Desacopla la lógica (Battle) de la interfaz.
 * OJO: se invocan desde el hilo del combate, NO desde el EDT; la UI debe
 * usar SwingUtilities.invokeLater antes de tocar componentes.
 */
public interface BattleListener {
    /** Un Pokémon atacó. modifier: 1.3 súper efectivo, 0.7 poco efectivo, 1.0 neutro. */
    void onTurn(String attacker, String defender, int damage, boolean critical, double modifier);

    /** El HP de un Pokémon cambió (también se emite al iniciar, con HP completo). */
    void onHpChanged(String pokemon, int hpActual);

    /** El combate terminó. */
    void onBattleEnded(String winner);
}
