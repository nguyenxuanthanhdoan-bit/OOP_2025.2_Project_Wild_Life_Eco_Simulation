use strict;
use warnings;

my $file = 'src/model/living_beings/animal/Animal.java';
open(my $fh, '<', $file) or die "Cannot open $file: $!";
my $content = do { local $/; <$fh> };
close($fh);

# Delete eat, eatMeat, eatCarcass, drink, growOlder bodies and replace with delegation
$content =~ s/public void eat\(Plant food\) \{.*?\n    \}/public void eat(Plant food) {\n        if (vitals != null) vitals.eat(food);\n    }/gs;
$content =~ s/public void eatMeat\(model\.items\.FoodSource food\) \{.*?\n    \}/public void eatMeat(model.items.FoodSource food) {\n        if (vitals != null) vitals.eatMeat(food);\n    }/gs;
$content =~ s/public void eatCarcass\(model\.items\.Carcass carcass, float deltaTime\) \{.*?\n    \}/public void eatCarcass(model.items.Carcass carcass, float deltaTime) {\n        if (vitals != null) vitals.eatCarcass(carcass, deltaTime);\n    }/gs;
$content =~ s/public void drink\(\) \{.*?\n    \}/public void drink() {\n        if (vitals != null) vitals.drink();\n    }/gs;
$content =~ s/public void growOlder\(double deltaTime\) \{.*?\n    \}/public void growOlder(double deltaTime) {\n        if (vitals != null) vitals.growOlder(deltaTime);\n    }/gs;

# Replace getters and setters
$content =~ s/public double getHealth\(\) \{ return health; \}/public double getHealth() { return vitals != null ? vitals.getHealth() : 0; }/g;
$content =~ s/public void setHealth\(double health\) \{.*?\n    \}/public void setHealth(double health) {\n        if (vitals != null) vitals.setHealth(health);\n    }/gs;
$content =~ s/public double getMaxHealth\(\) \{ return maxHealth; \}/public double getMaxHealth() { return vitals != null ? vitals.getMaxHealth() : 1; }/g;
$content =~ s/public void setMaxHealth\(double maxHealth\) \{ this\.maxHealth = maxHealth; \}/public void setMaxHealth(double maxHealth) { if (vitals != null) vitals.setMaxHealth(maxHealth); }/g;

$content =~ s/public double getHunger\(\) \{ return hunger; \}/public double getHunger() { return vitals != null ? vitals.getHunger() : 0; }/g;
$content =~ s/public void setHunger\(double hunger\) \{.*?\n    \}/public void setHunger(double hunger) {\n        if (vitals != null) vitals.setHunger(hunger);\n    }/gs;
$content =~ s/public double getMaxHunger\(\) \{ return maxHunger; \}/public double getMaxHunger() { return vitals != null ? vitals.getMaxHunger() : 1; }/g;
$content =~ s/public void setMaxHunger\(double maxHunger\) \{ this\.maxHunger = maxHunger; \}/public void setMaxHunger(double maxHunger) { if (vitals != null) vitals.setMaxHunger(maxHunger); }/g;
$content =~ s/public double getHungerDecayRate\(\) \{ return hungerDecayRate; \}/public double getHungerDecayRate() { return vitals != null ? vitals.getHungerDecayRate() : 0; }/g;
$content =~ s/public void setHungerDecayRate\(double hungerDecayRate\) \{ this\.hungerDecayRate = hungerDecayRate; \}/public void setHungerDecayRate(double hungerDecayRate) { if (vitals != null) vitals.setHungerDecayRate(hungerDecayRate); }/g;

$content =~ s/public double getThirst\(\) \{ return thirst; \}/public double getThirst() { return vitals != null ? vitals.getThirst() : 0; }/g;
$content =~ s/public void setThirst\(double thirst\) \{.*?\n    \}/public void setThirst(double thirst) {\n        if (vitals != null) vitals.setThirst(thirst);\n    }/gs;
$content =~ s/public double getMaxThirst\(\) \{ return maxThirst; \}/public double getMaxThirst() { return vitals != null ? vitals.getMaxThirst() : 1; }/g;
$content =~ s/public void setMaxThirst\(double maxThirst\) \{ this\.maxThirst = maxThirst; \}/public void setMaxThirst(double maxThirst) { if (vitals != null) vitals.setMaxThirst(maxThirst); }/g;
$content =~ s/public double getThirstDecayRate\(\) \{ return thirstDecayRate; \}/public double getThirstDecayRate() { return vitals != null ? vitals.getThirstDecayRate() : 0; }/g;
$content =~ s/public void setThirstDecayRate\(double thirstDecayRate\) \{ this\.thirstDecayRate = thirstDecayRate; \}/public void setThirstDecayRate(double thirstDecayRate) { if (vitals != null) vitals.setThirstDecayRate(thirstDecayRate); }/g;

$content =~ s/public double getAge\(\) \{ return age; \}/public double getAge() { return vitals != null ? vitals.getAge() : 0; }/g;
$content =~ s/public void setAge\(double age\) \{ this\.age = age; \}/public void setAge(double age) { if (vitals != null) vitals.setAge(age); }/g;
$content =~ s/public double getMaxAge\(\) \{ return maxAge; \}/public double getMaxAge() { return vitals != null ? vitals.getMaxAge() : 1; }/g;
$content =~ s/public void setMaxAge\(double maxAge\) \{ this\.maxAge = maxAge; \}/public void setMaxAge(double maxAge) { if (vitals != null) vitals.setMaxAge(maxAge); }/g;

$content =~ s/public boolean isAdult\(\) \{ return adult; \}/public boolean isAdult() { return vitals != null && vitals.isAdult(); }/g;
$content =~ s/public void setAdult\(boolean adult\) \{ this\.adult = adult; \}/public void setAdult(boolean adult) { if (vitals != null) vitals.setAdult(adult); }/g;
$content =~ s/public float getDamageBlinkTimer\(\) \{ return damageBlinkTimer; \}/public float getDamageBlinkTimer() { return vitals != null ? vitals.getDamageBlinkTimer() : 0; }/g;

open(my $out, '>', $file) or die "Cannot write $file: $!";
print $out $content;
close($out);
