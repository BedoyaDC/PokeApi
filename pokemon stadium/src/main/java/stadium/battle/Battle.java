package stadium.battle;

import stadium.model.Pokemon;

import java.util.Random;

/**
 * Reglas del combate por turnos. Corre en su propio hilo para no bloquear la UI
 * y comunica todo mediante un {@link BattleListener}.
 *
 * FÓRMULA DE DAÑO (documentada):
 *   base   = ATK * rand(0.6 - 1.0) - DEF * rand(0.2 - 0.5)
 *   dano   = max(1, base * 0.6) * efectividad * (crítico ? 1.5 : 1)
 * El factor 0.6 alarga el combate a ~3-5 turnos; el mínimo de 1 evita combates infinitos.
 * Crítico: 10 % de probabilidad. Efectividad (solo primer tipo):
 *   agua>fuego, fuego>planta, planta>agua -> x1.3; la relación inversa -> x0.7; resto x1.0.
 */
public class Battle {
    private static final double CRIT_CHANCE = 0.10;
    private static final double CRIT_MULTIPLIER = 1.5;
    private static final double SUPER_EFFECTIVE = 1.3;
    private static final double NOT_EFFECTIVE = 0.7;
    private static final double DAMAGE_SCALE = 0.6;
    private static final long TURN_DELAY_MS = 900;

    private final Pokemon a;
    private final Pokemon b;
    private final String labelA;
    private final String labelB;
    private final BattleListener listener;
    private final Random random = new Random();
    private volatile boolean running = false;

    public Battle(Pokemon a, Pokemon b, BattleListener listener) {
        this.a = a;
        this.b = b;
        this.listener = listener;
        // Si ambos Pokémon se llaman igual, los distinguimos para que los eventos sean inequívocos.
        boolean sameName = a.getName().equals(b.getName());
        this.labelA = sameName ? a.getName() + " (1)" : a.getName();
        this.labelB = sameName ? b.getName() + " (2)" : b.getName();
    }

    public String getLabelA() { return labelA; }
    public String getLabelB() { return labelB; }

    /** Inicia el combate en un hilo aparte. Ignora la llamada si ya está corriendo. */
    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::run, "battle-thread");
        t.setDaemon(true);
        t.start();
    }

    private void run() {
        try {
            a.heal();
            b.heal();
            listener.onHpChanged(labelA, a.getCurrentHp());
            listener.onHpChanged(labelB, b.getCurrentHp());

            // Inicia el más rápido; si empatan, aleatorio.
            boolean aTurn = a.getSpeed() != b.getSpeed() ? a.getSpeed() > b.getSpeed() : random.nextBoolean();

            while (!a.isFainted() && !b.isFainted()) {
                Pokemon attacker = aTurn ? a : b;
                Pokemon defender = aTurn ? b : a;
                String attackerLabel = aTurn ? labelA : labelB;
                String defenderLabel = aTurn ? labelB : labelA;

                boolean critical = random.nextDouble() < CRIT_CHANCE;
                double modifier = effectiveness(attacker.getPrimaryType(), defender.getPrimaryType());
                int damage = computeDamage(attacker, defender, critical, modifier);

                defender.takeDamage(damage);
                listener.onTurn(attackerLabel, defenderLabel, damage, critical, modifier);
                listener.onHpChanged(defenderLabel, defender.getCurrentHp());

                aTurn = !aTurn;
                Thread.sleep(TURN_DELAY_MS); // pausa para que se pueda "ver" el combate
            }

            listener.onBattleEnded(a.isFainted() ? labelB : labelA);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            running = false;
        }
    }

    private int computeDamage(Pokemon attacker, Pokemon defender, boolean critical, double modifier) {
        double atk = attacker.getAttack() * (0.6 + 0.4 * random.nextDouble());
        double def = defender.getDefense() * (0.2 + 0.3 * random.nextDouble());
        double damage = Math.max(1.0, (atk - def) * DAMAGE_SCALE) * modifier;
        if (critical) damage *= CRIT_MULTIPLIER;
        return Math.max(1, (int) Math.round(damage));
    }

    /** Efectividad simple basada solo en el primer tipo de cada Pokémon. */
    static double effectiveness(String attackType, String defendType) {
        if (beats(attackType, defendType)) return SUPER_EFFECTIVE;
        if (beats(defendType, attackType)) return NOT_EFFECTIVE;
        return 1.0;
    }

    private static boolean beats(String x, String y) {
        return (x.equals("water") && y.equals("fire"))
                || (x.equals("fire") && y.equals("grass"))
                || (x.equals("grass") && y.equals("water"));
    }
}
