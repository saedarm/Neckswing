package com.saedarm.neckswing;

import java.util.Arrays;

/**
 * Street-level pathfinding for poachers. The city is a grid of 3 m cells; buildings are blocked.
 * A breadth-first "distance to the giraffe" field is rebuilt a couple of times a second and
 * every poacher just walks downhill on it, so they go around buildings instead of into them.
 */
public final class Nav {
    public static final float CELL = 3f;
    public final int n;
    private final boolean[] blocked;
    private final short[] dist;
    private final int[] queue;
    private static final short FAR = Short.MAX_VALUE;

    public Nav(City c) {
        n = (int) Math.ceil(City.SIZE / CELL);
        blocked = new boolean[n * n];
        dist = new short[n * n];
        queue = new int[n * n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            float x = (i + 0.5f) * CELL, z = (j + 0.5f) * CELL;
            for (City.Building b : c.buildings) if (b.contains(x, z, 1.2f)) { blocked[i * n + j] = true; break; }
        }
        Arrays.fill(dist, FAR);
    }

    public int cell(float v) { return Math.max(0, Math.min(n - 1, (int) (v / CELL))); }
    public boolean blockedAt(float x, float z) { return blocked[cell(x) * n + cell(z)]; }

    /** rebuild the field toward (x, z). If that spot is a roof, start from the nearest street cells. */
    public void build(float x, float z) {
        Arrays.fill(dist, FAR);
        int head = 0, tail = 0;
        int ci = cell(x), cj = cell(z);
        if (!blocked[ci * n + cj]) { dist[ci * n + cj] = 0; queue[tail++] = ci * n + cj; }
        else {
            // seed a ring of open cells around the building the target is standing on
            for (int r = 1; r < 30 && tail == 0; r++)
                for (int di = -r; di <= r; di++) for (int dj = -r; dj <= r; dj++) {
                    if (Math.abs(di) != r && Math.abs(dj) != r) continue;
                    int i = ci + di, j = cj + dj;
                    if (i < 0 || j < 0 || i >= n || j >= n || blocked[i * n + j]) continue;
                    if (dist[i * n + j] == 0) continue;
                    dist[i * n + j] = 0; queue[tail++] = i * n + j;
                }
        }
        while (head < tail) {
            int c = queue[head++], i = c / n, j = c % n;
            short d = (short) (dist[c] + 1);
            if (i > 0) tail = visit(c - n, d, tail);
            if (i < n - 1) tail = visit(c + n, d, tail);
            if (j > 0) tail = visit(c - 1, d, tail);
            if (j < n - 1) tail = visit(c + 1, d, tail);
        }
    }

    private int visit(int c, short d, int tail) {
        if (blocked[c] || dist[c] <= d) return tail;
        dist[c] = d; queue[tail] = c; return tail + 1;
    }

    /** writes the direction to walk (unit vector) into out. Returns false if there's no path. */
    public boolean step(float x, float z, float[] out) {
        int i = cell(x), j = cell(z);
        int best = dist[i * n + j], bi = i, bj = j;
        for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) {
            int a = i + di, b = j + dj;
            if ((di == 0 && dj == 0) || a < 0 || b < 0 || a >= n || b >= n) continue;
            // no cutting diagonally past a corner
            if (di != 0 && dj != 0 && (blocked[(i + di) * n + j] || blocked[i * n + j + dj])) continue;
            int d = dist[a * n + b];
            if (d < best) { best = d; bi = a; bj = b; }
        }
        if (best == FAR || (bi == i && bj == j)) return false;
        float dx = (bi + 0.5f) * CELL - x, dz = (bj + 0.5f) * CELL - z, l = (float) Math.sqrt(dx * dx + dz * dz);
        if (l < 1e-3f) return false;
        out[0] = dx / l; out[1] = dz / l;
        return true;
    }
}
