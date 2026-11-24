package com.saedarm.neckswing;

import java.util.ArrayList;
import java.util.Random;

/**
 * All game rules. Plain Java, no libGDX. Fixed 60 Hz tick. Never draws or plays
 * sound: anything worth showing goes into {@link #events}.
 *
 * Coordinates: y is up, ground is y = 0. A yaw of 0 faces +z; dir(yaw) = (sin, cos).
 */
public final class City {

    // ---------------------------------------------------------------- feel
    public static final float DT = 1 / 60f;
    public static final float GRAVITY = 34f;
    public static final float RUN_SPEED = 18f, RUN_ACCEL = 70f, AIR_ACCEL = 18f, JUMP_V = 17f;
    public static final float NECK_MAX = 50f, NECK_MIN = 6f, REEL_RATE = 22f;
    public static final float SWING_PUMP = 24f, RELEASE_BOOST = 7f, MAX_SPEED = 78f;
    public static final float SPIN_TIME = 0.45f, SPIN_RADIUS = 7.5f, SPIN_COOLDOWN = 0.5f;
    public static final float SLAM_MIN_HEIGHT = 6f, SLAM_RADIUS = 13f, SLAM_DAMAGE = 70f;
    public static final float BODY_RADIUS = 1.4f, HEAD_HEIGHT = 7f;
    public static final float HP_MAX = 100f, REGEN = 5f, REGEN_DELAY = 4f;

    public static final float CAR_ACCEL = 30f, CAR_MAX = 46f, CAR_REVERSE = 14f, CAR_STEER = 1.9f, CAR_DRAG = 0.6f;
    public static final float AI_CAR_SPEED = 15f, CAR_RADIUS = 2.4f;

    public static final float BULLET_SPEED = 80f, BULLET_DAMAGE = 7f, PUNCH_DAMAGE = 14f;

    // powers: bellow, tongue, kick, lookout, munch
    public static final int P_BELLOW = 0, P_TONGUE = 1, P_KICK = 2, P_LOOK = 3, P_MUNCH = 4, POWERS = 5;
    public static final String[] POWER_NAMES = {"Bellow", "Tongue", "Kick", "Lookout", "Munch"};
    public static final float[] POWER_COOLDOWN = {8f, 2.5f, 1.2f, 15f, 4f};
    public static final float BELLOW_RADIUS = 24f, BELLOW_STUN = 4f;
    public static final float TONGUE_RANGE = 20f, KICK_RANGE = 5.5f, KICK_DAMAGE = 85f, LOOK_TIME = 8f, MUNCH_HEAL = 40f;

    // ---------------------------------------------------------------- world
    public static final int BLOCKS = 9;
    public static final float BLOCK = 72f, STREET = 22f, LOT = BLOCK - STREET;
    public static final float SIZE = BLOCKS * BLOCK;
    public static final int ZOO_I = 4, ZOO_J = 4;

    // ---------------------------------------------------------------- input
    public static final int B_JUMP = 1, B_SWING = 2, B_ATTACK = 4, B_ENTER = 8, B_BELLOW = 16, B_TONGUE = 32,
            B_KICK = 64, B_LOOK = 128, B_MUNCH = 256, B_HORN = 512, B_TALK = 1024, B_ZIP = 2048, B_CANCEL = 4096;
    public static final int[] POWER_BITS = {B_BELLOW, B_TONGUE, B_KICK, B_LOOK, B_MUNCH};

    public static final class Input {
        public float mx, mz;            // world-space move wish, length <= 1
        public float throttle, steer;   // driving, -1..1 (steer +1 = left)
        public float aimYaw;            // where the camera looks, used for swing aim when standing still
        public boolean swingHeld, reelHeld;
        public int presses;             // latched until the next tick
        public void press(int bits) { presses |= bits; }
        public boolean pressed(int bit) { return (presses & bit) != 0; }
    }

    // ---------------------------------------------------------------- events
    public static final int EV_SWING_ATTACH = 1, EV_SWING_RELEASE = 2, EV_JUMP = 3, EV_LAND = 4, EV_SPIN = 5,
            EV_HIT = 6, EV_KICK = 7, EV_SLAM = 8, EV_BELLOW = 9, EV_TONGUE = 10, EV_MUNCH = 11, EV_LOOK = 12,
            EV_SHOT = 13, EV_PLAYER_HURT = 14, EV_ENEMY_DOWN = 15, EV_CAR_ENTER = 16, EV_CAR_EXIT = 17,
            EV_CRASH = 18, EV_HORN = 19, EV_RING = 20, EV_MISSION_START = 21, EV_MISSION_DONE = 22,
            EV_MISSION_FAIL = 23, EV_PICKUP = 24, EV_DELIVER = 25, EV_DIED = 26, EV_RESPAWN = 27, EV_QUIP = 28,
            EV_NO_ANCHOR = 29, EV_SHIELD_BREAK = 30, EV_NOT_READY = 31, EV_CRIME = 32, EV_CAR_KICK = 33,
            EV_STEP = 34, EV_SPLAT = 35, EV_BANNER = 36, EV_ZIP = 37, EV_ALERT = 38, EV_SNIPER_AIM = 39,
            EV_SNIPER_SHOT = 40, EV_CAR_FIRE = 41, EV_EXPLODE = 42, EV_PED_HIT = 43, EV_PED_TALK = 44, EV_PED_SCREAM = 45,
            EV_ACT_START = 46, EV_CHECKPOINT = 47, EV_ACT_DONE = 48, EV_ACT_FAIL = 49, EV_LEAF = 50, EV_ALL_LEAVES = 51,
            EV_BULLET_CAR = 52, EV_CHARGE = 53;

    public static final class Event {
        public final int type, a;
        public final float x, y, z;
        Event(int type, int a, float x, float y, float z) { this.type = type; this.a = a; this.x = x; this.y = y; this.z = z; }
    }
    public final ArrayList<Event> events = new ArrayList<>();
    private void ev(int type, int a, float x, float y, float z) { events.add(new Event(type, a, x, y, z)); }

    // ---------------------------------------------------------------- quips
    public static final String[] QUIPS = {
        "HMMMMMMMMM!", "Giraffes hum at night. This is the loud version.", "Everybody freeze. Way down there.",
        "Eighteen inches of purple tongue, buddy.", "Get over here. Slurp.", "My tongue is purple and so is my justice.",
        "Lions have tried. Lions have failed.", "One kick. That's all a lion ever gets.", "Hoof, meet face.",
        "Heads up. Way up.", "I can see your house from here. And your crimes.", "Six meters of lookout tower, baby.",
        "Acacia? Don't mind if I do.", "Seventy five pounds of leaves a day. Don't judge me.", "Thorns? I have a leather tongue.",
        "No leaves here. Just concrete and sadness.", "Find me a tree. Or a rooftop garden.", "That's a parking garage. I can't eat that.",
        "Long neck, short temper.", "I sleep thirty minutes a day. I'm cranky.",
        "My heart weighs twenty five pounds and all of it belongs to this city.", "Who wants to meet the ossicones?",
        "Sunroof down. Neck up.", "I don't have a license. I have a neck.",
        "Another day, another neck.", "The savanna would be proud.",
        "Ow. That's a lot of neck to get shot.", "I'll be back. Taller.",
        "Nothing to wrap my neck around!",
        "Give me a second, I'm a giraffe.",
        "NECK SLAM!", "Gravity is on my side today.",
    };
    public static final int[][] Q_POWER = {{0, 3}, {3, 6}, {6, 9}, {9, 12}, {12, 15}};
    public static final int[] Q_MUNCH_FAIL = {15, 18}, Q_TALK = {18, 22}, Q_CAR = {22, 24}, Q_DONE = {24, 26},
            Q_HURT = {26, 27}, Q_DEAD = {27, 28}, Q_NO_ANCHOR = {28, 29}, Q_NOT_READY = {29, 30}, Q_SLAM = {30, 32};

    /** what pedestrians shout. 0-5 when the giraffe is nearby, 6-9 when there's a fight, 10-13 when hit. */
    public static final String[] PED_LINES = {
        "Is that a giraffe?!", "Nice neck!", "Mom, the giraffe is back!", "Can I get a selfie?",
        "He's taller in person.", "Does he have a license for that neck?",
        "Get 'em, giraffe!", "Run! Poachers!", "Not again!", "Somebody call the zoo!",
        "My hip!", "I'm suing the zoo!", "Watch the hooves!", "Hey! I was walking here!",
    };
    /** what poachers shout when they spot you */
    public static final String[] ALERT_LINES = {"There he is!", "Giraffe! Up high!", "Get the long-necked freak!", "Flank him!", "Cover me!"};

    // ---------------------------------------------------------------- entities
    public static final class Building {
        public float x0, z0, x1, z1, h; public int color; public boolean garden;
        public boolean contains(float x, float z, float r) { return x + r > x0 && x - r < x1 && z + r > z0 && z - r < z1; }
    }
    public static final class Tree { public float x, z, h, r; }

    public static final int M_FOOT = 0, M_AIR = 1, M_SWING = 2, M_DRIVE = 3, M_DEAD = 4;
    public static final class Giraffe {
        public float x, y, z, vx, vy, vz, yaw;
        public int mode = M_FOOT;
        public float hp = HP_MAX, hurtT, deadT, sinceHurt = 99;
        public float ax, ay, az, rope;           // neck anchor while swinging
        public float spinT, spinCd; public int spinId;
        public boolean slamming;
        public final float[] cd = new float[POWERS];
        public int car = -1;
        public boolean carrying;
        public float lookT, runPhase, airT, stepT, tongueT, kickT, munchT;
        public float tongueX, tongueY, tongueZ;
        public boolean climbing;
        public float zipT, zipCd;                // neck zip in progress / cooldown
        public boolean grabOk;                   // preview: where the neck would grab right now
        public float gx, gy, gz;
        public float speed() { return (float) Math.sqrt(vx * vx + vy * vy + vz * vz); }
        public float hSpeed() { return (float) Math.sqrt(vx * vx + vz * vz); }
    }

    public static final int K_GRUNT = 0, K_BRUTE = 1, K_BOSS = 2, K_SNIPER = 3;
    public static final int E_IDLE = 0, E_CHASE = 1, E_STUNNED = 2, E_FLYING = 3, E_DEAD = 4;
    public static final float SNIPER_AIM = 1.6f, SNIPER_LOCK = 1.25f, SNIPER_RANGE = 170f, SNIPER_DAMAGE = 22f;
    public static final class Enemy {
        public int kind, group, state, lastSpin = -1, id;
        public float x, y, z, vx, vy, vz, yaw, hp, maxHp;
        public float shootT, stunT, disarmT, deadT, walkPhase, punchT, flashT;
        public boolean shield; public float shieldT;
        public boolean los, onRoof; public float losT, strafe = 1, strafeT, burst, retreatT, chargeT, chargeCd;
        public float aimT, aimX, aimY, aimZ;   // sniper laser
        public boolean alive() { return state != E_DEAD; }
    }

    public static final class Bullet { public float x, y, z, vx, vy, vz, life, dmg; }

    public static final int C_AI = 0, C_PLAYER = 1, C_PARKED = 2, C_WRECK = 3;
    public static final float CAR_HP = 100f;
    public static final class Car {
        public float x, y, z, yaw, speed, vy, roll, rollV, crashT;
        public int color, state, dir;
        public boolean airborne;
        public float hp = CAR_HP, burnT, wreckT, hitT;
        public boolean usable() { return state != C_WRECK && burnT <= 0; }
    }

    public static final int PD_WALK = 0, PD_FLEE = 1, PD_DOWN = 2;
    public static final class Ped {
        public float x, y, z, yaw, vx, vy, vz, t, walkPhase, speed, talkT;
        public int xi, zi, txi, tzi, state, shirt, skin;
        public boolean hat;
        public float fx, fz;   // what they're running from
    }

    public static final int ACT_SWING = 0, ACT_DRIVE = 1;
    public static final class Activity {
        public int type; public String name;
        public float sx, sz;              // start beacon
        public float[][] pts;             // x, y, z, yaw per checkpoint
        public float gold, silver, bronze;
    }

    public static final class Leaf { public float x, y, z; }

    public static final class Ring { public float x, y, z, yaw; public boolean done; }

    public final ArrayList<Building> buildings = new ArrayList<>();
    public final ArrayList<Tree> trees = new ArrayList<>();
    public final ArrayList<Enemy> enemies = new ArrayList<>();
    public final ArrayList<Bullet> bullets = new ArrayList<>();
    public final ArrayList<Car> cars = new ArrayList<>();
    public final ArrayList<Ring> rings = new ArrayList<>();
    public final ArrayList<Ped> peds = new ArrayList<>();
    public final ArrayList<Activity> acts = new ArrayList<>();
    public final ArrayList<Leaf> leaves = new ArrayList<>();
    public final int[][] lotType = new int[BLOCKS][BLOCKS];
    public Nav nav;
    private float navT;
    public static final int LOT_CITY = 0, LOT_PARK = 1, LOT_ZOO = 2;
    public final Giraffe g = new Giraffe();

