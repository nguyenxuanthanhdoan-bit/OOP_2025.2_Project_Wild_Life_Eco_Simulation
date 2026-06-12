# 1. Remove sensory fields from Animal.java
perl -0777 -pi -e 's/protected double visionRange;//g' src/model/living_beings/animal/Animal.java
perl -0777 -pi -e 's/private final Map<UUID, Float> unsafeFoodMemory = new HashMap<\(\)>();//g' src/model/living_beings/animal/Animal.java
perl -0777 -pi -e 's/protected float radarCooldown = 0f;//g' src/model/living_beings/animal/Animal.java
perl -0777 -pi -e 's/protected boolean cachedThreat = false;//g' src/model/living_beings/animal/Animal.java
perl -0777 -pi -e 's/protected float gardenThreatCooldown = 0f;//g' src/model/living_beings/animal/Animal.java
perl -0777 -pi -e 's/protected boolean cachedGardenThreat = false;//g' src/model/living_beings/animal/Animal.java

# We need a proper script to delete the DangerZone class and methods
