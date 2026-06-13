package model.strategies;

import core.Vector2;
import model.entity.Entity;
import model.living_beings.Animal;
import model.living_beings.Human;
import model.living_beings.LivingBeing;
import model.navigation.PathNavigator;
import model.navigation.PathNavigator.MovementContext;
import model.navigation.TerrainNavigator;
import model.structures.Bush;
import model.structures.House;
import model.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Chạy trốn khỏi mối đe dọa và sử dụng nơi trú nếu có thể tiếp cận.
 */
public class ScaredStrategy implements IStrategy {
    private static final float RUN_SPEED_MULTIPLIER = 1.8f;
    private static final float FLEE_TARGET_DISTANCE = 220.0f;
    private static final float FLEE_REACH_DISTANCE = 20.0f;
    private static final float FLEE_COMMIT_SECONDS = 2.0f;
    private static final float FLEE_REPATH_SECONDS = 0.65f;
    private static final float FLEE_NO_PROGRESS_SECONDS = 0.6f;
    private static final float UNAVAILABLE_SHELTER_SECONDS = 2.5f;
    private static final int MAX_FAILED_DIRECTIONS = 4;

    private static final float[] FLEE_ANGLES = {
            0.0f, 30.0f, -30.0f, 60.0f, -60.0f,
            90.0f, -90.0f, 120.0f, -120.0f, 150.0f, -150.0f, 180.0f
    };
    private static final float[] FLEE_DISTANCE_MULTIPLIERS = {1.0f, 0.75f, 0.5f, 0.25f};

    private final PathNavigator shelterNavigator = new PathNavigator();
    private final PathNavigator fleeNavigator = new PathNavigator();
    private final List<Vector2> failedFleeDirections = new ArrayList<>();
    private final Vector2 lastFleePosition = new Vector2();

    private Vector2 fleeTarget;
    private boolean hasLastFleePosition;
    private float fleeCommitTimer;
    private float fleeNoProgressTimer;
    private Entity unavailableShelter;
    private float unavailableShelterTimer;

    @Override
    public void execute(LivingBeing owner, World world, float deltaTime) {
        if (!(owner instanceof Animal)) return;
        Animal ownerAnimal = (Animal) owner;
        if (!ownerAnimal.canUseStrategy(ScaredStrategy.class)) return;
        if (world == null || world.getSpatialGrid() == null) return;

        fleeCommitTimer = Math.max(0.0f, fleeCommitTimer - deltaTime);
        unavailableShelterTimer = Math.max(0.0f, unavailableShelterTimer - deltaTime);
        if (unavailableShelterTimer <= 0.0f) {
            unavailableShelter = null;
        }

        List<Entity> neighbors = world.getSpatialGrid().getNeighbors(
                ownerAnimal.getPosition(), ownerAnimal.getThreatScanRange());
        List<Animal> predators = new ArrayList<>();
        List<Bush> bushes = new ArrayList<>();
        List<House> houses = new ArrayList<>();
        Human human = ownerAnimal instanceof Human ? (Human) ownerAnimal : null;

        for (Entity neighbor : neighbors) {
            if (neighbor instanceof Animal && neighbor != ownerAnimal) {
                Animal other = (Animal) neighbor;
                if (!ownerAnimal.isThreatenedBy(other)) continue;
                if (other.getCurrentStrategy() instanceof SleepStrategy) continue;

                float maxDist = ownerAnimal.getThreatDetectionRange(other);
                if (ownerAnimal.getPosition().distanceSquared(other.getPosition()) <= maxDist * maxDist) {
                    predators.add(other);
                }
            } else if (ownerAnimal.getProfile().canHide() && neighbor instanceof Bush) {
                Bush bush = (Bush) neighbor;
                if ((!bush.isOccupied() || bush == ownerAnimal.getHiddenInBush())
                        && bush != unavailableShelter) {
                    bushes.add(bush);
                }
            } else if (human != null && human.canUseHouse() && neighbor instanceof House) {
                House house = (House) neighbor;
                if (house == unavailableShelter) continue;
                if (human.getHomeSettlement() != null
                        && !human.getHomeSettlement().containsHouse(house)) {
                    continue;
                }
                if ((house.hasSpace() || house == human.getHiddenInHouse())
                        && human.isInHomeArea(house.getPosition())) {
                    houses.add(house);
                }
            }
        }

        for (Animal predator : predators) {
            ownerAnimal.markDangerZone(predator, 300.0f, 8.0f);
        }

        if (predators.isEmpty()) {
            if (human != null && human.getHiddenInHouse() != null) {
                human.tryExitHouseSafely();
            }
            shelterNavigator.clear();
            clearFleeRoute();
            ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed());
            ownerAnimal.setActionState("walk");
            return;
        }

