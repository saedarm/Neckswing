package com.saedarm.neckswing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap;

/** Sound effects with distance falloff and pan, plus crossfading music. Survives having no audio device. */
public final class Sfx {
    public static final String[] NAMES = {"thwip", "release", "jump", "land", "spin", "hit", "hit_big", "kick", "slam", "bellow",
            "tongue", "munch", "look", "gunshot", "hurt", "down", "car_door", "crash", "horn", "ring", "mission_start",
            "mission_done", "fail", "pickup", "deliver", "died", "grunt", "no_anchor", "shield_break", "not_ready", "crime",
            "step", "splat", "click", "engine", "wind",
            "zip", "alert", "laser", "sniper", "fire", "explode", "scream", "checkpoint", "race_start", "medal", "leaf", "charge",
            "ping", "buy", "ped_hit"};
    private final HashMap<String, Sound> sounds = new HashMap<>();
    private final HashMap<String, Long> last = new HashMap<>();
    public float volume = 1f, musicVolume = 0.6f;
    private Music menu, city, action, current, fading;
    private float fadeT;
    private final Vector3 listener = new Vector3(), right = new Vector3();
    private long engineId = -1, windId = -1;

    public Sfx() {
        for (String n : NAMES) {
            try { sounds.put(n, Gdx.audio.newSound(Gdx.files.internal("sfx/" + n + ".wav"))); }
            catch (Throwable t) { /* no audio device or missing file */ }
        }
        try {
            menu = Gdx.audio.newMusic(Gdx.files.internal("music/menu.wav"));
            city = Gdx.audio.newMusic(Gdx.files.internal("music/city.wav"));
            action = Gdx.audio.newMusic(Gdx.files.internal("music/action.wav"));
            for (Music m : new Music[]{menu, city, action}) { m.setLooping(true); m.setVolume(0); }
        } catch (Throwable t) { menu = city = action = null; }
    }

    public void listener(Vector3 pos, Vector3 rightDir) { listener.set(pos); right.set(rightDir); }

    public void play(String name) { play(name, 1f, 1f, 0f); }

    public void play(String name, float vol, float pitch, float pan) {
        Sound s = sounds.get(name);
        if (s == null || volume <= 0) return;
        long now = System.currentTimeMillis();
        Long l = last.get(name);
        if (l != null && now - l < 40) return;
        last.put(name, now);
        try { s.play(vol * volume, pitch, pan); } catch (Throwable ignored) { }
    }

    /** positional: quieter with distance, panned by side. */
    public void at(String name, float x, float y, float z, float vol, float pitch) {
        float dx = x - listener.x, dy = y - listener.y, dz = z - listener.z;
        float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float fall = 1f / (1f + d * d / 900f);
        if (fall < 0.03f) return;
        float pan = d < 0.1f ? 0 : City.clamp((dx * right.x + dz * right.z) / d, -1, 1) * 0.8f;
        play(name, vol * fall, pitch, pan);
    }

    /** looping car engine, pitch follows speed. speed < 0 stops it. */
    public void engine(float speed) {
        Sound s = sounds.get("engine");
        if (s == null) return;
        try {
            if (speed < 0) { if (engineId != -1) { s.stop(engineId); engineId = -1; } return; }
            if (engineId == -1) engineId = s.loop(0.35f * volume);
            s.setPitch(engineId, 0.6f + Math.min(1.4f, Math.abs(speed) / 30f));
            s.setVolume(engineId, (0.25f + Math.min(0.3f, Math.abs(speed) / 100f)) * volume);
        } catch (Throwable ignored) { }
    }

    /** looping wind while swinging fast. */
    public void wind(float speed) {
        Sound s = sounds.get("wind");
        if (s == null) return;
        try {
            if (windId == -1) windId = s.loop(0);
            float v = City.clamp((speed - 15) / 50f, 0, 0.6f) * volume;
            s.setVolume(windId, v);
            s.setPitch(windId, 0.8f + Math.min(0.6f, speed / 100f));
        } catch (Throwable ignored) { }
    }

    public static final int MUSIC_MENU = 0, MUSIC_CITY = 1, MUSIC_ACTION = 2, MUSIC_NONE = 3;
    public void music(int which) {
        Music want = which == MUSIC_MENU ? menu : which == MUSIC_CITY ? city : which == MUSIC_ACTION ? action : null;
        if (want == current) return;
        if (fading != null && fading != want) { try { fading.stop(); } catch (Throwable ignored) { } }
        fading = current; current = want; fadeT = 0;
        if (current != null) { try { current.setVolume(0); current.play(); } catch (Throwable ignored) { } }
    }

    public void update(float dt) {
        fadeT = Math.min(1, fadeT + dt / 1.5f);
        try {
            if (current != null) current.setVolume(fadeT * musicVolume);
            if (fading != null) {
                fading.setVolume((1 - fadeT) * musicVolume);
                if (fadeT >= 1) { fading.stop(); fading = null; }
            }
        } catch (Throwable ignored) { }
    }

    public void dispose() {
        for (Sound s : sounds.values()) s.dispose();
        for (Music m : new Music[]{menu, city, action}) if (m != null) m.dispose();
    }
}
