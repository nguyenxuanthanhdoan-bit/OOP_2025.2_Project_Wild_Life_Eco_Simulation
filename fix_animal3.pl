use strict;
use warnings;

my $file = 'src/model/living_beings/animal/Animal.java';
open(my $fh, '<', $file) or die "Cannot open $file: $!";
my $content = do { local $/; <$fh> };
close($fh);

$content =~ s/public void takeDamage\(double amount\) \{.*?\n    \}/public void takeDamage(double amount) {\n        if (vitals != null) vitals.takeDamage(amount);\n    }/gs;
$content =~ s/public void heal\(double amount\) \{.*?\n    \}/public void heal(double amount) {\n        if (vitals != null) vitals.heal(amount);\n    }/gs;

# Now for the update() method
$content =~ s/        \/\/ Xử lý decay hunger\/thirst\n.*?\/\/ Mất kiểm soát nếu quá đói\/khát\n        if \(hunger <= 0 \|\| thirst <= 0\) \{\n            setSpeed\(baseSpeed \* 0\.3f\); \/\/ Chậm 70%\n            takeDamage\(5\.0 \* deltaTime\); \/\/ Mất máu từ từ\n        \}/if (vitals != null) vitals.update(deltaTime);/gs;

open(my $out, '>', $file) or die "Cannot write $file: $!";
print $out $content;
close($out);
