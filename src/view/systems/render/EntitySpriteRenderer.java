package view.systems.render;

import core.GameConfig;
import core.Vector2;
import model.entity.Entity;
import model.living_beings.animal.Animal;
import model.living_beings.Tiger;
import model.living_beings.Wolf;
import model.strategies.IStrategy;
import view.systems.Camera;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EntitySpriteRenderer {

    private AssetManager assetManager;
    private EnvironmentOverlayRenderer overlayRenderer;
    private final GameConfig config = GameConfig.getInstance();
    private final float FRAME_DURATION = config.ANIMATION_FRAME_DURATION;

    public EntitySpriteRenderer(AssetManager assetManager, EnvironmentOverlayRenderer overlayRenderer) {
        this.assetManager = assetManager;
        this.overlayRenderer = overlayRenderer;
    }

    public void renderAnimalSpriteAndUI(Animal animal, Graphics2D g2d, Vector2 screenPos, float zoom, float animationTimer, RenderSystem renderSystem, Camera camera) {
        drawDynamicAnimatedSprite(animal, g2d, screenPos, zoom, animationTimer);

        // Highlight selected animal
        if (animal == renderSystem.getSelectedEntity()) {
            g2d.setColor(new Color(255, 235, 60, 200)); // Glowing gold
            g2d.setStroke(new BasicStroke(Math.max(2.0f, 3.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6f, 6f}, (float)(animationTimer * 20.0f) % 12f));
            int selSize = (int) ((animal.getSize() + 10) * zoom);
            g2d.drawOval((int)screenPos.x - selSize/2, (int)screenPos.y - selSize/2, selSize, selSize);
        }

        // Draw AI Vision Range
        if (renderSystem.isShowAIVision()) {
            g2d.setColor(new Color(100, 255, 100, 20)); // Light transparent green
            int radius = (int)(animal.getVisionRange() * zoom);
            g2d.fillOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
            g2d.setColor(new Color(100, 255, 100, 90));
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawOval((int)screenPos.x - radius, (int)screenPos.y - radius, radius * 2, radius * 2);
        }

        // Draw Debug Path — chỉ hiện cho con vật đang được chọn
        if (renderSystem.isShowDebugPath() && animal == renderSystem.getSelectedEntity() && animal.getCurrentStrategy() != null) {
            IStrategy strategy = animal.getCurrentStrategy();
            java.util.List<Vector2> path = strategy.getPath();
            Vector2 animalScreen = camera.worldToScreen(animal.getPosition());

            if (path != null && !path.isEmpty()) {
                g2d.setColor(new Color(255, 200, 0, 200)); // Cam — đường vòng A*
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
                    g2d.setColor(new Color(255, 50, 50, 200)); // Đỏ — target
                    g2d.drawLine((int)prev.x, (int)prev.y, (int)scrTarget.x, (int)scrTarget.y);
                    g2d.fillRect((int)scrTarget.x - 5, (int)scrTarget.y - 5, 10, 10);
                }
            } else {
                Vector2 targetPos = strategy.getTarget();
                if (targetPos != null) {
                    Vector2 scrTarget = camera.worldToScreen(targetPos);
                    g2d.setColor(new Color(80, 200, 255, 180)); // Xanh dương
                    g2d.setStroke(new BasicStroke(Math.max(1.5f, 2.0f * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 5f}, 0f));
                    g2d.drawLine((int)animalScreen.x, (int)animalScreen.y, (int)scrTarget.x, (int)scrTarget.y);
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.fillOval((int)scrTarget.x - 5, (int)scrTarget.y - 5, 10, 10);
                }
            }
        }

        if ((renderSystem.showStrategyLabelAll || (renderSystem.isShowDebugPath() && animal == renderSystem.getSelectedEntity())) && animal.getCurrentStrategy() != null) {
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

        if (zoom >= config.STATUS_BAR_MIN_ZOOM && (renderSystem.isShowHungerBar() || renderSystem.isShowThirstBar() || renderSystem.isShowSpeciesName())) {
            int barW = Math.max(24, (int)(32 * zoom));
            int barH = Math.max(2, Math.min(5, Math.round(2.4f * zoom)));
            int barX = (int)screenPos.x - barW / 2;
            int barY = (int)screenPos.y - (int)((animal.getSize() / 2 + 10) * zoom);

            int currentY = barY;

            if (renderSystem.isShowSpeciesName()) {
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

            if (renderSystem.isShowHealthBar()) {
                drawHealthHearts(g2d, barX, currentY - 8 - (int)(6 * zoom), barW, animal.getHealth(), animal.getMaxHealth(), zoom);
            }

            if (renderSystem.isShowHungerBar() || renderSystem.isShowThirstBar()) {
                double hungerRatio = animal.getHunger() / animal.getMaxHunger();
                double thirstRatio = animal.getThirst() / animal.getMaxThirst();
                drawCombinedStatusBar(g2d, barX, currentY, barW, barH, hungerRatio, thirstRatio);
                currentY += barH + 2;
            }
        }
    }

    public void drawLantern(model.structures.Lantern lantern, Graphics2D g2d, Vector2 screenPos, float zoom) {
        String type = lantern.getLanternType();
        BufferedImage sheet = assetManager.getAsset(type);
        if (sheet == null) sheet = assetManager.getAsset("lantern");
        if (sheet == null) return;

        float darknessAlpha = 0f;
        if (lantern.getWorld() != null) {
            darknessAlpha = overlayRenderer.getDarknessAlpha(lantern.getWorld().getTimeOfDay());
        }

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

    private void drawCombinedStatusBar(Graphics2D g2d, int x, int y, int width, int height,
                                       double hungerRatio, double thirstRatio) {
        double clampedHunger = Math.max(0.0, Math.min(1.0, hungerRatio));
        double clampedThirst = Math.max(0.0, Math.min(1.0, thirstRatio));

        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(x - 1, y - 1, width + 2, height + 2);
        g2d.setColor(new java.awt.Color(50, 50, 50));
        g2d.fillRect(x, y, width, height);

        int hungerFill = (int) (width * clampedHunger / 2);
        if (hungerFill > 0) {
            g2d.setColor(new java.awt.Color(220, 60, 60));
            g2d.fillRect(x, y, hungerFill, height);
        }

        int thirstFill = (int) (width * clampedThirst / 2);
        if (thirstFill > 0) {
            g2d.setColor(new java.awt.Color(60, 140, 240));
            g2d.fillRect(x + width - thirstFill, y, thirstFill, height);
        }

        g2d.setColor(java.awt.Color.BLACK);
        g2d.drawLine(x + width / 2, y, x + width / 2, y + height);
    }

    private void drawHealthHearts(Graphics2D g2d, int x, int y, int barW, double currentHp, double maxHp, float zoom) {
        BufferedImage heartImg = assetManager.getAsset("heart");
        if (heartImg == null) return;

        int hpPerHeart = 20;
        int maxHearts = (int) Math.ceil(maxHp / hpPerHeart);
        double currentHearts = currentHp / hpPerHeart;

        int baseHeartSize = 8;
        int heartSize = Math.max(5, (int)(baseHeartSize * zoom));
        int spacing = Math.max(1, (int)(1 * zoom));
        int maxPerRow = 8;
        
        for (int i = 0; i < maxHearts; i++) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;
            
            int heartsInThisRow = Math.min(maxPerRow, maxHearts - row * maxPerRow);
            int rowWidth = heartsInThisRow * heartSize + (heartsInThisRow - 1) * spacing;
            int startX = x + (barW - rowWidth) / 2;
            
            int hx = startX + col * (heartSize + spacing);
            int hy = y - row * (heartSize + spacing);

            if (currentHearts >= i + 1) {
                g2d.drawImage(heartImg, hx, hy, heartSize, heartSize, null);
            } else if (currentHearts > i) {
                double fraction = currentHearts - i;
                int partialW = (int) (heartSize * fraction);
                if (partialW > 0) {
                    g2d.drawImage(heartImg, 
                        hx, hy, hx + partialW, hy + heartSize,
                        0, 0, (int)(heartImg.getWidth() * fraction), heartImg.getHeight(),
                        null);
                }
            } else {
                Composite original = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2d.drawImage(heartImg, hx, hy, heartSize, heartSize, null);
                g2d.setComposite(original);
            }
        }
    }

    public void drawHunterProjectile(model.items.FireballProjectile projectile,
                                      Graphics2D g2d, Vector2 screenPos, float zoom) {
        BufferedImage dart = assetManager.getAsset("dart");
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

    private void drawDynamicAnimatedSprite(model.living_beings.animal.Animal animal,
                                           Graphics2D g2d, Vector2 screenPos,
                                           float zoom, float animationTimer) {
        String species = animal.getSpriteKey();
        String state = animal.getAnimationState();
        boolean moving = animal.getSpeed() > config.MOVEMENT_SPEED_EPSILON;

        BufferedImage sheet = getAnimalAnimationSheet(species, state);
        if (sheet == null) return;

        BufferedImage base = assetManager.getAsset(species + "_west");
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
        if (totalFrames <= 0) return;

        boolean isRun = "run".equals(state) || "attack".equals(state);
        float currentFrameDuration = isRun ? (FRAME_DURATION * 0.6f) : FRAME_DURATION;
        if ("sleep".equals(state) || "eat".equals(state) || "drink".equals(state)) {
            currentFrameDuration = FRAME_DURATION * 1.5f;
        }

        int frameIdx = 0;
        if (moving || "sleep".equals(state) || "eat".equals(state) || "drink".equals(state) || "attack".equals(state)) {
            frameIdx = (int) (animationTimer / currentFrameDuration) % totalFrames;
        }

        if (totalFrames == 1) frameIdx = 0;

        int drawSize = (int) (animal.getSize() * zoom);

        int dstX1 = (int)screenPos.x - drawSize/2;
        int dstY1 = (int)screenPos.y - drawSize/2;
        int dstX2 = (int)screenPos.x + drawSize/2;
        int dstY2 = (int)screenPos.y + drawSize/2;

        if (animal.isFacingRight() && !("sleep".equals(state))) {
            int temp = dstX1;
            dstX1 = dstX2;
            dstX2 = temp;
        }

        if (animal.isAdult() && ("tiger".equals(species))) {
            drawTiger((Tiger) animal, g2d, screenPos, zoom, animationTimer);
            return;
        }
        if (animal.isAdult() && ("wolf".equals(species))) {
            drawWolf((Wolf) animal, g2d, screenPos, zoom, animationTimer);
            return;
        }

        int col = frameIdx % cols;
        int row = frameIdx / cols;

        int srcX1 = col * frameW;
        int srcY1 = row * frameH;
        int srcX2 = srcX1 + frameW;
        int srcY2 = srcY1 + frameH;

        g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
    }

    private BufferedImage getAnimalAnimationSheet(String species, String state) {
        BufferedImage sheet;

        if ("walk".equals(state) || "run".equals(state) || "idle".equals(state) || "attack".equals(state)) {
            return getWalkRunSheet(species, state);
        } else if ("eat".equals(state) || "drink".equals(state)) {
            return getEatDrinkSheet(species);
        }

        sheet = assetManager.getAsset(species + "_" + state);
        if (sheet != null) return sheet;

        if ("attack".equals(state)) {
            sheet = assetManager.getAsset(species + "_run");
            if (sheet != null) return sheet;
        }
        if ("run".equals(state)) {
            sheet = assetManager.getAsset(species + "_walk");
            if (sheet != null) return sheet;
        }
        if ("walk".equals(state) || "idle".equals(state) || "sleep".equals(state)) {
            sheet = assetManager.getAsset(species + "_idle");
            if (sheet != null) return sheet;
            sheet = assetManager.getAsset(species + "_west");
            if (sheet != null) return sheet;
        }

        sheet = assetManager.getAsset(species + "_walk");
        if (sheet == null) sheet = assetManager.getAsset(species + "_idle");
        if (sheet == null) sheet = assetManager.getAsset(species + "_west");
        if (sheet == null) sheet = assetManager.getAsset(species + ".png");
        return sheet;
    }

    private BufferedImage getWalkRunSheet(String species, String state) {
        if ("wolf".equals(species)) {
            return assetManager.getAsset("wolf_west");
        }
        BufferedImage sheet = assetManager.getAsset(species + "_" + state);
        if (sheet == null) sheet = assetManager.getAsset(species + "_walk");
        if (sheet == null) sheet = assetManager.getAsset(species + "_idle");
        if (sheet == null) sheet = assetManager.getAsset(species + "_west");
        return sheet;
    }

    private BufferedImage getEatDrinkSheet(String species) {
        if ("wolf".equals(species)) {
            return assetManager.getAsset("wolf_west");
        }

        BufferedImage sheet = assetManager.getAsset(species + "_eat");
        if (sheet == null) sheet = assetManager.getAsset(species + "_drink");
        if (sheet == null) sheet = assetManager.getAsset(species + "_west");
        return sheet;
    }

    private void drawTiger(Tiger t, Graphics2D g2d, Vector2 screenPos, float zoom, float animationTimer) {
        BufferedImage sheet = assetManager.getAsset("tiger");
        if (sheet != null) {
            int frameW = sheet.getWidth() / 3;
            int frameH = sheet.getHeight() / 2;
            int frameIdx = 0;
            if (t.getSpeed() > config.MOVEMENT_SPEED_EPSILON) {
                frameIdx = (int) (1 + (animationTimer / FRAME_DURATION) % 5);
            }

            int drawSize = (int) (t.getSize() * zoom);

            int dstX1 = (int)screenPos.x - drawSize/2;
            int dstY1 = (int)screenPos.y - drawSize/2;
            int dstX2 = (int)screenPos.x + drawSize/2;
            int dstY2 = (int)screenPos.y + drawSize/2;

            if (t.isFacingRight()) {
                int temp = dstX1;
                dstX1 = dstX2;
                dstX2 = temp;
            }

            int col = frameIdx % 3;
            int row = frameIdx / 3;

            int srcX1 = col * frameW;
            int srcY1 = row * frameH;
            int srcX2 = (col + 1) * frameW;
            int srcY2 = (row + 1) * frameH;

            g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
            return;
        }

        int size = (int) (t.getSize() * zoom);
        int halfSize = size / 2;
        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(235, 115, 20)); // Tiger Orange
        g.fillRoundRect(x - halfSize, y - halfSize / 2, size, (int)(size * 0.7f), 15, 15);

        g.setColor(new Color(250, 240, 220));
        g.fillRoundRect(x - halfSize / 2, y + (int)(size * 0.05f), halfSize * 2, (int)(size * 0.15f), 5, 5);

        boolean facingRight = t.isFacingRight();
        int headX = facingRight ? (x + halfSize / 3) : (x - halfSize / 3 - size / 4);
        int headY = y - halfSize / 3;
        int headW = (int) (size * 0.5f);
        int headH = (int) (size * 0.5f);

        g.setColor(new Color(40, 40, 40));
        int[] earLeftX = {headX + 5, headX + 15, headX + 8};
        int[] earLeftY = {headY, headY, headY - 12};
        int[] earRightX = {headX + headW - 15, headX + headW - 5, headX + headW - 8};
        int[] earRightY = {headY, headY, headY - 12};
        g.fillPolygon(earLeftX, earLeftY, 3);
        g.fillPolygon(earRightX, earRightY, 3);

        g.setColor(new Color(250, 210, 210));
        int[] innerEarLeftX = {headX + 7, headX + 13, headX + 9};
        int[] innerEarLeftY = {headY, headY, headY - 8};
        int[] innerEarRightX = {headX + headW - 13, headX + headW - 7, headX + headW - 9};
        int[] innerEarRightY = {headY, headY, headY - 8};
        g.fillPolygon(innerEarLeftX, innerEarLeftY, 3);
        g.fillPolygon(innerEarRightX, innerEarRightY, 3);

        g.setColor(new Color(235, 115, 20));
        g.fillOval(headX, headY, headW, headH);

        g.setColor(new Color(30, 30, 30));
        g.setStroke(new BasicStroke(2 * zoom));
        g.drawLine(headX + headW / 2, headY, headX + headW / 2, headY + 8);
        g.drawLine(headX + headW / 2 - 4, headY, headX + headW / 2 - 4, headY + 6);
        g.drawLine(headX + headW / 2 + 4, headY, headX + headW / 2 + 4, headY + 6);

        g.drawLine(x, y - halfSize / 2, x, y + (int)(size * 0.1f));
        g.drawLine(x - size / 4, y - halfSize / 2, x - size / 4, y);
        g.drawLine(x + size / 4, y - halfSize / 2, x + size / 4, y);

        g.setColor(new Color(80, 200, 120));
        int eyeY = headY + headH / 3;
        int eyeSize = Math.max(3, (int) (4 * zoom));
        if (facingRight) {
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.8f), eyeY, eyeSize, eyeSize);
        } else {
            g.fillOval(headX + (int)(headW * 0.2f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
        }

        g.setColor(new Color(250, 240, 220));
        int snoutW = (int) (headW * 0.4f);
        int snoutH = (int) (headH * 0.3f);
        int snoutX = headX + (headW - snoutW) / 2;
        int snoutY = headY + (int)(headH * 0.5f);
        g.fillOval(snoutX, snoutY, snoutW, snoutH);
        g.setColor(Color.BLACK);
        g.fillOval(snoutX + snoutW / 2 - 2, snoutY + 1, 4, 3);

        g.dispose();
    }

    private void drawWolf(Wolf w, Graphics2D g2d, Vector2 screenPos, float zoom, float animationTimer) {
        BufferedImage sheet = assetManager.getAsset("wolf");
        if (sheet != null) {
            int frameW = sheet.getWidth() / 2;
            int frameH = sheet.getHeight() / 2;
            int frameIdx = (int) (animationTimer / FRAME_DURATION) % 4;

            int drawSize = (int) (w.getSize() * zoom);

            int dstX1 = (int)screenPos.x - drawSize/2;
            int dstY1 = (int)screenPos.y - drawSize/2;
            int dstX2 = (int)screenPos.x + drawSize/2;
            int dstY2 = (int)screenPos.y + drawSize/2;

            if (w.isFacingRight()) {
                int temp = dstX1;
                dstX1 = dstX2;
                dstX2 = temp;
            }

            int col = frameIdx % 2;
            int row = frameIdx / 2;

            int srcX1 = col * frameW;
            int srcY1 = row * frameH;
            int srcX2 = (col + 1) * frameW;
            int srcY2 = (row + 1) * frameH;

            g2d.drawImage(sheet, dstX1, dstY1, dstX2, dstY2, srcX1, srcY1, srcX2, srcY2, null);
            return;
        }

        int size = (int) (w.getSize() * zoom);
        int halfSize = size / 2;
        int x = (int) screenPos.x;
        int y = (int) screenPos.y;

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean facingRight = w.isFacingRight();
        g.setColor(new Color(60, 70, 80));
        if (facingRight) {
            int[] tailX = {x - halfSize, x - halfSize - size / 2, x - halfSize + size / 6};
            int[] tailY = {y + size / 6, y + size / 4, y - size / 6};
            g.fillPolygon(tailX, tailY, 3);
        } else {
            int[] tailX = {x + halfSize, x + halfSize + size / 2, x + halfSize - size / 6};
            int[] tailY = {y + size / 6, y + size / 4, y - size / 6};
            g.fillPolygon(tailX, tailY, 3);
        }

        g.setColor(new Color(75, 85, 95));
        g.fillRoundRect(x - halfSize, y - halfSize / 2, size, (int)(size * 0.7f), 12, 12);

        g.setColor(new Color(200, 205, 210));
        int maneW = (int) (size * 0.35f);
        int maneX = facingRight ? (x + halfSize / 6) : (x - halfSize / 3 - maneW);
        g.fillRoundRect(maneX, y - halfSize / 4, maneW, (int)(size * 0.5f), 8, 8);

        int headX = facingRight ? (x + halfSize / 3) : (x - halfSize / 3 - size / 4);
        int headY = y - halfSize / 3;
        int headW = (int) (size * 0.48f);
        int headH = (int) (size * 0.48f);

        g.setColor(new Color(55, 65, 75));
        int[] earLeftX = {headX + 4, headX + 12, headX + 6};
        int[] earLeftY = {headY + 2, headY + 2, headY - 14};
        int[] earRightX = {headX + headW - 12, headX + headW - 4, headX + headW - 6};
        int[] earRightY = {headY + 2, headY + 2, headY - 14};
        g.fillPolygon(earLeftX, earLeftY, 3);
        g.fillPolygon(earRightX, earRightY, 3);

        g.setColor(new Color(110, 120, 130));
        int[] innerLeftX = {headX + 6, headX + 10, headX + 7};
        int[] innerLeftY = {headY + 2, headY + 2, headY - 9};
        int[] innerRightX = {headX + headW - 10, headX + headW - 6, headX + headW - 7};
        int[] innerRightY = {headY + 2, headY + 2, headY - 9};
        g.fillPolygon(innerLeftX, innerLeftY, 3);
        g.fillPolygon(innerRightX, innerRightY, 3);

        g.setColor(new Color(75, 85, 95));
        g.fillOval(headX, headY, headW, headH);

        g.setColor(new Color(255, 191, 0));
        int eyeY = headY + headH / 3 + 1;
        int eyeSize = Math.max(2, (int) (3 * zoom));
        if (facingRight) {
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.8f), eyeY, eyeSize, eyeSize);
        } else {
            g.fillOval(headX + (int)(headW * 0.2f), eyeY, eyeSize, eyeSize);
            g.fillOval(headX + (int)(headW * 0.5f), eyeY, eyeSize, eyeSize);
        }

        g.setColor(new Color(55, 65, 75));
        int snoutW = (int) (headW * 0.5f);
        int snoutH = (int) (headH * 0.25f);
        int snoutX = facingRight ? (headX + headW - snoutW / 3) : (headX - snoutW * 2 / 3);
        int snoutY = headY + (int)(headH * 0.55f);
        g.fillRoundRect(snoutX, snoutY, snoutW, snoutH, 4, 4);

        g.dispose();
    }
}
