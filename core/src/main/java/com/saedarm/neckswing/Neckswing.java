package com.saedarm.neckswing;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.ArrayList;

/** The desktop game: screens, camera, drawing, HUD, and turning rule events into sound and juice. */
public final class Neckswing extends ApplicationAdapter {
    enum Scr { TITLE, SETTINGS, CONTROLS, PLAY, PAUSE, SKILLS }

    // launch flags
    private final boolean solo, autoplay;
    private final String shotFile; private final float shotTime;
    private final int startMission, giveRespect, startRace;
    private final boolean openTree;

    private Scr scr = Scr.TITLE, settingsBack = Scr.TITLE, controlsBack = Scr.TITLE, skillsBack = Scr.PLAY;
    private City city;
    private Bot bot;
    private final City.Input input = new City.Input();
    private Art art;
    private Sfx sfx;
    private Voice voice;
    private Save save;

    private PerspectiveCamera cam;
    private ModelBatch batch;
    private Environment env;
    private ModelInstance cityInst;
    private SpriteBatch sb;
    private ShapeRenderer sr;
    private ExtendViewport ui;
    private BitmapFont f18, f26, f46, f110;
    private final GlyphLayout layout = new GlyphLayout();
    private static final Color SKY = new Color(0.62f, 0.80f, 0.95f, 1);

    private float camYaw = (float) Math.PI, camPitch = 0.25f, mouseIdle, shake, freeze, accum, clock, hurtFlash, fov = 70;
    private float bannerT, bubbleT, playTime, titleT;
    private String banner = "", bannerSub = "", bubble = "";
    private Color bannerColor = Color.WHITE;
    private String lastHeard = ""; private float heardT;