    // ---------------------------------------------------------------- missions
    public static final int MISSIONS = 5;
    public static final String[] MISSION_NAMES = {"Stretch Your Neck", "Poacher Problem", "Hot Wheels, Long Neck",
            "Leaf Delivery", "The Taxidermist"};
    public static final String[] MISSION_BRIEF = {
        "Swing through all 7 rings before the clock runs out.",
        "Poachers are in the park on the east side. Take them all out.",
        "Get in any car and drive to the marker before time runs out.",
        "Grab the acacia crate off the tallest roof and bring it to the zoo.",
        "The Taxidermist wants your head on his wall. Bellow to break his shield.",
    };
    public int mission;              // next mission to play (MISSIONS = all done, free roam)
    public boolean missionActive;
    public int stage;
    public float missionTimer;       // counts down when > 0 and timed
    public boolean timed;
    public float ox, oy, oz; public boolean hasObjective;   // current objective point
    public String objectiveText = "";
    public final float[][] missionStart = new float[MISSIONS][2];
    public float crateX, crateY, crateZ; public int crateState; // 0 waiting, 1 carried, 2 delivered
    public Enemy boss;
    public float crimeT = 30f; public int crimesCleared;
    public float crimeX, crimeZ; public boolean crimeActive;
    public int respect;
    public float time;
    private float quipT, pedTalkT;
    private int nextEnemyId;

    // ---------------------------------------------------------------- side activities + progress
    public boolean activitiesEnabled = true;     // Sim turns these off
    public int act = -1, actNext;                // running activity and its next checkpoint
    public float actTime;
    public float[] raceBest;                     // 0 = never finished
    public int leafMask;                         // which golden leaves are collected
    public int skills;                           // owned skill bitmask (Skills.java)

    public final Random rng;

    // ---------------------------------------------------------------- skill-driven stats
    public boolean has(int skill) { return Skills.has(skills, skill); }
    public float neckMax() { return NECK_MAX + (has(Skills.LONG_NECK) ? 10 : 0) + (has(Skills.LONGER_NECK) ? 10 : 0); }
    public float hpMax() { return HP_MAX + (has(Skills.THICK_HIDE) ? 25 : 0) + (has(Skills.THICKER_HIDE) ? 50 : 0); }
    public float runSpeed() { return RUN_SPEED * (has(Skills.LONG_LEGS) ? 1.2f : 1); }
    public float pump() { return SWING_PUMP * (has(Skills.SWING_PUMP) ? 1.3f : 1); }
    public float reelRate() { return REEL_RATE * (has(Skills.STRONG_REEL) ? 1.5f : 1); }
    public float regen() { return has(Skills.FAST_HEALER) ? REGEN * 2.5f : REGEN; }
    public float regenDelay() { return has(Skills.FAST_HEALER) ? 2f : REGEN_DELAY; }
    public float kickDamage() { return KICK_DAMAGE * (has(Skills.HARD_HOOVES) ? 1.5f : 1); }
    public float slamRadius() { return SLAM_RADIUS * (has(Skills.HARD_HOOVES) ? 1.3f : 1); }
    public float tongueRange() { return TONGUE_RANGE * (has(Skills.LONG_TONGUE) ? 1.5f : 1); }
    public float bellowRadius() { return BELLOW_RADIUS * (has(Skills.BIG_LUNGS) ? 1.4f : 1); }
    public float munchHeal() { return has(Skills.LEAF_STOMACH) ? 75 : MUNCH_HEAL; }
    public float lookTime() { return has(Skills.EAGLE_EYE) ? 14 : LOOK_TIME; }
    public float cooldown(int power) {
        float c = POWER_COOLDOWN[power];
        if (power == P_BELLOW && has(Skills.BIG_LUNGS)) c *= 0.75f;
        if (has(Skills.QUICK_REFLEXES)) c *= 0.8f;
        return c;
    }
    /** buy a skill with respect. Returns false if locked, owned or too expensive. */
    public boolean buy(int skill) {
        if (has(skill) || !Skills.unlocked(skills, skill) || respect < Skills.cost(skill)) return false;
        respect -= Skills.cost(skill);
        skills |= 1 << skill;
        if (skill == Skills.THICK_HIDE || skill == Skills.THICKER_HIDE) g.hp = hpMax();
        return true;
    }

    public City(long seed) {
        rng = new Random(seed);
        buildCity(new Random(1977));   // the city itself is always the same
        nav = new Nav(this);
        spawnCars(36);
        buildActivities();
        buildLeaves();
        raceBest = new float[acts.size()];
        spawnPeds(70);
        respawn();
        missionStart[0] = new float[]{4 * BLOCK, 5 * BLOCK};
        missionStart[1] = new float[]{6 * BLOCK, 3 * BLOCK};
        missionStart[2] = new float[]{3 * BLOCK, 6 * BLOCK};
        missionStart[3] = new float[]{5 * BLOCK, 6 * BLOCK};
        missionStart[4] = new float[]{4 * BLOCK, 3 * BLOCK};
        refreshObjective();
    }

