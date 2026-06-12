package audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý toàn bộ âm thanh trong game.
 * - Nhạc nền (ingame.wav): loop liên tục, âm lượng nhỏ.
 * - Tiếng động vật:
 *     + Click chọn con vật → phát tiếng loài đó 1 lần.
 *     + Camera zoom đủ gần + viewport có <= MAX_ANIMALS_FOR_AMBIENT con → phát tiếng ambient.
 */
public class SoundManager {

    // =============================================
    // BẢNG MAP TÊN LOÀI SANG TÊN FILE
    // =============================================

    /**
     * Map tên loài (getSpeciesName()) sang tên file .wav tương ứng.
     * Hỗ trợ cả tiếng Việt và tiếng Anh.
     */
    private static final Map<String, String> SPECIES_TO_FILE = new HashMap<>();
    static {
        SPECIES_TO_FILE.put("hươu", "deer");
        SPECIES_TO_FILE.put("deer", "deer");
        SPECIES_TO_FILE.put("sói", "wolf");
        SPECIES_TO_FILE.put("wolf", "wolf");
        SPECIES_TO_FILE.put("hổ", "tiger");
        SPECIES_TO_FILE.put("tiger", "tiger");
        SPECIES_TO_FILE.put("thỏ", "rabbit");
        SPECIES_TO_FILE.put("rabbit", "rabbit");
        SPECIES_TO_FILE.put("voi", "elephant");
        SPECIES_TO_FILE.put("elephant", "elephant");
        SPECIES_TO_FILE.put("cá mập", "shark");
        SPECIES_TO_FILE.put("shark", "shark");
        SPECIES_TO_FILE.put("cá", "fish");
        SPECIES_TO_FILE.put("fish", "fish");
        // Fox
        SPECIES_TO_FILE.put("fox", "fox");
        // Human và Hunter cùng dùng human.wav
        SPECIES_TO_FILE.put("human", "human");
        SPECIES_TO_FILE.put("villager", "human");
        SPECIES_TO_FILE.put("dân làng", "human");
        SPECIES_TO_FILE.put("fisherman", "human");
        SPECIES_TO_FILE.put("ngư dân", "human");
        SPECIES_TO_FILE.put("thợ săn", "human");
        SPECIES_TO_FILE.put("hunter", "human");
    }

    // =============================================
    // CẤU HÌNH
    // =============================================

    private static final String SOUND_DIR = "resources/assets/Sounds/";

    /** Âm lượng nhạc nền (0.0 – 1.0). */
    private float bgmVolume = 0.3f;

    /** Âm lượng tiếng động vật khi click (0.0 – 1.0). */
    private float clickSoundVolume = 0.8f;

    /** Âm lượng tiếng ambient (khi zoom gần, ít con) (0.0 – 1.0). */
    private float ambientSoundVolume = 0.5f;

    /** Mức zoom tối thiểu để bật tiếng ambient. */
    public static final float MIN_ZOOM_FOR_AMBIENT = 1.5f;

    /** Số con vật tối đa trong viewport để kích hoạt ambient sound. */
    public static final int MAX_ANIMALS_FOR_AMBIENT = 3;

    /** Cooldown giữa 2 lần phát tiếng ambient của CÙNG 1 loài (giây). */
    private static final float AMBIENT_COOLDOWN_PER_SPECIES = 8.0f;

    /** Cooldown tối thiểu khi click liên tiếp (giây). */
    private static final float CLICK_COOLDOWN = 1.5f;

    // =============================================
    // TRẠNG THÁI NỘI BỘ
    // =============================================

    private Clip bgmClip;
    private FloatControl bgmVolumeControl;

    /** Cache các Clip tiếng động vật (load 1 lần, tái sử dụng). */
    private final Map<String, byte[]> animalSoundCache = new HashMap<>();

    /** Cooldown từng loài (tên loài → thời gian còn lại, giây). */
    private final Map<String, Float> ambientCooldowns = new HashMap<>();

    /** Cooldown click sound. */
    private float clickCooldownRemaining = 0f;

    // =============================================
    // NHẠC NỀN (BACKGROUND MUSIC)
    // =============================================

    /**
     * Phát nhạc nền ingame.wav với vòng lặp vô tận.
     * An toàn khi gọi nhiều lần.
     */
    public void playBGM() {
        if (bgmClip != null && bgmClip.isRunning()) return;

        try {
            File soundFile = new File(SOUND_DIR + "ingame.wav");
            if (!soundFile.exists()) {
                System.err.println("[SoundManager] Không tìm thấy file: " + soundFile.getAbsolutePath());
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);

            if (bgmClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                bgmVolumeControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                applyVolume(bgmVolumeControl, bgmVolume);
            }

            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[SoundManager] Lỗi phát nhạc nền: " + e.getMessage());
        }
    }

