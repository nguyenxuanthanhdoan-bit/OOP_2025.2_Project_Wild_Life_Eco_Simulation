package model.world;

import core.GameConfig;
import model.entity.Entity;
import java.util.Random;

public class TimeWeatherSystem {
    private float dayTimer = 0.0f;
    private int gameDay = 1;
    private World.Season currentSeason = World.Season.GROWING;
    private int winterDays = 0; 
    private float winterProgress = 0.0f; 
    private World.Weather currentWeather = World.Weather.SUNNY;
    private float weatherTimer = 0.0f;
    private float timeOfDay = 6.0f; 
    private float timeScale = 0.2f;

    private final World world;
    private final Random random = new Random();

    public TimeWeatherSystem(World world) {
        this.world = world;
    }

    public void update(float deltaTime) {
        // Progress day/season/weather
        dayTimer += deltaTime;
        if (dayTimer >= 60.0f) { // 60 seconds per day
            dayTimer = 0.0f;
            gameDay++;
            
            if (currentSeason == World.Season.GROWING) {
                int animalCount = 0;
                for (Entity e : world.getEntities()) {
                    if (e instanceof model.living_beings.Animal && e.isAlive()) {
                        animalCount++;
                    }
                }
                if (animalCount >= GameConfig.getInstance().MAX_INITIAL_ANIMAL_COUNT * 0.90f) {
                    setSeason(World.Season.WINTER);
                    winterDays = 0;
                }
            } else if (currentSeason == World.Season.WINTER) {
                winterDays++;
                if (winterDays >= 2) { // Kéo dài 2 ngày
                    setSeason(World.Season.GROWING);
                }
            }
        }

        weatherTimer += deltaTime;
        if (weatherTimer >= 30.0f) { // Change weather every 30 seconds
            weatherTimer = 0.0f;
            changeRandomWeather();
        }

        // Cập nhật thời gian trong ngày
        timeOfDay += deltaTime * timeScale;
        if (timeOfDay >= 24.0f) {
            timeOfDay -= 24.0f;
        }

        // Cập nhật tiến trình mùa đông
        float winterTransitionRate = 1.0f / GameConfig.getInstance().WINTER_TRANSITION_SECONDS;
        if (currentSeason == World.Season.WINTER) {
            if (winterProgress < 1.0f) {
                winterProgress += deltaTime * winterTransitionRate;
                if (winterProgress > 1.0f) winterProgress = 1.0f;
            }
        } else {
            if (winterProgress > 0.0f) {
                winterProgress -= deltaTime * winterTransitionRate;
                if (winterProgress < 0.0f) winterProgress = 0.0f;
            }
        }
    }

    public void nextSeason() {
        World.Season[] seasons = World.Season.values();
        int nextIdx = (currentSeason.ordinal() + 1) % seasons.length;
        setSeason(seasons[nextIdx]);
    }

    public void setSeason(World.Season season) {
        if (this.currentSeason != season) {
            this.currentSeason = season;
            changeRandomWeather(); // Force immediate weather change
            world.publishEvent(WorldEventType.SEASON_CHANGED, null, "manual_change");
        }
    }

    public void changeRandomWeather() {
        World.Weather[] weathers = World.Weather.values();
        if (currentSeason == World.Season.WINTER) {
            int r = random.nextInt(10);
            if (r < 6) this.currentWeather = World.Weather.SNOWY;
            else if (r < 8) this.currentWeather = World.Weather.STORMY;
            else this.currentWeather = World.Weather.RAINY;
        } else if (currentSeason == World.Season.GROWING) {
            int r = random.nextInt(10);
            if (r < 7) this.currentWeather = World.Weather.SUNNY;
            else if (r < 9) this.currentWeather = World.Weather.STORMY;
            else this.currentWeather = World.Weather.RAINY;
        } else {
            this.currentWeather = weathers[random.nextInt(weathers.length)];
        }
    }

    public void setWeather(World.Weather weather) {
        this.currentWeather = weather;
    }

    public void reset() {
        this.gameDay = 1;
        this.dayTimer = 0.0f;
        this.currentSeason = World.Season.GROWING;
        this.winterProgress = 0.0f;
        this.currentWeather = World.Weather.SUNNY;
        this.weatherTimer = 0.0f;
        this.timeOfDay = 6.0f;
    }

    public float getDayTimer() { return dayTimer; }
    public int getGameDay() { return gameDay; }
    public World.Season getCurrentSeason() { return currentSeason; }
    public float getWinterProgress() { return winterProgress; }
    public World.Weather getCurrentWeather() { return currentWeather; }
    public float getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float timeOfDay) { this.timeOfDay = timeOfDay; }
}