        if (human != null && human.canUseHouse()) {
            if (handleHouseShelter(human, houses, predators, world, deltaTime)) {
                return;
            }
            fleeFromPredators(ownerAnimal, predators, neighbors, world, deltaTime);
            return;
        }

        Bush bestBush = findBestBush(ownerAnimal, bushes, predators);
        if (bestBush == null) {
            fleeFromPredators(ownerAnimal, predators, neighbors, world, deltaTime);
            return;
        }

        float distToBush = ownerAnimal.getPosition().distanceTo(bestBush.getPosition());
        if (distToBush <= bestBush.getRadius()) {
            if (!ownerAnimal.isHidden()) ownerAnimal.hideInBush(bestBush);
            shelterNavigator.clear();
            clearFleeRoute();
            ownerAnimal.setActionState("idle");
            ownerAnimal.setSpeed(0);
            return;
        }

        if (ownerAnimal.isHidden()) ownerAnimal.exitBush();
        ownerAnimal.setActionState("run");
        ownerAnimal.setSpeed(ownerAnimal.getBaseSpeed() * RUN_SPEED_MULTIPLIER);
        shelterNavigator.moveTo(ownerAnimal, world, bestBush.getPosition(), deltaTime,
                bestBush.getRadius(), 0.5f, MovementContext.FLEEING);

