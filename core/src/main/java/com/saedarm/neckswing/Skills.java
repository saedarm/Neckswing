package com.saedarm.neckswing;

/**
 * The giraffe skill tree: three branches of five, bought in order with respect.
 * City keeps the owned set as a bitmask; this class only holds the data.
 */
public final class Skills {
    public static final int BRANCHES = 3, TIERS = 5, COUNT = BRANCHES * TIERS;
    public static final String[] BRANCH_NAMES = {"NECK", "BODY", "POWERS"};

    // index = branch * TIERS + tier
    public static final int LONG_NECK = 0, SWING_PUMP = 1, STRONG_REEL = 2, LONGER_NECK = 3, NECK_ZIP = 4,
            THICK_HIDE = 5, LONG_LEGS = 6, FAST_HEALER = 7, HARD_HOOVES = 8, THICKER_HIDE = 9,
            LONG_TONGUE = 10, BIG_LUNGS = 11, LEAF_STOMACH = 12, EAGLE_EYE = 13, QUICK_REFLEXES = 14;

    public static final String[] NAMES = {
        "Long Neck", "Swing Pump", "Strong Reel", "Longer Neck", "Neck Zip",
        "Thick Hide", "Long Legs", "Fast Healer", "Hard Hooves", "Thicker Hide",
        "Long Tongue", "Big Lungs", "Leaf Stomach", "Eagle Eye", "Quick Reflexes",
    };
    public static final String[] DESC = {
        "Neck reaches 10 m farther, so you can grab things you couldn't before.",
        "You build speed 30% faster while swinging.",
        "Shift reels your neck in 50% faster, so you climb quicker.",
        "Another 10 m of neck. 70 m total.",
        "Press Q (or say \"zip\") to yank yourself straight to the grab marker.",
        "+25 max health.",
        "Run 20% faster on foot.",
        "Health comes back 2.5x faster, starting 2 s after a hit instead of 4.",
        "Kick does 50% more damage. Neck Slam hits a 30% wider circle.",
        "+50 more max health.",
        "Tongue reaches 50% farther.",
        "Bellow covers 40% more ground and recharges 25% faster.",
        "Munch heals 75 instead of 40.",
        "Lookout lasts 14 s and shows golden leaves and snipers on the minimap.",
        "Every power recharges 20% faster.",
    };
    public static final int[] COST = {100, 200, 350, 550, 800};

    public static int cost(int skill) { return COST[skill % TIERS]; }
    public static boolean has(int mask, int skill) { return (mask & (1 << skill)) != 0; }
    /** a skill can be bought once the one above it in the same branch is owned */
    public static boolean unlocked(int mask, int skill) { return skill % TIERS == 0 || has(mask, skill - 1); }
    public static int owned(int mask) { return Integer.bitCount(mask & ((1 << COUNT) - 1)); }
}
