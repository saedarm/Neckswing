package com.saedarm.neckswing.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.saedarm.neckswing.Neckswing;

public final class Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Neckswing");
        cfg.setWindowedMode(1280, 720);
        cfg.useVsync(true);
        cfg.setForegroundFPS(144);
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4);
        new Lwjgl3Application(new Neckswing(args), cfg);
    }
}