        if (shelterNavigator.isBlocked()) {
            markShelterUnavailable(bestBush);
            fleeFromPredators(ownerAnimal, predators, neighbors, world, deltaTime);
        } else {
            clearFleeRoute();
        }
    }

    private Bush findBestBush(Animal owner, List<Bush> bushes, List<Animal> predators) {
        Bush bestBush = null;
        float bestScore = -Float.MAX_VALUE;

        for (Bush bush : bushes) {
            float predatorDistance = 0.0f;
            for (Animal predator : predators) {
                predatorDistance += bush.getPosition().distanceTo(predator.getPosition());
            }
            float ownerDistance = bush.getPosition().distanceTo(owner.getPosition());
            float score = predatorDistance - ownerDistance * 2.0f;
            if (score > bestScore) {
                bestScore = score;
                bestBush = bush;
            }
        }
        return bestBush;
    }

    private boolean handleHouseShelter(Human human, List<House> houses, List<Animal> predators,
                                       World world, float deltaTime) {
        House bestHouse = null;
        float bestScore = -Float.MAX_VALUE;

        for (House house : houses) {
            if (!house.hasSpace() && house != human.getHiddenInHouse()) continue;

            float predatorDistance = 0.0f;
            for (Animal predator : predators) {
                predatorDistance += house.getPosition().distanceTo(predator.getPosition());
            }
            float humanDistance = house.getPosition().distanceTo(human.getPosition());
            float score = predatorDistance - humanDistance * 2.0f;
            if (score > bestScore) {
                bestScore = score;
                bestHouse = house;
            }
        }

        if (bestHouse == null) return false;

        float enterRange = human.getSize() / 2 + bestHouse.getSize() / 2 + 12.0f;
        if (human.getPosition().distanceTo(bestHouse.getPosition()) <= enterRange) {
            if (human.enterHouse(bestHouse)) {
                shelterNavigator.clear();
                clearFleeRoute();
                return true;
            }
            markShelterUnavailable(bestHouse);
            return false;
        }

        if (human.getHiddenInHouse() != null && !human.tryExitHouseSafely()) {
            human.setSpeed(0);
            human.setActionState("idle");
            return true;
        }

        human.setActionState("run");
        human.setSpeed(human.getBaseSpeed() * RUN_SPEED_MULTIPLIER);
        Vector2 target = PathNavigator.findInteractionPoint(human, world, bestHouse, enterRange);
        shelterNavigator.moveTo(human, world, target, deltaTime,
                8.0f, 0.5f, MovementContext.SEEKING_STRUCTURE);

        if (shelterNavigator.isBlocked()) {
            markShelterUnavailable(bestHouse);
            return false;
        }
        clearFleeRoute();
        return true;
    }

    private void fleeFromPredators(Animal owner, List<Animal> predators,
                                   List<Entity> neighbors, World world, float deltaTime) {
        shelterNavigator.clear();
        if (owner.isHidden()) owner.exitBush();
        owner.setActionState("run");
        owner.setSpeed(owner.getBaseSpeed() * RUN_SPEED_MULTIPLIER);

        updateFleeProgress(owner, deltaTime);
        if (fleeTarget != null && fleeNoProgressTimer >= FLEE_NO_PROGRESS_SECONDS) {
            rememberFailedDirection(owner, fleeTarget);
            clearCurrentFleeTarget();
        }

        if (!isFleeTargetUseful(owner, predators, world)) {
            clearCurrentFleeTarget();
            fleeTarget = selectFleeTarget(owner, predators, neighbors, world);
            fleeCommitTimer = fleeTarget == null ? 0.0f : FLEE_COMMIT_SECONDS;
        }

        if (fleeTarget == null) {
            moveDirectlyAway(owner, predators, world, deltaTime);
            return;
        }

        boolean reached = fleeNavigator.moveTo(owner, world, fleeTarget, deltaTime,
                FLEE_REACH_DISTANCE, FLEE_REPATH_SECONDS, MovementContext.FLEEING);
        if (reached) {
            clearCurrentFleeTarget();
            failedFleeDirections.clear();
        } else if (fleeNavigator.isBlocked()) {
            rememberFailedDirection(owner, fleeTarget);
            clearCurrentFleeTarget();
        }
    }

    private Vector2 selectFleeTarget(Animal owner, List<Animal> predators,
                                    List<Entity> neighbors, World world) {
        Vector2 origin = owner.getPosition();
        Vector2 threatCenter = getThreatCenter(predators);
        Vector2 away = origin.copy().subtract(threatCenter);
        if (away.lengthSquared() <= Vector2.EPSILON) {
            double angle = Math.toRadians(Math.floorMod(owner.getId().hashCode(), 360));
            away.set((float) Math.cos(angle), (float) Math.sin(angle));
        } else {
            away.normalize();
        }

        boolean preferPositiveAngles = (owner.getId().hashCode() & 1) == 0;
        float currentThreatDistance = nearestThreatDistance(origin, predators);
        float margin = Math.max(12.0f, owner.getSize() * 0.5f);
        Vector2 bestDirectTarget = null;
        Vector2 bestPathTarget = null;
        float bestDirectScore = -Float.MAX_VALUE;
        float bestPathScore = -Float.MAX_VALUE;

        for (float angle : FLEE_ANGLES) {
            float orderedAngle = preferPositiveAngles ? angle : -angle;
            Vector2 direction = rotate(away, orderedAngle);
            if (isFailedDirection(direction)) continue;

            for (float multiplier : FLEE_DISTANCE_MULTIPLIERS) {
                Vector2 candidate = origin.copy().add(
                        direction.copy().scale(FLEE_TARGET_DISTANCE * multiplier));
                candidate.x = Math.max(margin, Math.min(world.getWidth() - margin, candidate.x));
                candidate.y = Math.max(margin, Math.min(world.getHeight() - margin, candidate.y));

                if (!world.isValidPositionFor(owner, candidate)) continue;
                if (origin.distanceTo(candidate) <= FLEE_REACH_DISTANCE + 8.0f) continue;

                float threatDistance = nearestThreatDistance(candidate, predators);
                float gain = threatDistance - currentThreatDistance;
                float score = threatDistance * 2.0f + gain * 4.0f;
                score += scoreOpenSpace(owner, world, candidate);
                score -= scoreCrowding(owner, neighbors, candidate);
                score -= Math.abs(orderedAngle) * 0.25f;

                boolean direct = TerrainNavigator.hasWalkableLine(
                        world, owner, origin, candidate, MovementContext.FLEEING);
                if (direct && score > bestDirectScore) {
                    bestDirectScore = score;
                    bestDirectTarget = candidate;
                } else if (!direct && score > bestPathScore) {
                    bestPathScore = score;
                    bestPathTarget = candidate;
                }
            }
        }

        return bestDirectTarget != null ? bestDirectTarget : bestPathTarget;
    }

    private boolean isFleeTargetUseful(Animal owner, List<Animal> predators, World world) {
        if (fleeTarget == null || !world.isValidPositionFor(owner, fleeTarget)) return false;
        if (fleeCommitTimer > 0.0f) return true;

        float currentDistance = nearestThreatDistance(owner.getPosition(), predators);
        float targetDistance = nearestThreatDistance(fleeTarget, predators);
        return targetDistance >= currentDistance - 20.0f;
    }

    private void updateFleeProgress(Animal owner, float deltaTime) {
        if (fleeTarget == null || !hasLastFleePosition) {
            lastFleePosition.set(owner.getPosition());
            hasLastFleePosition = true;
            fleeNoProgressTimer = 0.0f;
            return;
        }

        if (lastFleePosition.distanceSquared(owner.getPosition()) > 1.0f) {
            lastFleePosition.set(owner.getPosition());
            fleeNoProgressTimer = 0.0f;
        } else {
            fleeNoProgressTimer += deltaTime;
        }
    }

    private float scoreOpenSpace(Animal owner, World world, Vector2 candidate) {
        float score = 0.0f;
        float probeDistance = Math.max(32.0f, owner.getSize());
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            Vector2 probe = new Vector2(
                    candidate.x + (float) Math.cos(angle) * probeDistance,
                    candidate.y + (float) Math.sin(angle) * probeDistance);
            if (world.isValidPositionFor(owner, probe)) score += 12.0f;
        }
        return score;
    }

    private float scoreCrowding(Animal owner, List<Entity> neighbors, Vector2 candidate) {
        float penalty = 0.0f;
        for (Entity neighbor : neighbors) {
            if (!(neighbor instanceof Animal) || neighbor == owner) continue;
            Animal other = (Animal) neighbor;
            if (!owner.getSpeciesName().equals(other.getSpeciesName())) continue;

            float distance = candidate.distanceTo(other.getPosition());
            if (distance < 80.0f) {
                penalty += (80.0f - distance) * 2.0f;
            }
        }
        return penalty;
    }

    private Vector2 getThreatCenter(List<Animal> predators) {
        Vector2 center = new Vector2();
        for (Animal predator : predators) {
            center.add(predator.getPosition());
        }
        return center.scale(1.0f / predators.size());
    }

    private float nearestThreatDistance(Vector2 position, List<Animal> predators) {
        float nearest = Float.MAX_VALUE;
        for (Animal predator : predators) {
            nearest = Math.min(nearest, position.distanceTo(predator.getPosition()));
        }
        return nearest;
    }

    private void rememberFailedDirection(Animal owner, Vector2 target) {
        Vector2 direction = target.copy().subtract(owner.getPosition());
        if (direction.lengthSquared() <= Vector2.EPSILON) return;
        direction.normalize();
        failedFleeDirections.add(direction);
        if (failedFleeDirections.size() > MAX_FAILED_DIRECTIONS) {
            failedFleeDirections.remove(0);
        }
    }

    private boolean isFailedDirection(Vector2 direction) {
        for (Vector2 failed : failedFleeDirections) {
            float dot = direction.x * failed.x + direction.y * failed.y;
            if (dot > 0.9f) return true;
        }
        return false;
    }

    private void moveDirectlyAway(Animal owner, List<Animal> predators,
                                  World world, float deltaTime) {
        Vector2 direction = owner.getPosition().copy().subtract(getThreatCenter(predators));
        if (direction.lengthSquared() <= Vector2.EPSILON) {
            double angle = Math.toRadians(Math.floorMod(owner.getId().hashCode(), 360));
            direction.set((float) Math.cos(angle), (float) Math.sin(angle));
        } else {
            direction.normalize();
        }

        Vector2 avoidance = AvoidanceStrategy.getAvoidanceForce(owner, world, direction);
        direction.add(avoidance);
        if (direction.lengthSquared() > Vector2.EPSILON) direction.normalize();
        owner.move(direction, deltaTime);
    }

    private Vector2 rotate(Vector2 vector, float degrees) {
        double radians = Math.toRadians(degrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2(
                vector.x * cos - vector.y * sin,
                vector.x * sin + vector.y * cos);
    }

    private void markShelterUnavailable(Entity shelter) {
        unavailableShelter = shelter;
        unavailableShelterTimer = UNAVAILABLE_SHELTER_SECONDS;
        shelterNavigator.clear();
    }

    private void clearCurrentFleeTarget() {
        fleeNavigator.clear();
        fleeTarget = null;
        fleeCommitTimer = 0.0f;
        fleeNoProgressTimer = 0.0f;
        hasLastFleePosition = false;
    }

    private void clearFleeRoute() {
        clearCurrentFleeTarget();
        failedFleeDirections.clear();
    }

    @Override
    public void onExit(LivingBeing owner, World world) {
        shelterNavigator.clear();
        clearFleeRoute();
        unavailableShelter = null;
        unavailableShelterTimer = 0.0f;
    }

    @Override
    public boolean shouldInterrupt(LivingBeing owner, World world) {
        return false;
    }

    @Override
    public boolean handlesStuckRecovery() {
        return true;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public String getName() {
        return "Scared";
    }

    @Override
    public Vector2 getTarget() {
        Vector2 shelterTarget = shelterNavigator.getLastTarget();
        return shelterTarget != null ? shelterTarget : fleeTarget;
    }

    @Override
    public List<Vector2> getPath() {
        return !shelterNavigator.getPath().isEmpty()
                ? shelterNavigator.getPath()
                : fleeNavigator.getPath();
    }
}
