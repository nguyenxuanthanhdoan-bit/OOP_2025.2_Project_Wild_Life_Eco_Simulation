package view.systems;

import core.DisplayMode;
import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.strategies.IStrategy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EntityRenderer {
    private final Camera camera;
    private final MinimalRenderer minimalRenderer;
    private final OverlayRenderer overlayRenderer;
    private final GameConfig config = GameConfig.getInstance();

    public EntityRenderer(Camera camera, MinimalRenderer minimalRenderer, OverlayRenderer overlayRenderer) {
        this.camera = camera;
        this.minimalRenderer = minimalRenderer;
        this.overlayRenderer = overlayRenderer;
    }

    public void renderEntity(Entity e, Graphics2D g2d, float animationTimer, DisplayMode displayMode,
                             boolean showHungerBar, boolean showThirstBar, boolean showHealthBar,
                             boolean showSpeciesName, boolean showDebugPath, boolean showStrategyLabelAll,
                             boolean showAIVision, Entity selectedEntity) {
        if (displayMode == DisplayMode.REALISTIC) {
            Vector2 screenPos = camera.worldToScreen(e.getPosition());
            float zoom = camera.getZoomLevel();

            if (e instanceof model.living_beings.Animal) {
                model.living_beings.Animal animal = (model.living_beings.Animal) e;
                if (animal.isHidden()) return;

                drawDynamicAnimatedSprite(animal, g2d, screenPos, zoom, animationTimer);

                if (animal == selectedEntity) {
                    g2d.setColor(new Color(255, 235, 60, 200));
                    g2d.setStroke(new BasicStroke(Math.max(2.0f, 3.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6f, 6f}, (float)(animationTimer * 20.0f) % 12f));
                    int selSize = (int) ((animal.getSize() + 10) * zoom);
                    g2d.drawOval((int)screenPos.x - selSize/2, (int)screenPos.y - selSize/2, selSize, selSize);
                }

                if (showAIVision) {
                    g2d.setColor(new Color(100, 255, 100, 20));
                    int radius = (int)(animal.getVisionRange() * zoom);
                    g2d.fillOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
                    g2d.setColor(new Color(100, 255, 100, 90));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
                }

                if (showDebugPath && animal == selectedEntity && animal.getCurrentStrategy() != null) {
                    IStrategy strategy = animal.getCurrentStrategy();
                    java.util.List<Vector2> path = strategy.getPath();
                    Vector2 animalScreen = camera.worldToScreen(animal.getPosition());

                    if (path != null && !path.isEmpty()) {
                        g2d.setColor(new Color(255, 200, 0, 200));
                        g2d.setStroke(new BasicStroke(Math.max(1.5f, 2.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        Vector2 prev = animalScreen;
                        for (Vector2 wp : path) {
                            Vector2 scrWp = camera.worldToScreen(wp);
                            g2d.drawLine((int)prev.x, (int)prev.y, (int)scrWp.x, (int)scrWp.y);
                            g2d.fillOval((int)scrWp.x - 4, (int)scrWp.y - 4, 8, 8);
                            prev = scrWp;
                        }
                        Vector2 targetPos = strategy.getTarget();
                        if (targetPos != null) {
                            Vector2 scrTarget = camera.worldToScreen(targetPos);
                            g2d.setColor(new Color(255, 50, 50, 200));
                            g2d.drawLine((int)prev.x, (int)prev.y, (int)scrTarget.x, (int)scrTarget.y);
                            g2d.fillRect((int)scrTarget.x - 5, (int)scrTarget.y - 5, 10, 10);
                        }
                    } else {
                        Vector2 targetPos = strategy.getTarget();
                        if (targetPos != null) {
                            Vector2 scrTarget = camera.worldToScreen(targetPos);
                            g2d.setColor(new Color(80, 200, 255, 180));
                            g2d.setStroke(new BasicStroke(Math.max(1.5f, 2.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 5f}, 0f));
                            g2d.drawLine((int)animalScreen.x, (int)animalScreen.y, (int)scrTarget.x, (int)scrTarget.y);
                            g2d.setStroke(new BasicStroke(1.0f));
                            g2d.fillOval((int)scrTarget.x - 5, (int)scrTarget.y - 5, 10, 10);
                        }
                    }
                }

                if ((showStrategyLabelAll || (showDebugPath && animal == selectedEntity)) && animal.getCurrentStrategy() != null) {
                    IStrategy strategy = animal.getCurrentStrategy();
                    Vector2 animalScreen = camera.worldToScreen(animal.getPosition());
                    String strategyLabel = "[" + strategy.getName() + "] " + animal.getActionState();
                    g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, Math.max(10, (int)(11 * zoom))));
                    java.awt.FontMetrics fm = g2d.getFontMetrics();
                    int labelW = fm.stringWidth(strategyLabel) + 6;
                    int labelH = fm.getHeight();
                    int labelX = (int)animalScreen.x - labelW / 2;
                    int labelY = (int)animalScreen.y - (int)((animal.getSize() / 2 + 36) * zoom) - labelH;
                    g2d.setColor(new Color(0, 0, 0, 160));
                    g2d.fillRoundRect(labelX - 1, labelY - 1, labelW + 2, labelH + 2, 4, 4);
                    g2d.setColor(new Color(255, 240, 80));
                    g2d.drawString(strategyLabel, labelX + 3, labelY + fm.getAscent());
                }
                if (zoom >= config.STATUS_BAR_MIN_ZOOM && (showHungerBar || showThirstBar || showSpeciesName)) {
                    int barW = Math.max(24, (int)(32 * zoom));
                    int barH = Math.max(2, Math.min(5, Math.round(2.4f * zoom)));
                    int barX = (int)screenPos.x - barW / 2;
                    int barY = (int)screenPos.y - (int)((animal.getSize() / 2 + 10) * zoom);

                    int currentY = barY;

                    if (showSpeciesName) {
                        g2d.setFont(new Font("SansSerif", Font.BOLD, (int)Math.max(10, 11 * zoom)));
                        String text = animal.getSpeciesName() + (animal.isAdult() ? "" : " (Child)");
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = (int)screenPos.x - fm.stringWidth(text) / 2;
                        int textY = currentY - 4;
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(text, textX + 1, textY + 1);
                        g2d.setColor(new Color(230, 240, 255));
                        g2d.drawString(text, textX, textY);
                    }

                    if (showHealthBar) {
                        overlayRenderer.drawHealthHearts(g2d, barX, currentY - 8 - (int)(6 * zoom), barW, animal.getHealth(), animal.getMaxHealth(), zoom);
                    }

                    if (showHungerBar || showThirstBar) {
                        double hungerRatio = animal.getHunger() / animal.getMaxHunger();
                        double thirstRatio = animal.getThirst() / animal.getMaxThirst();
                        overlayRenderer.drawCombinedStatusBar(g2d, barX, currentY, barW, barH, hungerRatio, thirstRatio);
                        currentY += barH + 2;
                    }
                }
            } else if (e instanceof model.items.FireballProjectile) {
                drawHunterProjectile((model.items.FireballProjectile) e, g2d, screenPos, zoom);
            } else if (e instanceof model.structures.Lantern) {
                drawLantern((model.structures.Lantern) e, g2d, screenPos, zoom, overlayRenderer.getDarknessAlpha(e.getWorld() != null ? e.getWorld().getTimeOfDay() : 0), animationTimer);
            } else {
                BufferedImage img = null;
                String variant = e.getImageVariant();
                
                if (e instanceof model.plants.FruitTree
                        && e.getWorld() != null
                        && e.getWorld().getWinterProgress() >= config.WINTER_TREE_SPRITE_THRESHOLD) {
                    if (variant.matches("(?i)Tree_[1-6]")) {
                        variant = "tree_winter_1";
                    } else {
                        variant = "tree_winter_2";
                    }
                }

                if (variant != null && !variant.isEmpty()) {
                    img = AssetManager.getInstance().getAsset(variant.toLowerCase());
                }

                if (img != null) {
                    float aspect = (float) img.getHeight() / img.getWidth();
                    int w = (int) (e.getSize() * zoom);
                    int h = (int) (w * aspect);
                    
                    java.awt.Composite originalComposite = g2d.getComposite();
                    if (e instanceof model.plants.Seaweed) {
                        g2d.setComposite(java.awt.AlphaComposite.getInstance(
                                java.awt.AlphaComposite.SRC_OVER, 0.65f));
                    }

                    g2d.drawImage(img, (int)screenPos.x - w / 2,
                            (int) screenPos.y - h / 2, w, h, null);

                    if (e instanceof model.plants.Seaweed) {
                        g2d.setComposite(originalComposite);
                    }

                    if (e instanceof model.structures.Boat) {
                        model.structures.Boat boat = (model.structures.Boat) e;
                        if (boat.getState() == model.structures.Boat.BoatState.FISHING) {
                            BufferedImage net = AssetManager.getInstance().getAsset("fishing_net");
                            if (net != null) {
                                int netW = (int) (32 * zoom);
                                int netH = (int) (32 * zoom);
                                g2d.drawImage(net, (int)screenPos.x + w/2, (int)screenPos.y - netH/2, netW, netH, null);
                            }
                        }
                    }
                }
            }
        } else {
            minimalRenderer.renderEntity(e, g2d);
        }
    }

    private void drawLantern(model.structures.Lantern lantern, Graphics2D g2d, Vector2 screenPos, float zoom, float darknessAlpha, float animationTimer) {
        String type = lantern.getLanternType();
        BufferedImage sheet = AssetManager.getInstance().getAsset(type);
        if (sheet == null) sheet = AssetManager.getInstance().getAsset("lantern");
        if (sheet == null) return;

        int frameWidth = sheet.getWidth();
        int frameHeight = sheet.getHeight();
        int columns = 1;

        if (sheet.getWidth() == 90 && sheet.getHeight() == 49) {
            frameWidth = 30;
            frameHeight = 49;
            columns = 3;
        }

        int frameIndex = 0;
        if (columns > 1) {
            if (darknessAlpha <= 0.01f) {
                frameIndex = 0;
            } else {
                int animatedFrames = Math.max(1, Math.min(2, columns - 1));
                frameIndex = 1 + ((int)(lantern.getAnimationTime() * 5) % animatedFrames);
            }
        }

        int col = frameIndex % columns;
        int row = frameIndex / columns;
        int srcX = col * frameWidth;
        int srcY = row * frameHeight;

        float baseSize = 16f;
        if (type != null && (type.equals("lantern_2") || type.equals("lantern_3"))) {
            baseSize = 32f;
        }

        int w = (int) (baseSize * zoom);
        int h = (int) (baseSize * frameHeight / (float)frameWidth * zoom);

        g2d.drawImage(sheet,
                (int)screenPos.x - w / 2, (int)screenPos.y - h / 2,
                (int)screenPos.x + w / 2, (int)screenPos.y + h / 2,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight, null);
    }

    private void drawDynamicAnimatedSprite(model.living_beings.Animal animal,
                                           Graphics2D g2d, Vector2 screenPos,
                                           float zoom, float animationTimer) {
        String species = animal.getSpriteKey();
        String state = animal.getAnimationState();
        boolean moving = animal.getSpeed() > config.MOVEMENT_SPEED_EPSILON;

        BufferedImage sheet = getAnimalAnimationSheet(species, state);
        if (sheet == null) return;

        BufferedImage base = AssetManager.getInstance().getAsset(species + "_west");
        if (base == null) base = sheet;

        int baseFrameW = Math.max(1, base.getWidth());
        int baseFrameH = Math.max(1, base.getHeight());
        int cols = Math.max(1, Math.round(sheet.getWidth() / (float) baseFrameW));
        int rows = Math.max(1, Math.round(sheet.getHeight() / (float) baseFrameH));
        int frameW = Math.max(1, sheet.getWidth() / cols);
        int frameH = Math.max(1, sheet.getHeight() / rows);

        if ("sleep".equals(state)) {
            frameH = Math.max(1, sheet.getHeight());
            frameW = Math.max(1, Math.min(frameH, sheet.getWidth()));
            cols = Math.max(1, sheet.getWidth() / frameW);
            rows = 1;
        }
        int totalFrames = cols * rows;
        if (totalFrames <= 0) totalFrames = 1;

        int frameIdx = 0;
        if (totalFrames > 1) {
            float animSpeed = (state.equals("run")) ? config.ANIMATION_FRAME_DURATION * 0.6f : config.ANIMATION_FRAME_DURATION;
            if ("elephant".equals(species)) {
                animSpeed *= 1.5f;
            }
            frameIdx = (int) (animationTimer / animSpeed) % totalFrames;
        }

        int drawSize = (int) (animal.getSize() * zoom);

        int dstX1 = (int) screenPos.x - drawSize / 2;
        int dstY1 = (int) screenPos.y - drawSize / 2;
        int dstX2 = dstX1 + drawSize;
        int dstY2 = dstY1 + drawSize;

        boolean shouldFlip = animal.isFacingRight() != animal.isSpriteFacingRightByDefault();
        if (animal instanceof model.living_beings.Fish) {
            shouldFlip = !animal.isFacingRight();
        }

        if (shouldFlip) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        int srcX1 = (frameIdx % cols) * frameW;
        int srcY1 = (frameIdx / cols) * frameH;
        int srcX2 = srcX1 + frameW;
        int srcY2 = srcY1 + frameH;

        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        if (animal instanceof model.living_beings.Fish && moving) {
            double swimWobble = Math.sin(animationTimer * 15.0) * Math.toRadians(5);
            g2d.rotate(swimWobble, screenPos.x, screenPos.y);
        }

        java.awt.Composite originalComposite = g2d.getComposite();
        if (animal instanceof model.living_beings.Fish) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.65f));
        }

        g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);

        if (animal.getDamageBlinkTimer() > 0) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_ATOP, 0.5f));
            g2d.setColor(java.awt.Color.RED);
            int drawX = Math.min(dstX1, dstX2);
            int drawY = Math.min(dstY1, dstY2);
            g2d.fillOval(drawX, drawY, drawSize, drawSize);
            g2d.setComposite(originalComposite);
        }

        if (animal instanceof model.living_beings.Fish) {
            g2d.setComposite(originalComposite);
        }

        g2d.setTransform(oldTransform);
    }

    private BufferedImage getAnimalAnimationSheet(String species, String state) {
        AssetManager assets = AssetManager.getInstance();
        BufferedImage sheet;
        if ("eat".equals(state) || "drink".equals(state)) {
            return getEatDrinkSheet(species);
        }

        sheet = assets.getAsset(species + "_" + state);
        if (sheet != null) return sheet;

        if ("attack".equals(state)) {
            sheet = assets.getAsset(species + "_run");
            if (sheet != null) return sheet;
        }
        if ("run".equals(state)) {
            sheet = assets.getAsset(species + "_walk");
            if (sheet != null) return sheet;
        }
        if ("walk".equals(state) || "idle".equals(state) || "sleep".equals(state)) {
            sheet = assets.getAsset(species + "_idle");
            if (sheet != null) return sheet;
            sheet = assets.getAsset(species + "_west");
            if (sheet != null) return sheet;
        }

        sheet = assets.getAsset(species + "_walk");
        if (sheet == null) sheet = assets.getAsset(species + "_idle");
        if (sheet == null) sheet = assets.getAsset(species + "_west");
        if (sheet == null) sheet = assets.getAsset(species + ".png");
        return sheet;
    }

    private BufferedImage getEatDrinkSheet(String species) {
        AssetManager assets = AssetManager.getInstance();
        if ("wolf".equals(species)) {
            return assets.getAsset("wolf_west");
        }

        BufferedImage sheet = assets.getAsset(species + "_eat");
        if (sheet == null) sheet = assets.getAsset(species + "_drink");
        if (sheet == null) sheet = assets.getAsset(species + "_west");
        return sheet;
    }

    private void drawHunterProjectile(model.items.FireballProjectile projectile,
                                      Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage dart = AssetManager.getInstance().getAsset("dart");
        if (dart != null) {
            int drawSize = Math.max(8, Math.round(projectile.getSize() * zoom));
            java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(screenPos.x, screenPos.y);
            g2d.rotate(projectile.getRotationRadians());
            g2d.drawImage(dart, -drawSize / 2, -drawSize / 2, drawSize, drawSize, null);
            g2d.setTransform(oldTransform);
            return;
        }

        Entity e = projectile;
        int size = Math.max(8, Math.round(e.getSize() * zoom));
        int glow = Math.max(size + 8, Math.round(size * 1.8f));

        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2d.setColor(new Color(255, 80, 25));
        g2d.fillOval((int) screenPos.x - glow / 2, (int) screenPos.y - glow / 2, glow, glow);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
        g2d.setColor(new Color(220, 20, 20));
        g2d.fillOval((int) screenPos.x - size / 2, (int) screenPos.y - size / 2, size, size);

        int core = Math.max(3, size / 2);
        g2d.setColor(new Color(255, 210, 80));
        g2d.fillOval((int) screenPos.x - core / 2, (int) screenPos.y - core / 2, core, core);
        g2d.setComposite(oldComposite);
    }
}