    // ================================================================= city build
    private void buildCity(Random r) {
        for (int i = 0; i < BLOCKS; i++) for (int j = 0; j < BLOCKS; j++) lotType[i][j] = r.nextFloat() < 0.12f ? LOT_PARK : LOT_CITY;
        lotType[ZOO_I][ZOO_J] = LOT_ZOO;
        lotType[6][2] = LOT_PARK;   // mission 2 park
        lotType[2][6] = LOT_PARK;   // boss park
        lotType[3][4] = LOT_CITY; lotType[5][4] = LOT_CITY; lotType[4][5] = LOT_CITY; lotType[4][3] = LOT_CITY;
        float centre = SIZE / 2f;
        for (int i = 0; i < BLOCKS; i++) for (int j = 0; j < BLOCKS; j++) {
            float lx = i * BLOCK + STREET / 2, lz = j * BLOCK + STREET / 2;
            float cx = lx + LOT / 2, cz = lz + LOT / 2;
            float d = (float) Math.hypot(cx - centre, cz - centre) / (SIZE * 0.7f);
            float tall = Math.max(0.35f, 1.25f - d);
            if (lotType[i][j] == LOT_PARK) {
                int n = 5 + r.nextInt(4);
                for (int k = 0; k < n; k++) {
                    Tree t = new Tree();
                    t.x = lx + 6 + r.nextFloat() * (LOT - 12); t.z = lz + 6 + r.nextFloat() * (LOT - 12);
                    t.h = 8f + r.nextFloat() * 3f; t.r = 3.5f + r.nextFloat() * 1.5f;
                    trees.add(t);
                }
                continue;
            }
            if (lotType[i][j] == LOT_ZOO) {
                for (int k = 0; k < 4; k++) {
                    Tree t = new Tree();
                    t.x = lx + 8 + (k % 2) * (LOT - 16); t.z = lz + 8 + (k / 2) * (LOT - 16);
                    t.h = 9.5f; t.r = 4.5f; trees.add(t);
                }
                continue;
            }
            float m = 3f;
            int split = r.nextFloat() < 0.45f ? 1 : (r.nextFloat() < 0.5f ? 2 : 4);
            if (split == 1) {
                addBuilding(r, lx + m, lz + m, lx + LOT - m, lz + LOT - m, (30 + r.nextFloat() * 70) * tall);
            } else if (split == 2) {
                boolean alongX = r.nextBoolean();
                float half = LOT / 2f;
                for (int k = 0; k < 2; k++) {
                    if (alongX) addBuilding(r, lx + m + k * half, lz + m, lx + (k + 1) * half - m, lz + LOT - m, (20 + r.nextFloat() * 60) * tall);
                    else addBuilding(r, lx + m, lz + m + k * half, lx + LOT - m, lz + (k + 1) * half - m, (20 + r.nextFloat() * 60) * tall);
                }
            } else {
                float half = LOT / 2f;
                for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++)
                    addBuilding(r, lx + m + a * half, lz + m + b * half, lx + (a + 1) * half - m, lz + (b + 1) * half - m, (15 + r.nextFloat() * 45) * tall);
            }
        }
    }

    private void addBuilding(Random r, float x0, float z0, float x1, float z1, float h) {
        Building b = new Building();
        b.x0 = x0; b.z0 = z0; b.x1 = x1; b.z1 = z1; b.h = Math.max(12f, h);
        b.color = r.nextInt(6); b.garden = r.nextFloat() < 0.18f;
        buildings.add(b);
    }

    private void spawnCars(int n) {
        for (int k = 0; k < n; k++) {
            Car c = new Car();
            c.color = rng.nextInt(6);
            c.dir = rng.nextInt(4);
            int line = 1 + rng.nextInt(BLOCKS - 1);
            float along = BLOCK * (0.3f + rng.nextFloat() * (BLOCKS - 0.6f));
            placeOnLane(c, line * BLOCK, along);
            c.state = rng.nextFloat() < 0.25f ? C_PARKED : C_AI;
            c.speed = c.state == C_AI ? AI_CAR_SPEED : 0;
            cars.add(c);
        }
    }

    /** Put a car on its lane: street at 'line' (the fixed coordinate), 'along' the travel axis. */
    private void placeOnLane(Car c, float line, float along) {
        float lane = 5f;
        switch (c.dir) {
            case 0: c.x = along; c.z = line + lane; c.yaw = (float) Math.PI / 2; break;   // +x, right side is +z
            case 1: c.z = along; c.x = line - lane; c.yaw = 0; break;                    // +z, right side is -x
            case 2: c.x = along; c.z = line - lane; c.yaw = -(float) Math.PI / 2; break; // -x
            default: c.z = along; c.x = line + lane; c.yaw = (float) Math.PI; break;     // -z
        }
    }

    // ================================================================= side activities, leaves, peds
    private static final int[][] SWING_STARTS = {{2, 2}, {6, 7}, {7, 2}};
    private static final int[][] DRIVE_STARTS = {{1, 7}, {7, 5}};
    private static final String[] SWING_NAMES = {"Rooftop Rush", "Downtown Dash", "Skyline Sprint"};
    private static final String[] DRIVE_NAMES = {"Savanna 500", "Rush Hour Run"};

    private void buildActivities() {
        for (int k = 0; k < SWING_STARTS.length; k++) acts.add(route(ACT_SWING, SWING_NAMES[k], SWING_STARTS[k], 10, new Random(500 + k)));
        for (int k = 0; k < DRIVE_STARTS.length; k++) acts.add(route(ACT_DRIVE, DRIVE_NAMES[k], DRIVE_STARTS[k], 9, new Random(700 + k)));
    }

    /** a random walk over the street intersections, no U-turns */
    private Activity route(int type, String name, int[] start, int steps, Random r) {
        Activity a = new Activity();
        a.type = type; a.name = name;
        a.sx = start[0] * BLOCK; a.sz = start[1] * BLOCK;
        int ci = start[0], cj = start[1], pdi = 0, pdj = 0;
        a.pts = new float[steps][];
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int s = 0; s < steps; s++) {
            int[] pick = null;
            for (int tries = 0; tries < 20 && pick == null; tries++) {
                int[] d = dirs[r.nextInt(4)];
                int ni = ci + d[0], nj = cj + d[1];
                if (ni < 1 || nj < 1 || ni > BLOCKS - 1 || nj > BLOCKS - 1) continue;
                if (d[0] == -pdi && d[1] == -pdj && (pdi != 0 || pdj != 0)) continue;
                pick = d;
            }
            if (pick == null) pick = new int[]{-pdi, -pdj};
            int ni = ci + pick[0], nj = cj + pick[1];
            float yaw = (float) Math.atan2(pick[0], pick[1]);
            if (type == ACT_SWING) a.pts[s] = new float[]{(ci + ni) * BLOCK / 2f, 22 + r.nextFloat() * 16, (cj + nj) * BLOCK / 2f, yaw};
            else a.pts[s] = new float[]{ni * BLOCK, 0, nj * BLOCK, yaw};
            ci = ni; cj = nj; pdi = pick[0]; pdj = pick[1];
        }
        float len = steps * BLOCK + (type == ACT_SWING ? BLOCK / 2 : 0);
        float fast = type == ACT_SWING ? 24 : 27;
        a.gold = Math.round(len / fast); a.silver = Math.round(len / (fast * 0.75f)); a.bronze = Math.round(len / (fast * 0.52f));
        return a;
    }

    public static final int LEAF_COUNT = 25;
    private void buildLeaves() {
        Random r = new Random(42);
        ArrayList<Building> tall = new ArrayList<>();
        for (Building b : buildings) if (b.h > 22) tall.add(b);
        while (leaves.size() < 18 && !tall.isEmpty()) {
            Building b = tall.remove(r.nextInt(tall.size()));
            Leaf l = new Leaf();
            l.x = b.x0 + 3 + r.nextFloat() * (b.x1 - b.x0 - 6); l.z = b.z0 + 3 + r.nextFloat() * (b.z1 - b.z0 - 6); l.y = b.h + 1.6f;
            leaves.add(l);
        }
        ArrayList<Tree> ts = new ArrayList<>(trees);
        while (leaves.size() < LEAF_COUNT && !ts.isEmpty()) {
            Tree t = ts.remove(r.nextInt(ts.size()));
            Leaf l = new Leaf(); l.x = t.x; l.z = t.z; l.y = t.h + 2.2f;
            leaves.add(l);
        }
    }
    public int leavesFound() { return Integer.bitCount(leafMask); }

    /** pedestrians walk a lattice of sidewalk corners; odd steps along a block, even steps across a crosswalk */
    public static float lat(int i) {
        int b = i / 2;
        return (i % 2 == 0) ? b * BLOCK + STREET / 2 - 1.5f : (b + 1) * BLOCK - STREET / 2 + 1.5f;
    }
    private static final int LAT = BLOCKS * 2;

    private void spawnPeds(int n) {
        for (int k = 0; k < n; k++) {
            Ped p = new Ped();
            p.xi = rng.nextInt(LAT); p.zi = rng.nextInt(LAT);
            p.x = lat(p.xi); p.z = lat(p.zi);
            p.shirt = rng.nextInt(6); p.skin = rng.nextInt(3); p.hat = rng.nextFloat() < 0.3f;
            p.speed = 2f + rng.nextFloat() * 1.2f;
            pickPedTarget(p, -1, -1);
            // start part way down the first leg so they don't all bunch at corners
            float f = rng.nextFloat();
            p.x += (lat(p.txi) - p.x) * f; p.z += (lat(p.tzi) - p.z) * f;
            peds.add(p);
        }
    }

    private void pickPedTarget(Ped p, int fromXi, int fromZi) {
        int[][] opts = new int[4][]; int n = 0;
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] o : d) {
            int a = p.xi + o[0], b = p.zi + o[1];
            if (a < 0 || b < 0 || a >= LAT || b >= LAT) continue;
            if (a == fromXi && b == fromZi) continue;
            opts[n++] = new int[]{a, b};
        }
        if (n == 0) { p.txi = fromXi; p.tzi = fromZi; return; }
        int[] o = opts[rng.nextInt(n)];
        p.txi = o[0]; p.tzi = o[1];
    }

    /** true if a straight line between the two points passes through a building */
    public boolean segBlocked(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        for (Building b : buildings) {
            if (Math.min(ay, by) >= b.h) continue;
            if (Math.max(ax, bx) < b.x0 || Math.min(ax, bx) > b.x1 || Math.max(az, bz) < b.z0 || Math.min(az, bz) > b.z1) continue;
            float t0 = 0, t1 = 1;
            float[] lo = {b.x0, 0, b.z0}, hi = {b.x1, b.h, b.z1}, o = {ax, ay, az}, v = {dx, dy, dz};
            boolean miss = false;
            for (int k = 0; k < 3 && !miss; k++) {
                if (Math.abs(v[k]) < 1e-6f) { if (o[k] < lo[k] || o[k] > hi[k]) miss = true; continue; }
                float ta = (lo[k] - o[k]) / v[k], tb = (hi[k] - o[k]) / v[k];
                if (ta > tb) { float t = ta; ta = tb; tb = t; }
                t0 = Math.max(t0, ta); t1 = Math.min(t1, tb);
                if (t0 > t1) miss = true;
            }
            if (!miss) return true;
        }
        return false;
    }

    // ================================================================= queries
    public float groundAt(float x, float z, float r) {
        float best = 0;
        for (Building b : buildings) if (b.h > best && b.contains(x, z, -r * 0.3f)) best = b.h;
        return best;
    }
    public Building buildingAt(float x, float z) {
        for (Building b : buildings) if (b.contains(x, z, 0)) return b;
        return null;
    }
    public boolean insideBuilding(float x, float y, float z) {
        for (Building b : buildings) if (y < b.h && b.contains(x, z, 0)) return true;
        return false;
    }
    public static float dist2(float ax, float az, float bx, float bz) { float dx = ax - bx, dz = az - bz; return dx * dx + dz * dz; }
    public static float dist3(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = ax - bx, dy = ay - by, dz = az - bz; return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public float zooX() { return (ZOO_I + 0.5f) * BLOCK; }
    public float zooZ() { return (ZOO_J + 0.5f) * BLOCK; }
    public int aliveIn(int group) { int n = 0; for (Enemy e : enemies) if (e.alive() && e.group == group) n++; return n; }

    /** Push a body (cylinder radius r, from y up) out of every building. Returns true if it landed on a roof. */
    private boolean wallHit;
    private boolean resolve(float[] p, float[] v, float r, float prevY) {
        boolean landed = false;
        wallHit = false;
        for (Building b : buildings) {
            if (p[1] >= b.h || !b.contains(p[0], p[2], r)) continue;
            if (prevY >= b.h - 0.6f) { p[1] = b.h; if (v[1] < 0) v[1] = 0; landed = true; continue; }
            float pl = (p[0] + r) - b.x0, pr = b.x1 - (p[0] - r), pn = (p[2] + r) - b.z0, pf = b.z1 - (p[2] - r);
            float min = Math.min(Math.min(pl, pr), Math.min(pn, pf));
            wallHit = true;
            if (min == pl) { p[0] -= pl; if (v[0] > 0) v[0] = 0; }
            else if (min == pr) { p[0] += pr; if (v[0] < 0) v[0] = 0; }
            else if (min == pn) { p[2] -= pn; if (v[2] > 0) v[2] = 0; }
            else { p[2] += pf; if (v[2] < 0) v[2] = 0; }
        }
        return landed;
    }
    private final float[] tp = new float[3], tv = new float[3];

    // ================================================================= tick
    public void tick(Input in) {
        time += DT;
        quipT -= DT;
        Giraffe p = g;
        for (int i = 0; i < POWERS; i++) p.cd[i] = Math.max(0, p.cd[i] - DT);
        p.spinCd -= DT; p.lookT -= DT; p.tongueT -= DT; p.kickT -= DT; p.munchT -= DT; p.hurtT -= DT; p.zipCd -= DT;
        p.sinceHurt += DT;

        if (p.mode == M_DEAD) {
            p.deadT -= DT;
            p.grabOk = false;
            if (p.deadT <= 0) { respawn(); ev(EV_RESPAWN, 0, p.x, p.y, p.z); }
        } else {
            if (p.sinceHurt > regenDelay()) p.hp = Math.min(hpMax(), p.hp + regen() * DT);
            if (p.mode == M_DRIVE) tickDriving(in); else tickGiraffe(in);
            // show where the neck would grab (also used by Neck Zip)
            if (p.mode == M_SWING) { p.grabOk = true; p.gx = p.ax; p.gy = p.ay; p.gz = p.az; }
            else p.grabOk = anchorFor(in) && p.mode != M_DEAD;
            if (p.mode != M_DEAD) tickPowers(in);
            if (in.pressed(B_TALK)) quip(Q_TALK, true);
            if (in.pressed(B_HORN) && p.mode == M_DRIVE) { ev(EV_HORN, 0, p.x, p.y, p.z); scare(p.x, p.z, 18, false); }
        }
        navT -= DT;
        if (navT <= 0) { navT = 0.4f; nav.build(p.x, p.z); }
        tickCars();
        tickEnemies();
        tickBullets();
        tickPeds();
        tickLeaves();
        tickMission();
        tickActivity(in);
        in.presses = 0;
    }

    // ----------------------------------------------------------------- giraffe
    private void tickGiraffe(Input in) {
        Giraffe p = g;
        float prevY = p.y;
        boolean grounded = p.mode == M_FOOT;

        // enter car
        if (in.pressed(B_ENTER)) {
            int best = -1; float bd = 6.5f * 6.5f;
            for (int i = 0; i < cars.size(); i++) {
                Car c = cars.get(i); if (c.airborne || !c.usable()) continue;
                float d = dist2(c.x, c.z, p.x, p.z);
                if (d < bd && Math.abs(c.y - p.y) < 4) { bd = d; best = i; }
            }
            if (best >= 0) {
                Car c = cars.get(best);
                c.state = C_PLAYER; p.car = best; p.mode = M_DRIVE; p.vx = p.vy = p.vz = 0;
                ev(EV_CAR_ENTER, best, c.x, c.y, c.z);
                quip(Q_CAR, false);
                return;
            }
        }

        // neck zip: fly straight at the grab point
        if (in.pressed(B_ZIP) && has(Skills.NECK_ZIP) && p.zipCd <= 0) {
            if (anchorFor(in)) {
                p.ax = p.gx; p.ay = p.gy; p.az = p.gz;
                p.zipT = 1.2f; p.zipCd = 1.2f; p.mode = M_AIR; p.slamming = false;
                ev(EV_ZIP, 0, p.ax, p.ay, p.az);
            } else ev(EV_NO_ANCHOR, 0, p.x, p.y, p.z);
        }
        if (p.zipT > 0) {
            p.zipT -= DT;
            float dx = p.ax - p.x, dy = p.ay + 0.5f - p.y, dz = p.az - p.z, d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < 3f || p.zipT <= 0 || in.pressed(B_JUMP)) {
                p.zipT = 0; p.vx *= 0.5f; p.vz *= 0.5f; p.vy = 12f;   // pop up over the ledge
            } else { p.vx = dx / d * 62f; p.vy = dy / d * 62f; p.vz = dz / d * 62f; }
        }

        // swing attach / release
        if (in.pressed(B_SWING) && p.mode != M_SWING) {
            if (findAnchor(in)) {
                p.zipT = 0;
                if (p.mode == M_FOOT) p.vy = JUMP_V * 0.7f;
                p.mode = M_SWING; p.slamming = false;
                ev(EV_SWING_ATTACH, 0, p.ax, p.ay, p.az);
            } else {
                ev(EV_NO_ANCHOR, 0, p.x, p.y, p.z);
                quip(Q_NO_ANCHOR, false);
            }
        }
        if (p.mode == M_SWING && !in.swingHeld) {
            float s = p.speed();
            if (s > 1) { p.vx += p.vx / s * RELEASE_BOOST * 0.5f; p.vz += p.vz / s * RELEASE_BOOST * 0.5f; }
            p.vy += RELEASE_BOOST;
            p.mode = M_AIR;
            ev(EV_SWING_RELEASE, 0, p.x, p.y, p.z);
        }

        // jump
        if (in.pressed(B_JUMP) && grounded) {
            p.vy = JUMP_V; p.mode = M_AIR; grounded = false;
            ev(EV_JUMP, 0, p.x, p.y, p.z);
        }

        // attack: spin on the ground, slam from high up
        if (in.pressed(B_ATTACK)) {
            float above = p.y - groundAt(p.x, p.z, BODY_RADIUS);
            if (p.mode == M_AIR && above > SLAM_MIN_HEIGHT && !p.slamming) {
                p.slamming = true; p.vy = -60f; p.vx *= 0.3f; p.vz *= 0.3f;
                quip(Q_SLAM, false);
            } else if (p.spinCd <= 0) {
                p.spinT = SPIN_TIME; p.spinCd = SPIN_COOLDOWN; p.spinId++;
                ev(EV_SPIN, 0, p.x, p.y, p.z);
            }
        }

        // movement
        float mx = in.mx, mz = in.mz;
        float ml = (float) Math.sqrt(mx * mx + mz * mz);
        if (ml > 1) { mx /= ml; mz /= ml; ml = 1; }
        float speedMul = p.carrying ? 0.8f : 1f;
        if (p.zipT > 0) {
            // velocity was set above; no gravity while zipping
        } else if (p.mode == M_FOOT) {
            float tx = mx * runSpeed() * speedMul, tz = mz * runSpeed() * speedMul;
            float dx = tx - p.vx, dz = tz - p.vz, dl = (float) Math.sqrt(dx * dx + dz * dz);
            float step = RUN_ACCEL * DT;
            if (dl > step) { dx = dx / dl * step; dz = dz / dl * step; }
            p.vx += dx; p.vz += dz;
            p.vy -= GRAVITY * DT;
        } else {
            float acc = p.mode == M_SWING ? pump() : AIR_ACCEL;
            p.vx += mx * acc * DT; p.vz += mz * acc * DT;
            p.vy -= GRAVITY * DT;
            if (p.mode == M_AIR) { p.vx *= 1 - 0.15f * DT; p.vz *= 1 - 0.15f * DT; }
        }
        float s = p.speed();
        if (s > MAX_SPEED) { p.vx *= MAX_SPEED / s; p.vy *= MAX_SPEED / s; p.vz *= MAX_SPEED / s; }

        p.x += p.vx * DT; p.y += p.vy * DT; p.z += p.vz * DT;

        // neck constraint
        if (p.mode == M_SWING) {
            if (in.reelHeld) p.rope = Math.max(NECK_MIN, p.rope - reelRate() * DT);
            float rx = p.x - p.ax, ry = (p.y + HEAD_HEIGHT * 0.6f) - p.ay, rz = p.z - p.az;
            float d = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
            if (d > p.rope && d > 0.01f) {
                rx /= d; ry /= d; rz /= d;
                p.x = p.ax + rx * p.rope; p.y = p.ay + ry * p.rope - HEAD_HEIGHT * 0.6f; p.z = p.az + rz * p.rope;
                float vr = p.vx * rx + p.vy * ry + p.vz * rz;
                if (vr > 0) { p.vx -= rx * vr; p.vy -= ry * vr; p.vz -= rz * vr; }
            }
        }

        // world collision
        tp[0] = p.x; tp[1] = p.y; tp[2] = p.z; tv[0] = p.vx; tv[1] = p.vy; tv[2] = p.vz;
        boolean roof = resolve(tp, tv, BODY_RADIUS, prevY);
        p.x = tp[0]; p.y = tp[1]; p.z = tp[2]; p.vx = tv[0]; p.vy = tv[1]; p.vz = tv[2];
        // neck pulling us into a wall: climb it instead of getting pinned
        if (p.mode == M_SWING && wallHit && p.y + HEAD_HEIGHT * 0.6f < p.ay + 2) {
            p.vy = Math.max(p.vy, 16f);
            p.rope = Math.max(NECK_MIN, Math.min(p.rope, dist3(p.x, p.y + HEAD_HEIGHT * 0.6f, p.z, p.ax, p.ay, p.az)));
            p.climbing = true;
        } else p.climbing = false;
        p.x = clamp(p.x, 2, SIZE - 2); p.z = clamp(p.z, 2, SIZE - 2);
        boolean onGround = roof;
        if (p.y <= 0) { p.y = 0; if (p.vy < 0) p.vy = 0; onGround = true; }

        if (onGround && p.vy <= 0.01f) {
            if (p.mode == M_AIR) {
                ev(EV_LAND, (int) Math.min(99, -Math.min(0, p.vy) + p.airT * 10), p.x, p.y, p.z);
                if (p.slamming) doSlam();
                p.mode = M_FOOT;
            } else if (p.mode == M_SWING && p.y < p.ay - 4) {
                // dragging along the ground on a long neck: let go
                p.mode = M_FOOT;
                ev(EV_SWING_RELEASE, 0, p.x, p.y, p.z);
            }
            p.airT = 0;
        } else if (p.mode == M_FOOT) {
            // walked off a roof
            p.mode = M_AIR;
        }
        if (p.mode != M_FOOT) p.airT += DT;

        // facing follows velocity
        float hs = p.hSpeed();
        if (hs > 1.5f) p.yaw = lerpAngle(p.yaw, (float) Math.atan2(p.vx, p.vz), Math.min(1, 10 * DT));
        else if (ml > 0.1f) p.yaw = lerpAngle(p.yaw, (float) Math.atan2(mx, mz), Math.min(1, 10 * DT));
        if (p.mode == M_FOOT) {
            p.runPhase += hs * DT * 0.55f;
            p.stepT -= hs * DT;
            if (p.stepT <= 0 && hs > 3) { p.stepT = 4.5f; ev(EV_STEP, 0, p.x, p.y, p.z); }
        }

        // spin attack
        if (p.spinT > 0) {
            p.spinT -= DT;
            float bonus = hs * 1.6f;
            for (Enemy e : enemies) {
                if (!e.alive() || e.lastSpin == p.spinId) continue;
                if (dist2(e.x, e.z, p.x, p.z) < SPIN_RADIUS * SPIN_RADIUS && Math.abs(e.y - p.y) < 6) {
                    e.lastSpin = p.spinId;
                    hurtEnemy(e, 32 + bonus, p.x, p.z, 16 + hs * 0.6f, 8, 0);
                }
            }
            hitPeds(p.x, p.y, p.z, SPIN_RADIUS, 12, true);
        }

        // swinging body-check: plough through enemies at speed
        if (p.mode == M_SWING || p.mode == M_AIR) {
            float sp = p.speed();
            if (sp > 16) for (Enemy e : enemies) {
                if (!e.alive() || e.state == E_FLYING) continue;
                if (dist3(e.x, e.y + 1.5f, e.z, p.x, p.y + 2, p.z) < 4.2f) hurtEnemy(e, sp * 1.8f, p.x, p.z, sp * 0.7f, 10, 0);
            }
            if (sp > 16) hitPeds(p.x, p.y, p.z, 3.5f, sp * 0.6f, true);
        }

        // carrying the crate / grabbing it by touch
        if (mission == 3 && missionActive && crateState == 0 && dist3(p.x, p.y + 2, p.z, crateX, crateY, crateZ) < 4.5f) grabCrate();
    }

    private boolean findAnchor(Input in) {
        Giraffe p = g;
        if (!anchorFor(in)) return false;
        p.ax = p.gx; p.ay = p.gy; p.az = p.gz;
        p.rope = dist3(p.ax, p.ay, p.az, p.x, p.y + HEAD_HEIGHT * 0.6f, p.z) * 0.92f;
        return true;
    }

    /** where the neck would grab right now; result in g.gx/gy/gz. Same rule the swing uses, so the marker never lies. */
    public boolean anchorFor(Input in) {
        Giraffe p = g;
        float reach = neckMax();
        float fx, fz;
        float hs = p.hSpeed();
        if (hs > 4) { fx = p.vx / hs; fz = p.vz / hs; }
        else if (in.mx * in.mx + in.mz * in.mz > 0.04f) { float l = (float) Math.hypot(in.mx, in.mz); fx = in.mx / l; fz = in.mz / l; }
        else { fx = (float) Math.sin(in.aimYaw); fz = (float) Math.cos(in.aimYaw); }
        float dx = p.x + fx * 26, dy = p.y + 24, dz = p.z + fz * 26;
        float best = Float.MAX_VALUE, bx = 0, by = 0, bz = 0;
        for (Building b : buildings) {
            if (b.h < p.y + 5) continue;
            float qx = clamp(dx, b.x0, b.x1), qz = clamp(dz, b.z0, b.z1);
            float qy = Math.min(b.h, Math.max(p.y + 9, dy));
            float d = dist3(qx, qy, qz, p.x, p.y + HEAD_HEIGHT * 0.6f, p.z);
            if (d > reach || d < NECK_MIN) continue;
            // prefer points ahead of us
            float ahead = (qx - p.x) * fx + (qz - p.z) * fz;
            float score = dist3(qx, qy, qz, dx, dy, dz) - ahead * 0.3f;
            if (score < best) { best = score; bx = qx; by = qy; bz = qz; }
        }
        for (Tree t : trees) {
            float ty = t.h - 0.5f;
            if (ty < p.y + 3) continue;
            float d = dist3(t.x, ty, t.z, p.x, p.y + HEAD_HEIGHT * 0.6f, p.z);
            if (d > reach * 0.6f || d < NECK_MIN) continue;
            float score = dist3(t.x, ty, t.z, dx, dy, dz) + 6;
            if (score < best) { best = score; bx = t.x; by = ty; bz = t.z; }
        }
        if (best == Float.MAX_VALUE) return false;
        p.gx = bx; p.gy = by; p.gz = bz;
        return true;
    }

    private void doSlam() {
        Giraffe p = g;
        p.slamming = false;
        ev(EV_SLAM, 0, p.x, p.y, p.z);
        float R = slamRadius();
        for (Enemy e : enemies) {
            if (!e.alive()) continue;
            float d = (float) Math.sqrt(dist2(e.x, e.z, p.x, p.z));
            if (d < R && Math.abs(e.y - p.y) < 5) hurtEnemy(e, SLAM_DAMAGE * (1 - d / R * 0.5f), p.x, p.z, 14, 16, 0);
        }
        for (Car c : cars) if (c.state != C_PLAYER && c.state != C_WRECK && dist2(c.x, c.z, p.x, p.z) < R * R && Math.abs(c.y - p.y) < 4) {
            launchCar(c, p.x, p.z, 10); damageCar(c, 35);
        }
        hitPeds(p.x, p.y, p.z, R * 0.6f, 10, true);
        scare(p.x, p.z, 30, true);
    }

    // ----------------------------------------------------------------- powers
    private void tickPowers(Input in) {
        Giraffe p = g;
        for (int i = 0; i < POWERS; i++) {
            if (!in.pressed(POWER_BITS[i])) continue;
            if (p.mode == M_DRIVE && i != P_BELLOW && i != P_LOOK) continue;
            if (p.cd[i] > 0) { ev(EV_NOT_READY, i, p.x, p.y, p.z); quip(Q_NOT_READY, false); continue; }
            if (usePower(i)) { p.cd[i] = cooldown(i); quip(Q_POWER[i], true); }
        }
    }

    private boolean usePower(int i) {
        Giraffe p = g;
        float fx = (float) Math.sin(p.yaw), fz = (float) Math.cos(p.yaw);
        switch (i) {
            case P_BELLOW: {
                ev(EV_BELLOW, 0, p.x, p.y, p.z);
                for (Enemy e : enemies) {
                    if (!e.alive()) continue;
                    float d = dist3(e.x, e.y, e.z, p.x, p.y, p.z);
                    if (d > bellowRadius()) continue;
                    if (e.shield) { e.shield = false; e.shieldT = 5f; ev(EV_SHIELD_BREAK, e.id, e.x, e.y, e.z); }
                    e.stunT = BELLOW_STUN * (e.kind == K_BOSS ? 0.6f : 1f);
                    e.aimT = 0;
                    if (e.state != E_FLYING) e.state = E_STUNNED;
                    hurtEnemy(e, 8, p.x, p.z, 4, 3, 1);
                }
                for (Bullet b : bullets) b.life = 0;
                scare(p.x, p.z, bellowRadius() * 1.3f, true);
                return true;
            }
            case P_TONGUE: {
                // crate first
                if (mission == 3 && missionActive && crateState == 0 &&
                        dist3(crateX, crateY, crateZ, p.x, p.y + HEAD_HEIGHT, p.z) < tongueRange()) {
                    p.tongueT = 0.35f; p.tongueX = crateX; p.tongueY = crateY; p.tongueZ = crateZ;
                    ev(EV_TONGUE, 1, crateX, crateY, crateZ);
                    grabCrate();
                    return true;
                }
                Enemy best = null; float bd = tongueRange();
                for (Enemy e : enemies) {
                    if (!e.alive()) continue;
                    float dx = e.x - p.x, dz = e.z - p.z;
                    float d = dist3(e.x, e.y, e.z, p.x, p.y, p.z);
                    float ahead = (dx * fx + dz * fz) / Math.max(0.01f, (float) Math.sqrt(dx * dx + dz * dz));
                    if (d < bd && ahead > 0.2f) { bd = d; best = e; }
                }
                if (best == null) { // lick nothing: still a tongue flick
                    p.tongueT = 0.3f; p.tongueX = p.x + fx * 8; p.tongueY = p.y + HEAD_HEIGHT - 1; p.tongueZ = p.z + fz * 8;
                    ev(EV_TONGUE, 0, p.x, p.y, p.z);
                    return true;
                }
                p.tongueT = 0.35f; p.tongueX = best.x; p.tongueY = best.y + 2; p.tongueZ = best.z;
                ev(EV_TONGUE, 2, best.x, best.y, best.z);
                // yank toward us
                float dx = p.x - best.x, dz = p.z - best.z, l = Math.max(0.1f, (float) Math.sqrt(dx * dx + dz * dz));
                best.disarmT = 5f;
                hurtEnemy(best, 15, best.x - dx / l, best.z - dz / l, 0, 0, 1);
                best.vx = dx / l * Math.min(30, l * 2.2f); best.vz = dz / l * Math.min(30, l * 2.2f); best.vy = 9;
                best.state = E_FLYING;
                return true;
            }
            case P_KICK: {
                p.kickT = 0.3f;
                ev(EV_KICK, 0, p.x, p.y, p.z);
                for (Enemy e : enemies) {
                    if (!e.alive()) continue;
                    float dx = e.x - p.x, dz = e.z - p.z, d = (float) Math.sqrt(dx * dx + dz * dz);
                    if (d > KICK_RANGE || Math.abs(e.y - p.y) > 4) continue;
                    if ((dx * fx + dz * fz) < -1f) continue;  // giraffes kick in every direction, just not straight back
                    hurtEnemy(e, kickDamage(), p.x, p.z, 34, 14, 0);
                }
                for (Car c : cars) {
                    if (c.state == C_PLAYER || c.airborne || c.state == C_WRECK) continue;
                    if (dist2(c.x, c.z, p.x + fx * 2.5f, p.z + fz * 2.5f) < 5.5f * 5.5f && Math.abs(c.y - p.y) < 3) {
                        launchCar(c, p.x, p.z, 24); ev(EV_CAR_KICK, 0, c.x, c.y, c.z);
                        damageCar(c, has(Skills.HARD_HOOVES) ? 60 : 40);
                    }
                }
                hitPeds(p.x + fx * 2, p.y, p.z + fz * 2, KICK_RANGE * 0.8f, 20, true);
                return true;
            }
            case P_LOOK: {
                p.lookT = lookTime();
                ev(EV_LOOK, 0, p.x, p.y, p.z);
                return true;
            }
            case P_MUNCH: {
                boolean food = false;
                for (Tree t : trees)
                    if (dist3(t.x, t.h, t.z, p.x, p.y + HEAD_HEIGHT, p.z) < t.r + 4) { food = true; p.tongueX = t.x; p.tongueY = t.h; p.tongueZ = t.z; }
                Building b = buildingAt(p.x, p.z);
                if (b != null && b.garden && Math.abs(p.y - b.h) < 0.5f) { food = true; p.tongueX = p.x + fx * 2; p.tongueY = p.y + 1; p.tongueZ = p.z + fz * 2; }
                if (!food) { quip(Q_MUNCH_FAIL, true); return false; }
                p.munchT = 1f;
                p.hp = Math.min(hpMax(), p.hp + munchHeal());
                ev(EV_MUNCH, 0, p.tongueX, p.tongueY, p.tongueZ);
                return true;
            }
        }
        return false;
    }

    private void quip(int[] pool, boolean force) {
        if (!force && quipT > 0) return;
        quipT = 3f;
        int q = pool[0] + rng.nextInt(pool[1] - pool[0]);
        ev(EV_QUIP, q, g.x, g.y, g.z);
    }

    // ----------------------------------------------------------------- driving
    private void tickDriving(Input in) {
        Giraffe p = g;
        Car c = cars.get(p.car);
        if (in.pressed(B_ENTER) || in.pressed(B_SWING) || in.pressed(B_JUMP)) {
            exitCar(in.pressed(B_SWING) || in.pressed(B_JUMP));
            if (in.pressed(B_SWING) && findAnchor(in)) { p.mode = M_SWING; ev(EV_SWING_ATTACH, 0, p.ax, p.ay, p.az); }
            return;
        }
        if (!c.airborne) {
            float t = in.throttle;
            if (t > 0) c.speed += (c.speed < 0 ? CAR_ACCEL * 2 : CAR_ACCEL) * t * DT;
            else if (t < 0) c.speed += (c.speed > 0 ? CAR_ACCEL * 2.2f : CAR_ACCEL * 0.6f) * t * DT;
            else c.speed -= c.speed * CAR_DRAG * DT;
            c.speed = clamp(c.speed, -CAR_REVERSE, CAR_MAX);
            float grip = clamp(c.speed / 8f, -1, 1);
            c.yaw += in.steer * CAR_STEER * grip * DT;
        }
        moveCar(c);
        // bumping other cars hurts both of you
        for (Car o : cars) {
            if (o == c || o.airborne || Math.abs(o.y - c.y) > 2) continue;
            float dx = o.x - c.x, dz = o.z - c.z, d2 = dx * dx + dz * dz;
            if (d2 > 4.4f * 4.4f || d2 < 1e-4f) continue;
            float d = (float) Math.sqrt(d2), push = 4.4f - d;
            o.x += dx / d * push * 0.6f; o.z += dz / d * push * 0.6f;
            c.x -= dx / d * push * 0.4f; c.z -= dz / d * push * 0.4f;
            float rel = Math.abs(c.speed - (o.state == C_AI ? o.speed : 0));
            if (rel > 6 && c.crashT <= 0) {
                damageCar(c, rel * 0.7f); damageCar(o, rel * 1.1f);
                ev(EV_CRASH, 0, (c.x + o.x) / 2, c.y, (c.z + o.z) / 2);
                c.crashT = 0.5f;
                if (o.state == C_AI) o.state = C_PARKED;
                o.speed = c.speed * 0.5f; o.yaw = c.yaw;
                c.speed *= 0.35f;
            }
        }
        if (p.car < 0) return;   // the crash blew the car up and threw us out
        p.x = c.x; p.y = c.y; p.z = c.z; p.yaw = c.yaw;
        p.vx = (float) Math.sin(c.yaw) * c.speed; p.vz = (float) Math.cos(c.yaw) * c.speed; p.vy = 0;
        if (Math.abs(c.speed) > 9) for (Enemy e : enemies) {
            if (!e.alive() || e.state == E_FLYING) continue;
            if (dist2(e.x, e.z, c.x, c.z) < 3.4f * 3.4f && Math.abs(e.y - c.y) < 3) {
                hurtEnemy(e, Math.abs(c.speed) * 3.2f, c.x, c.z, Math.abs(c.speed) * 0.8f, 12, 0);
                ev(EV_SPLAT, 0, e.x, e.y, e.z);
            }
        }
    }

    private void exitCar(boolean hop) {
        Giraffe p = g;
        Car c = cars.get(p.car);
        c.state = C_PARKED;
        float rx = -(float) Math.cos(c.yaw), rz = (float) Math.sin(c.yaw);
        p.x = c.x + rx * 3.5f; p.z = c.z + rz * 3.5f; p.y = c.y;
        p.vx = (float) Math.sin(c.yaw) * c.speed * 0.6f; p.vz = (float) Math.cos(c.yaw) * c.speed * 0.6f;
        c.speed *= 0.5f;
        p.vy = hop ? JUMP_V : 0;
        p.mode = hop ? M_AIR : M_FOOT;
        if (!hop) { tp[0] = p.x; tp[1] = p.y; tp[2] = p.z; tv[0] = tv[1] = tv[2] = 0; resolve(tp, tv, BODY_RADIUS, p.y); p.x = tp[0]; p.z = tp[2]; }
        p.car = -1;
        ev(EV_CAR_EXIT, 0, p.x, p.y, p.z);
    }

    private void moveCar(Car c) {
        float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
        if (c.airborne) {
            c.vy -= GRAVITY * DT;
            c.y += c.vy * DT;
            c.roll += c.rollV * DT;
            c.x += fx * c.speed * DT; c.z += fz * c.speed * DT;
            float gy = groundAt(c.x, c.z, CAR_RADIUS);
            if (c.y <= gy && c.vy < 0) {
                float impact = -c.vy;
                c.y = gy; c.vy = 0; c.airborne = false; c.roll = 0; c.rollV = 0; c.speed *= 0.3f;
                ev(EV_CRASH, 1, c.x, c.y, c.z);
                if (impact > 8) damageCar(c, impact * 1.2f);
            }
        } else {
            c.x += fx * c.speed * DT; c.z += fz * c.speed * DT;
        }
        float prevY = c.y;
        tp[0] = c.x; tp[1] = c.y; tp[2] = c.z; tv[0] = fx * c.speed; tv[1] = c.vy; tv[2] = fz * c.speed;
        float before = c.speed;
        resolve(tp, tv, CAR_RADIUS, prevY);
        boolean hit = Math.abs(tp[0] - c.x) > 1e-4f || Math.abs(tp[2] - c.z) > 1e-4f;
        c.x = clamp(tp[0], 2, SIZE - 2); c.z = clamp(tp[2], 2, SIZE - 2);
        if (!c.airborne) { float gy = groundAt(c.x, c.z, CAR_RADIUS); if (c.y > gy + 0.5f) { c.airborne = true; } else c.y = gy; }
        if (hit) {
            c.speed = -before * 0.3f;
            if (Math.abs(before) > 10 && c.crashT <= 0) {
                ev(EV_CRASH, 0, c.x, c.y, c.z); c.crashT = 0.5f;
                damageCar(c, Math.abs(before) * 0.7f);
            }
        }
        c.crashT -= DT;
    }

    /** Cars catch fire at 0 HP and blow up 2.5 s later. */
    public void damageCar(Car c, float dmg) {
        if (c.state == C_WRECK || c.burnT > 0) return;
        c.hp -= dmg; c.hitT = 0.15f;
        if (c.hp <= 0) {
            c.hp = 0; c.burnT = 2.5f;
            if (c.state == C_AI) c.state = C_PARKED;
            ev(EV_CAR_FIRE, cars.indexOf(c), c.x, c.y, c.z);
        }
    }

    private void explode(Car c) {
        Giraffe p = g;
        int idx = cars.indexOf(c);
        boolean inside = p.car == idx;
        if (inside) exitCar(true);
        c.state = C_WRECK; c.wreckT = 25f; c.burnT = 0; c.speed = 0;
        ev(EV_EXPLODE, 0, c.x, c.y + 1, c.z);
        if (inside) hurtPlayer(30, c.x, c.z);
        else if (p.mode != M_DEAD) {
            float d = dist3(p.x, p.y, p.z, c.x, c.y, c.z);
            if (d < 10) {
                hurtPlayer(32 * (1 - d / 10) + 6, c.x, c.z);
                if (p.mode != M_DEAD && p.mode != M_DRIVE) {
                    float dx = p.x - c.x, dz = p.z - c.z, l = Math.max(0.5f, (float) Math.sqrt(dx * dx + dz * dz));
                    p.vx = dx / l * 22; p.vz = dz / l * 22; p.vy = 14; p.mode = M_AIR;
                }
            }
        }
        for (Enemy e : enemies) if (e.alive() && dist3(e.x, e.y, e.z, c.x, c.y, c.z) < 10) hurtEnemy(e, 95, c.x, c.z, 28, 16, 0);
        for (Car o : cars) if (o != c && o.state != C_PLAYER && dist2(o.x, o.z, c.x, c.z) < 9 * 9 && Math.abs(o.y - c.y) < 4) {
            damageCar(o, 55); launchCar(o, c.x, c.z, 14);
        }
        hitPeds(c.x, c.y, c.z, 9, 0, false);
        scare(c.x, c.z, 45, true);
    }

    private void respawnCar(Car c) {
        // somewhere out of sight
        for (int tries = 0; tries < 20; tries++) {
            c.dir = rng.nextInt(4);
            int line = 1 + rng.nextInt(BLOCKS - 1);
            float along = BLOCK * (0.3f + rng.nextFloat() * (BLOCKS - 0.6f));
            placeOnLane(c, line * BLOCK, along);
            if (dist2(c.x, c.z, g.x, g.z) > 150 * 150) break;
        }
        c.y = 0; c.vy = 0; c.roll = 0; c.rollV = 0; c.airborne = false;
        c.hp = CAR_HP; c.burnT = 0; c.wreckT = 0;
        c.color = rng.nextInt(6); c.state = C_AI; c.speed = AI_CAR_SPEED;
    }

    private void launchCar(Car c, float fromX, float fromZ, float power) {
        float dx = c.x - fromX, dz = c.z - fromZ, l = Math.max(0.1f, (float) Math.sqrt(dx * dx + dz * dz));
        if (c.state == C_AI) c.state = C_PARKED;
        c.airborne = true; c.vy = power * 0.7f; c.rollV = 6 + rng.nextFloat() * 6;
        c.yaw = (float) Math.atan2(dx, dz); c.speed = power;
        // anyone standing near the car gets hit too
        for (Enemy e : enemies) if (e.alive() && dist2(e.x, e.z, c.x, c.z) < 16) hurtEnemy(e, 60, fromX, fromZ, power, 10, 0);
    }

    private void tickCars() {
        for (Car c : cars) {
            c.hitT -= DT;
            if (c.burnT > 0) { c.burnT -= DT; if (c.burnT <= 0) explode(c); }
            if (c.state == C_WRECK) {
                if (c.airborne) moveCar(c);
                c.wreckT -= DT;
                if (c.wreckT <= 0 && dist2(c.x, c.z, g.x, g.z) > 120 * 120) respawnCar(c);
                continue;
            }
            if (c.state == C_PLAYER) continue;
            if (c.state == C_PARKED || c.airborne) {
                if (!c.airborne) { c.speed -= c.speed * 2f * DT; if (Math.abs(c.speed) < 0.2f) c.speed = 0; }
                if (c.speed != 0 || c.airborne) moveCar(c);
                continue;
            }
            // AI: slow down for things in front
            float fx = (float) Math.sin(c.yaw), fz = (float) Math.cos(c.yaw);
            float target = AI_CAR_SPEED;
            for (Car o : cars) {
                if (o == c) continue;
                float dx = o.x - c.x, dz = o.z - c.z;
                float ahead = dx * fx + dz * fz, side = Math.abs(dx * fz - dz * fx);
                if (ahead <= 0 || ahead >= 10 || side >= 3) continue;
                if (o.state == C_AI || o.state == C_PLAYER) { target = 0; break; }
                // a parked car or wreck in the lane: creep up and shove it to the curb
                target = Math.min(target, 4);
                if (ahead < 5.5f && !o.airborne) {
                    float rx = -(float) Math.cos(c.yaw), rz = (float) Math.sin(c.yaw);   // right of travel = curb side
                    o.x += rx * 5 * DT; o.z += rz * 5 * DT;
                }
            }
            // and for people in the crosswalk
            if (target > 0) for (Ped q : peds) {
                if (q.state == PD_DOWN) continue;
                float dx = q.x - c.x, dz = q.z - c.z;
                float ahead = dx * fx + dz * fz, side = Math.abs(dx * fz - dz * fx);
                if (ahead > 0 && ahead < 9 && side < 2.6f) { target = 0; break; }
            }
            float dxp = g.x - c.x, dzp = g.z - c.z;
            float ap = dxp * fx + dzp * fz, sp = Math.abs(dxp * fz - dzp * fx);
            if (ap > 0 && ap < 9 && sp < 3 && g.y < c.y + 3) target = 0;
            c.speed += clamp(target - c.speed, -40 * DT, 12 * DT);
            boolean xAxis = c.dir == 0 || c.dir == 2;
            float oldAlong = xAxis ? c.x : c.z;
            c.x += fx * c.speed * DT; c.z += fz * c.speed * DT;
            float along = xAxis ? c.x : c.z;
            int k = Math.round(along / BLOCK);
            float cross = k * BLOCK;
            if ((oldAlong - cross) * (along - cross) <= 0 && oldAlong != along) {
                int lineK = Math.round((xAxis ? c.z : c.x) / BLOCK);
                int[] options = new int[3]; int n = 0;
                int[] cand = {c.dir, (c.dir + 1) % 4, (c.dir + 3) % 4};
                for (int d : cand) {
                    boolean pos = d == 0 || d == 1;
                    int streetK = d == c.dir ? lineK : k;      // street we'd be driving on
                    int fromK = d == c.dir ? k : lineK;        // where we are along it
                    if (streetK < 1 || streetK > BLOCKS - 1) continue;
                    if (pos && fromK >= BLOCKS - 1) continue;
                    if (!pos && fromK <= 1) continue;
                    options[n++] = d;
                }
                int nd;
                if (n == 0) nd = (c.dir + 2) % 4;
                else {
                    nd = options[rng.nextInt(n)];
                    if (rng.nextFloat() < 0.4f) for (int i = 0; i < n; i++) if (options[i] == c.dir) nd = c.dir;
                }
                if (nd != c.dir) {
                    boolean nowX = nd == 0 || nd == 2;
                    c.dir = nd;
                    // new street is the cross street (k) unless we U-turned on our own street
                    if (nowX == xAxis) placeOnLane(c, lineK * BLOCK, along);
                    else placeOnLane(c, cross, lineK * BLOCK);
                }
            }
        }
    }

    // ----------------------------------------------------------------- enemies
    private Enemy spawnEnemy(int kind, int group, float x, float z, float y) {
        Enemy e = new Enemy();
        e.id = nextEnemyId++;
        e.kind = kind; e.group = group; e.x = x; e.z = z; e.y = y;
        e.maxHp = e.hp = kind == K_GRUNT ? 50 : kind == K_BRUTE ? 140 : kind == K_SNIPER ? 40 : 1100;
        e.shootT = 1 + rng.nextFloat() * 2;
        e.yaw = rng.nextFloat() * 6.28f;
        e.state = E_IDLE;
        e.onRoof = y > 1;
        e.strafe = rng.nextBoolean() ? 1 : -1; e.strafeT = 1 + rng.nextFloat() * 2;
        e.losT = rng.nextFloat() * 0.3f;
        if (kind == K_BOSS) e.shield = true;
        enemies.add(e);
        return e;
    }

    /** put a sniper on the roof of the building closest to (x, z) that's at least minH tall */
    private Enemy spawnSniper(int group, float x, float z, float minH) {
        Building b = null; float bd = Float.MAX_VALUE;
        for (Building o : buildings) {
            if (o.h < minH) continue;
            float d = dist2((o.x0 + o.x1) / 2, (o.z0 + o.z1) / 2, x, z);
            if (d < bd) { bd = d; b = o; }
        }
        if (b == null) return null;
        // stand at the edge facing the street, so he can actually see down
        float sx = (b.x0 + b.x1) / 2, sz = (b.z0 + b.z1) / 2;
        if (Math.abs(x - sx) > Math.abs(z - sz)) sx = x > sx ? b.x1 - 1.2f : b.x0 + 1.2f;
        else sz = z > sz ? b.z1 - 1.2f : b.z0 + 1.2f;
        return spawnEnemy(K_SNIPER, group, sx, sz, b.h);
    }

    private void spawnGroup(int group, float cx, float cz, int grunts, int brutes, float spread) {
        for (int k = 0; k < grunts + brutes; k++) {
            float x = cx + (rng.nextFloat() - 0.5f) * spread, z = cz + (rng.nextFloat() - 0.5f) * spread;
            x = clamp(x, 4, SIZE - 4); z = clamp(z, 4, SIZE - 4);
            float y = groundAt(x, z, 0.6f);
            spawnEnemy(k < grunts ? K_GRUNT : K_BRUTE, group, x, z, y);
        }
    }

    /** damage, knock away from (fx,fz). stunOnly=1 means don't launch. */
    private void hurtEnemy(Enemy e, float dmg, float fx, float fz, float knock, float up, int stunOnly) {
        if (e.kind == K_BOSS && e.shield) {
            dmg *= 0.05f; knock *= 0.2f; up *= 0.2f;
        }
        e.hp -= dmg; e.flashT = 0.15f;
        respect += 1;
        ev(EV_HIT, e.kind * 8 + (dmg > 60 ? 2 : dmg > 25 ? 1 : 0), e.x, e.y + 2, e.z);
        if (stunOnly == 0 && knock > 0) {
            float dx = e.x - fx, dz = e.z - fz, l = Math.max(0.1f, (float) Math.sqrt(dx * dx + dz * dz));
            float mass = e.kind == K_GRUNT || e.kind == K_SNIPER ? 1 : e.kind == K_BRUTE ? 1.8f : 4f;
            e.aimT = 0;
            e.vx = dx / l * knock / mass; e.vz = dz / l * knock / mass; e.vy = up / mass + 3;
            e.state = E_FLYING;
        }
        if (e.hp <= 0 && e.state != E_FLYING) killEnemy(e);
    }

    private void killEnemy(Enemy e) {
        e.state = E_DEAD; e.deadT = 4f; e.hp = 0;
        respect += e.kind == K_BOSS ? 300 : e.kind == K_BRUTE ? 25 : e.kind == K_SNIPER ? 30 : 10;
        e.aimT = 0;
        ev(EV_ENEMY_DOWN, e.kind, e.x, e.y, e.z);
    }

    private void tickEnemies() {
        Giraffe p = g;
        boolean playerAlive = p.mode != M_DEAD;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.flashT -= DT; e.disarmT -= DT; e.punchT -= DT;
            if (e.state == E_DEAD) {
                if (e.vy != 0 || e.y > groundAt(e.x, e.z, 0.5f)) fall(e);
                e.deadT -= DT; if (e.deadT <= 0) enemies.remove(i);
                continue;
            }
            if (e.kind == K_BOSS && !e.shield) { e.shieldT -= DT; if (e.shieldT <= 0) e.shield = true; }
            if (e.state == E_FLYING) {
                fall(e);
                if (e.vy == 0) { if (e.hp <= 0) killEnemy(e); else e.state = e.stunT > 0 ? E_STUNNED : E_CHASE; }
                continue;
            }
            if (e.state == E_STUNNED) {
                e.stunT -= DT; fall(e);
                if (e.stunT <= 0) e.state = E_CHASE;
                continue;
            }
            float dx = p.x - e.x, dz = p.z - e.z, d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dz * dz));
            float dy = p.y - e.y;
            // can he actually see the giraffe? (checked a few times a second, not every tick)
            e.losT -= DT;
            if (e.losT <= 0) {
                e.losT = 0.2f + rng.nextFloat() * 0.1f;
                float range = e.kind == K_SNIPER ? SNIPER_RANGE : 120;
                e.los = playerAlive && d < range && !segBlocked(e.x, e.y + 2.6f, e.z, p.x, p.y + 3.5f, p.z);
            }
            boolean sees = e.los;
            float aggro = e.kind == K_SNIPER ? SNIPER_RANGE : e.group == 1 ? 110 : 75;
            // mission poachers are hunting you and come looking; street crews only react to what they see
            if (playerAlive && e.state == E_IDLE && ((sees && d < aggro) || d < 22 || (e.group == 1 && d < 90))) { e.state = E_CHASE; alert(e); }
            else if (e.state == E_CHASE && (d > aggro * 1.6f || !playerAlive)) e.state = E_IDLE;

            float speed = e.kind == K_GRUNT ? 7 : e.kind == K_BRUTE ? 9.5f : e.kind == K_BOSS ? 5f : 0;
            float wx = 0, wz = 0;
            if (e.kind == K_SNIPER) {
                tickSniper(e, sees);
            } else if (e.state == E_CHASE) {
                e.yaw = (float) Math.atan2(dx, dz);
                boolean ground = !e.onRoof;
                if (e.kind == K_BRUTE) {
                    e.chargeCd -= DT;
                    if (e.chargeT > 0) {
                        // committed charge: straight line, fast, can't turn
                        e.chargeT -= DT; speed = 24; wx = e.aimX; wz = e.aimZ;
                        e.yaw = (float) Math.atan2(wx, wz);
                        if (d < 3.4f && Math.abs(dy) < 3 && e.punchT <= 0) {
                            e.punchT = 1.2f; e.chargeT = 0;
                            hurtPlayer(PUNCH_DAMAGE * 1.8f, e.x, e.z);
                            if (p.mode == M_FOOT) { p.vx += e.aimX * 18; p.vz += e.aimZ * 18; p.vy = 9; p.mode = M_AIR; }
                        }
                    } else if (sees && d > 5 && d < 16 && Math.abs(dy) < 3 && e.chargeCd <= 0) {
                        e.chargeT = 0.75f; e.chargeCd = 4.5f; e.aimX = dx / d; e.aimZ = dz / d;
                        ev(EV_CHARGE, 0, e.x, e.y + 3, e.z);
                    } else if (sees && Math.abs(dy) < 3) { wx = dx / d; wz = dz / d; }
                    else if (ground && nav.step(e.x, e.z, step)) { wx = step[0]; wz = step[1]; }
                    else { wx = dx / d; wz = dz / d; }
                    if (e.chargeT <= 0 && d < 3.2f && Math.abs(dy) < 3 && e.punchT <= 0) {
                        e.punchT = 1.1f;
                        hurtPlayer(PUNCH_DAMAGE, e.x, e.z);
                    }
                } else {
                    float keep = e.kind == K_BOSS ? 14 : 18;
                    e.retreatT -= DT;
                    // hurt grunts fall back for a bit, then come again
                    if (e.kind == K_GRUNT && e.hp < e.maxHp * 0.4f && e.retreatT < -8) e.retreatT = 2.5f;
                    if (e.retreatT > 0) { wx = -dx / d; wz = -dz / d; speed *= 1.3f; }
                    else if (!sees) {
                        // no line of sight: walk the streets toward him (around buildings, not into them)
                        if (ground && nav.step(e.x, e.z, step)) { wx = step[0]; wz = step[1]; }
                        else if (ground) { wx = dx / d; wz = dz / d; }
                    } else {
                        if (d > keep + 3) { wx = dx / d; wz = dz / d; }
                        else if (d < keep - 5) { wx = -dx / d * 0.7f; wz = -dz / d * 0.7f; }
                        // sidestep while shooting so they're not sitting ducks
                        e.strafeT -= DT;
                        if (e.strafeT <= 0) { e.strafe = -e.strafe; e.strafeT = 1.2f + rng.nextFloat() * 2f; }
                        wx += -dz / d * e.strafe * 0.7f; wz += dx / d * e.strafe * 0.7f;
                    }
                    // shoot only when they can see you, in short bursts
                    if (sees && d < 75 && e.disarmT <= 0 && Math.abs(dy) < 60) {
                        e.shootT -= DT;
                        if (e.shootT <= 0) {
                            if (e.kind == K_BOSS) {
                                e.shootT = 1.1f;
                                for (int k = 0; k < 3; k++) shoot(e, (k - 1) * 0.12f);
                            } else {
                                shoot(e, 0);
                                bullets.get(bullets.size() - 1).dmg = 5;   // burst rounds hit softer
                                e.burst++;
                                if (e.burst >= 3) { e.burst = 0; e.shootT = 2.0f + rng.nextFloat() * 1.2f; }
                                else e.shootT = 0.22f;
                            }
                        }
                    } else e.shootT = Math.max(e.shootT, 0.45f);   // reaction time after spotting you again
                }
            } else {
                // amble
                e.yaw += (rng.nextFloat() - 0.5f) * 2 * DT;
                wx = (float) Math.sin(e.yaw) * 0.25f; wz = (float) Math.cos(e.yaw) * 0.25f;
                if (e.onRoof) wx = wz = 0;
            }
            // separation
            for (Enemy o : enemies) {
                if (o == e || !o.alive()) continue;
                float sx = e.x - o.x, sz = e.z - o.z, s2 = sx * sx + sz * sz;
                if (s2 < 4 && s2 > 0.001f) { float s = (float) Math.sqrt(s2); wx += sx / s * 0.6f; wz += sz / s * 0.6f; }
            }
            float ny = e.y;
            float nx = e.x + wx * speed * DT, nz = e.z + wz * speed * DT;
            // stay on your roof if you're up on one
            float gy = groundAt(nx, nz, 0.6f);
            if (e.y > 1 && gy < e.y - 0.5f) { nx = e.x; nz = e.z; }
            tp[0] = nx; tp[1] = ny; tp[2] = nz; tv[0] = tv[1] = tv[2] = 0;
            resolve(tp, tv, 0.7f, e.y);
            e.x = clamp(tp[0], 1, SIZE - 1); e.z = clamp(tp[2], 1, SIZE - 1);
            if (wx != 0 || wz != 0) e.walkPhase += speed * DT * 0.8f;
            fall(e);
        }
    }

    private final float[] step = new float[2];

    /** first poacher to see you shouts, everyone in his gang nearby joins in */
    private void alert(Enemy e) {
        ev(EV_ALERT, rng.nextInt(ALERT_LINES.length), e.x, e.y + 4, e.z);
        for (Enemy o : enemies) if (o != e && o.alive() && o.group == e.group && o.state == E_IDLE && dist2(o.x, o.z, e.x, e.z) < 50 * 50) o.state = E_CHASE;
        scare(e.x, e.z, 25, false);
    }

    /** snipers paint you with a laser, lock the aim, then fire. Keep moving after the lock and it misses. */
    private void tickSniper(Enemy e, boolean sees) {
        Giraffe p = g;
        if (e.shootT > 0) { e.shootT -= DT; e.aimT = 0; return; }
        if (e.state != E_CHASE || !sees || e.disarmT > 0 || p.mode == M_DEAD) { e.aimT = Math.max(0, e.aimT - DT * 2); return; }
        e.yaw = (float) Math.atan2(p.x - e.x, p.z - e.z);
        if (e.aimT == 0) ev(EV_SNIPER_AIM, e.id, e.x, e.y, e.z);
        e.aimT += DT;
        if (e.aimT < SNIPER_LOCK) { e.aimX = p.x; e.aimY = p.y + (p.mode == M_DRIVE ? 1.5f : 3.5f); e.aimZ = p.z; }
        if (e.aimT >= SNIPER_AIM) {
            e.aimT = 0; e.shootT = 1.4f;
            float sx = e.x, sy = e.y + 2.4f, sz = e.z;
            float dx = e.aimX - sx, dy = e.aimY - sy, dz = e.aimZ - sz, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            Bullet b = new Bullet();
            b.x = sx; b.y = sy; b.z = sz;
            b.vx = dx / l * 240; b.vy = dy / l * 240; b.vz = dz / l * 240;
            b.life = 1.0f; b.dmg = SNIPER_DAMAGE;
            bullets.add(b);
            ev(EV_SNIPER_SHOT, 0, sx, sy, sz);
            scare(sx, sz, 40, false);
        }
    }

    private void fall(Enemy e) {
        float prevY = e.y;
        e.vy -= GRAVITY * DT;
        e.x += e.vx * DT; e.y += e.vy * DT; e.z += e.vz * DT;
        tp[0] = e.x; tp[1] = e.y; tp[2] = e.z; tv[0] = e.vx; tv[1] = e.vy; tv[2] = e.vz;
        boolean roof = resolve(tp, tv, 0.7f, prevY);
        e.x = clamp(tp[0], 1, SIZE - 1); e.y = tp[1]; e.z = clamp(tp[2], 1, SIZE - 1); e.vx = tv[0]; e.vy = tv[1]; e.vz = tv[2];
        if (e.y <= 0) { e.y = 0; roof = true; }
        if (roof && e.vy <= 0) {
            if (e.state == E_FLYING && prevY - e.y > 0.05f && Math.abs(e.vx) + Math.abs(e.vz) > 10) ev(EV_SPLAT, 1, e.x, e.y, e.z);
            e.vy = 0; e.vx *= 0.5f; e.vz *= 0.5f;
            if (Math.abs(e.vx) < 0.5f) e.vx = 0;
            if (Math.abs(e.vz) < 0.5f) e.vz = 0;
        }
    }

    private void shoot(Enemy e, float angleOff) {
        Giraffe p = g;
        float sx = e.x, sy = e.y + 2.6f, sz = e.z;
        float tx = p.x, ty = p.y + 3f, tz = p.z;
        float d = dist3(sx, sy, sz, tx, ty, tz);
        float spread = 0.035f + p.speed() * 0.006f + (p.mode == M_DRIVE ? 0.04f : 0);
        tx += (float) rng.nextGaussian() * d * spread; ty += (float) rng.nextGaussian() * d * spread * 0.6f; tz += (float) rng.nextGaussian() * d * spread;
        float dx = tx - sx, dy = ty - sy, dz = tz - sz, l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ca = (float) Math.cos(angleOff), sa = (float) Math.sin(angleOff);
        float rx = dx * ca - dz * sa, rz = dx * sa + dz * ca;
        Bullet b = new Bullet();
        b.x = sx; b.y = sy; b.z = sz;
        b.vx = rx / l * BULLET_SPEED; b.vy = dy / l * BULLET_SPEED; b.vz = rz / l * BULLET_SPEED;
        b.life = 1.2f; b.dmg = BULLET_DAMAGE;
        bullets.add(b);
        ev(EV_SHOT, e.kind, sx, sy, sz);
        scare(sx, sz, 25, false);
    }

    private void tickBullets() {
        Giraffe p = g;
        outer:
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.life -= DT;
            // fast sniper rounds move several metres a tick: check in small steps so they can't skip past you
            float sp = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy + b.vz * b.vz);
            int n = Math.max(1, (int) Math.ceil(sp * DT / 1.2f));
            for (int s = 0; s < n; s++) {
                b.x += b.vx * DT / n; b.y += b.vy * DT / n; b.z += b.vz * DT / n;
                if (b.life <= 0 || b.y < 0 || insideBuilding(b.x, b.y, b.z)) { bullets.remove(i); continue outer; }
                if (p.mode != M_DEAD) {
                    // giraffe: body plus neck up to the head
                    float hy = clamp(b.y - p.y, 0, HEAD_HEIGHT);
                    if (dist3(b.x, b.y, b.z, p.x, p.y + hy, p.z) < (p.mode == M_DRIVE ? 2.4f : 1.6f)) {
                        if (p.mode == M_DRIVE) {
                            damageCar(cars.get(p.car), b.dmg * 0.6f);
                            hurtPlayer(b.dmg * 0.5f, b.x - b.vx, b.z - b.vz);
                        } else hurtPlayer(b.dmg, b.x - b.vx, b.z - b.vz);
                        bullets.remove(i);
                        continue outer;
                    }
                }
                for (Car c : cars) {
                    if (c.state == C_WRECK || c.state == C_PLAYER) continue;
                    if (Math.abs(b.y - (c.y + 1.2f)) < 1.4f && dist2(b.x, b.z, c.x, c.z) < 2.6f * 2.6f) {
                        damageCar(c, b.dmg * 0.6f);
                        ev(EV_BULLET_CAR, 0, b.x, b.y, b.z);
                        bullets.remove(i);
                        continue outer;
                    }
                }
            }
        }
    }

    private void hurtPlayer(float dmg, float fromX, float fromZ) {
        Giraffe p = g;
        if (p.mode == M_DEAD) return;
        p.hp -= dmg; p.sinceHurt = 0; p.hurtT = 0.25f;
        ev(EV_PLAYER_HURT, (int) dmg, p.x, p.y, p.z);
        if (p.hp < 35 && rng.nextFloat() < 0.15f) quip(Q_HURT, false);
        if (p.hp <= 0) {
            p.hp = 0;
            if (p.car >= 0) { cars.get(p.car).state = C_PARKED; p.car = -1; }
            p.mode = M_DEAD; p.deadT = 3f; p.carrying = false;
            ev(EV_DIED, 0, p.x, p.y, p.z);
            quip(Q_DEAD, true);
            if (missionActive) failMission("You got poached.");
        }
    }

    private void respawn() {
        Giraffe p = g;
        p.x = zooX(); p.z = zooZ() - LOT / 2f - STREET / 2f; p.y = 0;
        p.vx = p.vy = p.vz = 0; p.yaw = (float) Math.PI;
        p.hp = hpMax(); p.mode = M_FOOT; p.car = -1; p.carrying = false; p.sinceHurt = 99; p.zipT = 0;
        if (act >= 0) failActivity("You got poached.");
        for (int i = 0; i < POWERS; i++) p.cd[i] = 0;
    }

    // ----------------------------------------------------------------- missions
    public void startMission(int m) {
        mission = m;
        missionActive = true; stage = 0; timed = false; missionTimer = 0;
        clearGroup(1);
        rings.clear(); boss = null; crateState = 0;
        switch (m) {
            case 0: {
                float z = 5 * BLOCK, x0 = 4 * BLOCK + 40;
                for (int k = 0; k < 7; k++) {
                    Ring r = new Ring();
                    r.x = x0 + k * 42; r.z = z + (k % 3 - 1) * 4f; r.y = 20 + (k % 2) * 12 + k * 1.5f; r.yaw = (float) Math.PI / 2;
                    rings.add(r);
                }
                timed = true; missionTimer = 90;
                break;
            }
            case 1: {
                float cx = 6.5f * BLOCK, cz = 2.5f * BLOCK;
                spawnGroup(1, cx, cz, 8, 2, LOT - 8);
                spawnSniper(1, cx + LOT, cz, 20);
                break;
            }
            case 2: {
                timed = true; missionTimer = 70;
                break;
            }
            case 3: {
                Building tall = null;
                for (Building b : buildings) {
                    float cx = (b.x0 + b.x1) / 2, cz = (b.z0 + b.z1) / 2;
                    if (dist2(cx, cz, zooX(), zooZ()) < 160 * 160) continue;
                    if (tall == null || b.h > tall.h) tall = b;
                }
                crateX = (tall.x0 + tall.x1) / 2; crateZ = (tall.z0 + tall.z1) / 2; crateY = tall.h + 1;
                break;
            }
            case 4: {
                float cx = 2.5f * BLOCK, cz = 6.5f * BLOCK;
                boss = spawnEnemy(K_BOSS, 1, cx, cz, 0);
                spawnGroup(1, cx, cz, 8, 2, LOT - 10);
                spawnSniper(1, cx + LOT, cz + 10, 20);
                break;
            }
        }
        ev(EV_MISSION_START, m, g.x, g.y, g.z);
        refreshObjective();
    }

    private void clearGroup(int group) {
        for (int i = enemies.size() - 1; i >= 0; i--) if (enemies.get(i).group == group) enemies.remove(i);
    }

    private void failMission(String why) {
        missionActive = false;
        clearGroup(1); rings.clear(); boss = null; g.carrying = false; crateState = 0;
        objectiveText = why;
        ev(EV_MISSION_FAIL, mission, g.x, g.y, g.z);
    }

    private void completeMission() {
        missionActive = false;
        respect += 500;
        ev(EV_MISSION_DONE, mission, g.x, g.y, g.z);
        quip(Q_DONE, true);
        mission++;
        rings.clear(); g.carrying = false;
        refreshObjective();
    }

    private void grabCrate() {
        crateState = 1; g.carrying = true;
        ev(EV_PICKUP, 0, crateX, crateY, crateZ);
        stage = 1; timed = true; missionTimer = 100;
        // rooftop snipers along the way
        float sx = crateX, sz = crateZ, tx = zooX(), tz = zooZ();
        for (int k = 1; k <= 3; k++) {
            float f = k / 4f;
            float px = sx + (tx - sx) * f, pz = sz + (tz - sz) * f;
            Building b = null; float bd = Float.MAX_VALUE;
            for (Building o : buildings) {
                float d = dist2((o.x0 + o.x1) / 2, (o.z0 + o.z1) / 2, px, pz);
                if (d < bd) { bd = d; b = o; }
            }
            for (int n = 0; n < 2; n++) {
                float x = b.x0 + 3 + rng.nextFloat() * (b.x1 - b.x0 - 6), z = b.z0 + 3 + rng.nextFloat() * (b.z1 - b.z0 - 6);
                spawnEnemy(K_GRUNT, 1, x, z, b.h);
            }
            spawnSniper(1, px + 30, pz - 30, 25);
        }
    }

    private void tickMission() {
        Giraffe p = g;
        if (missionActive && timed) {
            missionTimer -= DT;
            if (missionTimer <= 0) { failMission("Out of time."); return; }
        }
        if (!missionActive) {
            if (mission < MISSIONS && p.mode != M_DEAD && act < 0) {
                float[] s = missionStart[mission];
                if (dist2(p.x, p.z, s[0], s[1]) < 7 * 7 && p.y < 6) startMission(mission);
            }
            if (act < 0) tickCrime();
            refreshObjective();
            return;
        }
        switch (mission) {
            case 0: {
                Ring next = null;
                for (Ring r : rings) if (!r.done) { next = r; break; }
                if (next != null && dist3(p.x, p.y + 3, p.z, next.x, next.y, next.z) < 7f) {
                    next.done = true;
                    ev(EV_RING, rings.indexOf(next), next.x, next.y, next.z);
                    missionTimer += 3;
                }
                boolean all = true; for (Ring r : rings) all &= r.done;
                if (all) completeMission();
                break;
            }
            case 1:
                if (aliveIn(1) == 0) completeMission();
                break;
            case 2: {
                float tx = 8 * BLOCK, tz = 1 * BLOCK;
                if (stage == 0 && p.mode == M_DRIVE && dist2(p.x, p.z, tx, tz) < 14 * 14) {
                    stage = 1; timed = false;
                    spawnGroup(1, tx - 30, tz + 30, 3, 3, 30);
                    spawnSniper(1, tx - 20, tz + 50, 15);
                    ev(EV_BANNER, 2, p.x, p.y, p.z);
                } else if (stage == 1 && aliveIn(1) == 0) completeMission();
                break;
            }
            case 3:
                if (crateState == 1 && dist2(p.x, p.z, zooX(), zooZ()) < 22 * 22 && p.y < 15) {
                    crateState = 2; p.carrying = false;
                    ev(EV_DELIVER, 0, p.x, p.y, p.z);
                    completeMission();
                }
                break;
            case 4:
                if (boss != null && !boss.alive()) { clearGroupSlowly(); completeMission(); }
                break;
        }
        refreshObjective();
    }

    private void clearGroupSlowly() {
        for (Enemy e : enemies) if (e.group == 1 && e.alive()) { e.state = E_STUNNED; e.stunT = 99; e.group = 2; }
    }

    private void tickCrime() {
        if (mission < MISSIONS) return;   // crimes start once the story is done
        if (crimeActive) {
            if (aliveIn(3) == 0) {
                crimeActive = false; crimesCleared++; respect += 150; crimeT = 25 + rng.nextFloat() * 20;
                ev(EV_MISSION_DONE, 99, g.x, g.y, g.z);
            }
            return;
        }
        crimeT -= DT;
        if (crimeT > 0) return;
        float ang = rng.nextFloat() * 6.28f, d = 90 + rng.nextFloat() * 120;
        float x = clamp(g.x + (float) Math.sin(ang) * d, BLOCK, SIZE - BLOCK), z = clamp(g.z + (float) Math.cos(ang) * d, BLOCK, SIZE - BLOCK);
        x = Math.round(x / BLOCK) * BLOCK; z = Math.round(z / BLOCK) * BLOCK;
        crimeX = x; crimeZ = z; crimeActive = true;
        spawnGroup(3, x, z, 3 + rng.nextInt(3), rng.nextInt(2), 16);
        if (rng.nextFloat() < 0.4f) spawnSniper(3, x + 30, z + 30, 15);
        ev(EV_CRIME, 0, x, 0, z);
    }

    public void refreshObjective() {
        hasObjective = false;
        if (act >= 0) {
            Activity a = acts.get(act);
            float[] pt = a.pts[Math.min(actNext, a.pts.length - 1)];
            ox = pt[0]; oy = pt[1]; oz = pt[2]; hasObjective = true;
            objectiveText = (a.type == ACT_SWING ? "Swing race: " : "Street race: ") + "checkpoint " + (actNext + 1) + "/" + a.pts.length
                    + (a.type == ACT_DRIVE && g.mode != M_DRIVE ? "  (get back in a car!)" : "") + "   Backspace quits";
            return;
        }
        if (!missionActive) {
            if (mission < MISSIONS) {
                ox = missionStart[mission][0]; oz = missionStart[mission][1]; oy = 0; hasObjective = true;
                objectiveText = "Next: " + MISSION_NAMES[mission] + "  (walk into the yellow beacon)";
            } else if (crimeActive) {
                ox = crimeX; oz = crimeZ; oy = 0; hasObjective = true;
                objectiveText = "Crime in progress: " + aliveIn(3) + " poachers";
            } else {
                objectiveText = "Free roam. The city is quiet. For now.";
            }
            return;
        }
        Giraffe p = g;
        switch (mission) {
            case 0: {
                int done = 0; Ring next = null;
                for (Ring r : rings) { if (r.done) done++; else if (next == null) next = r; }
                if (next != null) { ox = next.x; oy = next.y; oz = next.z; hasObjective = true; }
                objectiveText = "Swing through the rings  " + done + "/" + rings.size();
                break;
            }
            case 1: {
                Enemy n = nearest(1); if (n != null) { ox = n.x; oy = n.y; oz = n.z; hasObjective = true; }
                objectiveText = "Poachers left: " + aliveIn(1);
                break;
            }
            case 2:
                if (stage == 0) {
                    ox = 8 * BLOCK; oz = 1 * BLOCK; oy = 0; hasObjective = true;
                    objectiveText = p.mode == M_DRIVE ? "Drive to the marker" : "Get in a car (E)";
                } else {
                    Enemy n = nearest(1); if (n != null) { ox = n.x; oy = n.y; oz = n.z; hasObjective = true; }
                    objectiveText = "It's an ambush! Poachers left: " + aliveIn(1);
                }
                break;
            case 3:
                if (crateState == 0) { ox = crateX; oy = crateY; oz = crateZ; objectiveText = "Grab the leaf crate off the tallest roof (touch it or Tongue it)"; }
                else { ox = zooX(); oy = 0; oz = zooZ(); objectiveText = "Bring the crate to the zoo"; }
                hasObjective = true;
                break;
            case 4:
                if (boss != null) { ox = boss.x; oy = boss.y; oz = boss.z; hasObjective = true; }
                objectiveText = boss != null && boss.shield ? "The Taxidermist is shielded. BELLOW at him!" : "Shield's down. Hit him!";
                break;
        }
    }

    public Enemy nearest(int group) {
        Enemy best = null; float bd = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (!e.alive() || (group >= 0 && e.group != group)) continue;
            float d = dist2(e.x, e.z, g.x, g.z);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    // ----------------------------------------------------------------- pedestrians
    private float screamT;

    private void tickPeds() {
        Giraffe p = g;
        pedTalkT -= DT; screamT -= DT;
        for (Ped q : peds) {
            q.talkT -= DT;
            if (q.state == PD_WALK) {
                float tx = lat(q.txi), tz = lat(q.tzi);
                float dx = tx - q.x, dz = tz - q.z, d = (float) Math.sqrt(dx * dx + dz * dz);
                if (d < 0.4f) {
                    int fx = q.xi, fz = q.zi;
                    q.xi = q.txi; q.zi = q.tzi;
                    pickPedTarget(q, fx, fz);
                } else {
                    // wait at the curb while a car is coming through the crosswalk
                    boolean crossing = (q.xi / 2 != q.txi / 2) || (q.zi / 2 != q.tzi / 2);
                    if (crossing && d > STREET + 1 && carNear(q.x + dx / d * 6, q.z + dz / d * 6, 12)) continue;
                    q.yaw = (float) Math.atan2(dx, dz);
                    float s = Math.min(d, q.speed * DT);
                    q.x += dx / d * s; q.z += dz / d * s;
                    q.walkPhase += q.speed * DT * 1.8f;
                }
            } else if (q.state == PD_FLEE) {
                float dx = q.x - q.fx, dz = q.z - q.fz, d = Math.max(0.1f, (float) Math.sqrt(dx * dx + dz * dz));
                q.yaw = (float) Math.atan2(dx, dz);
                tp[0] = q.x + dx / d * 7.5f * DT; tp[1] = 0; tp[2] = q.z + dz / d * 7.5f * DT;
                tv[0] = tv[1] = tv[2] = 0;
                resolve(tp, tv, 0.5f, 0);
                q.x = clamp(tp[0], 2, SIZE - 2); q.z = clamp(tp[2], 2, SIZE - 2);
                q.walkPhase += 7.5f * DT * 1.8f;
                q.t -= DT;
                if (q.t <= 0) backToSidewalk(q);
            } else {
                // knocked down: fly, land, lie there, get up and run
                float prevY = q.y;
                q.vy -= GRAVITY * DT;
                q.x += q.vx * DT; q.y += q.vy * DT; q.z += q.vz * DT;
                tp[0] = q.x; tp[1] = q.y; tp[2] = q.z; tv[0] = q.vx; tv[1] = q.vy; tv[2] = q.vz;
                boolean roof = resolve(tp, tv, 0.5f, prevY);
                q.x = clamp(tp[0], 2, SIZE - 2); q.y = tp[1]; q.z = clamp(tp[2], 2, SIZE - 2); q.vx = tv[0]; q.vy = tv[1]; q.vz = tv[2];
                if (q.y <= 0) { q.y = 0; roof = true; }
                if (roof && q.vy <= 0) { q.vy = 0; q.vx *= 0.85f; q.vz *= 0.85f; }
                q.t -= DT;
                if (q.t <= 0 && q.vy == 0) {
                    if (q.y > 1) { q.y = 0; q.x = lat(q.xi); q.z = lat(q.zi); }  // landed on a roof: quietly put them back
                    q.state = PD_FLEE; q.t = 3;
                }
            }
            if (q.state == PD_DOWN) continue;
            // cars run people over
            for (Car c : cars) {
                if (c.state == C_WRECK) continue;
                float sp = Math.abs(c.speed);
                if (sp < 7 && !c.airborne) continue;
                if (dist2(c.x, c.z, q.x, q.z) < 2.8f * 2.8f && Math.abs(c.y - q.y) < 2.5f) {
                    knock(q, c.x, c.z, sp * 0.8f + 6, 8);
                    if (c.state == C_PLAYER) { respect = Math.max(0, respect - 5); ev(EV_PED_HIT, 1, q.x, q.y + 2, q.z); }
                    break;
                }
            }
        }
        // chatter
        if (pedTalkT <= 0 && p.mode != M_DEAD) {
            pedTalkT = 3.5f + rng.nextFloat() * 4;
            Ped best = null; float bd = 20 * 20;
            for (Ped q : peds) {
                if (q.state == PD_DOWN || q.talkT > 0) continue;
                float d = dist2(q.x, q.z, p.x, p.z);
                if (d < bd) { bd = d; best = q; }
            }
            if (best != null) {
                boolean fight = false;
                for (Enemy e : enemies) if (e.alive() && e.state == E_CHASE && dist2(e.x, e.z, p.x, p.z) < 50 * 50) { fight = true; break; }
                int line = best.state == PD_FLEE || fight ? 6 + rng.nextInt(4) : rng.nextInt(6);
                best.talkT = 8;
                ev(EV_PED_TALK, line, best.x, best.y + 3, best.z);
            }
        }
    }

    private boolean carNear(float x, float z, float r) {
        for (Car c : cars) if (c.state != C_WRECK && Math.abs(c.speed) > 2 && dist2(c.x, c.z, x, z) < r * r) return true;
        return false;
    }

    private void backToSidewalk(Ped q) {
        int bi = 0, bj = 0; float bd = Float.MAX_VALUE;
        for (int i = 0; i < LAT; i++) { float d = Math.abs(lat(i) - q.x); if (d < bd) { bd = d; bi = i; } }
        bd = Float.MAX_VALUE;
        for (int j = 0; j < LAT; j++) { float d = Math.abs(lat(j) - q.z); if (d < bd) { bd = d; bj = j; } }
        q.xi = q.txi = bi; q.zi = q.tzi = bj;
        q.state = PD_WALK;
    }

    private void knock(Ped q, float fromX, float fromZ, float power, float up) {
        float dx = q.x - fromX, dz = q.z - fromZ, l = Math.max(0.1f, (float) Math.sqrt(dx * dx + dz * dz));
        q.vx = dx / l * power; q.vz = dz / l * power; q.vy = up;
        q.state = PD_DOWN; q.t = 3f; q.fx = fromX; q.fz = fromZ;
        if (q.talkT <= 0) { q.talkT = 5; ev(EV_PED_TALK, 10 + rng.nextInt(4), q.x, q.y + 3, q.z); }
    }

    /** the giraffe (or a blast) hits bystanders. byPlayer costs respect. */
    private void hitPeds(float x, float y, float z, float r, float power, boolean byPlayer) {
        for (Ped q : peds) {
            if (q.state == PD_DOWN || Math.abs(q.y - y) > 5 || dist2(q.x, q.z, x, z) > r * r) continue;
            knock(q, x, z, power > 0 ? power : 14, 9);
            if (byPlayer) { respect = Math.max(0, respect - 10); ev(EV_PED_HIT, 0, q.x, q.y + 2, q.z); }
        }
    }

    /** make bystanders run from (x, z) */
    private void scare(float x, float z, float r, boolean loud) {
        boolean any = false;
        for (Ped q : peds) {
            if (q.state == PD_DOWN || dist2(q.x, q.z, x, z) > r * r) continue;
            if (q.state != PD_FLEE) any = true;
            q.state = PD_FLEE; q.fx = x; q.fz = z; q.t = 3 + rng.nextFloat() * 3;
        }
        if (any && screamT <= 0 && (loud || rng.nextFloat() < 0.5f)) { screamT = 2f; ev(EV_PED_SCREAM, 0, x, 0, z); }
    }

    // ----------------------------------------------------------------- golden leaves
    private void tickLeaves() {
        Giraffe p = g;
        if (p.mode == M_DEAD) return;
        for (int i = 0; i < leaves.size(); i++) {
            if ((leafMask & (1 << i)) != 0) continue;
            Leaf l = leaves.get(i);
            if (dist3(p.x, p.y + 4, p.z, l.x, l.y, l.z) < 5.5f) {
                leafMask |= 1 << i;
                respect += 50;
                ev(EV_LEAF, i, l.x, l.y, l.z);
                if (leavesFound() == leaves.size()) { respect += 1000; ev(EV_ALL_LEAVES, 0, l.x, l.y, l.z); }
            }
        }
    }

    // ----------------------------------------------------------------- races
    public static final int MEDAL_GOLD = 0, MEDAL_SILVER = 1, MEDAL_BRONZE = 2, MEDAL_NONE = 3;
    private static final int[] MEDAL_REWARD = {400, 250, 120, 40, 0};
    public int lastActGain;
    private float actHintT;

    public int medalFor(int k, float t) {
        if (t <= 0) return 4;
        Activity a = acts.get(k);
        return t <= a.gold ? MEDAL_GOLD : t <= a.silver ? MEDAL_SILVER : t <= a.bronze ? MEDAL_BRONZE : MEDAL_NONE;
    }

    private void tickActivity(Input in) {
        Giraffe p = g;
        actHintT -= DT;
        if (act < 0) {
            if (!activitiesEnabled || missionActive || p.mode == M_DEAD) return;
            for (int k = 0; k < acts.size(); k++) {
                Activity a = acts.get(k);
                if (dist2(p.x, p.z, a.sx, a.sz) > 7 * 7 || p.y > 6) continue;
                if (a.type == ACT_DRIVE && p.mode != M_DRIVE) {
                    if (actHintT <= 0) { actHintT = 4; ev(EV_BANNER, 100 + k, p.x, p.y, p.z); }
                    continue;
                }
                act = k; actNext = 0; actTime = 0;
                ev(EV_ACT_START, k, p.x, p.y, p.z);
                refreshObjective();
                return;
            }
            return;
        }
        Activity a = acts.get(act);
        if (in.pressed(B_CANCEL)) { failActivity("Race cancelled."); return; }
        actTime += DT;
        if (actTime > a.bronze * 2f) { failActivity("Too slow. Try again."); return; }
        float[] pt = a.pts[actNext];
        boolean hit = a.type == ACT_SWING
                ? dist3(p.x, p.y + 3, p.z, pt[0], pt[1], pt[2]) < 7.5f
                : p.mode == M_DRIVE && dist2(p.x, p.z, pt[0], pt[2]) < 11 * 11;
        if (hit) {
            actNext++;
            ev(EV_CHECKPOINT, actNext, pt[0], pt[1], pt[2]);
            if (actNext >= a.pts.length) finishActivity();
        }
        refreshObjective();
    }

    private void finishActivity() {
        int k = act;
        float t = actTime;
        int medal = medalFor(k, t), old = medalFor(k, raceBest[k]);
        lastActGain = Math.max(medal == MEDAL_NONE ? 0 : 20, MEDAL_REWARD[medal] - MEDAL_REWARD[old]);
        respect += lastActGain;
        if (raceBest[k] <= 0 || t < raceBest[k]) raceBest[k] = t;
        act = -1;
        ev(EV_ACT_DONE, k * 8 + medal, g.x, g.y, g.z);
        refreshObjective();
    }

    public void failActivity(String why) {
        int k = act;
        act = -1;
        ev(EV_ACT_FAIL, k, g.x, g.y, g.z);
        refreshObjective();
        objectiveText = why;
    }

    // ---------------------------------------------------------------- utils
    public static float clamp(float v, float a, float b) { return v < a ? a : v > b ? b : v; }
    public static float lerpAngle(float a, float b, float t) {
        float d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return a + d * t;
    }
}
