# Neckswing

A libGDX desktop game where you play a giraffe who gets around a city by wrapping his neck around buildings and swinging, Spider-Man style. You run, swing, steal cars, and beat up poachers with giraffe powers: a bellow that stuns everyone nearby, an 18-inch tongue that yanks people over, a kick that launches people and cars, a lookout mode that shows enemies through walls, and a leaf munch that heals. Respect you earn buys upgrades in a 15-skill tree. The city has pedestrians, cars that take damage and blow up, swing races, street races, and 25 golden leaves hidden on rooftops. On Windows you can trigger the powers by saying them out loud, and the giraffe talks back in a low synthesized voice.

## How a game works

You start outside the zoo in the middle of Savanna City. A yellow beacon marks where the next mission starts; walk into it. There are five story missions. Finish them all and the game switches to free roam, where poacher crimes pop up around the city every 25 to 45 seconds. Dying fails the current mission or race and puts you back at the zoo. Everything saves automatically: mission, respect, skills, leaves, and race best times.

| # | Mission | What you do | Limit |
|---|---|---|---|
| 1 | Stretch Your Neck | Swing through 7 rings over a street. Each ring adds 3 s. | 90 s |
| 2 | Poacher Problem | Clear 8 poachers, 2 brutes and a rooftop sniper out of the east park. | none |
| 3 | Hot Wheels, Long Neck | Get in any car and drive to the marker. Then survive the ambush (with a sniper). | 70 s to arrive |
| 4 | Leaf Delivery | Grab the crate off the tallest roof (touch it or Tongue it), carry it to the zoo. Rooftop shooters and 3 snipers spawn along the way. | 100 s after grabbing |
| 5 | The Taxidermist | Boss fight. His shield takes 5% damage. Bellow drops it for 5 s. One sniper covers him. | none |

### Seeing where the neck grabs

Before you swing, a green spinning diamond shows exactly where the neck will wrap, with a dotted line back to your head. It's the same calculation the swing uses, so it never lies. If the spot is off screen, a green arrow at the edge of the screen points to it; if it's on screen, a green bracket frames it. No marker means there's nothing in reach, and clicking will just get you "Nothing to wrap my neck around!"

### Moves and powers

| Move | Effect | Cooldown |
|---|---|---|
| Neck swing | Wraps the neck around the green marker (nearest good point on a building or tree ahead and above you, up to 50 m). Hold to swing, let go to fling. Shift reels in. If the neck pulls you into a wall you climb it. | none |
| Neck Zip (skill) | Q flies you straight to the green marker at 62 m/s and pops you over the ledge. Space cancels. | 1.2 s |
| Body check | Swinging into a poacher above 16 m/s hits him for 1.8 x your speed. | none |
| Neck spin | Spins the neck in a 7.5 m circle. 32 damage plus 1.6 x your running speed. | 0.5 s |
| Neck slam | Attack while 6 m+ in the air: dive straight down, 70 damage in a 13 m circle, flips and damages cars. | none |
| 1 Bellow | Stuns everyone within 24 m for 4 s, deletes bullets in flight, cancels sniper aim, drops the boss shield. | 8 s |
| 2 Tongue | Yanks the nearest poacher in front of you (20 m), takes his gun for 5 s. Also grabs the crate. | 2.5 s |
| 3 Kick | 85 damage, launches people. Kick a car and it flies and takes 40 damage. | 1.2 s |
| 4 Lookout | Red markers over every poacher through walls, all poachers on the minimap, 8 s. | 15 s |
| 5 Munch | Heal 40 by eating from a tree canopy or a rooftop garden. Doesn't use the cooldown if there's nothing to eat. | 4 s |

Health is 100 and regenerates 5 per second after 4 seconds without getting hit. Bullets miss more the faster you're moving.

## Skill tree

Press K (or say "skills", or use the pause menu). Respect is the currency: you earn it for hits, takedowns, missions, crimes, races and golden leaves, and lose some for hurting bystanders. Each branch unlocks top to bottom. Costs are 100, 200, 350, 550, 800 per tier, 6,000 for everything.

