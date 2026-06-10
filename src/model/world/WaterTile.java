package model.world;

public class WaterTile {
    private WaterBiome biome;
    private boolean connectedToOcean;
    private int x; // Tọa độ tile x
    private int y; // Tọa độ tile y

    public WaterTile(WaterBiome biome, boolean connectedToOcean, int x, int y) {
        this.biome = biome;
        this.connectedToOcean = connectedToOcean;
        this.x = x;
        this.y = y;
    }

    public WaterBiome getBiome() {
        return biome;
    }

    public void setBiome(WaterBiome biome) {
        this.biome = biome;
    }

    public boolean isConnectedToOcean() {
        return connectedToOcean;
    }

    public void setConnectedToOcean(boolean connectedToOcean) {
        this.connectedToOcean = connectedToOcean;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
