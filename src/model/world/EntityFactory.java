package model.world;

import core.Vector2;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Rabbit;
import model.living_beings.Tiger;
import model.living_beings.Wolf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Registry tạo entity theo tên loài.
 * Khi thêm loài mới, đăng ký tại đây hoặc gọi registerAnimal() thay vì sửa switch ở nhiều nơi.
 */
public final class EntityFactory {
    @FunctionalInterface
    public interface AnimalCreator {
        Animal create(Vector2 position, int index, Random random);
    }

    private static final Map<String, AnimalCreator> ANIMAL_CREATORS = new LinkedHashMap<>();
    private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        registerAnimal("Thỏ", (position, index, random) -> new Rabbit(position));
        registerAnimal("Hươu", (position, index, random) -> new Deer(position, 1 + Math.max(0, index) / 16));
        registerAnimal("Voi", (position, index, random) -> new Elephant(position, 1 + Math.max(0, index) / 6));
        registerAnimal("Sói", (position, index, random) -> new Wolf(position));
        registerAnimal("Hổ", (position, index, random) -> new Tiger(position));
    }

    private EntityFactory() {}

    public static void registerAnimal(String speciesName, AnimalCreator creator) {
        if (speciesName == null || speciesName.trim().isEmpty() || creator == null) return;
        String key = normalize(speciesName);
        ANIMAL_CREATORS.put(key, creator);
        DISPLAY_NAMES.put(key, speciesName.trim());
    }

    public static Animal createAnimal(String speciesName, Vector2 position) {
        return createAnimal(speciesName, position, 0, new Random());
    }

    public static Animal createAnimal(String speciesName, Vector2 position, int index, Random random) {
        if (position == null) return null;
        AnimalCreator creator = ANIMAL_CREATORS.get(normalize(speciesName));
        if (creator == null) return null;
        return creator.create(position, index, random == null ? new Random() : random);
    }

    public static boolean canCreateAnimal(String speciesName) {
        return ANIMAL_CREATORS.containsKey(normalize(speciesName));
    }

    public static Set<String> getRegisteredAnimalSpecies() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(DISPLAY_NAMES.values()));
    }

    private static String normalize(String speciesName) {
        return speciesName == null ? "" : speciesName.trim().toLowerCase(Locale.ROOT);
    }
}
