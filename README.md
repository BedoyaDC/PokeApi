# PokeApi
Pokemon stadium lite

# Pokémon Stadium

Mini-aplicación de escritorio en **Java Swing** que simula un combate por turnos entre dos Pokémon
obtenidos en vivo desde [PokeAPI](https://pokeapi.co/api/v2/pokemon/{name}).

Materia: Desarrollo de Software III — Universidad del Valle, Sede Tuluá.

## Requisitos
- Java 11 o superior
- Maven 3.6+ (descarga automáticamente `org.json`)
- Conexión a internet

## Ejecución
Abrir la carpeta como proyecto Maven en **IntelliJ IDEA** y ejecutar la clase `Main`.

> La ficha de cada jugador está diseñada con el **GUI Designer de IntelliJ** (`PokemonPanel.form`).
> Por eso debe compilarse con IntelliJ (*Settings > Build Tools > Maven > Runner* / *Build and run using: IntelliJ IDEA*),
> que es quien procesa los `.form`. Con `mvn exec:java` por consola el formulario no se inicializa.

## Uso
1. En cada lado pulsa **Random** o escribe un nombre (ej. `charmander`) y pulsa **Load** (o Enter).
2. Cuando ambos estén cargados se habilita **Fight!**.
3. Observa el log; al terminar puedes pulsar Fight! otra vez (revancha) o cargar otros Pokémon.

## Diseño
Paquetes: `model` (`Pokemon`), `api` (`PokeApiClient`, `PokeApiException`), `battle` (`Battle`, `BattleListener`)
y `ui` (`MainFrame`, `PokemonPanel` + `PokemonPanel.form`). La ficha muestra ID, nombre, peso, altura, tipos, stats y barra de HP. `PokeApiClient` usa `java.net.http.HttpClient` y `org.json`; sus llamadas
son bloqueantes, por eso la UI las ejecuta siempre en un `SwingWorker`. Los errores (no encontrado, red, JSON)
se muestran en la barra de estado y en el log sin congelar la ventana.

`Battle` contiene las reglas y corre en su propio hilo (con pausa entre turnos). No conoce Swing: notifica
mediante `BattleListener` (`onTurn`, `onHpChanged`, `onBattleEnded`). `MainFrame` implementa esa interfaz y
actualiza la pantalla únicamente desde esos eventos usando `SwingUtilities.invokeLater`.

**Fórmula de daño:** `base = ATK·rand(0.6–1.0) − DEF·rand(0.2–0.5)`; `daño = max(1, base·0.6) · efectividad · crítico`.
Crítico 10 % (x1.5). Efectividad por primer tipo: agua>fuego, fuego>planta, planta>agua (x1.3), inversa (x0.7),
resto x1.0. Inicia el de mayor Speed (empate: aleatorio). El HP nunca baja de 0.

## Capturas de pantalla
# Pokémon Stadium

Mini-aplicación de escritorio en **Java Swing** que simula un combate por turnos entre dos Pokémon
obtenidos en vivo desde [PokeAPI](https://pokeapi.co/api/v2/pokemon/{name}).

Materia: Desarrollo de Software III — Universidad del Valle, Sede Tuluá.

## Requisitos
- Java 11 o superior
- Maven 3.6+ (descarga automáticamente `org.json`)
- Conexión a internet

## Ejecución
Abrir la carpeta como proyecto Maven en **IntelliJ IDEA** y ejecutar la clase `Main`.

> La ficha de cada jugador está diseñada con el **GUI Designer de IntelliJ** (`PokemonPanel.form`).
> Por eso debe compilarse con IntelliJ (*Settings > Build Tools > Maven > Runner* / *Build and run using: IntelliJ IDEA*),
> que es quien procesa los `.form`. Con `mvn exec:java` por consola el formulario no se inicializa.

## Uso
1. En cada lado pulsa **Random** o escribe un nombre (ej. `charmander`) y pulsa **Load** (o Enter).
2. Cuando ambos estén cargados se habilita **Fight!**.
3. Observa el log; al terminar puedes pulsar Fight! otra vez (revancha) o cargar otros Pokémon.

## Diseño
Paquetes: `model` (`Pokemon`), `api` (`PokeApiClient`, `PokeApiException`), `battle` (`Battle`, `BattleListener`)
y `ui` (`MainFrame`, `PokemonPanel` + `PokemonPanel.form`). La ficha muestra ID, nombre, peso, altura, tipos, stats y barra de HP. `PokeApiClient` usa `java.net.http.HttpClient` y `org.json`; sus llamadas
son bloqueantes, por eso la UI las ejecuta siempre en un `SwingWorker`. Los errores (no encontrado, red, JSON)
se muestran en la barra de estado y en el log sin congelar la ventana.

`Battle` contiene las reglas y corre en su propio hilo (con pausa entre turnos). No conoce Swing: notifica
mediante `BattleListener` (`onTurn`, `onHpChanged`, `onBattleEnded`). `MainFrame` implementa esa interfaz y
actualiza la pantalla únicamente desde esos eventos usando `SwingUtilities.invokeLater`.

**Fórmula de daño:** `base = ATK·rand(0.6–1.0) − DEF·rand(0.2–0.5)`; `daño = max(1, base·0.6) · efectividad · crítico`.
Crítico 10 % (x1.5). Efectividad por primer tipo: agua>fuego, fuego>planta, planta>agua (x1.3), inversa (x0.7),
resto x1.0. Inicia el de mayor Speed (empate: aleatorio). El HP nunca baja de 0.

## Capturas de pantalla
<img width="690" height="464" alt="Captura de pantalla 2026-10-07 112953" src="https://github.com/user-attachments/assets/bd810e4f-7e08-4150-98db-521b9ec9234a" />


