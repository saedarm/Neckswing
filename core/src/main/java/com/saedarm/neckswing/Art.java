package com.saedarm.neckswing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

/** Every model in the game, built in code. Instances are pooled per model and reset each frame. */
public final class Art implements Disposable {
    private static final long ATTR = Usage.Position | Usage.Normal | Usage.TextureCoordinates;
    private final ModelBuilder mb = new ModelBuilder();
    private final Array<Disposable> owned = new Array<>();

    public Model city, ring, beacon, coil, shadow, shieldBubble, crate;
    public Model spotted, spottedDark, hoof, tongue, ossicone, eye;
    public Model skin, vest, vestBrute, coat, pants, hat, gun, boot;
    public final Model[] carBody = new Model[6];
    public Model carCabin, wheel, glassDark, light, tail;
    public Model bullet, spark, debris, leaf, star, smoke;
    public Model grab, laser, fire, wreck, goldLeaf, beaconBlue, beaconPurple, ghillie, rifle, hair, checker;
    public final Model[] shirts = new Model[6], skins = new Model[3];
    public static final Color[] SHIRT_COLORS = {
        new Color(0.20f, 0.45f, 0.85f, 1), new Color(0.85f, 0.25f, 0.45f, 1), new Color(0.95f, 0.85f, 0.30f, 1),
        new Color(0.30f, 0.70f, 0.45f, 1), new Color(0.55f, 0.35f, 0.75f, 1), new Color(0.92f, 0.92f, 0.90f, 1)};

    public static final Color[] CAR_COLORS = {
        new Color(0.85f, 0.15f, 0.12f, 1), new Color(0.15f, 0.35f, 0.85f, 1), new Color(0.95f, 0.8f, 0.15f, 1),
        new Color(0.9f, 0.9f, 0.9f, 1), new Color(0.15f, 0.15f, 0.17f, 1), new Color(0.2f, 0.65f, 0.35f, 1)};
    public static final Color[] BUILDING_TINTS = {
        new Color(0.86f, 0.80f, 0.68f, 1), new Color(0.62f, 0.68f, 0.76f, 1), new Color(0.72f, 0.40f, 0.32f, 1),
        new Color(0.45f, 0.70f, 0.72f, 1), new Color(0.90f, 0.75f, 0.55f, 1), new Color(0.40f, 0.43f, 0.50f, 1)};

