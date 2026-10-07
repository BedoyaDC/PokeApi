package stadium.model;

import java.awt.Image;
import java.util.List;

/** Modelo de un Pokémon: datos básicos, stats, sprite y HP actual. */
public class Pokemon {
    private final int id;
    private final String name;
    private final List<String> types;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private final int weightHg; // PokeAPI: hectogramos
    private final int heightDm; // PokeAPI: decímetros
    private final Image sprite; // puede ser null si la API no trae imagen
    private int currentHp;

    public Pokemon(int id, String name, List<String> types, int hp, int attack,
                   int defense, int speed, int weightHg, int heightDm, Image sprite) {
        this.id = id;
        this.weightHg = weightHg;
        this.heightDm = heightDm;
        this.name = name;
        this.types = types;
        this.maxHp = hp;
        this.currentHp = hp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.sprite = sprite;
    }

    /** Resta daño; el HP nunca baja de 0. */
    public void takeDamage(int damage) {
        currentHp = Math.max(0, currentHp - damage);
    }

    /** Restaura el HP al máximo (se usa al iniciar cada combate). */
    public void heal() {
        currentHp = maxHp;
    }

    public boolean isFainted() { return currentHp <= 0; }

    /** Solo el primer tipo cuenta para la efectividad. */
    public String getPrimaryType() { return types.isEmpty() ? "" : types.get(0); }

    public int getId() { return id; }
    /** Peso en kilogramos (la API lo entrega en hectogramos). */
    public double getWeightKg() { return weightHg / 10.0; }
    /** Altura en metros (la API la entrega en decímetros). */
    public double getHeightM() { return heightDm / 10.0; }
    public String getName() { return name; }
    public List<String> getTypes() { return types; }
    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }
    public Image getSprite() { return sprite; }
}
