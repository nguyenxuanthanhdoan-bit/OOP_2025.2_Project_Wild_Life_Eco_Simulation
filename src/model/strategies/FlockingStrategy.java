package model.strategies;

import core.Vector2;
import model.living_beings.LivingBeing;
import model.living_beings.Animal;
import model.living_beings.DietType;
import model.navigation.PathNavigator;
import model.world.World;
import model.entity.Entity;
import java.util.List;
import java.util.ArrayList;

public class FlockingStrategy extends PassiveStrategy {
    protected float separationWeight = 1.5f;
    protected float alignmentWeight = 1.0f;
    protected float cohesionWeight = 1.0f;
    protected final PassiveStrategy wanderDelegate = new PassiveStrategy();
    protected final PathNavigator flockNavigator = new PathNavigator();

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;
        if (world == null || world.getSpatialGrid() == null) return;

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(ownerAnimal.getPosition(), (float) ownerAnimal.getVisionRange());
        
        // Let subclass handle predators first
        if (handlePredators(ownerAnimal, neighbors, world, deltaTime)) {
            return;
        }

        List<Animal> flock = new ArrayList<>();
        for (Entity e : neighbors) {
            if (e instanceof Animal && e != ownerAnimal && e.isAlive()) {
                Animal other = (Animal) e;
                if (other.getSpeciesName().equals(ownerAnimal.getSpeciesName())) {
                    flock.add(other);
                }
            }
        }

        if (flock.isEmpty()) {
            ownerAnimal.setActionState("idle");
            wanderDelegate.execute(owner, world, deltaTime);
            return;
        }

        ownerAnimal.setActionState("run"); // Or walk, let's use walk
        // Wait, if velocity > 0, it will be mapped to walk if actionState is not run/attack

        Vector2 separation = new Vector2();
        Vector2 alignment = new Vector2();
        Vector2 cohesion = new Vector2();
        
        // Wait, Animal doesn't have velocity property, we can only infer it from oldPos and newPos, or use speed.
        // Actually, alignment is hard without a direction property. We'll skip alignment or use facingRight.
        // Since LivingBeing doesn't store velocity vector, we'll just do Separation and Cohesion.
        
        int count = 0;
        for (Animal other : flock) {
            float dist = ownerAnimal.getPosition().distanceTo(other.getPosition());
            if (dist > 0 && dist < 50.0f) {
                Vector2 diff = ownerAnimal.getPosition().copy().subtract(other.getPosition());
                diff.normalize();
                diff.scale(1.0f / dist);
                separation.add(diff);
            }
            cohesion.add(other.getPosition());
            
            Vector2 otherVelocity = other.getCurrentVelocity();
            if (otherVelocity.lengthSquared() > 1.0f) {
                otherVelocity.normalize();
                alignment.add(otherVelocity);
            }
            
            count++;
        }

        Vector2 moveDir = new Vector2();
        if (count > 0) {
            cohesion.scale(1.0f / count);
            cohesion.subtract(ownerAnimal.getPosition());
            if (cohesion.lengthSquared() > 0) cohesion.normalize();
            
            if (separation.lengthSquared() > 0) separation.normalize();
            
            if (alignment.lengthSquared() > 0) alignment.normalize();
            
            moveDir.add(separation.scale(separationWeight));
            moveDir.add(cohesion.scale(cohesionWeight));
            moveDir.add(alignment.scale(alignmentWeight));
        }

        if (moveDir.lengthSquared() > 0) {
            moveDir.normalize();
            
            Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(ownerAnimal, world, moveDir);
            if (avoidance.lengthSquared() > 0) {
                moveDir.add(avoidance);
                if (moveDir.lengthSquared() > 0) moveDir.normalize();
            }

            ownerAnimal.setActionState("idle"); // Will be mapped to walk
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            Vector2 flockTarget = ownerAnimal.getPosition().copy().add(moveDir.copy().scale(160.0f));
            clampToWorld(flockTarget, ownerAnimal, world);
            flockNavigator.moveTo(ownerAnimal, world, flockTarget, deltaTime, 18.0f, 1.5f);
        } else {
            wanderDelegate.execute(owner, world, deltaTime);
        }
    }

    /** Subclasses override this to react to predators */
    protected boolean handlePredators(Animal owner, List<Entity> neighbors, World world, float deltaTime) {
        return false;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public String getName() {
        return "Flocking";
    }

    protected void clampToWorld(Vector2 target, Animal owner, World world) {
        float margin = owner.getSize() / 2;
        target.x = Math.max(margin, Math.min(world.getWidth() - margin, target.x));
        target.y = Math.max(margin, Math.min(world.getHeight() - margin, target.y));
    }

    // ==========================================
    // NESTED CLASSES FOR SPECIFIC SPECIES
    // ==========================================

    public static class DeerFlock extends FlockingStrategy {
        @Override
        protected boolean handlePredators(Animal owner, List<Entity> neighbors, World world, float deltaTime) {
            for (Entity e : neighbors) {
                if (e instanceof Animal && ((Animal)e).getDietType() == DietType.CARNIVORE) {
                    // Đàn giải tán, chuyển sang ScaredStrategy
                    owner.setStrategy(new ScaredStrategy());
                    return true;
                }
            }
            return false;
        }
    }

    public static class WolfFlock extends FlockingStrategy {
        @Override
        protected boolean handlePredators(Animal owner, List<Entity> neighbors, World world, float deltaTime) {
            for (Entity e : neighbors) {
                if (e instanceof Animal && ((Animal)e).getDietType() == DietType.CARNIVORE) {
                    Animal predator = (Animal) e;
                    if (predator.getSize() > owner.getSize()) {
                        // Kẻ địch mạnh hơn -> rã đàn, chạy trốn
                        owner.setStrategy(new ScaredStrategy());
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class ElephantFlock extends FlockingStrategy {
        @Override
        protected boolean handlePredators(Animal owner, List<Entity> neighbors, World world, float deltaTime) {
            List<Animal> predators = new ArrayList<>();
            for (Entity e : neighbors) {
                if (e instanceof Animal && ((Animal)e).getDietType() == DietType.CARNIVORE) {
                    predators.add((Animal) e);
                }
            }

            if (!predators.isEmpty()) {
                // Không bỏ chạy, giữ đội hình
                Vector2 predatorCenter = new Vector2();
                for (Animal p : predators) predatorCenter.add(p.getPosition());
                predatorCenter.scale(1.0f / predators.size());

                Vector2 dir = new Vector2();
                if (owner.isAdult()) {
                    // Trưởng thành tiến về phía kẻ thù để chắn
                    dir = predatorCenter.copy().subtract(owner.getPosition());
                } else {
                    // Con non lùi lại hoặc đứng yên
                    dir = owner.getPosition().copy().subtract(predatorCenter);
                }

                if (dir.lengthSquared() > 0) {
                    dir.normalize();
                    
                    Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(owner, world, dir);
                    if (avoidance.lengthSquared() > 0) {
                        dir.add(avoidance);
                        if (dir.lengthSquared() > 0) dir.normalize();
                    }
                    
                    owner.setActionState("idle");
                    owner.setSpeed(owner.getBaseSpeed());
                    Vector2 target = owner.getPosition().copy().add(dir.copy().scale(160.0f));
                    clampToWorld(target, owner, world);
                    flockNavigator.moveTo(owner, world, target, deltaTime, 18.0f, 1.0f);
                }
                return true; // handled
            }
            return false;
        }
    }

    @Override
    public void forceStateChange() {
        super.forceStateChange();
        wanderDelegate.forceStateChange();
        flockNavigator.clear();
    }
}