    public Art(City c) {
        Texture spots = tex(spotsPixmap(new Random(4), new Color(0.93f, 0.72f, 0.36f, 1), new Color(0.50f, 0.27f, 0.10f, 1)));
        Texture windows = tex(windowsPixmap(new Random(9)));
        windows.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        windows.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

        spotted = mb.createBox(1, 1, 1, new Material(TextureAttribute.createDiffuse(spots)), ATTR);
        spottedDark = box(new Color(0.42f, 0.24f, 0.10f, 1));
        hoof = box(new Color(0.18f, 0.13f, 0.10f, 1));
        tongue = box(new Color(0.45f, 0.20f, 0.55f, 1));
        ossicone = box(new Color(0.35f, 0.22f, 0.12f, 1));
        eye = box(new Color(0.05f, 0.05f, 0.05f, 1));
        skin = box(new Color(0.85f, 0.65f, 0.50f, 1));
        vest = box(new Color(0.95f, 0.50f, 0.10f, 1));
        vestBrute = box(new Color(0.70f, 0.12f, 0.12f, 1));
        coat = box(new Color(0.92f, 0.90f, 0.84f, 1));
        pants = box(new Color(0.35f, 0.33f, 0.22f, 1));
        hat = box(new Color(0.75f, 0.66f, 0.45f, 1));
        gun = box(new Color(0.12f, 0.12f, 0.13f, 1));
        boot = box(new Color(0.20f, 0.15f, 0.10f, 1));
        for (int i = 0; i < 6; i++) carBody[i] = box(CAR_COLORS[i]);
        carCabin = box(new Color(0.75f, 0.80f, 0.85f, 1));
        glassDark = box(new Color(0.15f, 0.20f, 0.28f, 1));
        wheel = box(new Color(0.08f, 0.08f, 0.08f, 1));
        light = emissive(new Color(1f, 0.95f, 0.7f, 1));
        tail = emissive(new Color(0.9f, 0.1f, 0.1f, 1));
        bullet = emissive(new Color(1f, 0.9f, 0.3f, 1));
        spark = emissive(new Color(1f, 1f, 0.8f, 1));
        debris = box(new Color(0.55f, 0.55f, 0.55f, 1));
        leaf = box(new Color(0.30f, 0.65f, 0.20f, 1));
        star = emissive(new Color(1f, 0.9f, 0.2f, 1));
        smoke = translucent(new Color(0.85f, 0.85f, 0.85f, 1), 0.5f);
        grab = emissive(new Color(0.3f, 1f, 0.45f, 1));
        laser = mb.createBox(1, 1, 1, new Material(ColorAttribute.createDiffuse(1f, 0.1f, 0.1f, 1), ColorAttribute.createEmissive(1f, 0.1f, 0.1f, 1),
                new BlendingAttribute(0.75f)), Usage.Position | Usage.Normal);
        fire = emissive(new Color(1f, 0.5f, 0.1f, 1));
        wreck = box(new Color(0.12f, 0.11f, 0.10f, 1));
        goldLeaf = emissive(new Color(1f, 0.82f, 0.15f, 1));
        ghillie = box(new Color(0.30f, 0.38f, 0.22f, 1));
        rifle = box(new Color(0.08f, 0.08f, 0.09f, 1));
        hair = box(new Color(0.20f, 0.13f, 0.08f, 1));
        checker = emissive(new Color(1f, 1f, 1f, 1));
        for (int i = 0; i < 6; i++) shirts[i] = box(SHIRT_COLORS[i]);
        Color[] sk = {new Color(0.95f, 0.78f, 0.62f, 1), new Color(0.70f, 0.50f, 0.35f, 1), new Color(0.40f, 0.27f, 0.18f, 1)};
        for (int i = 0; i < 3; i++) skins[i] = box(sk[i]);
        shadow = mb.createCylinder(1, 0.05f, 1, 16, new Material(ColorAttribute.createDiffuse(0, 0, 0, 1),
                new BlendingAttribute(0.35f)), Usage.Position | Usage.Normal);
        shieldBubble = mb.createSphere(1, 1, 1, 18, 12, new Material(ColorAttribute.createDiffuse(0.3f, 0.7f, 1f, 1),
                new BlendingAttribute(0.3f), IntAttribute.createCullFace(GL20.GL_NONE)), Usage.Position | Usage.Normal);
        beacon = mb.createCylinder(1, 1, 1, 20, new Material(ColorAttribute.createDiffuse(1f, 0.85f, 0.1f, 1),
                ColorAttribute.createEmissive(0.6f, 0.5f, 0.05f, 1), new BlendingAttribute(0.4f), IntAttribute.createCullFace(GL20.GL_NONE)),
                Usage.Position | Usage.Normal);
        beaconBlue = mb.createCylinder(1, 1, 1, 20, new Material(ColorAttribute.createDiffuse(0.2f, 0.6f, 1f, 1),
                ColorAttribute.createEmissive(0.1f, 0.35f, 0.7f, 1), new BlendingAttribute(0.4f), IntAttribute.createCullFace(GL20.GL_NONE)),
                Usage.Position | Usage.Normal);
        beaconPurple = mb.createCylinder(1, 1, 1, 20, new Material(ColorAttribute.createDiffuse(0.75f, 0.3f, 1f, 1),
                ColorAttribute.createEmissive(0.45f, 0.15f, 0.65f, 1), new BlendingAttribute(0.4f), IntAttribute.createCullFace(GL20.GL_NONE)),
                Usage.Position | Usage.Normal);
        ring = ringModel();
        coil = coilModel();
        crate = crateModel();
        city = cityModel(c, windows);
        for (Model m : new Model[]{spotted, spottedDark, hoof, tongue, ossicone, eye, skin, vest, vestBrute, coat, pants, hat, gun, boot,
                carCabin, glassDark, wheel, light, tail, bullet, spark, debris, leaf, star, smoke, shadow, shieldBubble, beacon, ring, coil, crate, city,
                grab, laser, fire, wreck, goldLeaf, beaconBlue, beaconPurple, ghillie, rifle, hair, checker})
            owned.add(m);
        for (Model m : carBody) owned.add(m);
        for (Model m : shirts) owned.add(m);
        for (Model m : skins) owned.add(m);
    }

