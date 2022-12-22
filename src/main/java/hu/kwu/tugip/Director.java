/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.Sounder.loadWavToBuffer;
import static hu.kwu.tugip.Sounder.selectedLine;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Director {

    private final static ConcurrentHashMap<String, byte[]> wavBufferLookUpTable = new ConcurrentHashMap<>(); // Filenames to binary buffers
    private final static Stack<Director> directorStack; // Stack of directors - as a sound file queue

    private int targetKeyCode = -1;
    private byte[] wavBuffer = null;
    private SounderThread mySounderThread = null;

    public static void play() {
        Director current = directorStack.peek();
        if (null == current) {
            System.err.println("DEBUG: D.play() but empty.");
            return;
        }
//        System.err.println("DEBUG: D.play(): " + current.toString());
        current.mySounderThread.start();
    }

    static {
        directorStack = new Stack();

        selectedLine.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent le) {
                // If we close a line (sound ended) and the first Director is an error sound (has a 0 KeyCode) then we start the next one.
                if (le.getType() == LineEvent.Type.CLOSE) {
                    if (!directorStack.empty()) {
                        Director first = directorStack.peek();
                        if ((first.targetKeyCode == 0) || (first.targetKeyCode == -2)) {
                            directorStack.pop().mySounderThread.selfDestruct();
                            if (!directorStack.empty()) {
                                SounderThread NST = directorStack.peek().mySounderThread;
                                if ((NST != null) && (!NST.isAlive())) {
                                    NST.start();
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    public Director(String wavFileName, int targetKeyCode) {
        if (wavFileName == null) { // No sound file, this is a special command Director (like "evaluate typist")
            this.targetKeyCode = targetKeyCode;
            mySounderThread = null;
        } else {
            this.targetKeyCode = targetKeyCode;
            if (wavBufferLookUpTable.containsKey(wavFileName)) {
                wavBuffer = wavBufferLookUpTable.get(wavFileName);
                System.err.println("DEBUG Director: looked up " + wavFileName + " with " + targetKeyCode + " as " + this.toString());
            } else {
                System.err.println("DEBUG Director: loading " + wavFileName + " with " + targetKeyCode + " as " + this.toString());
                try {
                    wavBuffer = loadWavToBuffer(wavFileName);
                    wavBufferLookUpTable.put(wavFileName, wavBuffer);
                } catch (UnsupportedAudioFileException | IOException E) {
                    App.SingletonGUI.setIntensity(255, false);
                    System.err.println("Exception in Director(" + wavFileName + "," + targetKeyCode + "): " + E.toString());
                }
            }
            mySounderThread = new SounderThread(wavBuffer);
        }
    }

    public static void addNew(String wavFileName, int targetKeyCode) {
        directorStack.push(new Director(wavFileName, targetKeyCode));
    }

    public static void selfDestruct() {
        while (!directorStack.isEmpty()) {
            Director current = directorStack.pop();
            current.mySounderThread.selfDestruct();
            App.SingletonGUI.close();
        }
    }

    public static String[] generateNumberFileNames(int input) {
        String[] TFNP = new String[]{"hiba"};
        if (input < 0) {
            throw new RuntimeException("Can not generateNumber(" + input + ")");
        } else if (((input < 11) && (input >= 0)) || (input == 20) || (input == 100)) {
            TFNP = new String[]{"" + input};
        } else if ((input >= 11) && (input < 20)) {
            TFNP = new String[]{"" + input % 10, "10en"};
        } else if ((input > 20) && (input < 30)) {
            TFNP = new String[]{"" + input % 10, "20on"};
        } else if ((input > 20) && (input % 10 == 0)) {
            TFNP = new String[]{"" + input};
        } else if ((input > 20) && (input < 100)) {
            TFNP = new String[]{"" + (input % 10), "" + (input / 10) * 10};
        } else {
            throw new RuntimeException("Can not generateNumber(" + input + ")");
        }
        return (TFNP);
    }

    public static String[] generateTextFileNames(String input) {
        Deque<String> tmpDeque = new ArrayDeque<>();

        for (String CS : input.split(" ")) {
            tmpDeque.addFirst(CS);
        }

        return tmpDeque.toArray(new String[0]);
    }

    public static void generateEnding(int goodPoints, int badPoints, int currentPercent, int targetPercent) {
        // "0 jo es 0 rossz leutesed volt az eredmenyed 0 szazalek ez a lecke 0 szazalektol sikeres"
        Director.addNew(App.SYSTEMSOUNDDIR + (currentPercent >= targetPercent ? "yuhuu" : "ooo") + ".wav", -2);
        for (String CS : generateTextFileNames("szazalektol sikeres")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(targetPercent)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("szazalek ez a lecke")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(currentPercent)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("rossz leutesed volt az eredmenyed")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(badPoints)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("jo es")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(goodPoints)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        Director.addNew(App.SYSTEMSOUNDDIR + (currentPercent >= targetPercent ? "yuhuu" : "ooo") + ".wav", -2);
    }

    /**
     * Checks targetKeyCode input against expected keyCode in the Stack.
     *
     * @param inputKeyCode - the KeyCode the typist entered<br>
     * If the inputKeyCode is VK_ESCAPE, terminate the application.<br>
     * If the expected keyCode is 0, ignore it, try the next Director in the
     * stack (error sound).<br>
     * If the expected keyCode is -1, accept VK_SPACE and return false or if not
     * VK_SPACE, ignore it.<br>
     * If the expected keyCode is -2, accept anything and close the GUI.<br>
     * Id the expected keyCode is -3, evaluate the typist and construct the
     * necessary Directors.<br>
     * If the expected keyCode matches the input, destruct the Director on the
     * top of the stack and return true.<br>
     * If the expected keyCode differs from the input, push an error sound on
     * the top of it and return false.<br>
     *
     * @return true, if the KeyCode is consumed and typist should progress a
     * character forward, false otherwise<br>
     */
    public static boolean consumeKeyDown(int inputKeyCode) {
        boolean consumed = false;

//        System.err.println("DEBUG: consumeKeyDown: in: " + inputKeyCode);
        if (directorStack.empty()) {
            return (false);
        }

        Director current = directorStack.pop();

        if (current.targetKeyCode == -3) {
            generateEnding(App.L.goodCount, App.L.badCount, App.L.getCurrentPercent(), App.L.passPercent);
            if (!directorStack.empty()) {
                SounderThread NST = directorStack.peek().mySounderThread;
                if ((NST != null) && (!NST.isAlive())) {
                    NST.start();
                }
            }
            return(false);
        }

//        System.err.println("DEBUG: consumeKeyDown: current: " + current.targetKeyCode + " from " + current);
        if ((current.targetKeyCode == -2) || (inputKeyCode == KeyEvent.VK_ESCAPE)) {
//            System.err.println("DEBUG: got -2 (victory sound) - quit.");
            current.mySounderThread.selfDestruct();
            selfDestruct();
            return (false);
        }

        if (current.targetKeyCode == -1) {
            if (inputKeyCode == KeyEvent.VK_SPACE) {
//              System.err.println("DEBUG: got space during intro - stop sound and return false.");
                current.mySounderThread.selfDestruct();
            } else {
                // We are waiting for a space to skip the intro - pop current back, ignore keystroke
                directorStack.push(current);
                return (false);
            }
        } else {
            // We have to find the first non-zero inputKeyCode - they are error sounds, need no keypress
            // If the first non-zero matches, we need to stop and remove all pending error sounds before (including) current
            // If the first non-zero does not match, we need to insert another error sound right before it, and we have to rewind it
            if (current.targetKeyCode == 0) {
                Director currentError = current; // This have to be saved as it may be half-played
                int i = 0;
                for (i = 0; i < directorStack.size(); i++) {
                    if (directorStack.get(i).targetKeyCode != 0) {
                        break;
                    }
                }
                if (i > directorStack.size()) {
                    throw new RuntimeException("FATAL: CAN NOT HAPPEN: ONLY ERRORS IN DIRECTORSTACK.");
                }
                Director target = directorStack.get(i);
                if (target.targetKeyCode == inputKeyCode) {
                    App.L.goodCount++;
                    for (; i > 0; i--) { // We remove all error sounds and this one, so we can start the next sound
                        directorStack.pop().mySounderThread.selfDestruct();
                    }
                    consumed = true;
                } else {
                    App.L.badCount++;
                    // Wrong keycode, we just insert an extra error sound and push back the current one
                    directorStack.push(new Director("systemsounds/hiba.wav", 0)); // Push an error sound in front of it
                    directorStack.push(current);
                }
            } else {
                if (current.targetKeyCode == inputKeyCode) {
                    App.L.goodCount++;
                    current.mySounderThread.selfDestruct(); // Proper key hit - stop sount, return true.
                    consumed = true;
                } else {
                    App.L.badCount++;
                    directorStack.push(current);            // Push back the popped item
                    current.mySounderThread.selfDestruct();     // Replace the SounderThread (rewind)
                    current.mySounderThread = new SounderThread(current.wavBuffer);
                    directorStack.push(new Director("systemsounds/hiba.wav", 0)); // Push an error sound in front of it
                }
            }
        }

        if (!directorStack.empty()) {
            SounderThread NST = directorStack.peek().mySounderThread;
            if ((NST != null) && (!NST.isAlive())) {
                NST.start();
            }
        }
//        System.err.println("DEBUG: returning from consumeKeyDown(" + inputKeyCode + ") with " + consumed);
        return (consumed);
    }

}
