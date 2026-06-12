use strict;
use warnings;

my $file = 'src/model/living_beings/animal/Animal.java';
open(my $fh, '<', $file) or die "Cannot open $file: $!";
my $content = do { local $/; <$fh> };
close($fh);

$content =~ s/this\.maxHealth = maxHealth;.*?this\.adult = false;/this.vitals = new VitalStatsComponent(this);\n        this.vitals.setMaxHealth(maxHealth);\n        this.vitals.setHealth(maxHealth);\n        this.vitals.setMaxHunger(maxHunger);\n        this.vitals.setHunger(maxHunger);\n        this.vitals.setHungerDecayRate(hungerDecayRate);\n        this.vitals.setMaxThirst(maxThirst);\n        this.vitals.setThirst(maxThirst);\n        this.vitals.setThirstDecayRate(thirstDecayRate);\n        this.vitals.setAge(0.0);\n        this.vitals.setMaxAge(maxAge);\n        this.visionRange = visionRange;\n        this.dietType = dietType;\n        this.vitals.setAdult(false);/gs;

$content =~ s/this\.dietType = dietType;\n        this\.age = 0\.0;/this.dietType = dietType;\n        this.vitals = new VitalStatsComponent(this);\n        this.vitals.setAge(0.0);/g;

open(my $out, '>', $file) or die "Cannot write $file: $!";
print $out $content;
close($out);
