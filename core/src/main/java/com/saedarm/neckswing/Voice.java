package com.saedarm.neckswing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Voice commands and a talking giraffe, using the speech engine built into Windows
 * (System.Speech through PowerShell). No extra downloads. On other systems, or if
 * Windows has no recognizer, it just reports that it's off and the keys still work.
 *
 * Two helper processes:
 *  - listener: recognizes a fixed list of phrases from the default microphone, prints "HEARD phrase"
 *  - speaker: reads lines on stdin and speaks them in a low, slow voice
 */
public final class Voice {
    public static final String[] PHRASES = {
        "bellow", "hum", "lick", "tongue", "grab", "kick", "lookout", "look", "munch", "snack", "eat",
        "spin", "neck spin", "slam", "neck slam", "jump", "drive", "car", "get out", "honk",
        "talk", "say something", "hello giraffe", "pause", "zip", "neck zip", "cancel", "skills",
    };

    private Process listener, speaker;
    private Writer speakerIn;
    private final ConcurrentLinkedQueue<String> heard = new ConcurrentLinkedQueue<>();
    public volatile String status = "Voice off";
    public volatile boolean listening, speaking;
    private volatile long quietUntil;

    public static boolean supported() { return System.getProperty("os.name", "").toLowerCase().contains("win"); }

    public void start(boolean listen, boolean speak) {
        stop();
        if (!supported()) { status = "Voice commands need Windows"; return; }
        if (listen) startListener();
        if (speak) startSpeaker();
        if (!listen) status = speak ? "Giraffe voice on, mic off" : "Voice off";
    }

    private static String encode(String script) {
        return Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
    }

    private void startListener() {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < PHRASES.length; i++) { if (i > 0) list.append(','); list.append('\'').append(PHRASES[i]).append('\''); }
        String script =
            "$ErrorActionPreference='Stop'\n" +
            "try {\n" +
            " Add-Type -AssemblyName System.Speech\n" +
            " $r = New-Object System.Speech.Recognition.SpeechRecognitionEngine\n" +
            " $r.SetInputToDefaultAudioDevice()\n" +
            " $c = New-Object System.Speech.Recognition.Choices\n" +
            " $c.Add([string[]]@(" + list + "))\n" +
            " $gb = New-Object System.Speech.Recognition.GrammarBuilder\n" +
            " $gb.Append($c)\n" +
            " $r.LoadGrammar((New-Object System.Speech.Recognition.Grammar($gb)))\n" +
            " [Console]::Out.WriteLine('READY'); [Console]::Out.Flush()\n" +
            " while ($true) {\n" +
            "  $res = $r.Recognize([TimeSpan]::FromSeconds(5))\n" +
            "  if ($res -ne $null -and $res.Confidence -gt 0.55) { [Console]::Out.WriteLine('HEARD ' + $res.Text); [Console]::Out.Flush() }\n" +
            " }\n" +
            "} catch { [Console]::Out.WriteLine('ERROR ' + $_.Exception.Message); [Console]::Out.Flush() }\n";
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encode(script));
            pb.redirectErrorStream(true);
            listener = pb.start();
            status = "Starting microphone...";
            Thread t = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(listener.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("READY")) { listening = true; status = "Mic on: say a power"; }
                        else if (line.startsWith("HEARD ")) {
                            if (System.currentTimeMillis() > quietUntil) heard.add(line.substring(6).trim().toLowerCase());
                        } else if (line.startsWith("ERROR ")) { listening = false; status = "Mic error: " + line.substring(6); }
                    }
                } catch (Exception ignored) { }
                listening = false;
                if (status.startsWith("Mic on") || status.startsWith("Starting")) status = "Mic stopped";
            }, "voice-listen");
            t.setDaemon(true); t.start();
        } catch (Exception e) {
            status = "Couldn't start PowerShell for voice";
        }
    }

    private void startSpeaker() {
        String script =
            "Add-Type -AssemblyName System.Speech\n" +
            "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer\n" +
            "$s.Volume = 100\n" +
            "while (($l = [Console]::In.ReadLine()) -ne $null) {\n" +
            " try { $s.SpeakSsml(\"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'><prosody pitch='x-low' rate='-15%'>\" + $l + \"</prosody></speak>\") } catch { $s.Speak($l) }\n" +
            " [Console]::Out.WriteLine('DONE'); [Console]::Out.Flush()\n" +
            "}\n";
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", encode(script));
            pb.redirectErrorStream(true);
            speaker = pb.start();
            speakerIn = new OutputStreamWriter(speaker.getOutputStream(), StandardCharsets.UTF_8);
            Thread t = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(speaker.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) if (line.trim().equals("DONE")) { speaking = false; quietUntil = System.currentTimeMillis() + 400; }
                } catch (Exception ignored) { }
            }, "voice-speak");
            t.setDaemon(true); t.start();
        } catch (Exception e) { speaker = null; speakerIn = null; }
    }

    /** speak a line in the giraffe voice. Returns false if there's no speaker (caller plays a grunt instead). */
    public boolean say(String text) {
        if (speakerIn == null) return false;
        if (speaking) return true; // don't queue up a backlog
        String clean = text.replaceAll("[^A-Za-z0-9 ,.!?']", " ");
        try {
            speakerIn.write(clean + "\n"); speakerIn.flush();
            speaking = true;
            // the mic would hear the giraffe and fire powers: ignore it while he talks
            quietUntil = System.currentTimeMillis() + 800 + clean.length() * 75L;
            return true;
        } catch (Exception e) { speakerIn = null; return false; }
    }

    public String poll() { return heard.poll(); }

    public void stop() {
        listening = false; speaking = false;
        if (listener != null) { listener.destroy(); listener = null; }
        if (speaker != null) { speaker.destroy(); speaker = null; speakerIn = null; }
        heard.clear();
    }

    /** map a heard phrase to input bits. airborne decides whether "slam"/"spin" means slam. */
    public static int bitsFor(String phrase) {
        switch (phrase) {
            case "bellow": case "hum": return City.B_BELLOW;
            case "lick": case "tongue": case "grab": return City.B_TONGUE;
            case "kick": return City.B_KICK;
            case "lookout": case "look": return City.B_LOOK;
            case "munch": case "snack": case "eat": return City.B_MUNCH;
            case "spin": case "neck spin": case "slam": case "neck slam": return City.B_ATTACK;
            case "jump": return City.B_JUMP;
            case "drive": case "car": case "get out": return City.B_ENTER;
            case "honk": return City.B_HORN;
            case "talk": case "say something": case "hello giraffe": return City.B_TALK;
            case "zip": case "neck zip": return City.B_ZIP;
            case "cancel": return City.B_CANCEL;
            default: return 0;
        }
    }
}