    private Texture tex(Pixmap p) { Texture t = new Texture(p, true); p.dispose(); owned.add(t); return t; }
    private Model box(Color c) { return mb.createBox(1, 1, 1, new Material(ColorAttribute.createDiffuse(c)), Usage.Position | Usage.Normal); }
    private Model emissive(Color c) {
        return mb.createBox(1, 1, 1, new Material(ColorAttribute.createDiffuse(c), ColorAttribute.createEmissive(c)), Usage.Position | Usage.Normal);
    }
    private Model translucent(Color c, float a) {
        return mb.createBox(1, 1, 1, new Material(ColorAttribute.createDiffuse(c), new BlendingAttribute(a)), Usage.Position | Usage.Normal);
    }

    // ------------------------------------------------------------ textures
    private static Pixmap spotsPixmap(Random r, Color base, Color spot) {
        int n = 64;
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        // giraffe pattern: voronoi cells with tan borders
        int cells = 9;
        float[] cx = new float[cells], cy = new float[cells];
        for (int i = 0; i < cells; i++) { cx[i] = r.nextFloat() * n; cy[i] = r.nextFloat() * n; }
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float d1 = 1e9f, d2 = 1e9f;
            for (int i = 0; i < cells; i++) for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++) {
                float dx = x - (cx[i] + ox * n), dy = y - (cy[i] + oy * n), d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) d2 = d;
            }
            p.setColor(d2 - d1 < 2.6f ? base : spot);
            p.drawPixel(x, y);
        }
        return p;
    }

    private static Pixmap windowsPixmap(Random r) {
        int n = 64;
        Pixmap p = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        p.setColor(1, 1, 1, 1); p.fill();
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
            float lit = r.nextFloat();
            if (lit < 0.25f) p.setColor(0.95f, 0.88f, 0.55f, 1);
            else p.setColor(0.20f + lit * 0.1f, 0.26f + lit * 0.1f, 0.36f + lit * 0.12f, 1);
            p.fillRectangle(i * 16 + 3, j * 16 + 3, 10, 10);
        }
        return p;
    }

    // ------------------------------------------------------------ static city
    private static final long PN = Usage.Position | Usage.Normal;
    private MeshPartBuilder flat(String id, float r, float g, float b) {
        return mb.part(id, GL20.GL_TRIANGLES, PN, new Material(ColorAttribute.createDiffuse(r, g, b, 1)));
    }
    /** a flat quad split into tiles, so per-vertex fog doesn't wash it out */
    private static void tiledGround(MeshPartBuilder p, float x0, float z0, float x1, float z1, float y, float tile) {
        for (float x = x0; x < x1; x += tile) for (float z = z0; z < z1; z += tile) {
            float xe = Math.min(x1, x + tile), ze = Math.min(z1, z + tile);
            p.rect(x, y, ze, xe, y, ze, xe, y, z, x, y, z, 0, 1, 0);
        }
    }

    // Each part is filled completely before the next part starts: parts with the same
    // vertex attributes share one mesh, so writing to an earlier part later would land
    // in the wrong material.
    private Model cityModel(City c, Texture windows) {
        mb.begin();
        float e = 400, S = City.SIZE;
        MeshPartBuilder p = flat("asphalt", 0.24f, 0.24f, 0.26f);
        tiledGround(p, -2, -2, S + 2, S + 2, 0, 24);

        p = flat("outer", 0.40f, 0.58f, 0.30f);
        tiledGround(p, -e, -e, -2, S + e, 0.02f, 60);
        tiledGround(p, S + 2, -e, S + e, S + e, 0.02f, 60);
        tiledGround(p, -2, -e, S + 2, -2, 0.02f, 60);
        tiledGround(p, -2, S + 2, S + 2, S + e, 0.02f, 60);

        p = flat("sidewalk", 0.66f, 0.65f, 0.62f);
        for (int i = 0; i < City.BLOCKS; i++) for (int j = 0; j < City.BLOCKS; j++) {
            float x0 = i * City.BLOCK + City.STREET / 2 - 2, z0 = j * City.BLOCK + City.STREET / 2 - 2, w = City.LOT + 4;
            BoxShapeBuilder.build(p, x0 + w / 2, 0.12f, z0 + w / 2, w, 0.24f, w);
        }
        for (int pass = 0; pass < 2; pass++) {
            int want = pass == 0 ? City.LOT_PARK : City.LOT_ZOO;
            p = pass == 0 ? flat("grass", 0.34f, 0.60f, 0.26f) : flat("sand", 0.86f, 0.76f, 0.50f);
            for (int i = 0; i < City.BLOCKS; i++) for (int j = 0; j < City.BLOCKS; j++) {
                if (c.lotType[i][j] != want) continue;
                float x0 = i * City.BLOCK + City.STREET / 2 + 0.5f, z0 = j * City.BLOCK + City.STREET / 2 + 0.5f, w = City.LOT - 1;
                tiledGround(p, x0, z0, x0 + w, z0 + w, 0.27f, 13);
            }
        }
        // lane dashes
        p = flat("dash", 0.95f, 0.8f, 0.2f);
        for (int k = 1; k < City.BLOCKS; k++) {
            float L = k * City.BLOCK;
            for (float a = 4; a < S - 4; a += 8) {
                if (Math.abs(a + 2 - Math.round((a + 2) / City.BLOCK) * City.BLOCK) < City.STREET / 2 + 1) continue;
                p.rect(L - 0.2f, 0.03f, a + 4, L + 0.2f, 0.03f, a + 4, L + 0.2f, 0.03f, a, L - 0.2f, 0.03f, a, 0, 1, 0);
                p.rect(a, 0.03f, L - 0.2f, a, 0.03f, L + 0.2f, a + 4, 0.03f, L + 0.2f, a + 4, 0.03f, L - 0.2f, 0, 1, 0);
            }
        }
        // building walls with tiled windows, one part per tint
        float tile = 14f;
        for (int tint = 0; tint < 6; tint++) {
            MeshPartBuilder w = mb.part("walls" + tint, GL20.GL_TRIANGLES, ATTR,
                    new Material(TextureAttribute.createDiffuse(windows), ColorAttribute.createDiffuse(BUILDING_TINTS[tint])));
            for (City.Building b : c.buildings) {
                if (b.color != tint) continue;
                float h = b.h, x0 = b.x0, x1 = b.x1, z0 = b.z0, z1 = b.z1;
                float uw = (x1 - x0) / tile, ud = (z1 - z0) / tile;
                // split walls into floors-ish bands so fog stays smooth on tall towers
                int bands = Math.max(1, (int) (h / 20));
                for (int k = 0; k < bands; k++) {
                    float y0 = h * k / bands, y1 = h * (k + 1) / bands, v0 = y0 / tile, v1 = y1 / tile;
                    w.setUVRange(0, v1, uw, v0); w.rect(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);
                    w.setUVRange(0, v1, uw, v0); w.rect(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);
                    w.setUVRange(0, v1, ud, v0); w.rect(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
                    w.setUVRange(0, v1, ud, v0); w.rect(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);
                }
            }
        }
        p = flat("roof", 0.50f, 0.50f, 0.52f);
        for (City.Building b : c.buildings) p.rect(b.x0, b.h, b.z1, b.x1, b.h, b.z1, b.x1, b.h, b.z0, b.x0, b.h, b.z0, 0, 1, 0);
        p = flat("ledge", 0.80f, 0.79f, 0.75f);
        for (City.Building b : c.buildings) {
            float h = b.h, x0 = b.x0, x1 = b.x1, z0 = b.z0, z1 = b.z1;
            BoxShapeBuilder.build(p, (x0 + x1) / 2, h + 0.4f, z0 + 0.3f, x1 - x0, 0.8f, 0.6f);
            BoxShapeBuilder.build(p, (x0 + x1) / 2, h + 0.4f, z1 - 0.3f, x1 - x0, 0.8f, 0.6f);
            BoxShapeBuilder.build(p, x0 + 0.3f, h + 0.4f, (z0 + z1) / 2, 0.6f, 0.8f, z1 - z0);
            BoxShapeBuilder.build(p, x1 - 0.3f, h + 0.4f, (z0 + z1) / 2, 0.6f, 0.8f, z1 - z0);
            // rooftop clutter: AC unit and a water tank on bigger roofs
            if (x1 - x0 > 18) BoxShapeBuilder.build(p, x0 + 5, h + 1.2f, z0 + 5, 4, 2.4f, 3);
        }
        p = flat("garden", 0.28f, 0.62f, 0.22f);
        for (City.Building b : c.buildings) {
            if (!b.garden) continue;
            float h = b.h, x0 = b.x0, x1 = b.x1, z0 = b.z0, z1 = b.z1;
            BoxShapeBuilder.build(p, (x0 + x1) / 2, h + 0.3f, (z0 + z1) / 2, (x1 - x0) * 0.6f, 0.6f, (z1 - z0) * 0.6f);
            for (int k = 0; k < 4; k++)
                BoxShapeBuilder.build(p, x0 + (x1 - x0) * (0.3f + 0.4f * (k % 2)), h + 1.4f, z0 + (z1 - z0) * (0.3f + 0.4f * (k / 2)), 1.8f, 1.8f, 1.8f);
        }
        // acacia trees
        p = flat("trunk", 0.42f, 0.30f, 0.18f);
        for (City.Tree t : c.trees) {
            BoxShapeBuilder.build(p, t.x, t.h * 0.45f, t.z, 0.7f, t.h * 0.9f, 0.7f);
            BoxShapeBuilder.build(p, t.x + 0.9f, t.h * 0.75f, t.z, 2.0f, 0.4f, 0.4f);
        }
        p = flat("canopy", 0.36f, 0.58f, 0.20f);
        for (City.Tree t : c.trees) {
            BoxShapeBuilder.build(p, t.x, t.h, t.z, t.r * 2, 1.2f, t.r * 1.8f);
            BoxShapeBuilder.build(p, t.x + 0.5f, t.h + 0.8f, t.z - 0.3f, t.r * 1.4f, 0.8f, t.r * 1.3f);
        }
        // zoo fence and gate
        p = flat("fence", 0.55f, 0.38f, 0.22f);
        float zx0 = City.ZOO_I * City.BLOCK + City.STREET / 2 + 1, zz0 = City.ZOO_J * City.BLOCK + City.STREET / 2 + 1, zl = City.LOT - 2;
        for (int side = 0; side < 4; side++) {
            for (float a = 0; a <= zl; a += 3) {
                float px = side < 2 ? zx0 + a : (side == 2 ? zx0 : zx0 + zl);
                float pz = side < 2 ? (side == 0 ? zz0 : zz0 + zl) : zz0 + a;
                if (side == 0 && Math.abs(a - zl / 2) < 6) continue;
                BoxShapeBuilder.build(p, px, 1.2f, pz, 0.4f, 2.4f, 0.4f);
            }
            if (side < 2) {
                float pz = side == 0 ? zz0 : zz0 + zl;
                if (side == 0) {
                    BoxShapeBuilder.build(p, zx0 + (zl / 2 - 6) / 2, 1.8f, pz, zl / 2 - 6, 0.3f, 0.2f);
                    BoxShapeBuilder.build(p, zx0 + zl - (zl / 2 - 6) / 2, 1.8f, pz, zl / 2 - 6, 0.3f, 0.2f);
                } else BoxShapeBuilder.build(p, zx0 + zl / 2, 1.8f, pz, zl, 0.3f, 0.2f);
            } else {
                float px = side == 2 ? zx0 : zx0 + zl;
                BoxShapeBuilder.build(p, px, 1.8f, zz0 + zl / 2, 0.2f, 0.3f, zl);
            }
        }
        BoxShapeBuilder.build(p, zx0 + zl / 2 - 6, 4, zz0, 0.8f, 8, 0.8f);
        BoxShapeBuilder.build(p, zx0 + zl / 2 + 6, 4, zz0, 0.8f, 8, 0.8f);
        BoxShapeBuilder.build(p, zx0 + zl / 2, 8.2f, zz0, 13.5f, 1.6f, 0.6f);
        return mb.end();
    }

    private Model ringModel() {
        mb.begin();
        MeshPartBuilder p = mb.part("ring", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
                new Material(ColorAttribute.createDiffuse(0.2f, 0.9f, 1f, 1), ColorAttribute.createEmissive(0.1f, 0.5f, 0.6f, 1)));
        Matrix4 m = new Matrix4();
        int n = 20;
        for (int i = 0; i < n; i++) {
            float a = i * MathUtils.PI2 / n;
            m.setToTranslation(MathUtils.cos(a) * 5.5f, MathUtils.sin(a) * 5.5f, 0).rotateRad(Vector3.Z, a);
            p.setVertexTransform(m);
            BoxShapeBuilder.build(p, 0, 0, 0, 0.7f, 1.9f, 0.7f);
        }
        return mb.end();
    }

    private Model coilModel() {
        mb.begin();
        MeshPartBuilder p = mb.part("coil", GL20.GL_TRIANGLES, ATTR, spotted.materials.first());
        Matrix4 m = new Matrix4();
        int n = 14;
        for (int i = 0; i < n * 2; i++) {
            float a = i * MathUtils.PI2 / n;
            m.setToTranslation(MathUtils.cos(a) * 1.1f, (i - n) * 0.07f, MathUtils.sin(a) * 1.1f).rotateRad(Vector3.Y, -a);
            p.setVertexTransform(m);
            BoxShapeBuilder.build(p, 0, 0, 0, 0.55f, 0.55f, 0.6f);
        }
        return mb.end();
    }

    private Model crateModel() {
        mb.begin();
        MeshPartBuilder wood = mb.part("wood", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(0.65f, 0.45f, 0.25f, 1)));
        BoxShapeBuilder.build(wood, 0, 0.8f, 0, 1.8f, 1.6f, 1.8f);
        MeshPartBuilder lv = mb.part("leaves", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(0.3f, 0.7f, 0.2f, 1)));
        BoxShapeBuilder.build(lv, 0, 1.8f, 0, 1.6f, 0.6f, 1.6f);
        BoxShapeBuilder.build(lv, 0.3f, 2.2f, -0.2f, 0.9f, 0.5f, 0.9f);
        return mb.end();
    }

    // ------------------------------------------------------------ drawing with pooled instances
    private final java.util.IdentityHashMap<Model, Array<ModelInstance>> pools = new java.util.IdentityHashMap<>();
    private final java.util.IdentityHashMap<Model, int[]> used = new java.util.IdentityHashMap<>();
    public ModelBatch batch; public Environment env;

    public void resetPools() { for (int[] u : used.values()) u[0] = 0; }

    public ModelInstance inst(Model m) {
        Array<ModelInstance> pool = pools.get(m);
        if (pool == null) { pool = new Array<>(); pools.put(m, pool); used.put(m, new int[1]); }
        int[] u = used.get(m);
        if (u[0] >= pool.size) pool.add(new ModelInstance(m));
        return pool.get(u[0]++);
    }

    private final Vector3 tv = new Vector3(), tv2 = new Vector3(), sc = new Vector3();
    private final Quaternion tq = new Quaternion();

    /** draw model m with a parent transform, local offset, local scale. */
    public void part(Model m, Matrix4 parent, float x, float y, float z, float sx, float sy, float sz) {
        ModelInstance i = inst(m);
        i.transform.set(parent).translate(x, y, z).scale(sx, sy, sz);
        batch.render(i, env);
    }
    public void part(Model m, Matrix4 parent, float x, float y, float z, float sx, float sy, float sz, Vector3 axis, float deg, float px, float py, float pz) {
        // rotate about a pivot (px,py,pz) in parent space, then place
        ModelInstance i = inst(m);
        i.transform.set(parent).translate(px, py, pz).rotate(axis, deg).translate(x - px, y - py, z - pz).scale(sx, sy, sz);
        batch.render(i, env);
    }
    public void at(Model m, float x, float y, float z, float sx, float sy, float sz) {
        ModelInstance i = inst(m);
        i.transform.setToTranslation(x, y, z).scale(sx, sy, sz);
        batch.render(i, env);
    }
    public void at(Model m, Matrix4 t) { ModelInstance i = inst(m); i.transform.set(t); batch.render(i, env); }

    /** a box stretched from a to b */
    public void segment(Model m, float ax, float ay, float az, float bx, float by, float bz, float thick) {
        tv.set(bx - ax, by - ay, bz - az);
        float len = tv.len();
        if (len < 1e-3f) return;
        tv.scl(1 / len);
        tq.setFromCross(Vector3.Z, tv);
        ModelInstance i = inst(m);
        tv2.set((ax + bx) / 2, (ay + by) / 2, (az + bz) / 2);
        i.transform.set(tv2, tq, sc.set(thick, thick, len + thick * 0.6f));
        batch.render(i, env);
    }

    @Override public void dispose() { for (Disposable d : owned) d.dispose(); }
}