| Tier | Neck | Body | Powers |
|---|---|---|---|
| 1 (100) | Long Neck: +10 m reach | Thick Hide: +25 HP | Long Tongue: +50% range |
| 2 (200) | Swing Pump: build speed 30% faster | Long Legs: run 20% faster | Big Lungs: Bellow +40% radius, 25% faster recharge |
| 3 (350) | Strong Reel: reel in 50% faster | Fast Healer: regen 2.5x, starts after 2 s | Leaf Stomach: Munch heals 75 |
| 4 (550) | Longer Neck: another +10 m (70 m) | Hard Hooves: Kick +50%, Slam circle +30% | Eagle Eye: Lookout 14 s; snipers always on minimap, golden leaves during Lookout |
| 5 (800) | Neck Zip (Q) | Thicker Hide: +50 HP (175 total) | Quick Reflexes: every power recharges 20% faster |

All the numbers come from small methods on `City` (`neckMax()`, `hpMax()`, `cooldown(power)`...), so a new skill is one line in `Skills.java` and one line where the stat is read.

## Poachers

| Enemy | HP | What it does |
|---|---|---|
| Poacher (orange vest) | 50 | Keeps 18 m away, sidesteps while shooting, fires 3-round bursts (5 each) with a 2-3 s reload. Falls back for a few seconds when badly hurt, then comes again. |
| Brute (red vest) | 140 | Runs at you and punches (14). From 5-16 m with a clear line he shouts "CHARGE!" and runs straight at you at 24 m/s for 0.75 s; if it connects it's 25 damage and knocks you flying. Dodge sideways. |
| Sniper (green ghillie suit) | 40 | Stands at a roof edge up to 170 m away. Paints you with a thin red laser for 1.25 s while tracking you, then it thickens and blinks: the aim is now locked. 0.35 s later he fires (22 damage). Move after the lock and he misses. Bellow, getting hit, or losing line of sight resets him. |
| The Taxidermist (white coat) | 1100 | Shield bubble, fires 3 bullets at once |

What makes them smarter than before:
- **They need to see you to shoot.** Every poacher checks line of sight a few times a second (a ray against every building). No more shots through walls; break line of sight to stop the bullets.
- **They walk around buildings.** `Nav.java` keeps a 3 m grid of the streets and rebuilds a "distance to the giraffe" map 2.5 times a second. Poachers who can't see you walk downhill on that map, so they come around corners and flank instead of walking into walls. If you're on a roof they gather at the base of your building.
- **They call each other.** The first one to spot you shouts ("There he is!", "Flank him!") and everyone in his gang within 50 m joins in. Mission poachers hunt you within 90 m even without seeing you; street-crime crews only react to what they see.
- **They have a reaction time.** After losing sight of you there's a half-second pause before they fire again when you reappear.

## Cars

Every car has 100 HP. It shows a bar on the HUD when you're driving.

| Damage | Amount |
|---|---|
| Hitting a wall above 10 m/s | 0.7 x speed (about 32 at top speed) |
| Ramming another car | 0.7 x closing speed to you, 1.1 x to them |
| Landing from a flip | 1.2 x fall speed |
| Bullet | 0.6 x bullet damage (you take half the bullet too) |
| Kick / Neck Slam | 40 (60 with Hard Hooves) / 35 |
| Explosion nearby | 55 and a flip |

Below 40 HP the car smokes, the roof sags and the headlights go out. At 0 it catches fire ("Get out! (E)") and explodes 2.5 s later: 95 damage to poachers within 10 m, up to 38 to you on foot (and a knockback), 30 to you if you're still inside, 55 to nearby cars (so they can chain). The wreck smokes for 25 s, then quietly respawns as a new car somewhere you can't see.

## Pedestrians

70 people walk the sidewalks. They follow a grid of sidewalk corners, wait at the curb when a car is coming through the crosswalk, and AI traffic stops for them.