    public void pauseBGM() {
        if (bgmClip != null && bgmClip.isRunning()) bgmClip.stop();
    }

    public void resumeBGM() {
        if (bgmClip != null && !bgmClip.isRunning()) {
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        }
    }

    public void stopBGM() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    public void setBGMVolume(float volume) {
        this.bgmVolume = clamp(volume);
        if (bgmVolumeControl != null) applyVolume(bgmVolumeControl, bgmVolume);
    }

    public float getBGMVolume() { return bgmVolume; }

    // =============================================
    // TIẾNG ĐỘNG VẬT KHI CLICK
    // =============================================

    /**
     * Phát tiếng loài động vật khi người dùng click chọn nó.
     * Có cooldown 1.5 giây để tránh spam.
     *
     * @param speciesName tên loài (ví dụ "deer", "wolf", "tiger")
     */
    public void playAnimalSoundOnClick(String speciesName) {
        if (clickCooldownRemaining > 0) return;
        if (speciesName == null) return;

        String key = speciesName.toLowerCase();
        boolean played = playAnimalClipOnce(key, clickSoundVolume);
        if (played) {
            clickCooldownRemaining = CLICK_COOLDOWN;
        }
    }

    // =============================================
    // TIẾNG AMBIENT (AUTO KHI ZOOM + ÍT CON)
    // =============================================

    /**
     * Gọi mỗi frame từ GameLoop.
     * Tự động phát tiếng ambient khi thỏa điều kiện zoom và số con.
     *
     * @param deltaTime     thời gian frame (giây)
     * @param zoom          mức zoom hiện tại của camera
     * @param speciesInView tên loài (chỉ 1 loài chiếm đa số) đang trong viewport,
     *                      null nếu không đủ điều kiện
     */
    public void update(float deltaTime, float zoom, String speciesInView) {
        // Giảm cooldown click
        if (clickCooldownRemaining > 0) {
            clickCooldownRemaining = Math.max(0, clickCooldownRemaining - deltaTime);
        }

        // Giảm cooldown ambient của từng loài
        for (String key : ambientCooldowns.keySet()) {
            float remaining = ambientCooldowns.get(key) - deltaTime;
            ambientCooldowns.put(key, Math.max(0f, remaining));
        }

        // Kiểm tra điều kiện ambient
        if (speciesInView == null) return;
        if (zoom < MIN_ZOOM_FOR_AMBIENT) return;

        String key = speciesInView.toLowerCase();
        float cooldown = ambientCooldowns.getOrDefault(key, 0f);
        if (cooldown > 0) return;

        boolean played = playAnimalClipOnce(key, ambientSoundVolume);
        if (played) {
            ambientCooldowns.put(key, AMBIENT_COOLDOWN_PER_SPECIES);
        }
    }

    // =============================================
    // HELPER: PHÁT CLIP
    // =============================================

    /**
     * Load (hoặc lấy từ cache) và phát tiếng động vật 1 lần.
     * Dùng luồng riêng để không block GameLoop.
     *
     * @return true nếu phát thành công
     */
    private boolean playAnimalClipOnce(String speciesKey, float volume) {
        // Chuyển tên loài sang tên file qua bảng map
        String fileKey = SPECIES_TO_FILE.get(speciesKey.toLowerCase());
        if (fileKey == null) return false;

        String fileName = SOUND_DIR + fileKey + ".wav";
        File soundFile = new File(fileName);
        if (!soundFile.exists()) return false;

        // Phát trên daemon thread để không block game
        Thread soundThread = new Thread(() -> {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    applyVolume(ctrl, volume);
                }

                clip.start();

                // Tự động giải phóng sau khi phát xong
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("[SoundManager] Lỗi phát tiếng " + speciesKey + ": " + e.getMessage());
            }
        });
        soundThread.setDaemon(true);
        soundThread.start();
        return true;
    }

    // =============================================
    // UTILITY
    // =============================================

    /**
     * Chuyển volume tuyến tính (0.0–1.0) sang dB và áp dụng.
     */
    private void applyVolume(FloatControl ctrl, float volume) {
        float min = ctrl.getMinimum();
        float max = ctrl.getMaximum();
        float dB;
        if (volume <= 0.0f) {
            dB = min;
        } else {
            dB = (float) (20.0 * Math.log10(volume));
            dB = Math.max(min, Math.min(max, dB));
        }
        ctrl.setValue(dB);
    }

    private float clamp(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    // =============================================
    // GIẢI PHÓNG TÀI NGUYÊN
    // =============================================

    /** Gọi khi tắt game. */
    public void dispose() {
        stopBGM();
    }
}
