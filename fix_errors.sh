perl -pi -e 's/this\.getMaxHealth\(\)\s*=\s*(.*?);/this.setMaxHealth($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.getMaxHunger\(\)\s*=\s*(.*?);/this.setMaxHunger($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.getMaxThirst\(\)\s*=\s*(.*?);/this.setMaxThirst($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.health\s*=\s*(.*?);/this.setHealth($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.hunger\s*=\s*(.*?);/this.setHunger($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.hungerDecayRate\s*=\s*(.*?);/this.setHungerDecayRate($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.thirst\s*=\s*(.*?);/this.setThirst($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.thirstDecayRate\s*=\s*(.*?);/this.setThirstDecayRate($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.maxAge\s*=\s*(.*?);/this.setMaxAge($1);/g' src/model/living_beings/*.java
perl -pi -e 's/\.age\s*=\s*(.*?);/.setAge($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.adult\s*=\s*(.*?);/this.setAdult($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.isAdult\(\)\s*=\s*(.*?);/this.setAdult($1);/g' src/model/living_beings/*.java
perl -pi -e 's/this\.age\s*>\s*0/this.getAge() > 0/g' src/model/living_beings/*.java
perl -pi -e 's/this\.adult/this.isAdult()/g' src/model/living_beings/*.java
perl -pi -e 's/animal\.isAliveState\(\)/animal.isAlive()/g' src/model/structures/Well.java src/model/strategies/StrategySelector.java src/model/world/PopulationManager.java src/model/navigation/PathNavigator.java
perl -pi -e 's/other\.isAliveState\(\)/other.isAlive()/g' src/model/strategies/FlockingStrategy.java
perl -pi -e 's/food\.getNutritionValue\(\)/food.getNutrition()/g' src/model/living_beings/animal/VitalStatsComponent.java
