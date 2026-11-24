package com.saedarm.neckswing;

/** Headless balance run: the bot plays every mission N times and we print how it went. */
public final class Sim {
    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        System.out.printf("Neckswing sim, %d runs per mission, bot skill 0.5-1.0, 4 minute cap%n", runs);
        System.out.printf("%-24s %8s %9s %8s %8s%n", "mission", "cleared", "avg time", "deaths", "fails");
        for (int m = 0; m < City.MISSIONS; m++) {
            int wins = 0, deaths = 0, fails = 0; float total = 0;
            for (int r = 0; r < runs; r++) {
                City c = new City(1000L * m + r);
                c.mission = m;
                c.activitiesEnabled = false;
                Bot bot = new Bot(r, 0.5f + 0.5f * r / Math.max(1, runs - 1));
                City.Input in = new City.Input();
                // teleport next to the start beacon
                c.g.x = c.missionStart[m][0] + 3; c.g.z = c.missionStart[m][1] + 3;
                float t = 0; boolean done = false;
                while (t < 240) {
                    bot.fill(c, in);
                    c.tick(in);
                    for (City.Event e : c.events) {
                        if (e.type == City.EV_MISSION_DONE && e.a == m) done = true;
                        if (e.type == City.EV_DIED) deaths++;
                        if (e.type == City.EV_MISSION_FAIL) fails++;
                    }
                    c.events.clear();
                    t += City.DT;
                    if (done) break;
                }
                if (done) { wins++; total += t; }
            }
            System.out.printf("%-24s %7d%% %8.1fs %8d %8d%n", City.MISSION_NAMES[m], wins * 100 / runs,
                    wins > 0 ? total / wins : 0f, deaths, fails);
        }
    }
}
