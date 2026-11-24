package com.saedarm.neckswing;

import java.util.Random;

/** Autopilot giraffe. Fills the same Input the keyboard does. Used by Sim and --autoplay. */
public final class Bot {
    private final Random rng;
    public float skill;            // 0..1, how often it fumbles
    private float swingT, thinkT, wanderYaw;
    private boolean holding;

    public Bot(long seed, float skill) { rng = new Random(seed); this.skill = skill; }

    public void fill(City c, City.Input in) {
        City.Giraffe g = c.g;
        in.mx = in.mz = 0; in.throttle = in.steer = 0; in.reelHeld = false;
        thinkT -= City.DT;
        if (g.mode == City.M_DEAD) { in.swingHeld = false; holding = false; return; }

        // target point
        float tx, ty, tz;
        if (c.hasObjective) { tx = c.ox; ty = c.oy; tz = c.oz; }
        else {
            wanderYaw += (rng.nextFloat() - 0.5f) * 0.05f;
            tx = g.x + (float) Math.sin(wanderYaw) * 100; tz = g.z + (float) Math.cos(wanderYaw) * 100; ty = 0;
        }

        // nearby fight takes priority (except while carrying the crate)
        City.Enemy foe = c.nearest(-1);
        float foeD = foe == null ? 999 : (float) Math.sqrt(City.dist2(foe.x, foe.z, g.x, g.z));
        boolean fighting = foe != null && foeD < 22 && !g.carrying && Math.abs(foe.y - g.y) < 6;
        if ((c.missionActive && c.mission == 2 && c.stage == 0) || c.act >= 0) fighting = false;

        boolean fumble = rng.nextFloat() > 0.6f + skill * 0.4f;

        // the car mission: find a car and drive
        boolean needCar = (c.missionActive && c.mission == 2 && c.stage == 0) || (c.act >= 0 && c.acts.get(c.act).type == City.ACT_DRIVE);
        if (needCar) {
            if (g.mode == City.M_DRIVE) { drive(c, in, tx, tz); return; }
            City.Car best = null; float bd = Float.MAX_VALUE;
            for (City.Car car : c.cars) {
                if (car.airborne || car.y > 1 || !car.usable()) continue;
                float d = City.dist2(car.x, car.z, g.x, g.z);
                if (d < bd) { bd = d; best = car; }
            }
            if (best != null) {
                if (g.mode == City.M_SWING) { in.swingHeld = false; holding = false; }
                tx = best.x; tz = best.z; ty = 0;
                if (bd < 25 && g.mode == City.M_FOOT) in.press(City.B_ENTER);
                if (bd < 60 * 60) { walk(g, in, tx, tz); return; }
            }
        } else if (g.mode == City.M_DRIVE) {
            in.press(City.B_ENTER);
            return;
        }

        if (fighting) {
            if (g.mode == City.M_SWING) { in.swingHeld = false; holding = false; }
            walk(g, in, foe.x, foe.z);
            int near = 0;
            for (City.Enemy e : c.enemies) if (e.alive() && City.dist2(e.x, e.z, g.x, g.z) < 18 * 18) near++;
            if (fumble) return;
            if ((near >= 3 || (foe.kind == City.K_BOSS && foe.shield)) && g.cd[City.P_BELLOW] <= 0 && foeD < 20) in.press(City.B_BELLOW);
            else if (foeD < 4.5f && g.cd[City.P_KICK] <= 0) in.press(City.B_KICK);
            else if (foeD < 7) in.press(City.B_ATTACK);
            else if (foeD < 16 && g.cd[City.P_TONGUE] <= 0 && rng.nextFloat() < 0.05f) in.press(City.B_TONGUE);
            if (g.hp < 45 && g.cd[City.P_MUNCH] <= 0) in.press(City.B_MUNCH);
            return;
        }

        float dx = tx - g.x, dz = tz - g.z, dist = (float) Math.sqrt(dx * dx + dz * dz);
        float dy = ty - g.y;

        // crate: tongue it when in range
        if (c.missionActive && c.mission == 3 && c.crateState == 0) {
            float d3 = City.dist3(tx, ty, tz, g.x, g.y + City.HEAD_HEIGHT, g.z);
            if (d3 < City.TONGUE_RANGE - 1 && g.cd[City.P_TONGUE] <= 0) in.press(City.B_TONGUE);
        }

        boolean needHeight = dy > 6;
        if (dist < 14 && !needHeight) {
            if (g.mode == City.M_SWING) { in.swingHeld = false; holding = false; }
            walk(g, in, tx, tz);
            return;
        }

        // close to a floating target (rings): let go and steer in
        if (dist < 30 && !needHeight && g.mode != City.M_FOOT && g.y > ty - 3) {
            in.swingHeld = false; holding = false;
            in.mx = dx / dist; in.mz = dz / dist;
            return;
        }
        // travel by swinging
        in.mx = dx / dist; in.mz = dz / dist;
        in.aimYaw = (float) Math.atan2(dx, dz);
        if (g.mode == City.M_SWING) {
            swingT += City.DT;
            // release when moving up and forward past the anchor, or when going too long
            float ahead = (g.x - g.ax) * dx / dist + (g.z - g.az) * dz / dist;
            boolean rising = g.vy > 2;
            float wantY = needHeight ? ty + 4 : 0;
            if ((ahead > 2 && rising && g.y > wantY - 2) || swingT > 3.5f || (needHeight && g.y > ty + 3)) {
                in.swingHeld = false; holding = false;
            } else {
                in.swingHeld = true;
                if (needHeight && g.y < ty) in.reelHeld = true;
            }
        } else {
            swingT = 0;
            in.swingHeld = false;
            if (!fumble && thinkT <= 0) {
                if (g.mode == City.M_FOOT) { in.press(City.B_SWING); in.swingHeld = true; thinkT = 0.3f; }
                else if (g.mode == City.M_AIR && (g.vy < 4 || g.y < 8)) { in.press(City.B_SWING); in.swingHeld = true; thinkT = 0.25f; }
            }
        }
    }

