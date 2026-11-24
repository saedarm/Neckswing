package com.saedarm.neckswing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Settings and story progress, stored with libGDX Preferences. */
public final class Save {
    private final Preferences p;
    public float sound, music, sensitivity;
    public boolean invertY, fullscreen, voiceListen, voiceSpeak, showFps;
    public int mission, respect, skills, leaves;
    public float[] raceBest = new float[0];

    public Save() {
        p = Gdx.app.getPreferences("neckswing");
        sound = p.getFloat("sound", 0.9f);
        music = p.getFloat("music", 0.5f);
        sensitivity = p.getFloat("sensitivity", 1f);
        invertY = p.getBoolean("invertY", false);
        fullscreen = p.getBoolean("fullscreen", false);
        voiceListen = p.getBoolean("voiceListen", true);
        voiceSpeak = p.getBoolean("voiceSpeak", true);
        showFps = p.getBoolean("showFps", false);
        mission = p.getInteger("mission", 0);
        respect = p.getInteger("respect", 0);
        skills = p.getInteger("skills", 0);
        leaves = p.getInteger("leaves", 0);
        String rb = p.getString("raceBest", "");
        if (!rb.isEmpty()) {
            String[] parts = rb.split(",");
            raceBest = new float[parts.length];
            for (int i = 0; i < parts.length; i++) try { raceBest[i] = Float.parseFloat(parts[i]); } catch (NumberFormatException ignored) { }
        }
    }

    /** copy progress out of a running city */
    public void from(City c) {
        mission = c.mission; respect = c.respect; skills = c.skills; leaves = c.leafMask; raceBest = c.raceBest.clone();
    }
    /** load progress into a fresh city */
    public void into(City c) {
        c.respect = respect; c.skills = skills; c.leafMask = leaves;
        for (int i = 0; i < Math.min(raceBest.length, c.raceBest.length); i++) c.raceBest[i] = raceBest[i];
        c.g.hp = c.hpMax();
    }

    public void write() {
        p.putFloat("sound", sound); p.putFloat("music", music); p.putFloat("sensitivity", sensitivity);
        p.putBoolean("invertY", invertY); p.putBoolean("fullscreen", fullscreen);
        p.putBoolean("voiceListen", voiceListen); p.putBoolean("voiceSpeak", voiceSpeak); p.putBoolean("showFps", showFps);
        p.putInteger("mission", mission); p.putInteger("respect", respect);
        p.putInteger("skills", skills); p.putInteger("leaves", leaves);
        StringBuilder rb = new StringBuilder();
        for (int i = 0; i < raceBest.length; i++) { if (i > 0) rb.append(','); rb.append(raceBest[i]); }
        p.putString("raceBest", rb.toString());
        p.flush();
    }
}
