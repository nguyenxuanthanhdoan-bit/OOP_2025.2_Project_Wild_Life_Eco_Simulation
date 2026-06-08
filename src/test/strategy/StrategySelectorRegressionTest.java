package test.strategy;

import core.Vector2;
import model.items.Carcass;
import model.living_beings.Animal;
import model.living_beings.Deer;
import model.living_beings.Elephant;
import model.living_beings.Rabbit;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.plants.Grass;
import model.strategies.ForageStrategy;
import model.strategies.HunterStrategy;
import model.strategies.IStrategy;
import model.strategies.ScaredStrategy;
import model.world.PopulationManager;
import model.world.World;

public class StrategySelectorRegressionTest {
    public static void main(String[] args) {
        PopulationManager.setEnabled(false);

        testCriticalCarnivoreHuntsLivePrey();
        testCarnivoreEatsAvailableCarcass();
        testCarnivoreSkipsOwnSpeciesCarcass();
        testHerbivoreForagesPlants();
        testHerbivoreScaredByHigherLevelThreat();
        testElephantIsNotScaredByCarnivore();

        System.out.println("StrategySelectorRegressionTest passed.");
    }

    private static void testCriticalCarnivoreHuntsLivePrey() {
        World world = testWorld();
        Wolf wolf = new Wolf(new Vector2(100, 100));
        Rabbit rabbit = new Rabbit(new Vector2(118, 100));
        wolf.setHunger(1.0);

        world.addEntity(wolf);
        world.addEntity(rabbit);
        double rabbitHealth = rabbit.getHealth();

        world.update(0.2f);

        assertStrategy(wolf, HunterStrategy.class, "critical carnivore hunger should use HunterStrategy");
        assertTrue(rabbit.getHealth() < rabbitHealth, "wolf should attack nearby live prey when no carcass exists");
    }

    private static void testCarnivoreEatsAvailableCarcass() {
        World world = testWorld();
        Wolf wolf = new Wolf(new Vector2(100, 100));
        Carcass carcass = new Carcass(new Vector2(108, 100), 15.0f, 40.0f, 60.0f, 40.0f, "Thỏ");
        wolf.setHunger(10.0);

        world.addEntity(wolf);
        world.addEntity(carcass);
        double hunger = wolf.getHunger();

        world.update(0.2f);

        assertStrategy(wolf, HunterStrategy.class, "hungry carnivore should still use HunterStrategy when eating carcass");
        assertTrue(wolf.getHunger() > hunger, "wolf should recover hunger from edible carcass");
    }

    private static void testCarnivoreSkipsOwnSpeciesCarcass() {
        World world = testWorld();
        Tiger tiger = new Tiger(new Vector2(100, 100));
        Carcass tigerCarcass = new Carcass(new Vector2(108, 100), 30.0f, 120.0f, 150.0f, 120.0f, "Hổ");
        tiger.setHunger(20.0);

        world.addEntity(tiger);
        world.addEntity(tigerCarcass);
        float mass = tigerCarcass.getCurrentMass();

        world.update(0.2f);

        assertStrategy(tiger, HunterStrategy.class, "hungry tiger should search with HunterStrategy");
        assertTrue(tigerCarcass.getCurrentMass() == mass, "tiger should not eat own-species carcass by default");
    }

    private static void testHerbivoreForagesPlants() {
        World world = testWorld();
        Deer deer = new Deer(new Vector2(100, 100));
        Grass grass = new Grass(new Vector2(108, 100));
        deer.setHunger(20.0);

        world.addEntity(deer);
        world.addEntity(grass);
        double hunger = deer.getHunger();

        world.update(0.2f);

        assertStrategy(deer, ForageStrategy.class, "hungry herbivore should use ForageStrategy");
        assertTrue(deer.getHunger() > hunger, "deer should recover hunger from edible plant");
    }

    private static void testHerbivoreScaredByHigherLevelThreat() {
        World world = testWorld();
        Deer deer = new Deer(new Vector2(100, 100));
        Wolf wolf = new Wolf(new Vector2(130, 100));

        world.addEntity(deer);
        world.addEntity(wolf);
        world.update(0.2f);

        assertStrategy(deer, ScaredStrategy.class, "deer should be scared by higher-level hunting animal");
    }

    private static void testElephantIsNotScaredByCarnivore() {
        World world = testWorld();
        Elephant elephant = new Elephant(new Vector2(100, 100));
        Wolf wolf = new Wolf(new Vector2(130, 100));

        world.addEntity(elephant);
        world.addEntity(wolf);
        world.update(0.2f);

        assertTrue(!(elephant.getCurrentStrategy() instanceof ScaredStrategy),
                "elephant should not switch to ScaredStrategy when seeing carnivores");
    }

    private static World testWorld() {
        World world = new World();
        world.setWidth(500);
        world.setHeight(500);
        return world;
    }

    private static void assertStrategy(Animal animal, Class<? extends IStrategy> expected, String message) {
        assertTrue(expected.isInstance(animal.getCurrentStrategy()),
                message + " (actual=" + strategyName(animal) + ")");
    }

    private static String strategyName(Animal animal) {
        return animal.getCurrentStrategy() == null ? "null" : animal.getCurrentStrategy().getClass().getSimpleName();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
