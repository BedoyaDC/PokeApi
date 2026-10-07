package stadium.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import stadium.model.Pokemon;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cliente de PokeAPI. Sus métodos son BLOQUEANTES: deben llamarse desde un hilo
 * de fondo (SwingWorker), nunca desde el EDT.
 */
public class PokeApiClient {
    private static final String BASE_URL = "https://pokeapi.co/api/v2/pokemon/";
    private static final int MAX_POKEMON_ID = 1025; // IDs "normales" (sin formas especiales)

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Pokemon fetchByName(String name) throws PokeApiException {
        String clean = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) {
            throw new PokeApiException("Escribe el nombre de un Pokémon.");
        }
        return fetch(clean);
    }

    public Pokemon fetchRandom() throws PokeApiException {
        int id = ThreadLocalRandom.current().nextInt(1, MAX_POKEMON_ID + 1);
        return fetch(String.valueOf(id));
    }

    private Pokemon fetch(String idOrName) throws PokeApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + idOrName))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new PokeApiException("Pokémon no encontrado: " + idOrName);
            }
            if (response.statusCode() != 200) {
                throw new PokeApiException("Error del servidor (HTTP " + response.statusCode() + ")");
            }
            return parse(new JSONObject(response.body()));
        } catch (IOException e) {
            throw new PokeApiException("Error de red: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PokeApiException("La consulta fue interrumpida.", e);
        } catch (JSONException | IllegalArgumentException e) {
            throw new PokeApiException("Respuesta inválida de la API.", e);
        }
    }

    /** Convierte el JSON de PokeAPI en nuestro modelo Pokemon. */
    private Pokemon parse(JSONObject json) {
        int id = json.getInt("id");
        int weight = json.optInt("weight", 0);
        int height = json.optInt("height", 0);
        String name = capitalize(json.getString("name"));

        List<String> types = new ArrayList<>();
        JSONArray typesJson = json.getJSONArray("types");
        for (int i = 0; i < typesJson.length(); i++) {
            types.add(typesJson.getJSONObject(i).getJSONObject("type").getString("name"));
        }

        int hp = 0, attack = 0, defense = 0, speed = 0;
        JSONArray statsJson = json.getJSONArray("stats");
        for (int i = 0; i < statsJson.length(); i++) {
            JSONObject s = statsJson.getJSONObject(i);
            int value = s.getInt("base_stat");
            switch (s.getJSONObject("stat").getString("name")) {
                case "hp":      hp = value; break;
                case "attack":  attack = value; break;
                case "defense": defense = value; break;
                case "speed":   speed = value; break;
                default: break; // special-attack/special-defense no se usan
            }
        }

        String spriteUrl = json.getJSONObject("sprites").optString("front_default", "");
        Image sprite = spriteUrl.isEmpty() || spriteUrl.equals("null") ? null : downloadSprite(spriteUrl);

        return new Pokemon(id, name, types, hp, attack, defense, speed, weight, height, sprite);
    }

    /** Descarga el sprite; si falla devuelve null (el Pokémon se muestra sin imagen). */
    private Image downloadSprite(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