    public Neckswing(String[] args) {
        boolean s = false, a = false, tree = false; String shot = null; float st = 5; int m = -1, rs = 0, race = -1;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--solo": s = true; break;
                case "--autoplay": a = true; break;
                case "--shot": shot = args[++i]; st = Float.parseFloat(args[++i]); break;
                case "--mission": m = Integer.parseInt(args[++i]); s = true; break;
                case "--respect": rs = Integer.parseInt(args[++i]); break;
                case "--tree": tree = true; s = true; break;
                case "--race": race = Integer.parseInt(args[++i]); s = true; break;
            }
        }
        solo = s; autoplay = a; shotFile = shot; shotTime = st; startMission = m; giveRespect = rs; openTree = tree; startRace = race;
    }

    // ================================================================= setup
    @Override public void create() {
        save = new Save();
        sfx = new Sfx();
        sfx.volume = save.sound; sfx.musicVolume = save.music;
        voice = new Voice();
        cam = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 0.5f; cam.far = 760f;
        batch = new ModelBatch();
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.55f, 0.6f, 1f));
        env.set(new ColorAttribute(ColorAttribute.Fog, SKY));
        env.add(new DirectionalLight().set(0.75f, 0.72f, 0.65f, -0.4f, -1f, -0.25f));
        env.add(new DirectionalLight().set(0.18f, 0.2f, 0.25f, 0.5f, -0.3f, 0.6f));
        sb = new SpriteBatch();
        sr = new ShapeRenderer();
        ui = new ExtendViewport(1280, 720);
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Poppins-Bold.ttf"));
        f18 = font(gen, 18); f26 = font(gen, 26); f46 = font(gen, 46); f110 = font(gen, 110);
        gen.dispose();

        city = new City(System.nanoTime());
        art = new Art(city);
        art.batch = batch; art.env = env;
        cityInst = new ModelInstance(art.city);

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int k) { return onKey(k); }
            @Override public boolean touchDown(int x, int y, int pointer, int button) {
                if (scr == Scr.PLAY) {
                    if (!Gdx.input.isCursorCatched() && !autoplay) Gdx.input.setCursorCatched(true);
                    if (button == Buttons.LEFT) input.press(City.B_SWING);
                    if (button == Buttons.RIGHT) input.press(City.B_ATTACK);
                }
                return false;
            }
        });

        if (save.fullscreen) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        startVoice();
        if (solo) newGame(startMission >= 0 ? startMission : save.mission);
        else sfx.music(Sfx.MUSIC_MENU);
    }

    private BitmapFont font(FreeTypeFontGenerator gen, int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size; p.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear; p.magFilter = p.minFilter;
        return gen.generateFont(p);
    }

    private void startVoice() { voice.start(save.voiceListen, save.voiceSpeak); }

    private void newGame(int mission) {
        city = new City(System.nanoTime());
        city.mission = Math.min(mission, City.MISSIONS);
        save.into(city);
        city.refreshObjective();
        bot = new Bot(7, 0.9f);
        scr = Scr.PLAY; playTime = 0;
        particles.clear(); floats.clear();
        camYaw = city.g.yaw; camPitch = 0.25f;
        if (startMission >= 0 && solo) city.startMission(Math.min(startMission, City.MISSIONS - 1));
        city.respect += giveRespect;
        if (startRace >= 0 && startRace < city.acts.size()) {
            City.Activity a = city.acts.get(startRace);
            city.g.x = a.sx + 2; city.g.z = a.sz + 2;
            if (a.type == City.ACT_DRIVE) {   // park a car on the start so a race can begin
                City.Car car = city.cars.get(0); car.x = a.sx; car.z = a.sz + 3; car.speed = 0; car.y = 0; car.yaw = 0;
                car.state = City.C_PLAYER; city.g.car = 0; city.g.mode = City.M_DRIVE;
            }
        }
        if (openTree) openSkills(Scr.PLAY);
        if (!autoplay) Gdx.input.setCursorCatched(true);
        sfx.music(Sfx.MUSIC_CITY);
    }

    private boolean onKey(int k) {
        if (k == Keys.ESCAPE) {
            if (scr == Scr.PLAY) { scr = Scr.PAUSE; Gdx.input.setCursorCatched(false); sfx.play("click"); }
            else if (scr == Scr.PAUSE) unpause();
            else if (scr == Scr.SETTINGS) { save.write(); scr = settingsBack; }
            else if (scr == Scr.CONTROLS) scr = controlsBack;
            else if (scr == Scr.SKILLS) closeSkills();
            return true;
        }
        if (k == Keys.K) {
            if (scr == Scr.PLAY || scr == Scr.PAUSE) openSkills(scr);
            else if (scr == Scr.SKILLS) closeSkills();
            return true;
        }
        if (k == Keys.F11) { save.fullscreen = !save.fullscreen; applyFullscreen(); return true; }
        if (scr != Scr.PLAY) return false;
        switch (k) {
            case Keys.SPACE: input.press(City.B_JUMP); break;
            case Keys.F: input.press(City.B_ATTACK); break;
            case Keys.E: input.press(City.B_ENTER); break;
            case Keys.NUM_1: input.press(City.B_BELLOW); break;
            case Keys.NUM_2: input.press(City.B_TONGUE); break;
            case Keys.NUM_3: input.press(City.B_KICK); break;
            case Keys.NUM_4: input.press(City.B_LOOK); break;
            case Keys.NUM_5: input.press(City.B_MUNCH); break;
            case Keys.T: input.press(City.B_TALK); break;
            case Keys.H: input.press(City.B_HORN); break;
            case Keys.CONTROL_LEFT: input.press(City.B_SWING); break;
            case Keys.Q: input.press(City.B_ZIP); break;
            case Keys.BACKSPACE: input.press(City.B_CANCEL); break;
        }
        return true;
    }

    private void openSkills(Scr from) {
        skillsBack = from; scr = Scr.SKILLS; Gdx.input.setCursorCatched(false); sfx.play("click");
    }
    private void closeSkills() {
        save.from(city); save.write();
        if (skillsBack == Scr.PLAY) unpause(); else scr = skillsBack;
    }

    private void unpause() { scr = Scr.PLAY; if (!autoplay) Gdx.input.setCursorCatched(true); }
    private void applyFullscreen() {
        if (save.fullscreen) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else Gdx.graphics.setWindowedMode(1280, 720);
        save.write();
    }

    @Override public void resize(int w, int h) {
        ui.update(w, h, true);
        cam.viewportWidth = w; cam.viewportHeight = h; cam.update();
    }

    // ================================================================= frame
    @Override public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        clock += dt;
        sfx.update(dt);

        if (scr == Scr.PLAY) updatePlay(dt);
        else if (scr == Scr.TITLE || scr == Scr.SETTINGS || scr == Scr.CONTROLS) {
            titleT += dt;
            // the city keeps living behind the menu
            City.Input idle = new City.Input();
            accum += dt;
            while (accum >= City.DT) { city.tick(idle); city.events.clear(); accum -= City.DT; }
        }
        updateCamera(dt);
        updateParticles(dt);

        ScreenUtils.clear(SKY.r, SKY.g, SKY.b, 1, true);
        drawWorld();
        drawUi(dt);

        if (shotFile != null && clock >= shotTime) {
            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.absolute(shotFile), pm, -1, true);
            pm.dispose();
            Gdx.app.exit();
        }
    }

    private void updatePlay(float dt) {
        playTime += dt;
        // voice
        String said;
        while ((said = voice.poll()) != null) {
            lastHeard = said; heardT = 2f;
            if (said.equals("pause")) { onKey(Keys.ESCAPE); return; }
            if (said.equals("skills")) { openSkills(Scr.PLAY); return; }
            int bits = Voice.bitsFor(said);
            if (bits == City.B_ENTER && city.g.mode == City.M_DRIVE && said.equals("car")) bits = 0;
            input.press(bits);
        }
        if (!autoplay) readKeyboard();
        if (freeze > 0) { freeze -= dt; return; }
        accum += dt;
        while (accum >= City.DT) {
            if (autoplay) bot.fill(city, input);
            city.tick(input);
            handleEvents();
            accum -= City.DT;
        }
        // damaged cars smoke, burning cars flame
        for (City.Car c : city.cars) {
            if (City.dist2(c.x, c.z, city.g.x, city.g.z) > 170 * 170) continue;
            if (c.burnT > 0 || (c.state == City.C_WRECK && c.wreckT > 15)) {
                if (MathUtils.randomBoolean(0.7f)) puff(c.x + MathUtils.random(-1f, 1f), c.y + 2.2f, c.z + MathUtils.random(-1.5f, 1.5f), art.fire, 0.7f, 0.5f, 5);
                if (MathUtils.randomBoolean(0.4f)) puff(c.x, c.y + 3, c.z, art.smoke, 1.4f, 2f, 3);
            } else if (c.state == City.C_WRECK) {
                if (MathUtils.randomBoolean(0.15f)) puff(c.x, c.y + 2, c.z, art.smoke, 1.2f, 2.5f, 2);
            } else if (c.hp < 40) {
                if (MathUtils.randomBoolean(c.hp < 20 ? 0.5f : 0.2f)) puff(c.x, c.y + 2, c.z + 0f, art.smoke, 0.9f, 1.6f, 3);
            }
        }
        // car engine + wind
        if (city.g.mode == City.M_DRIVE) sfx.engine(city.cars.get(city.g.car).speed); else sfx.engine(-1);
        sfx.wind(city.g.mode == City.M_DRIVE ? 0 : city.g.speed());
        // music by danger
        boolean danger = false;
        for (City.Enemy e : city.enemies) if (e.alive() && e.state == City.E_CHASE && City.dist2(e.x, e.z, city.g.x, city.g.z) < 70 * 70) { danger = true; break; }
        sfx.music(danger || (city.missionActive && city.mission == 4) ? Sfx.MUSIC_ACTION : Sfx.MUSIC_CITY);
    }

    private void readKeyboard() {
        float f = 0, r = 0;
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) f += 1;
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) f -= 1;
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) r += 1;
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) r -= 1;
        float fx = MathUtils.sin(camYaw), fz = MathUtils.cos(camYaw);
        float rx = -fz, rz = fx;   // right of camera
        input.mx = fx * f + rx * r; input.mz = fz * f + rz * r;
        input.throttle = f; input.steer = -r;
        input.aimYaw = camYaw;
        input.swingHeld = Gdx.input.isButtonPressed(Buttons.LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_LEFT);
        input.reelHeld = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT);
    }

    // ================================================================= events -> juice
    private static final String[] POWS = {"WHAP!", "BONK!", "NECKED!", "THWACK!", "SMOOSH!", "OOF!"};

    private void handleEvents() {
        City.Giraffe g = city.g;
        for (City.Event e : city.events) {
            switch (e.type) {
                case City.EV_SWING_ATTACH: sfx.at("thwip", e.x, e.y, e.z, 1, 0.9f + MathUtils.random(0.2f)); burst(e.x, e.y, e.z, 6, art.debris, 6, 0.4f, 0.6f); break;
                case City.EV_SWING_RELEASE: sfx.play("release", 0.6f, 1, 0); break;
                case City.EV_JUMP: sfx.play("jump", 0.6f, 1, 0); break;
                case City.EV_LAND:
                    sfx.at("land", e.x, e.y, e.z, Math.min(1, 0.3f + e.a / 40f), 1);
                    if (e.a > 25) { burst(e.x, e.y + 0.3f, e.z, 10, art.smoke, 10, 1f, 0.6f); shake = Math.max(shake, e.a / 120f); }
                    break;
                case City.EV_SPIN: sfx.play("spin", 0.9f, 0.9f + MathUtils.random(0.2f), 0); break;
                case City.EV_HIT: {
                    int big = e.a % 8;
                    sfx.at(big == 2 ? "hit_big" : "hit", e.x, e.y, e.z, 1, 0.9f + MathUtils.random(0.25f));
                    burst(e.x, e.y, e.z, 6 + big * 4, art.spark, 12 + big * 6, 0.35f, 0.4f);
                    if (big >= 1) floatText(e.x, e.y + 3, e.z, POWS[MathUtils.random(POWS.length - 1)], Color.YELLOW);
                    if (big == 2) { freeze = Math.max(freeze, 0.05f); shake = Math.max(shake, 0.4f); }
                    break;
                }
                case City.EV_KICK: sfx.play("kick"); break;
                case City.EV_SLAM:
                    sfx.at("slam", e.x, e.y, e.z, 1, 1); shake = 1.2f; freeze = 0.07f;
                    ringBurst(e.x, e.y + 0.5f, e.z, 26, art.smoke, 22, 1.6f);
                    burst(e.x, e.y, e.z, 20, art.debris, 18, 0.6f, 1f);
                    break;
                case City.EV_BELLOW:
                    sfx.play("bellow"); shake = Math.max(shake, 0.6f);
                    ringBurst(e.x, e.y + 5, e.z, 34, art.smoke, 30, 1.2f);
                    break;
                case City.EV_TONGUE: sfx.play("tongue"); break;
                case City.EV_MUNCH: sfx.play("munch"); burst(e.x, e.y, e.z, 16, art.leaf, 6, 0.5f, 1f); floatText(g.x, g.y + 9, g.z, "+" + (int) City.MUNCH_HEAL + " HP", Color.GREEN); break;
                case City.EV_LOOK: sfx.play("look"); break;
                case City.EV_SHOT: sfx.at("gunshot", e.x, e.y, e.z, 0.7f, 0.9f + MathUtils.random(0.2f)); burst(e.x, e.y, e.z, 2, art.spark, 3, 0.25f, 0.1f); break;
                case City.EV_PLAYER_HURT: sfx.play("hurt", 0.7f, 1, 0); hurtFlash = 0.35f; shake = Math.max(shake, 0.25f); break;
                case City.EV_ENEMY_DOWN:
                    sfx.at("down", e.x, e.y, e.z, 1, e.a == City.K_BOSS ? 0.6f : 1);
                    floatText(e.x, e.y + 4, e.z, e.a == City.K_BOSS ? "+300" : e.a == City.K_BRUTE ? "+25" : "+10", Color.ORANGE);
                    break;
                case City.EV_CAR_ENTER: case City.EV_CAR_EXIT: sfx.play("car_door"); break;
                case City.EV_CRASH: case City.EV_CAR_KICK:
                    sfx.at("crash", e.x, e.y, e.z, 1, 0.9f + MathUtils.random(0.2f));
                    burst(e.x, e.y + 1, e.z, 14, art.debris, 12, 0.5f, 0.9f);
                    if (City.dist2(e.x, e.z, g.x, g.z) < 400) shake = Math.max(shake, 0.5f);
                    break;
                case City.EV_HORN: sfx.play("horn"); break;
                case City.EV_RING: {
                    int done = 0; for (City.Ring r : city.rings) if (r.done) done++;
                    sfx.play("ring", 1, 1 + done * 0.06f, 0);
                    burst(e.x, e.y, e.z, 18, art.spark, 14, 0.4f, 0.6f);
                    floatText(e.x, e.y + 3, e.z, done + "/" + city.rings.size() + "  +3s", Color.CYAN);
                    break;
                }
                case City.EV_MISSION_START:
                    sfx.play("mission_start");
                    showBanner(City.MISSION_NAMES[e.a], City.MISSION_BRIEF[e.a], Color.GOLD);
                    break;
                case City.EV_MISSION_DONE:
                    sfx.play("mission_done");
                    if (e.a == 99) showBanner("CRIME STOPPED", "+150 respect", Color.GREEN);
                    else {
                        showBanner("MISSION COMPLETE", city.mission >= City.MISSIONS ? "The city is safe. Free roam unlocked: crimes will pop up." : "Next up: " + City.MISSION_NAMES[city.mission], Color.GREEN);
                        save.from(city); save.write();
                    }
                    break;
                case City.EV_MISSION_FAIL: sfx.play("fail"); showBanner("MISSION FAILED", city.objectiveText, Color.SCARLET); break;
                case City.EV_PICKUP: sfx.play("pickup"); showBanner("GOT THE LEAVES", "Get them to the zoo. Watch the rooftops.", Color.GOLD); break;
                case City.EV_DELIVER: sfx.play("deliver"); break;
                case City.EV_DIED: sfx.play("died"); showBanner("POACHED", "Back to the zoo...", Color.SCARLET); shake = 0.8f; break;
                case City.EV_RESPAWN: camYaw = g.yaw; break;
                case City.EV_QUIP: {
                    bubble = City.QUIPS[e.a]; bubbleT = 3.2f;
                    if (!save.voiceSpeak || !voice.say(bubble)) sfx.play("grunt", 0.8f, 0.85f + MathUtils.random(0.3f), 0);
                    break;
                }
                case City.EV_NO_ANCHOR: sfx.play("no_anchor", 0.6f, 1, 0); break;
                case City.EV_SHIELD_BREAK: sfx.at("shield_break", e.x, e.y, e.z, 1, 1); showBanner("SHIELD DOWN!", "Hit him now!", Color.CYAN); burst(e.x, e.y + 3, e.z, 30, art.spark, 16, 0.5f, 0.7f); break;
                case City.EV_NOT_READY: sfx.play("not_ready", 0.5f, 1, 0); break;
                case City.EV_CRIME: sfx.play("crime"); showBanner("CRIME IN PROGRESS", "Poachers spotted. Follow the marker.", Color.ORANGE); break;
                case City.EV_STEP: sfx.play("step", 0.25f, 0.8f + MathUtils.random(0.3f), 0); break;
                case City.EV_SPLAT: sfx.at("splat", e.x, e.y, e.z, 0.8f, 1); break;
                case City.EV_BANNER:
                    if (e.a >= 100) showBanner(city.acts.get(e.a - 100).name, "Street race. Come back in a car to start it.", Color.VIOLET);
                    else { sfx.play("crime"); showBanner("IT'S A TRAP!", "Poachers were waiting. Take them out.", Color.ORANGE); }
                    break;
                case City.EV_ZIP: sfx.play("zip"); burst(e.x, e.y, e.z, 8, art.grab, 8, 0.3f, 0.4f); break;
                case City.EV_ALERT: sfx.at("alert", e.x, e.y, e.z, 1, 1); floatText(e.x, e.y, e.z, City.ALERT_LINES[e.a], Color.SALMON); break;
                case City.EV_SNIPER_AIM: sfx.play("laser", 0.8f, 1, 0); break;
                case City.EV_SNIPER_SHOT: sfx.at("sniper", e.x, e.y, e.z, 1.4f, 1); break;
                case City.EV_CHARGE: sfx.at("charge", e.x, e.y, e.z, 1, 1); floatText(e.x, e.y, e.z, "CHARGE!", Color.RED); break;
                case City.EV_CAR_FIRE: sfx.at("fire", e.x, e.y, e.z, 1, 1); if (e.a == g.car) showBanner("YOUR CAR IS ON FIRE", "Get out! (E)", Color.ORANGE); break;
                case City.EV_EXPLODE:
                    sfx.at("explode", e.x, e.y, e.z, 1.5f, 0.9f + MathUtils.random(0.2f));
                    burst(e.x, e.y, e.z, 40, art.fire, 22, 0.9f, 0.8f);
                    burst(e.x, e.y + 1, e.z, 25, art.smoke, 12, 1.6f, 1.6f);
                    burst(e.x, e.y, e.z, 20, art.debris, 26, 0.6f, 1.4f);
                    ringBurst(e.x, e.y + 0.5f, e.z, 30, art.smoke, 26, 1.8f);
                    { float d = (float) Math.sqrt(City.dist2(e.x, e.z, g.x, g.z)); shake = Math.max(shake, 1.4f - d / 60f); freeze = Math.max(freeze, d < 20 ? 0.06f : 0); }
                    floatText(e.x, e.y + 3, e.z, "KABOOM!", Color.ORANGE);
                    break;
                case City.EV_BULLET_CAR: sfx.at("ping", e.x, e.y, e.z, 0.6f, 0.8f + MathUtils.random(0.5f)); burst(e.x, e.y, e.z, 3, art.spark, 6, 0.2f, 0.2f); break;
                case City.EV_PED_HIT: sfx.at("ped_hit", e.x, e.y, e.z, 1, 1); floatText(e.x, e.y + 1, e.z, e.a == 1 ? "-5 respect" : "-10 respect", Color.SCARLET); break;
                case City.EV_PED_TALK: { floatText(e.x, e.y, e.z, City.PED_LINES[e.a], Color.WHITE); FloatText f = floats.get(floats.size() - 1); f.small = true; f.life = 2.4f; break; }
                case City.EV_PED_SCREAM: sfx.at("scream", e.x, 2, e.z, 0.6f, 0.9f + MathUtils.random(0.3f)); break;
                case City.EV_ACT_START: {
                    City.Activity a = city.acts.get(e.a);
                    sfx.play("race_start");
                    String best = city.raceBest[e.a] > 0 ? "   Best " + time(city.raceBest[e.a]) : "";
                    showBanner(a.name.toUpperCase(), "Gold " + time(a.gold) + "  Silver " + time(a.silver) + "  Bronze " + time(a.bronze) + best, Color.SKY);
                    break;
                }
                case City.EV_CHECKPOINT: sfx.play("checkpoint", 1, 1 + e.a * 0.04f, 0); burst(e.x, e.y + 1, e.z, 14, art.spark, 12, 0.4f, 0.5f); break;
                case City.EV_ACT_DONE: {
                    int k = e.a / 8, m = e.a % 8;
                    String[] medals = {"GOLD", "SILVER", "BRONZE", "FINISHED"};
                    Color[] cols = {Color.GOLD, Color.LIGHT_GRAY, new Color(0.85f, 0.55f, 0.3f, 1), Color.WHITE};
                    sfx.play(m < 3 ? "medal" : "mission_done");
                    showBanner(medals[m] + "  " + time(city.actTime), city.acts.get(k).name + "   +" + city.lastActGain + " respect   Best " + time(city.raceBest[k]), cols[m]);
                    save.from(city); save.write();
                    break;
                }
                case City.EV_ACT_FAIL: sfx.play("fail"); showBanner("RACE OVER", city.objectiveText, Color.SCARLET); break;
                case City.EV_LEAF:
                    sfx.play("leaf");
                    burst(e.x, e.y, e.z, 20, art.goldLeaf, 10, 0.4f, 0.8f);
                    floatText(e.x, e.y + 2, e.z, "Golden leaf " + city.leavesFound() + "/" + city.leaves.size() + "  +50", Color.GOLD);
                    save.from(city); save.write();
                    break;
                case City.EV_ALL_LEAVES: sfx.play("medal"); showBanner("ALL 25 GOLDEN LEAVES", "+1000 respect. Truly the tallest snacker.", Color.GOLD); break;
            }
        }
        city.events.clear();
    }

    private static String time(float t) {
        int s = (int) t;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60 + "." + (int) ((t - s) * 10);
    }

    private void showBanner(String t, String sub, Color c) { banner = t; bannerSub = sub; bannerT = 3.2f; bannerColor = c; }

    // ================================================================= particles
    private static final class Particle { float x, y, z, vx, vy, vz, life, max, size, grav; Model m; }
    private final ArrayList<Particle> particles = new ArrayList<>();
    private static final class FloatText { float x, y, z, life; String s; Color c; boolean small; }
    private final ArrayList<FloatText> floats = new ArrayList<>();

    private void burst(float x, float y, float z, int n, Model m, float speed, float size, float life) {
        for (int i = 0; i < n && particles.size() < 900; i++) {
            Particle p = new Particle();
            p.x = x; p.y = y; p.z = z; p.m = m;
            float a = MathUtils.random(MathUtils.PI2), u = MathUtils.random(-0.3f, 1f);
            float s = speed * MathUtils.random(0.4f, 1f);
            p.vx = MathUtils.cos(a) * s; p.vz = MathUtils.sin(a) * s; p.vy = u * s;
            p.size = size * MathUtils.random(0.6f, 1.3f); p.life = p.max = life * MathUtils.random(0.6f, 1.2f);
            p.grav = m == art.smoke ? -2 : 25;
            particles.add(p);
        }
    }
    /** one rising particle */
    private void puff(float x, float y, float z, Model m, float size, float life, float rise) {
        if (particles.size() > 900) return;
        Particle p = new Particle();
        p.x = x; p.y = y; p.z = z; p.m = m;
        p.vx = MathUtils.random(-0.6f, 0.6f); p.vz = MathUtils.random(-0.6f, 0.6f); p.vy = rise;
        p.size = size; p.life = p.max = life; p.grav = m == art.fire ? -3 : -1;
        particles.add(p);
    }

    private void ringBurst(float x, float y, float z, int n, Model m, float speed, float size) {
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            float a = i * MathUtils.PI2 / n;
            p.x = x; p.y = y; p.z = z; p.m = m;
            p.vx = MathUtils.cos(a) * speed; p.vz = MathUtils.sin(a) * speed; p.vy = 0.5f;
            p.size = size; p.life = p.max = 0.7f; p.grav = 0;
            particles.add(p);
        }
    }
    private void floatText(float x, float y, float z, String s, Color c) {
        FloatText f = new FloatText();
        f.x = x; f.y = y; f.z = z; f.s = s; f.c = c; f.life = 1.2f;
        floats.add(f);
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) { particles.remove(i); continue; }
            p.vy -= p.grav * dt;
            p.vx *= 1 - 2.5f * dt; p.vz *= 1 - 2.5f * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
            if (p.y < 0) { p.y = 0; p.vy *= -0.3f; }
        }
        for (int i = floats.size() - 1; i >= 0; i--) {
            FloatText f = floats.get(i);
            f.life -= dt; f.y += dt * 3;
            if (f.life <= 0) floats.remove(i);
        }
        shake = Math.max(0, shake - dt * 2.5f);
        hurtFlash = Math.max(0, hurtFlash - dt);
        bannerT -= dt; bubbleT -= dt; heardT -= dt;
    }

    // ================================================================= camera
    private final Vector3 target = new Vector3(), want = new Vector3(), tmp = new Vector3(), tmp2 = new Vector3();

    private void updateCamera(float dt) {
        City.Giraffe g = city.g;
        if (scr != Scr.PLAY && scr != Scr.PAUSE) {
            // slow orbit for the menus
            float a = titleT * 0.06f;
            target.set(City.SIZE / 2, 20, City.SIZE / 2);
            cam.position.set(target.x + MathUtils.sin(a) * 260, 140, target.z + MathUtils.cos(a) * 260);
            cam.fieldOfView = 60; cam.up.set(0, 1, 0); cam.lookAt(target); cam.update();
            return;
        }
        if (scr == Scr.PLAY) {
            float dx = Gdx.input.isCursorCatched() ? Gdx.input.getDeltaX() : 0, dy = Gdx.input.isCursorCatched() ? Gdx.input.getDeltaY() : 0;
            if (Math.abs(dx) + Math.abs(dy) > 0.5f) mouseIdle = 0; else mouseIdle += dt;
            camYaw -= dx * 0.0032f * save.sensitivity;
            camPitch += dy * 0.0032f * save.sensitivity * (save.invertY ? -1 : 1);
            camPitch = MathUtils.clamp(camPitch, -0.35f, 1.25f);
            float hs = g.hSpeed();
            boolean driving = g.mode == City.M_DRIVE;
            if ((mouseIdle > 1.2f && hs > 8) || autoplay || (driving && mouseIdle > 0.6f)) {
                float to = driving ? g.yaw : MathUtils.atan2(g.vx, g.vz);
                if (driving && city.cars.get(g.car).speed < -1) to = g.yaw;
                camYaw = City.lerpAngle(camYaw, to, Math.min(1, dt * (driving ? 3f : 1.8f)));
            }
        }
        float speed = g.mode == City.M_DRIVE ? Math.abs(city.cars.get(g.car).speed) : g.speed();
        float dist = (g.mode == City.M_DRIVE ? 14 : 15) + MathUtils.clamp(speed - 20, 0, 40) * 0.15f;
        target.set(g.x, g.y + (g.mode == City.M_DRIVE ? 4 : 6), g.z);
        float cp = MathUtils.cos(camPitch), spch = MathUtils.sin(camPitch);
        want.set(target.x - MathUtils.sin(camYaw) * dist * cp, target.y + dist * spch, target.z - MathUtils.cos(camYaw) * dist * cp);
        // pull in if a building is in the way
        float keep = 1f;
        for (int i = 1; i <= 24; i++) {
            float t = i / 24f;
            tmp.set(target).lerp(want, t);
            if (city.insideBuilding(tmp.x, tmp.y, tmp.z)) { keep = Math.max(0.12f, (i - 1.5f) / 24f); break; }
        }
        want.set(target).lerp(tmp2.set(target.x - MathUtils.sin(camYaw) * dist * cp, target.y + dist * spch, target.z - MathUtils.cos(camYaw) * dist * cp), keep);
        if (want.y < 1) want.y = 1;
        cam.position.set(want);
        if (shake > 0) cam.position.add(MathUtils.random(-1f, 1f) * shake, MathUtils.random(-1f, 1f) * shake, MathUtils.random(-1f, 1f) * shake);
        float wantFov = 70 + MathUtils.clamp(speed - 20, 0, 50) * 0.35f;
        fov += (wantFov - fov) * Math.min(1, dt * 3);
        cam.fieldOfView = fov;
        cam.up.set(0, 1, 0);
        cam.lookAt(target);
        cam.update();
        tmp.set(cam.direction).crs(cam.up).nor();
        sfx.listener(cam.position, tmp);
    }

    // ================================================================= world drawing
    private final Matrix4 m4 = new Matrix4(), m5 = new Matrix4();

    private void drawWorld() {
        art.resetPools();
        batch.begin(cam);
        batch.render(cityInst, env);
        City.Giraffe g = city.g;
        for (City.Car c : city.cars) drawCar(c, city.g.car >= 0 && city.cars.get(city.g.car) == c);
        for (City.Enemy e : city.enemies) drawEnemy(e);
        for (City.Ped q : city.peds) if (City.dist2(q.x, q.z, cam.position.x, cam.position.z) < 230 * 230) drawPed(q);
        // sniper lasers: thin while tracking, thick and blinking once locked
        for (City.Enemy e : city.enemies) {
            if (e.kind != City.K_SNIPER || e.aimT <= 0 || !e.alive()) continue;
            boolean locked = e.aimT >= City.SNIPER_LOCK;
            if (locked && ((int) (clock * 20)) % 2 == 0) continue;
            art.segment(art.laser, e.x, e.y + 2.4f, e.z, e.aimX, e.aimY, e.aimZ, locked ? 0.16f : 0.07f);
        }
        // where the neck will grab
        if (g.grabOk && g.mode != City.M_SWING && g.mode != City.M_DEAD && scr != Scr.TITLE) {
            float s = 1.7f + 0.25f * MathUtils.sin(clock * 8);
            m4.setToTranslation(g.gx, g.gy, g.gz).rotateRad(Vector3.Y, clock * 3).rotate(Vector3.X, 45).rotate(Vector3.Z, 45).scale(s, s, s);
            art.at(art.grab, m4);
            // dotted line from the head so you can see it's in reach
            float hx = g.x, hy = g.y + (g.mode == City.M_DRIVE ? 7 : 8.5f), hz = g.z;
            for (int i = 1; i < 10; i++) {
                float t = i / 10f;
                art.at(art.grab, hx + (g.gx - hx) * t, hy + (g.gy - hy) * t, hz + (g.gz - hz) * t, 0.3f, 0.3f, 0.3f);
            }
        }
        // golden leaves
        for (int i = 0; i < city.leaves.size(); i++) {
            if ((city.leafMask & (1 << i)) != 0) continue;
            City.Leaf l = city.leaves.get(i);
            if (City.dist2(l.x, l.z, cam.position.x, cam.position.z) > 200 * 200) continue;
            float bob = MathUtils.sin(clock * 2 + i) * 0.4f;
            m4.setToTranslation(l.x, l.y + bob, l.z).rotateRad(Vector3.Y, clock * 1.5f + i);
            art.part(art.goldLeaf, m4, 0, 0, 0, 1.6f, 0.12f, 2.4f);
            art.part(art.goldLeaf, m4, 0, 0, -1.4f, 0.15f, 0.15f, 0.8f);
            if (City.dist2(l.x, l.z, g.x, g.z) < 90 * 90) art.at(art.beacon, l.x, l.y + 12, l.z, 0.8f, 22, 0.8f);
        }
        // races: start beacons when free, checkpoints while racing
        if (city.act < 0 && !city.missionActive) {
            for (City.Activity a : city.acts)
                art.at(a.type == City.ACT_SWING ? art.beaconBlue : art.beaconPurple, a.sx, 40, a.sz, 6, 80, 6);
        } else if (city.act >= 0) {
            City.Activity a = city.acts.get(city.act);
            for (int i = city.actNext; i < Math.min(a.pts.length, city.actNext + 3); i++) {
                float[] pt = a.pts[i];
                boolean next = i == city.actNext, last = i == a.pts.length - 1;
                if (a.type == City.ACT_SWING) {
                    float s = next ? 1.15f + 0.08f * MathUtils.sin(clock * 6) : 0.85f;
                    m4.setToTranslation(pt[0], pt[1], pt[2]).rotateRad(Vector3.Y, pt[3]).scale(s, s, s);
                    art.at(last ? art.goldLeaf : art.ring, m4);
                    if (last) { m4.scale(0.9f, 0.9f, 0.9f); art.at(art.ring, m4); }
                } else {
                    art.at(next ? art.beaconPurple : art.beaconBlue, pt[0], 30, pt[2], next ? 11 : 6, 60, next ? 11 : 6);
                    if (last) for (int k = 0; k < 8; k++) art.at(k % 2 == 0 ? art.checker : art.wheel, pt[0] - 8 + k * 2.2f, 0.05f, pt[2], 2.2f, 0.1f, 2.2f);
                }
            }
        }
        if (g.mode != City.M_DRIVE && g.mode != City.M_DEAD) drawGiraffe(g);
        if (g.mode == City.M_DEAD) drawDeadGiraffe(g);
        // mission bits
        if (city.missionActive && city.mission == 0) {
            boolean first = true;
            for (City.Ring r : city.rings) {
                if (r.done) continue;
                float s = first ? 1f + 0.08f * MathUtils.sin(clock * 6) : 0.8f;
                m4.setToTranslation(r.x, r.y, r.z).rotateRad(Vector3.Y, r.yaw).scale(s, s, s);
                art.at(art.ring, m4);
                first = false;
            }
        }
        if (!city.missionActive && city.mission < City.MISSIONS) {
            float[] s = city.missionStart[city.mission];
            float p = 1 + 0.1f * MathUtils.sin(clock * 3);
            art.at(art.beacon, s[0], 40, s[1], 7 * p, 80, 7 * p);
        }
        if (city.missionActive && ((city.mission == 2 && city.stage == 0) || (city.mission == 3 && city.crateState == 1)))
            art.at(art.beacon, city.ox, 40, city.oz, 12, 80, 12);
        if (city.crimeActive && !city.missionActive) art.at(art.beacon, city.crimeX, 40, city.crimeZ, 5, 80, 5);
        if (city.missionActive && city.mission == 3 && city.crateState == 0) {
            m4.setToTranslation(city.crateX, city.crateY - 1, city.crateZ).rotateRad(Vector3.Y, clock);
            art.at(art.crate, m4);
            art.at(art.beacon, city.crateX, city.crateY + 30, city.crateZ, 4, 60, 4);
        }
        for (City.Bullet b : city.bullets) art.segment(art.bullet, b.x, b.y, b.z, b.x - b.vx * 0.03f, b.y - b.vy * 0.03f, b.z - b.vz * 0.03f, 0.18f);
        for (Particle p : particles) {
            float s = p.size * (p.m == art.smoke ? 1 + (1 - p.life / p.max) * 2 : p.life / p.max);
            art.at(p.m, p.x, p.y, p.z, s, s, s);
        }
        // blob shadows help judge height while swinging
        if (g.mode != City.M_DRIVE) {
            float gy = city.groundAt(g.x, g.z, 0.5f);
            float hgt = g.y - gy, s = MathUtils.clamp(3.2f - hgt * 0.03f, 1.2f, 3.2f);
            art.at(art.shadow, g.x, gy + 0.32f, g.z, s, 1, s * 1.3f);
        }
        batch.end();
    }

    private final Vector3 S = new Vector3(), H = new Vector3(), D = new Vector3();

    private void drawGiraffe(City.Giraffe g) {
        m4.setToTranslation(g.x, g.y, g.z).rotateRad(Vector3.Y, g.yaw);
        float hs = g.hSpeed();
        float swingAmt = g.mode == City.M_FOOT ? Math.min(1, hs / 6f) : 0;
        float phase = g.runPhase * 2.2f;
        float[] lx = {0.6f, -0.6f, 0.6f, -0.6f}, lz = {1.15f, 1.15f, -1.15f, -1.15f};
        for (int i = 0; i < 4; i++) {
            float ang;
            if (g.mode == City.M_FOOT) ang = MathUtils.sin(phase + (i == 0 || i == 3 ? 0 : MathUtils.PI)) * 38 * swingAmt;
            else if (g.mode == City.M_SWING) ang = -25 + MathUtils.sin(clock * 3 + i) * 8;
            else ang = i < 2 ? -35 : 35;
            if (g.kickT > 0 && i >= 2) ang = -80 * (g.kickT / 0.3f) + 10;
            art.part(art.spotted, m4, lx[i], 1.75f, lz[i], 0.4f, 3.5f, 0.4f, Vector3.X, ang, lx[i], 3.5f, lz[i]);
            art.part(art.hoof, m4, lx[i], 0.15f, lz[i], 0.46f, 0.3f, 0.46f, Vector3.X, ang, lx[i], 3.5f, lz[i]);
        }
        // body, slightly higher at the shoulders
        art.part(art.spotted, m4, 0, 4.25f, 0, 1.9f, 1.75f, 3.5f, Vector3.X, -7, 0, 4.25f, 0);
        art.part(art.spotted, m4, 0, 3.9f, -2.05f, 0.16f, 1.7f, 0.16f, Vector3.X, 25 + MathUtils.sin(clock * 4) * 15, 0, 4.7f, -1.9f);
        art.part(art.spottedDark, m4, 0, 3.0f, -2.45f, 0.3f, 0.6f, 0.3f, Vector3.X, 25 + MathUtils.sin(clock * 4) * 15, 0, 4.7f, -1.9f);

        S.set(0, 5.0f, 1.45f).mul(m4);   // shoulder
        float fx = MathUtils.sin(g.yaw), fz = MathUtils.cos(g.yaw);
        boolean wrapped = false;
        if (g.mode == City.M_SWING) {
            H.set(g.ax, g.ay, g.az); wrapped = true;
        } else if (g.spinT > 0) {
            float a = g.yaw + (1 - g.spinT / City.SPIN_TIME) * MathUtils.PI2 * 2;
            H.set(g.x + MathUtils.sin(a) * 6.5f, g.y + 5.6f, g.z + MathUtils.cos(a) * 6.5f);
        } else if (g.slamming) {
            H.set(g.x + fx * 3.5f, g.y + 1.5f, g.z + fz * 3.5f);
        } else {
            float bob = MathUtils.sin(phase * 2) * 0.25f * swingAmt;
            float lean = Math.min(1, hs / City.RUN_SPEED) * 1.3f;
            H.set(0, 8.8f + bob - lean * 0.6f, 2.6f + lean).mul(m4);
            if (g.mode == City.M_AIR) H.y -= 0.4f;
        }
        drawNeck(S, H, g.yaw, wrapped);
        if (g.carrying) {
            m5.setToTranslation(H.x, H.y - 2.6f, H.z).rotateRad(Vector3.Y, g.yaw).scale(0.8f, 0.8f, 0.8f);
            art.at(art.crate, m5);
            art.segment(art.hat, H.x, H.y - 0.3f, H.z, H.x, H.y - 1.0f, H.z, 0.1f);
        }
        if (g.tongueT > 0 || g.munchT > 0.6f) {
            D.set(H.x + fx * 0.9f, H.y - 0.2f, H.z + fz * 0.9f);
            float k = g.tongueT > 0 ? Math.min(1, (0.35f - g.tongueT) / 0.12f + 0.2f) : 1;
            art.segment(art.tongue, D.x, D.y, D.z, D.x + (g.tongueX - D.x) * k, D.y + (g.tongueY - D.y) * k, D.z + (g.tongueZ - D.z) * k, 0.22f);
        }
        if (g.lookT > 0) {
            // a little periscope glint
            art.at(art.star, H.x, H.y + 1.6f + MathUtils.sin(clock * 8) * 0.2f, H.z, 0.3f, 0.3f, 0.3f);
        }
    }

    private void drawNeck(Vector3 s, Vector3 h, float yaw, boolean wrapped) {
        D.set(h).sub(s);
        float len = D.len();
        int n = Math.max(3, (int) (len / 1.4f));
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            float thick = 0.85f - 0.3f * t0;
            art.segment(art.spotted, s.x + D.x * t0, s.y + D.y * t0, s.z + D.z * t0, s.x + D.x * t1, s.y + D.y * t1, s.z + D.z * t1, thick);
            if (i % 2 == 0) { // mane
                art.segment(art.spottedDark, s.x + D.x * t0, s.y + D.y * t0 + thick * 0.45f, s.z + D.z * t0,
                        s.x + D.x * t1, s.y + D.y * t1 + thick * 0.45f, s.z + D.z * t1, 0.18f);
            }
        }
        if (wrapped) {
            m5.setToTranslation(h.x, h.y, h.z).rotateRad(Vector3.Y, clock * 4);
            art.at(art.coil, m5);
            drawHead(h.x, h.y + 0.9f, h.z, yaw);
        } else drawHead(h.x, h.y, h.z, yaw);
    }

    private void drawHead(float x, float y, float z, float yaw) {
        m5.setToTranslation(x, y, z).rotateRad(Vector3.Y, yaw);
        art.part(art.spotted, m5, 0, 0, 0.35f, 0.7f, 0.72f, 1.5f);
        art.part(art.spottedDark, m5, 0, -0.12f, 1.12f, 0.6f, 0.5f, 0.2f);
        art.part(art.ossicone, m5, 0.22f, 0.62f, -0.2f, 0.14f, 0.6f, 0.14f);
        art.part(art.ossicone, m5, -0.22f, 0.62f, -0.2f, 0.14f, 0.6f, 0.14f);
        art.part(art.hoof, m5, 0.22f, 0.98f, -0.2f, 0.24f, 0.2f, 0.24f);
        art.part(art.hoof, m5, -0.22f, 0.98f, -0.2f, 0.24f, 0.2f, 0.24f);
        art.part(art.spotted, m5, 0.52f, 0.32f, -0.25f, 0.5f, 0.16f, 0.22f);
        art.part(art.spotted, m5, -0.52f, 0.32f, -0.25f, 0.5f, 0.16f, 0.22f);
        art.part(art.eye, m5, 0.36f, 0.15f, 0.35f, 0.08f, 0.16f, 0.16f);
        art.part(art.eye, m5, -0.36f, 0.15f, 0.35f, 0.08f, 0.16f, 0.16f);
    }

    private void drawDeadGiraffe(City.Giraffe g) {
        m4.setToTranslation(g.x, g.y + 1, g.z).rotateRad(Vector3.Y, g.yaw).rotate(Vector3.Z, 90);
        art.part(art.spotted, m4, 0, 0, 0, 1.9f, 1.75f, 3.5f);
        for (int i = 0; i < 4; i++) art.part(art.spotted, m4, -2.6f, (i % 2 - 0.5f) * 1.2f, i < 2 ? 1.1f : -1.1f, 3.4f, 0.4f, 0.4f);
        S.set(0, 0.3f, 1.6f).mul(m4);
        H.set(g.x + MathUtils.sin(g.yaw) * 7, g.y + 0.6f, g.z + MathUtils.cos(g.yaw) * 7);
        drawNeck(S, H, g.yaw, false);
        art.at(art.star, H.x + MathUtils.sin(clock * 5) * 0.8f, H.y + 1.5f, H.z + MathUtils.cos(clock * 5) * 0.8f, 0.3f, 0.3f, 0.3f);
    }

    private void drawEnemy(City.Enemy e) {
        float sc = e.kind == City.K_GRUNT ? 1.25f : e.kind == City.K_BRUTE ? 1.7f : 2.2f;
        m4.setToTranslation(e.x, e.y, e.z).rotateRad(Vector3.Y, e.yaw);
        if (e.state == City.E_DEAD) {
            float sink = Math.max(0, 1 - e.deadT) * 0.8f;
            m4.translate(0, 0.4f - sink, 0).rotate(Vector3.X, -90);
        } else if (e.state == City.E_FLYING) {
            m4.translate(0, 1.5f * sc, 0).rotate(Vector3.X, clock * 720).translate(0, -1.5f * sc, 0);
        }
        m4.scale(sc, sc, sc);
        Model torso = e.flashT > 0 ? art.spark : e.kind == City.K_GRUNT ? art.vest : e.kind == City.K_BRUTE ? art.vestBrute
                : e.kind == City.K_SNIPER ? art.ghillie : art.coat;
        float sw = e.state == City.E_CHASE || e.state == City.E_IDLE ? MathUtils.sin(e.walkPhase * 3) * 30 : 0;
        art.part(art.pants, m4, 0.2f, 0.6f, 0, 0.3f, 1.2f, 0.3f, Vector3.X, sw, 0, 1.2f, 0);
        art.part(art.pants, m4, -0.2f, 0.6f, 0, 0.3f, 1.2f, 0.3f, Vector3.X, -sw, 0, 1.2f, 0);
        art.part(art.boot, m4, 0.2f, 0.1f, 0.05f, 0.34f, 0.2f, 0.45f, Vector3.X, sw, 0, 1.2f, 0);
        art.part(art.boot, m4, -0.2f, 0.1f, 0.05f, 0.34f, 0.2f, 0.45f, Vector3.X, -sw, 0, 1.2f, 0);
        art.part(torso, m4, 0, 1.75f, 0, 0.95f, 1.15f, 0.55f);
        art.part(torso, m4, 0.62f, 1.8f, 0.15f, 0.25f, 0.9f, 0.25f, Vector3.X, -60, 0.62f, 2.2f, 0);
        art.part(torso, m4, -0.62f, 1.8f, 0.15f, 0.25f, 0.9f, 0.25f, Vector3.X, -60, -0.62f, 2.2f, 0);
        art.part(art.skin, m4, 0, 2.62f, 0, 0.5f, 0.55f, 0.5f);
        art.part(art.hat, m4, 0, 2.95f, 0, 0.95f, 0.08f, 0.95f);
        art.part(art.hat, m4, 0, 3.08f, 0, 0.55f, 0.25f, 0.55f);
        if (e.kind == City.K_SNIPER && e.disarmT <= 0 && e.state != City.E_DEAD) {
            art.part(art.rifle, m4, 0.15f, 2.3f, 1.0f, 0.12f, 0.16f, 2.2f);
            art.part(art.rifle, m4, 0.15f, 2.5f, 0.8f, 0.12f, 0.14f, 0.5f);   // scope
        } else if (e.kind != City.K_BRUTE && e.disarmT <= 0 && e.state != City.E_DEAD)
            art.part(art.gun, m4, 0, 1.95f, 0.75f, 0.14f, 0.18f, 1.1f);
        if (e.kind == City.K_BRUTE && e.chargeT > 0) art.part(art.fire, m4, 0, 3.6f, 0, 0.3f, 0.3f, 0.3f);
        if (e.state == City.E_STUNNED) {
            for (int i = 0; i < 3; i++) {
                float a = clock * 5 + i * 2.1f;
                art.part(art.star, m4, MathUtils.cos(a) * 0.55f, 3.4f, MathUtils.sin(a) * 0.55f, 0.15f, 0.15f, 0.15f);
            }
        }
        if (e.kind == City.K_BOSS && e.shield && e.alive()) art.at(art.shieldBubble, e.x, e.y + 3.3f, e.z, 7.5f, 8f, 7.5f);
    }

    private void drawPed(City.Ped q) {
        m4.setToTranslation(q.x, q.y + (q.y < 0.5f ? 0.24f : 0), q.z).rotateRad(Vector3.Y, q.yaw);
        if (q.state == City.PD_DOWN) m4.translate(0, 0.3f, 0).rotate(Vector3.X, -90);
        m4.scale(1.1f, 1.1f, 1.1f);
        float sw = q.state == City.PD_DOWN ? 0 : MathUtils.sin(q.walkPhase * 3) * (q.state == City.PD_FLEE ? 45 : 25);
        Model skin = art.skins[q.skin];
        art.part(art.pants, m4, 0.18f, 0.55f, 0, 0.26f, 1.1f, 0.26f, Vector3.X, sw, 0, 1.1f, 0);
        art.part(art.pants, m4, -0.18f, 0.55f, 0, 0.26f, 1.1f, 0.26f, Vector3.X, -sw, 0, 1.1f, 0);
        art.part(art.shirts[q.shirt], m4, 0, 1.6f, 0, 0.8f, 1.0f, 0.45f);
        // arms swing, or go up when they're running away
        float arm = q.state == City.PD_FLEE ? 160 : -sw;
        art.part(art.shirts[q.shirt], m4, 0.52f, 1.65f, 0, 0.2f, 0.85f, 0.2f, Vector3.X, arm, 0.52f, 2.0f, 0);
        art.part(art.shirts[q.shirt], m4, -0.52f, 1.65f, 0, 0.2f, 0.85f, 0.2f, Vector3.X, q.state == City.PD_FLEE ? 160 : sw, -0.52f, 2.0f, 0);
        art.part(skin, m4, 0, 2.38f, 0, 0.45f, 0.5f, 0.45f);
        art.part(q.hat ? art.hat : art.hair, m4, 0, 2.66f, -0.03f, 0.48f, 0.14f, 0.5f);
    }

    private void drawCar(City.Car c, boolean player) {
        m4.setToTranslation(c.x, c.y, c.z).rotateRad(Vector3.Y, c.yaw);
        if (c.roll != 0) m4.translate(0, 1.2f, 0).rotate(Vector3.Z, c.roll * MathUtils.radDeg).translate(0, -1.2f, 0);
        boolean wrecked = c.state == City.C_WRECK;
        Model body = wrecked ? art.wreck : c.hitT > 0 ? art.spark : art.carBody[c.color];
        art.part(body, m4, 0, 1.1f, 0, 2.6f, 1.0f, 5.0f);
        if (wrecked) {
            // burnt shell: crushed roof, no glass, sitting on the rims
            art.part(art.wreck, m4, 0.1f, 1.8f, -0.2f, 2.1f, 0.5f, 2.3f, Vector3.Z, 8, 0, 1.6f, 0);
            for (int i = 0; i < 4; i++) art.part(art.wreck, m4, i % 2 == 0 ? 1.3f : -1.3f, 0.35f, i < 2 ? 1.6f : -1.6f, 0.3f, 0.7f, 0.7f);
            return;
        }
        // dents: the roof sags and a bumper hangs as health drops
        float dmg = 1 - c.hp / City.CAR_HP;
        art.part(art.glassDark, m4, 0, 2.05f - dmg * 0.25f, -0.3f, 2.2f, 0.9f - dmg * 0.3f, 2.6f);
        art.part(body, m4, 0, 2.52f - dmg * 0.4f, -0.3f, 2.25f, 0.08f, 2.7f, Vector3.Z, dmg * 7, 0, 2.5f, -0.3f);
        for (int i = 0; i < 4; i++) art.part(art.wheel, m4, i % 2 == 0 ? 1.3f : -1.3f, 0.5f, i < 2 ? 1.6f : -1.6f, 0.45f, 1f, 1f);
        if (c.hp > 30) {
            art.part(art.light, m4, 0.8f, 1.15f, 2.52f, 0.5f, 0.3f, 0.06f);
            art.part(art.light, m4, -0.8f, 1.15f, 2.52f, 0.5f, 0.3f, 0.06f);
        } else art.part(art.debris, m4, 0, 0.75f, 2.6f, 2.6f, 0.3f, 0.3f, Vector3.X, 18, 0, 0.9f, 2.5f);
        art.part(art.tail, m4, 0.85f, 1.15f, -2.52f, 0.45f, 0.25f, 0.06f);
        art.part(art.tail, m4, -0.85f, 1.15f, -2.52f, 0.45f, 0.25f, 0.06f);
        if (player) {
            // giraffe neck out of the sunroof
            S.set(0, 2.3f, -0.4f).mul(m4);
            float wob = MathUtils.sin(clock * 9) * 0.2f;
            H.set(0.0f + wob, 7.6f, 0.2f - MathUtils.clamp(c.speed / 40f, -0.5f, 1.2f)).mul(m4);
            drawNeck(S, H, c.yaw, false);
            City.Giraffe g = city.g;
            if (g.carrying) {
                m5.setToTranslation(H.x, H.y - 2.6f, H.z).rotateRad(Vector3.Y, c.yaw).scale(0.8f, 0.8f, 0.8f);
                art.at(art.crate, m5);
            }
        }
    }

    // ================================================================= UI
    private static final class Btn { float x, y, w, h; String label; boolean hot; }
    private final ArrayList<Btn> btns = new ArrayList<>();
    private final Vector3 mouse = new Vector3();
    private boolean clicked;

    private boolean button(float cx, float y, float w, float h, String label) {
        Btn b = new Btn(); b.x = cx - w / 2; b.y = y; b.w = w; b.h = h; b.label = label;
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0); ui.unproject(mouse);
        b.hot = mouse.x >= b.x && mouse.x <= b.x + w && mouse.y >= b.y && mouse.y <= b.y + h;
        btns.add(b);
        if (b.hot && clicked) { sfx.play("click"); clicked = false; return true; }
        return false;
    }

    private void flushButtons() {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Btn b : btns) {
            sr.setColor(b.hot ? 0.98f : 0.95f, b.hot ? 0.78f : 0.65f, b.hot ? 0.25f : 0.2f, 0.95f);
            sr.rect(b.x, b.y, b.w, b.h);
        }
        sr.end();
        sb.begin();
        for (Btn b : btns) {
            f26.setColor(0.18f, 0.1f, 0.05f, 1);
            f26.draw(sb, b.label, b.x, b.y + b.h / 2 + 11, b.w, Align.center, false);
        }
        f26.setColor(Color.WHITE);
        sb.end();
        btns.clear();
    }

    private void drawUi(float dt) {
        clicked = Gdx.input.justTouched() && !Gdx.input.isCursorCatched();
        ui.apply();
        sb.setProjectionMatrix(ui.getCamera().combined);
        sr.setProjectionMatrix(ui.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        float W = ui.getWorldWidth(), Hh = ui.getWorldHeight();
        switch (scr) {
            case TITLE: drawTitle(W, Hh); break;
            case SETTINGS: dim(W, Hh, 0.55f); drawSettings(W, Hh); break;
            case CONTROLS: dim(W, Hh, 0.65f); drawControls(W, Hh); break;
            case PLAY: drawHud(W, Hh); break;
            case PAUSE: drawHud(W, Hh); dim(W, Hh, 0.55f); drawPause(W, Hh); break;
            case SKILLS: drawHud(W, Hh); dim(W, Hh, 0.75f); drawSkills(W, Hh); break;
        }
        if (save.showFps) { sb.begin(); f18.draw(sb, Gdx.graphics.getFramesPerSecond() + " fps", 10, Hh - 10); sb.end(); }
    }

    private void dim(float W, float H, float a) {
        sr.begin(ShapeRenderer.ShapeType.Filled); sr.setColor(0, 0, 0, a); sr.rect(0, 0, W, H); sr.end();
    }

    private void text(BitmapFont f, String s, float x, float y, Color c, int align, float width) {
        f.setColor(0, 0, 0, c.a * 0.6f);
        f.draw(sb, s, x + 2, y - 2, width, align, true);
        f.setColor(c);
        f.draw(sb, s, x, y, width, align, true);
        f.setColor(Color.WHITE);
    }

    private void drawTitle(float W, float H) {
        dim(W, H, 0.25f);
        sb.begin();
        text(f110, "NECKSWING", 0, H - 110, Color.GOLD, Align.center, W);
        text(f26, "He's tall. He's angry. He swings by the neck.", 0, H - 250, Color.WHITE, Align.center, W);
        text(f18, Voice.supported() ? voice.status : "Voice commands work on Windows. Keys work everywhere.", 0, 40, Color.LIGHT_GRAY, Align.center, W);
        sb.end();
        float y = H / 2 - 20;
        if (save.mission > 0 || save.respect > 0 || save.skills != 0) {
            String lbl = save.mission >= City.MISSIONS ? "Continue (free roam)" : "Continue: " + City.MISSION_NAMES[save.mission];
            if (button(W / 2, y, 420, 56, lbl)) newGame(save.mission);
            y -= 70;
        }
        if (button(W / 2, y, 420, 56, "New Game")) {
            save.mission = 0; save.respect = 0; save.skills = 0; save.leaves = 0; save.raceBest = new float[0]; save.write(); newGame(0);
        }
        y -= 70;
        if (button(W / 2, y, 420, 56, "Controls")) { controlsBack = Scr.TITLE; scr = Scr.CONTROLS; }
        y -= 70;
        if (button(W / 2, y, 420, 56, "Settings")) { settingsBack = Scr.TITLE; scr = Scr.SETTINGS; }
        y -= 70;
        if (button(W / 2, y, 420, 56, "Quit")) Gdx.app.exit();
        flushButtons();
    }

    private void drawPause(float W, float H) {
        sb.begin(); text(f46, "PAUSED", 0, H - 150, Color.WHITE, Align.center, W); sb.end();
        float y = H / 2 + 40;
        if (button(W / 2, y, 380, 56, "Resume")) unpause();
        if (button(W / 2, y - 70, 380, 56, "Skill Tree (K)")) openSkills(Scr.PAUSE);
        if (button(W / 2, y - 140, 380, 56, "Controls")) { controlsBack = Scr.PAUSE; scr = Scr.CONTROLS; }
        if (button(W / 2, y - 210, 380, 56, "Settings")) { settingsBack = Scr.PAUSE; scr = Scr.SETTINGS; }
        if (city.act >= 0 && button(W / 2, y - 280, 380, 56, "Quit Race")) { city.failActivity("Race cancelled."); unpause(); }
        if (button(W / 2, y - (city.act >= 0 ? 350 : 280), 380, 56, "Quit to Title")) {
            save.from(city); save.write();
            scr = Scr.TITLE; sfx.engine(-1); sfx.wind(0); sfx.music(Sfx.MUSIC_MENU);
            city = new City(System.nanoTime());
        }
        flushButtons();
    }

    private void drawSettings(float W, float H) {
        sb.begin(); text(f46, "SETTINGS", 0, H - 70, Color.WHITE, Align.center, W); sb.end();
        float y = H - 170, step = 60;
        String[] labels = {
            "Sound: " + Math.round(save.sound * 100) + "%",
            "Music: " + Math.round(save.music * 100) + "%",
            "Mouse sensitivity: " + String.format("%.1f", save.sensitivity),
            "Invert mouse Y: " + (save.invertY ? "on" : "off"),
            "Fullscreen (F11): " + (save.fullscreen ? "on" : "off"),
            "Voice commands (mic): " + (save.voiceListen ? "on" : "off"),
            "Giraffe talks out loud: " + (save.voiceSpeak ? "on" : "off"),
            "Show FPS: " + (save.showFps ? "on" : "off"),
        };
        for (int i = 0; i < labels.length; i++) {
            if (button(W / 2, y - i * step, 560, 48, labels[i])) {
                switch (i) {
                    case 0: save.sound = save.sound >= 1 ? 0 : Math.min(1, save.sound + 0.1f); sfx.volume = save.sound; break;
                    case 1: save.music = save.music >= 1 ? 0 : Math.min(1, save.music + 0.1f); sfx.musicVolume = save.music; break;
                    case 2: save.sensitivity = save.sensitivity >= 2.5f ? 0.3f : save.sensitivity + 0.2f; break;
                    case 3: save.invertY = !save.invertY; break;
                    case 4: save.fullscreen = !save.fullscreen; applyFullscreen(); break;
                    case 5: save.voiceListen = !save.voiceListen; startVoice(); break;
                    case 6: save.voiceSpeak = !save.voiceSpeak; startVoice(); break;
                    case 7: save.showFps = !save.showFps; break;
                }
                save.write();
            }
        }
        sb.begin(); text(f18, voice.status, 0, y - labels.length * step + 10, Color.LIGHT_GRAY, Align.center, W); sb.end();
        if (button(W / 2, 40, 300, 52, "Back")) { save.write(); scr = settingsBack; }
        flushButtons();
    }

    private int hoverSkill = -1;

    private void drawSkills(float W, float H) {
        City c = city;
        sb.begin();
        text(f46, "SKILL TREE", 0, H - 40, Color.GOLD, Align.center, W);
        text(f26, "Respect to spend: " + c.respect, 0, H - 100, Color.WHITE, Align.center, W);
        sb.end();
        float colW = 330, gap = 40, boxH = 62, rowGap = 16;
        float x0 = W / 2 - (colW * 3 + gap * 2) / 2, top = H - 190;
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0); ui.unproject(mouse);
        hoverSkill = -1;
        float[] bx = new float[Skills.COUNT], by = new float[Skills.COUNT];
        for (int k = 0; k < Skills.COUNT; k++) {
            int b = k / Skills.TIERS, t = k % Skills.TIERS;
            bx[k] = x0 + b * (colW + gap); by[k] = top - t * (boxH + rowGap) - boxH;
            if (mouse.x >= bx[k] && mouse.x <= bx[k] + colW && mouse.y >= by[k] && mouse.y <= by[k] + boxH) hoverSkill = k;
        }
        // connectors, then boxes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int k = 0; k < Skills.COUNT; k++) {
            if (k % Skills.TIERS == 0) continue;
            sr.setColor(c.has(k - 1) ? new Color(0.95f, 0.75f, 0.2f, 1) : new Color(0.35f, 0.35f, 0.35f, 1));
            sr.rect(bx[k] + colW / 2 - 3, by[k] + boxH, 6, rowGap);
        }
        for (int k = 0; k < Skills.COUNT; k++) {
            boolean own = c.has(k), open = Skills.unlocked(c.skills, k), afford = c.respect >= Skills.cost(k);
            Color col = own ? new Color(0.95f, 0.72f, 0.2f, 1) : open && afford ? new Color(0.30f, 0.62f, 0.32f, 1)
                    : open ? new Color(0.28f, 0.30f, 0.34f, 1) : new Color(0.16f, 0.16f, 0.18f, 1);
            if (k == hoverSkill) col.lerp(Color.WHITE, 0.2f);
            sr.setColor(col);
            sr.rect(bx[k], by[k], colW, boxH);
        }
        sr.end();
        sb.begin();
        for (int b = 0; b < Skills.BRANCHES; b++) text(f26, Skills.BRANCH_NAMES[b], x0 + b * (colW + gap), top + 44, Color.WHITE, Align.center, colW);
        for (int k = 0; k < Skills.COUNT; k++) {
            boolean own = c.has(k), open = Skills.unlocked(c.skills, k);
            Color tc = own ? new Color(0.2f, 0.1f, 0.02f, 1) : open ? Color.WHITE : Color.GRAY;
            f26.setColor(tc); f26.draw(sb, Skills.NAMES[k], bx[k] + 14, by[k] + boxH - 8);
            f18.setColor(tc); f18.draw(sb, own ? "Owned" : open ? Skills.cost(k) + " respect" : "Locked", bx[k] + 14, by[k] + 24);
        }
        f26.setColor(Color.WHITE); f18.setColor(Color.WHITE);
        String tip = hoverSkill >= 0 ? Skills.DESC[hoverSkill] : "Click a skill to buy it. Each branch unlocks top to bottom.";
        text(f18, tip, W * 0.1f, 126, Color.WHITE, Align.center, W * 0.8f);
        sb.end();
        if (clicked && hoverSkill >= 0) {
            clicked = false;
            if (c.buy(hoverSkill)) {
                sfx.play("buy");
                showBanner(Skills.NAMES[hoverSkill].toUpperCase(), Skills.DESC[hoverSkill], Color.GOLD);
                save.from(c); save.write();
            } else sfx.play("not_ready");
        }
        if (button(W / 2, 30, 300, 52, "Back (K)")) closeSkills();
        flushButtons();
    }

    private static final String[][] CONTROLS = {
        {"W A S D", "Run / steer the car", ""},
        {"Mouse", "Look around", ""},
        {"Hold Left Mouse (or Left Ctrl)", "Neck swing: wrap your neck around a building and swing", ""},
        {"Left Shift (while swinging)", "Reel your neck in to go higher", ""},
        {"Space", "Jump (hop out of a car)", "\"jump\""},
        {"Right Mouse or F", "Neck spin. In the air: Neck Slam", "\"spin\" / \"slam\""},
        {"E", "Get in / out of a car", "\"drive\" / \"get out\""},
        {"1", "Bellow: stun everyone nearby, breaks shields", "\"bellow\" / \"hum\""},
        {"2", "Tongue: yank a poacher and take his gun, or grab the crate", "\"lick\" / \"tongue\""},
        {"3", "Kick: one hoof, one launch. Works on cars too", "\"kick\""},
        {"4", "Lookout: see every poacher through walls for 8 s", "\"lookout\""},
        {"5", "Munch: eat leaves from a tree or rooftop garden to heal", "\"munch\" / \"snack\""},
        {"Q", "Neck Zip: fly straight to the green grab marker (skill)", "\"zip\""},
        {"K", "Skill tree", "\"skills\""},
        {"Backspace", "Quit the race you're in", "\"cancel\""},
        {"T", "Say something giraffe-ish", "\"talk\""},
        {"H", "Honk", "\"honk\""},
        {"Esc", "Pause", "\"pause\""},
    };

    private void drawControls(float W, float H) {
        sb.begin();
        text(f46, "CONTROLS", 0, H - 50, Color.WHITE, Align.center, W);
        float y = H - 120, x0 = W / 2 - 600;
        f18.setColor(Color.GOLD);
        f18.draw(sb, "Key", x0, y); f18.draw(sb, "What it does", x0 + 340, y); f18.draw(sb, "Or say", x0 + 1000, y);
        y -= 34;
        for (String[] r : CONTROLS) {
            f18.setColor(Color.WHITE); f18.draw(sb, r[0], x0, y);
            f18.setColor(Color.LIGHT_GRAY); f18.draw(sb, r[1], x0 + 340, y);
            f18.setColor(Color.CYAN); f18.draw(sb, r[2], x0 + 1000, y);
            y -= 27;
        }
        f18.setColor(Color.WHITE);
        text(f18, "Voice commands use the speech recognizer built into Windows. Turn them on or off in Settings.", 0, y - 10, Color.LIGHT_GRAY, Align.center, W);
        sb.end();
        if (button(W / 2, 30, 300, 52, "Back")) scr = controlsBack;
        flushButtons();
    }

    private final Vector3 proj = new Vector3();

    /** world -> ui coords. returns false if behind the camera. */
    private boolean toScreen(float x, float y, float z, Vector3 out) {
        tmp.set(x, y, z).sub(cam.position);
        boolean front = tmp.dot(cam.direction) > 0;
        out.set(x, y, z);
        cam.project(out, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        out.x = out.x / Gdx.graphics.getWidth() * ui.getWorldWidth();
        out.y = out.y / Gdx.graphics.getHeight() * ui.getWorldHeight();
        return front;
    }

    private void drawHud(float W, float H) {
        City c = city; City.Giraffe g = c.g;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        // hurt + lookout tint
        if (hurtFlash > 0) { sr.setColor(0.9f, 0, 0, hurtFlash * 0.6f); sr.rect(0, 0, W, H); }
        if (g.lookT > 0) { sr.setColor(1f, 0.8f, 0.1f, 0.08f); sr.rect(0, 0, W, H); }
        // health
        sr.setColor(0, 0, 0, 0.55f); sr.rect(20, 20, 320, 34);
        float hp = g.hp / c.hpMax();
        sr.setColor(hp > 0.5f ? 0.3f : hp > 0.25f ? 0.95f : 0.9f, hp > 0.5f ? 0.85f : hp > 0.25f ? 0.7f : 0.15f, 0.2f, 1);
        sr.rect(24, 24, 312 * hp, 26);
        // powers
        float pw = 128, ph = 64, px0 = W / 2 - (pw * City.POWERS + 10 * (City.POWERS - 1)) / 2;
        for (int i = 0; i < City.POWERS; i++) {
            float x = px0 + i * (pw + 10);
            boolean ready = g.cd[i] <= 0;
            sr.setColor(0, 0, 0, 0.6f); sr.rect(x, 16, pw, ph);
            sr.setColor(ready ? new Color(0.95f, 0.7f, 0.2f, 0.9f) : new Color(0.4f, 0.4f, 0.4f, 0.9f));
            sr.rect(x + 3, 19, (pw - 6) * (ready ? 1 : 1 - g.cd[i] / City.POWER_COOLDOWN[i]), 5);
        }
        // car health while driving
        if (g.mode == City.M_DRIVE) {
            City.Car car = c.cars.get(g.car);
            float ch = car.hp / City.CAR_HP;
            sr.setColor(0, 0, 0, 0.55f); sr.rect(20, 100, 220, 18);
            sr.setColor(car.burnT > 0 && ((int) (clock * 8)) % 2 == 0 ? Color.ORANGE : new Color(0.55f, 0.7f, 0.95f, 1));
            sr.rect(23, 103, 214 * Math.max(car.burnT > 0 ? 1 : 0, ch), 12);
        }
        // boss bar
        if (c.missionActive && c.mission == 4 && c.boss != null && c.boss.alive()) {
            sr.setColor(0, 0, 0, 0.6f); sr.rect(W / 2 - 300, H - 64, 600, 22);
            sr.setColor(c.boss.shield ? new Color(0.3f, 0.7f, 1f, 1) : new Color(0.9f, 0.2f, 0.2f, 1));
            sr.rect(W / 2 - 297, H - 61, 594 * c.boss.hp / c.boss.maxHp, 16);
        }
        // objective panel
        sr.setColor(0, 0, 0, 0.45f); sr.rect(16, H - 96, 620, 80);
        drawMinimapShapes(W, H);
        sr.end();

        // markers that need lines
        sr.begin(ShapeRenderer.ShapeType.Filled);
        if (c.hasObjective) {
            boolean front = toScreen(c.ox, c.oy + 2, c.oz, proj);
            float mx = proj.x, my = proj.y;
            if (!front) { mx = W - mx; my = 40; }
            mx = MathUtils.clamp(mx, 40, W - 40); my = MathUtils.clamp(my, 110, H - 140);
            float s = 14 + MathUtils.sin(clock * 6) * 2;
            sr.setColor(1, 0.85f, 0.1f, 0.95f);
            sr.triangle(mx - s, my, mx + s, my, mx, my - s * 1.4f);
            sr.triangle(mx - s, my, mx + s, my, mx, my + s * 0.8f);
        }
        // grab marker on the HUD: a green bracket around it, or an arrow at the screen edge if it's out of view
        if (g.grabOk && g.mode != City.M_SWING) {
            boolean front = toScreen(g.gx, g.gy, g.gz, proj);
            boolean inside = front && proj.x > 20 && proj.x < W - 20 && proj.y > 20 && proj.y < H - 20;
            sr.setColor(0.3f, 1f, 0.45f, 0.95f);
            if (inside) {
                float r = 22 + 3 * MathUtils.sin(clock * 8);
                sr.rectLine(proj.x - r, proj.y - r, proj.x - r + 9, proj.y - r, 3); sr.rectLine(proj.x - r, proj.y - r, proj.x - r, proj.y - r + 9, 3);
                sr.rectLine(proj.x + r, proj.y + r, proj.x + r - 9, proj.y + r, 3); sr.rectLine(proj.x + r, proj.y + r, proj.x + r, proj.y + r - 9, 3);
                sr.rectLine(proj.x - r, proj.y + r, proj.x - r + 9, proj.y + r, 3); sr.rectLine(proj.x - r, proj.y + r, proj.x - r, proj.y + r - 9, 3);
                sr.rectLine(proj.x + r, proj.y - r, proj.x + r - 9, proj.y - r, 3); sr.rectLine(proj.x + r, proj.y - r, proj.x + r, proj.y - r + 9, 3);
            } else {
                float ax = front ? proj.x : W - proj.x, ay = front ? proj.y : H - proj.y;
                float cx = W / 2, cy = H / 2, dx = ax - cx, dy = ay - cy, l = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
                dx /= l; dy /= l;
                float t = Math.min((W / 2 - 50) / Math.max(0.01f, Math.abs(dx)), (H / 2 - 60) / Math.max(0.01f, Math.abs(dy)));
                float px = cx + dx * t, py = cy + dy * t;
                sr.triangle(px + dx * 16, py + dy * 16, px - dy * 11, py + dx * 11, px + dy * 11, py - dx * 11);
            }
        }
        boolean lookout = g.lookT > 0;
        for (City.Enemy e : c.enemies) {
            if (!e.alive()) continue;
            if (!lookout && e.kind != City.K_BOSS) continue;
            if (!toScreen(e.x, e.y + 5, e.z, proj)) continue;
            sr.setColor(1, 0.15f, 0.1f, 0.9f);
            sr.triangle(proj.x - 9, proj.y + 14, proj.x + 9, proj.y + 14, proj.x, proj.y);
        }
        // speech bubble
        if (bubbleT > 0 && g.mode != City.M_DEAD) {
            float hy = g.mode == City.M_DRIVE ? g.y + 9 : g.y + 11;
            if (toScreen(g.x, hy, g.z, proj)) {
                layout.setText(f18, bubble, Color.BLACK, 340, Align.center, true);
                float bw = Math.min(360, layout.width + 30), bh = layout.height + 24;
                float bx = MathUtils.clamp(proj.x - bw / 2, 10, W - bw - 230), by = MathUtils.clamp(proj.y + 20, 120, H - bh - 110);
                bubbleBox = new float[]{bx, by, bw, bh};
                sr.setColor(1, 1, 1, Math.min(1, bubbleT * 3));
                sr.rect(bx, by, bw, bh);
                sr.triangle(proj.x - 10, by, proj.x + 10, by, MathUtils.clamp(proj.x, bx + 10, bx + bw - 10), by - 16);
            } else bubbleBox = null;
        } else bubbleBox = null;
        sr.end();

        sb.begin();
        text(f26, c.act >= 0 ? c.acts.get(c.act).name : c.missionActive ? City.MISSION_NAMES[c.mission] : (c.mission >= City.MISSIONS ? "Free Roam" : "Savanna City"), 30, H - 26, Color.GOLD, Align.left, 600);
        text(f18, c.objectiveText, 30, H - 62, Color.WHITE, Align.left, 600);
        if (c.missionActive && c.timed) {
            int t = (int) Math.ceil(c.missionTimer);
            text(f46, t / 60 + ":" + (t % 60 < 10 ? "0" : "") + t % 60, 0, H - 80, t <= 10 ? Color.SCARLET : Color.WHITE, Align.center, W);
        }
        if (c.missionActive && c.mission == 4 && c.boss != null && c.boss.alive())
            text(f18, "THE TAXIDERMIST" + (c.boss.shield ? "  (shielded)" : ""), 0, H - 70, Color.WHITE, Align.center, W);
        if (c.hasObjective) {
            float d = (float) Math.sqrt(City.dist2(c.ox, c.oz, g.x, g.z));
            boolean front = toScreen(c.ox, c.oy + 2, c.oz, proj);
            float mx = front ? proj.x : W - proj.x, my = front ? proj.y : 40;
            mx = MathUtils.clamp(mx, 40, W - 40); my = MathUtils.clamp(my, 110, H - 140);
            text(f18, (int) d + " m", mx - 50, my - 26, Color.GOLD, Align.center, 100);
        }
        text(f18, "HP", 30, 47, Color.WHITE, Align.left, 60);
        int afford = 0;
        for (int k = 0; k < Skills.COUNT; k++) if (!c.has(k) && Skills.unlocked(c.skills, k) && c.respect >= Skills.cost(k)) afford++;
        text(f18, "Respect " + c.respect + (afford > 0 ? "   K: skill ready!" : "   K: skills"), 20, 84, afford > 0 ? Color.GOLD : Color.WHITE, Align.left, 400);
        text(f18, "Golden leaves " + c.leavesFound() + "/" + c.leaves.size(), W - 236, H - 248, Color.GOLD, Align.left, 230);
        if (g.mode == City.M_DRIVE) text(f18, c.cars.get(g.car).burnT > 0 ? "ON FIRE - GET OUT (E)" : "Car", 248, 118, Color.WHITE, Align.left, 300);
        if (c.has(Skills.NECK_ZIP)) text(f18, g.zipCd > 0 ? "Zip..." : "Q  Neck Zip", 20, 150, g.zipCd > 0 ? Color.GRAY : new Color(0.4f, 1f, 0.5f, 1), Align.left, 200);
        // race clock
        if (c.act >= 0) {
            City.Activity a = c.acts.get(c.act);
            int m = c.medalFor(c.act, c.actTime);
            Color tc = m == 0 ? Color.GOLD : m == 1 ? Color.LIGHT_GRAY : m == 2 ? new Color(0.85f, 0.55f, 0.3f, 1) : Color.SCARLET;
            text(f46, time(c.actTime), 0, H - 80, tc, Align.center, W);
            float next = m == 0 ? a.gold : m == 1 ? a.silver : a.bronze;
            text(f18, (m <= 2 ? new String[]{"Gold pace", "Silver pace", "Bronze pace"}[m] + " (" + time(next) + ")" : "No medal pace"), 0, H - 136, tc, Align.center, W);
        }
        // sniper warning
        boolean painted = false, locked = false;
        for (City.Enemy e : c.enemies) if (e.kind == City.K_SNIPER && e.aimT > 0 && e.alive()) { painted = true; locked |= e.aimT >= City.SNIPER_LOCK; }
        if (painted) text(f26, locked ? "SNIPER LOCKED - MOVE!" : "SNIPER AIMING", 0, H / 2 + 120,
                locked ? Color.SCARLET : new Color(1, 0.4f, 0.4f, 0.8f + 0.2f * MathUtils.sin(clock * 12)), Align.center, W);
        px0 = W / 2 - (pw * City.POWERS + 10 * (City.POWERS - 1)) / 2;
        String[] say = {"\"bellow\"", "\"lick\"", "\"kick\"", "\"lookout\"", "\"munch\""};
        for (int i = 0; i < City.POWERS; i++) {
            float x = px0 + i * (pw + 10);
            boolean ready = g.cd[i] <= 0;
            text(f18, (i + 1) + "  " + City.POWER_NAMES[i], x, 74, ready ? Color.WHITE : Color.GRAY, Align.center, pw);
            f18.getData().setScale(0.75f);
            text(f18, say[i], x, 46, voice.listening ? Color.CYAN : Color.DARK_GRAY, Align.center, pw);
            f18.getData().setScale(1f);
        }
        if (bubbleBox != null) {
            f18.setColor(0.1f, 0.06f, 0.02f, Math.min(1, bubbleT * 3));
            f18.draw(sb, bubble, bubbleBox[0] + 15, bubbleBox[1] + bubbleBox[3] - 12, bubbleBox[2] - 30, Align.center, true);
            f18.setColor(Color.WHITE);
        }
        for (FloatText f : floats) {
            if (!toScreen(f.x, f.y, f.z, proj)) continue;
            Color col = new Color(f.c); col.a = Math.min(1, f.life * 2);
            if (f.small) text(f18, f.s, proj.x - 160, proj.y, col, Align.center, 320);
            else text(f26, f.s, proj.x - 100, proj.y, col, Align.center, 200);
        }
        if (bannerT > 0) {
            Color col = new Color(bannerColor); col.a = Math.min(1, bannerT * 2);
            text(f46, banner, 0, H * 0.68f, col, Align.center, W);
            Color w = new Color(1, 1, 1, col.a);
            text(f26, bannerSub, W * 0.15f, H * 0.68f - 64, w, Align.center, W * 0.7f);
        }
        // voice + hints
        String vs = voice.listening ? "Mic on" + (heardT > 0 ? ": heard \"" + lastHeard + "\"" : "") : voice.status;
        text(f18, vs, W - 520, 100, voice.listening ? Color.CYAN : Color.GRAY, Align.right, 500);
        if (playTime < 25 && !autoplay)
            text(f18, "Hold LEFT MOUSE to neck-swing to the GREEN marker   SHIFT reel in   SPACE jump   RIGHT MOUSE / F neck spin   E car   1-5 powers   K skills",
                    0, 130, new Color(1, 1, 1, Math.min(1, (25 - playTime) / 3)), Align.center, W);
        if (g.mode == City.M_SWING && g.climbing) text(f18, "climbing...", 0, H / 2 - 60, Color.WHITE, Align.center, W);
        if (!Gdx.input.isCursorCatched() && !autoplay && scr == Scr.PLAY) text(f18, "Click to grab the mouse", 0, H / 2, Color.WHITE, Align.center, W);
        sb.end();
        drawMinimapLines(W, H);
    }

    private float[] bubbleBox;
    private static final float MAP = 220;

    private float mapX(float W, float x) { return W - MAP - 16 + x / City.SIZE * MAP; }
    private float mapY(float H, float z) { return H - MAP - 16 + (1 - z / City.SIZE) * MAP; }

    private void drawMinimapShapes(float W, float H) {
        City c = city;
        sr.setColor(0.12f, 0.12f, 0.14f, 0.8f);
        sr.rect(W - MAP - 20, H - MAP - 20, MAP + 8, MAP + 8);
        float k = MAP / City.SIZE;
        for (int i = 0; i < City.BLOCKS; i++) for (int j = 0; j < City.BLOCKS; j++) {
            int t = c.lotType[i][j];
            if (t == City.LOT_CITY) continue;
            sr.setColor(t == City.LOT_PARK ? new Color(0.3f, 0.6f, 0.25f, 1) : new Color(0.85f, 0.7f, 0.4f, 1));
            float x0 = i * City.BLOCK + City.STREET / 2, z1 = (j + 1) * City.BLOCK - City.STREET / 2;
            sr.rect(mapX(W, x0), mapY(H, z1), City.LOT * k, City.LOT * k);
        }
        for (City.Building b : c.buildings) {
            float v = 0.35f + Math.min(0.4f, b.h / 250f);
            sr.setColor(v, v, v + 0.03f, 1);
            sr.rect(mapX(W, b.x0), mapY(H, b.z1), (b.x1 - b.x0) * k, (b.z1 - b.z0) * k);
        }
        for (City.Car car : c.cars) {
            sr.setColor(car.state == City.C_WRECK ? Color.DARK_GRAY : car.burnT > 0 ? Color.ORANGE : new Color(0.7f, 0.7f, 0.75f, 1));
            sr.rect(mapX(W, car.x) - 1, mapY(H, car.z) - 1, 2, 2);
        }
        for (City.Enemy e : c.enemies) {
            if (!e.alive()) continue;
            if (c.g.lookT <= 0 && City.dist2(e.x, e.z, c.g.x, c.g.z) > 130 * 130) continue;
            sr.setColor(1, 0.2f, 0.15f, 1); sr.circle(mapX(W, e.x), mapY(H, e.z), e.kind == City.K_BOSS ? 4.5f : 2.5f);
        }
        if (c.act < 0 && !c.missionActive) for (City.Activity a : c.acts) {
            sr.setColor(a.type == City.ACT_SWING ? new Color(0.3f, 0.65f, 1f, 1) : new Color(0.8f, 0.4f, 1f, 1));
            sr.rect(mapX(W, a.sx) - 4, mapY(H, a.sz) - 4, 8, 8);
        }
        boolean eagle = c.has(Skills.EAGLE_EYE);
        for (int i = 0; i < c.leaves.size(); i++) {
            if ((c.leafMask & (1 << i)) != 0) continue;
            City.Leaf l = c.leaves.get(i);
            if (!(eagle && c.g.lookT > 0) && City.dist2(l.x, l.z, c.g.x, c.g.z) > 60 * 60) continue;
            sr.setColor(1f, 0.85f, 0.15f, 1); sr.circle(mapX(W, l.x), mapY(H, l.z), 2.5f);
        }
        if (eagle) for (City.Enemy e : c.enemies) if (e.alive() && e.kind == City.K_SNIPER) {
            sr.setColor(1, 0.2f, 0.15f, 1); sr.rect(mapX(W, e.x) - 3, mapY(H, e.z) - 3, 6, 6);
        }
        if (c.hasObjective) { sr.setColor(1, 0.85f, 0.1f, 1); sr.circle(mapX(W, c.ox), mapY(H, c.oz), 5 + MathUtils.sin(clock * 6)); }
        sr.setColor(1, 1, 1, 1); sr.circle(mapX(W, c.g.x), mapY(H, c.g.z), 4);
    }

    private void drawMinimapLines(float W, float H) {
        City.Giraffe g = city.g;
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(1, 1, 1, 1);
        float x = mapX(W, g.x), y = mapY(H, g.z);
        sr.line(x, y, x + MathUtils.sin(g.yaw) * 10, y - MathUtils.cos(g.yaw) * 10);
        // camera view direction
        sr.setColor(1, 1, 1, 0.4f);
        sr.line(x, y, x + MathUtils.sin(camYaw + 0.5f) * 22, y - MathUtils.cos(camYaw + 0.5f) * 22);
        sr.line(x, y, x + MathUtils.sin(camYaw - 0.5f) * 22, y - MathUtils.cos(camYaw - 0.5f) * 22);
        sr.end();
    }

    @Override public void pause() { if (scr == Scr.PLAY && !autoplay) { scr = Scr.PAUSE; Gdx.input.setCursorCatched(false); } }

    @Override public void dispose() {
        voice.stop();
        if (save != null && city != null && scr != Scr.TITLE) { save.from(city); save.write(); }
        batch.dispose(); sb.dispose(); sr.dispose(); art.dispose(); sfx.dispose();
        f18.dispose(); f26.dispose(); f46.dispose(); f110.dispose();
    }
}