- Gunshots, explosions, sniper fire, a Bellow, a Neck Slam, a honk, or poachers spotting you make them run away with their arms up (and scream).
- Cars knock them flying. They lie there for 3 s, get up and run.
- Hurting them costs respect: -5 for running one over, -10 for a kick, spin, slam or swinging into them.
- They talk when you're near: "Is that a giraffe?!", "Does he have a license for that neck?", "Get 'em, giraffe!" during fights, "I'm suing the zoo!" when you knock them over.

## Side activities

Start beacons only show when no mission is running. Backspace (or "cancel", or the pause menu) quits a race. Taking more than twice the bronze time fails it.

| Activity | Start | What you do | Medals (gold / silver / bronze) |
|---|---|---|---|
| Rooftop Rush, Downtown Dash, Skyline Sprint | Blue beacons | Swing races: fly through 10 rings strung down the streets. The last one is gold. | 32 / 42 / 61 s |
| Savanna 500, Rush Hour Run | Purple beacons | Street races: drive into the start beacon in a car, then through 9 intersections. You have to be driving to hit a checkpoint. | 24 / 32 / 46 s |
| Golden leaves | Everywhere | 25 spinning gold leaves: 18 on rooftops, 7 on tree tops. A tall glint shows within 90 m. +50 each, +1000 for all of them. | |

Race rewards: first gold 400, silver 250, bronze 120. Upgrading a medal pays the difference; repeat finishes pay 20. The race clock changes color with the medal you're on pace for. Best times are saved.

## Controls

| Key | Does | Or say |
|---|---|---|
| W A S D | Run / drive | |
| Mouse | Look | |
| Hold Left Mouse (or Left Ctrl) | Neck swing to the green marker | |
| Left Shift | Reel the neck in | |
| Q | Neck Zip (needs the skill) | "zip" |
| Space | Jump, hop out of a car, cancel a zip | "jump" |
| Right Mouse or F | Neck spin (slam in the air) | "spin" / "slam" |
| E | Get in / out of a car | "drive" / "get out" |
| 1 to 5 | Bellow, Tongue, Kick, Lookout, Munch | "bellow", "lick", "kick", "lookout", "munch" |
| K | Skill tree | "skills" |
| Backspace | Quit the current race | "cancel" |
| T | Giraffe says something | "talk" |
| H | Honk (scares pedestrians) | "honk" |
| Esc | Pause | "pause" |
| F11 | Fullscreen | |

## Voice

`Voice.java` runs two hidden PowerShell helpers using `System.Speech`, which ships with Windows, so there's nothing to install.

- The listener only knows a fixed list of words (the "Or say" column), so it's fast and doesn't need the internet. Anything under 55% confidence is ignored.
- The speaker reads every quip in a low, slow voice (SSML pitch x-low). While the giraffe is talking the listener ignores the mic so he doesn't trigger his own powers ("One kick. That's all a lion ever gets.").
- Both can be turned off separately in Settings. If voice is off or you're not on Windows, quips show in a speech bubble with a grunt sound, and everything works from the keyboard.
- The HUD shows the mic state and the last word it heard.

If the mic never says "Mic on", Windows probably has no English speech recognizer installed (Settings > Time & Language > Speech).

## Running it