    private void walk(City.Giraffe g, City.Input in, float tx, float tz) {
        float dx = tx - g.x, dz = tz - g.z, d = (float) Math.sqrt(dx * dx + dz * dz);
        if (d > 0.5f) { in.mx = dx / d; in.mz = dz / d; in.aimYaw = (float) Math.atan2(dx, dz); }
    }

    private void drive(City c, City.Input in, float tx, float tz) {
        City.Giraffe g = c.g;
        City.Car car = c.cars.get(g.car);
        // Manhattan route: line up with the target's street, then drive along it
        float B = City.BLOCK;
        float sx = Math.round(tx / B) * B, sz = Math.round(tz / B) * B;
        float cx = Math.round(car.x / B) * B, cz = Math.round(car.z / B) * B;
        float wx, wz;
        boolean onZStreet = Math.abs(car.z - cz) < 9;   // on a street that runs along x
        boolean onXStreet = Math.abs(car.x - cx) < 9;
        if (Math.abs(car.x - sx) > 10 && onZStreet) { wx = sx; wz = cz; }
        else if (onXStreet) { wx = cx; wz = tz; }
        else if (onZStreet) { wx = cx; wz = cz; }
        else { wx = tx; wz = tz; }
        float dx = wx - car.x, dz = wz - car.z;
        float want = (float) Math.atan2(dx, dz);
        float diff = want - car.yaw;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        in.steer = City.clamp(diff * 2.5f, -1, 1);
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        in.throttle = Math.abs(diff) > 1.2f ? 0.3f : (d < 20 ? 0.6f : 1f);
        // pinned against something: back up hard with the wheel turned the other way, then try again
        if (reverseT > 0) { reverseT -= City.DT; in.throttle = -1; in.steer = -in.steer; if (Math.abs(in.steer) < 0.5f) in.steer = 1; return; }
        if (Math.abs(car.speed) < 2) stuck += City.DT; else stuck = 0;
        if (stuck > 1.2f) { stuck = 0; reverseT = 1.4f; }
    }

    private float stuck, reverseT;
}