- Windows: double-click `Play Neckswing.bat`. The first run builds the jar (needs Java 17+; it also finds Android Studio's bundled Java).
- Terminal: `./gradlew lwjgl3:run`
- Jar: `./gradlew lwjgl3:dist`, then `java -jar lwjgl3/build/libs/Neckswing.jar`

Debug flags: `--solo` (skip the title), `--mission N` (start mission N, 0-based), `--race N` (start at race N, 0-2 swing, 3-4 street, already in a car), `--respect N` (add respect), `--tree` (open the skill tree), `--autoplay` (the bot plays), `--shot file.png seconds` (save a screenshot and quit).

## Project layout

```
core/     all game code
lwjgl3/   Launcher.java and the runnable jar build
assets/   sfx/*.wav  music/*.wav  fonts/Poppins-Bold.ttf
tools/    make_sfx.py
Play Neckswing.bat
```

| File | What's in it |
|---|---|
| `City.java` | All rules: city layout, giraffe movement and swing physics, grab preview and Neck Zip, cars and car damage, poachers and snipers, bullets, pedestrians, powers, missions, crimes, races, golden leaves. No libGDX. Fixed 60 Hz tick. |
| `Skills.java` | The skill tree data: names, descriptions, costs, which ones are unlocked. |
| `Nav.java` | Street grid and the breadth-first "distance to the giraffe" field poachers follow. |
| `Bot.java` | Autopilot that fills the same `Input` the keyboard does. Swings toward objectives, finds and drives cars, fights. |
| `Sim.java` | Headless run: the bot plays each mission N times. `./gradlew core:sim -Pruns=20` |
| `Neckswing.java` | Screens (title, settings, controls, play, pause, skill tree), camera, all 3D drawing, HUD, minimap, particles, events into sound and juice. |
| `Art.java` | Every model built in code: the whole city as one static model, giraffe with a generated spot texture, poachers, snipers, pedestrians, cars and wrecks, rings, beacons, golden leaves, grab marker. Pooled instances. |
| `Sfx.java` | Positional sounds, engine and wind loops, crossfading music (menu, city, action). |
| `Voice.java` | Windows speech recognition and text-to-speech through PowerShell. |
| `Save.java` | Settings and progress (mission, respect, skills, leaves, race times) in libGDX Preferences. |
| `tools/make_sfx.py` | Synthesizes all 51 sounds and 3 music loops. Replace any wav with your own using the same name. |

## How events reach the screen

`City.tick()` never draws or plays sound. When something happens it appends an `Event(type, a, x, y, z)`: a hit, a swing attach, a quip, a car catching fire, a pedestrian shouting, a race checkpoint. `Neckswing.handleEvents()` reads the list after each tick and turns it into sounds, particles, "WHAP!" text, banners, screen shake, freeze frames, and speech. Quips and shouts are indexes into `City.QUIPS`, `City.PED_LINES` and `City.ALERT_LINES`.

## Tuning

All feel constants are at the top of `City.java`: gravity (34), run speed (18), neck length (6 to 50), swing pump (24), release boost (7), max speed (78), car speed (46) and HP (100), spin radius and damage, power cooldowns, bullet damage, sniper aim (1.6 s), lock (1.25 s), range (170 m) and damage (22). Skill costs are in `Skills.COST`. Race medal times come from the route length in `City.route()`. The city is generated from a fixed seed (1977) so it's the same every game.

## Last balance run

`core:sim -Pruns=12`, bot skill 0.5 to 1.0, no skills bought, 4 minute cap, retries after a fail count toward the time. Races and golden leaves are switched off in the sim.

| Mission | Cleared | Avg time | Deaths | Fails |
|---|---|---|---|---|
| Stretch Your Neck | 25% | 135 s | 0 | 17 |
| Poacher Problem | 75% | 131 s | 17 | 17 |
| Hot Wheels, Long Neck | 83% | 96 s | 4 | 4 |
| Leaf Delivery | 50% | 132 s | 0 | 1 |
| The Taxidermist | 66% | 102 s | 38 | 38 |

The bot never dodges sniper lasers and never buys skills, so the fights are harder for it than for a person; a few tiers of Body and Powers make a big difference. It's also bad at threading rings and landing on roofs, which is why missions 1 and 4 are low. Separate headless checks: ramming a wall 8 times sets a car on fire and it explodes 2.5 s later, throwing you out with 30 damage; poachers 99 m away on the other side of a block close to 16 m in 15 s by walking around buildings; all five races start, count every checkpoint and award medals; after 3 minutes none of the 70 pedestrians are stuck inside buildings.

## Not in this version

- Police or a wanted level for hurting pedestrians
- Wall running on foot (you only climb while the neck is pulling you)
- More than one city layout
- Controller support
- Online play
