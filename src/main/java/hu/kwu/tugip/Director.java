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
    private String wavFileName = null;
    private boolean playing = false;

    @Override
    public String toString() {
        return super.toString() + " " + targetKeyCode + " " + wavFileName + (isPlaying() ? " (playing)" : "");
    }

    public boolean isPlaying() {
        return (playing);
    }

    public static void refreshDebug() {
        App.SingletonGUI.D.setText(Director.toStringAll());
        App.SingletonGUI.D.repaint();
    }

    public static String toStringAll() {
        StringBuilder retVal = new StringBuilder();
        for (int i = directorStack.size() - 1; i >= 0; i--) {
            retVal.append(directorStack.get(i).toString());
            retVal.append("\n");
        }
        return (retVal.toString());
    }

    public static void playFirst() {
        if (!directorStack.empty()) {
            directorStack.peek().play();
        }
    }

    public void stoppedPlaying() {
        playing = false;
        refreshDebug();
    }

    public void play() {
        if (targetKeyCode == -4) {
            consumeKeyDown(KeyEvent.VK_ESCAPE);
        } else if (targetKeyCode == -3) {
            consumeKeyDown(KeyEvent.VK_SPACE);
        } else if ((mySounderThread != null) && (!mySounderThread.isAlive()) && (!mySounderThread.started)) {
            try {
                mySounderThread.start();
            } catch (IllegalThreadStateException E) {
                System.err.println("DEBUG: Why is this happening? E: " + E.toString());
            }
            directorStack.peek().playing = true;
            refreshDebug();
        }
    }

    static {
        directorStack = new Stack();

        selectedLine.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent le) {
//                System.err.println("DEBUG: le: "+le);
                // If we close a line (sound ended) and the first Director is an error sound or a skippables sound (has a 0 or -2 KeyCode) then we remove it and start the next one.
                if (le.getType() == LineEvent.Type.CLOSE) {
//                    System.err.println("DEBUG: CL: "+le);
                    if (!directorStack.empty()) {
                        Director first = directorStack.peek();
                        first.stoppedPlaying();

                        if ((first.targetKeyCode == 0) || (first.targetKeyCode == -2)) {
                            first.selfDestruct();

                            if (!directorStack.empty()) {
                                directorStack.peek().play();
                            }
                        }
                    }
                }
            }
        });
    }

    private Director(String wavFileName, int targetKeyCode) {
        this.wavFileName = wavFileName;
        if (wavFileName == null) { // No sound file, this is a special command Director (like "evaluate typist")
            this.targetKeyCode = targetKeyCode;
            mySounderThread = null;
        } else {
            this.targetKeyCode = targetKeyCode;
            if (wavBufferLookUpTable.containsKey(wavFileName)) {
                wavBuffer = wavBufferLookUpTable.get(wavFileName);
                System.err.println("DEBUG Director: lookup " + this.toString());
            } else {
                System.err.println("DEBUG Director: load   " + this.toString());
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
        refreshDebug();
    }

    public void selfDestruct() {
        directorStack.remove(this);
        if (mySounderThread != null) {
            mySounderThread.selfDestruct();
        }
        refreshDebug();
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

    public static void generateAnalysis(int goodPoints, int badPoints, int currentPercent, int targetPercent) {
        // "0 jo es 0 rossz leutesed volt az eredmenyed 0 szazalek ez a lecke 0 szazalektol sikeres"
        Director.addNew(null, -4);
        Director.addNew(App.SYSTEMSOUNDDIR + (currentPercent >= targetPercent ? "yuhuu" : "ooo") + ".wav", -2);
        for (String CS : generateTextFileNames("szazalektol_sikeres")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(targetPercent)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("szazalek_ez_a_lecke")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(currentPercent)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("rossz_leutesed_volt_az_eredmenyed")) {
            Director.addNew(App.SYSTEMSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(badPoints)) {
            Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("jo_es")) {
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
     * If the expected keyCode is 0, ignore it, try the next Director in the
     * stack (this is an error sound).<br>
     * If the expected keyCode is -1, accept VK_SPACE and return false or if not
     * VK_SPACE, ignore it (this is an intro sound).<br>
     * If the expected keyCode is -2, accept anything and skip all -2 Directors
     * (this is a quitting sound).<br>
     * If the expected keyCode is -3, evaluate the typist and construct the
     * necessary Directors (this is an end-of-typing command).<br>
     * If the expected keyCode is -4, this is an "end of lecture" command, quit
     * or restart or go to the next one.<br>
     * If the expected keyCode matches the input, destruct the Director on the
     * top of the stack, start the next and return true (the typist hit a
     * matching or good key)<br>
     * If the expected keyCode differs from the input, destroy this half-played
     * or fully played instance, create a fresh one, push it on the stack and
     * then push a freshly created error sound on the top of the stack, and
     * return false (the typist hit a bad / not matching key).<br>
     *
     * @return true, if the KeyCode is consumed and typist should progress a
     * character forward, false otherwise<br>
     */
    public static boolean consumeKeyDown(int inputKeyCode) {
        boolean consumed = false;

        if (inputKeyCode == KeyEvent.VK_ESCAPE) { // We should quit
            App.SingletonGUI.close();
            return (false);
        }

        if (directorStack.empty()) {
            return (false);
        }

        Director first = directorStack.peek();

        if (first.targetKeyCode == -2) { // We should skip all -2 sounds (typist analisys).
            first.selfDestruct();
            while ((!directorStack.isEmpty()) && (directorStack.peek().targetKeyCode == -2)) {
                directorStack.pop();
            }
            playFirst();
            return (false);
        }

        if (first.targetKeyCode == -3) { // We should generate the analysis of the type
            first.selfDestruct();
            generateAnalysis(App.L.goodCount, App.L.badCount, App.L.getCurrentPercent(), App.L.passPercent);
            playFirst();
            return (false);
        }

        if (first.targetKeyCode == -4) { // We should quit
            App.SingletonGUI.close();
            return (false);
        }

        if (first.targetKeyCode == -1) {
            if (inputKeyCode == KeyEvent.VK_SPACE) {
//              System.err.println("DEBUG: got space during intro - stop sound and return false.");
                first.selfDestruct();
                playFirst();
            } else {
                // We are waiting for a space to skip the intro - ignore keystroke
            }
            return (false);
        } else {
            // We have to find the first non-zero inputKeyCode - error sounds no keypress, but the first normal ones after errors do
            // If the first non-zero matches, we need to stop and remove all pending error sounds before (including) current
            // If the first non-zero does not match, we need to insert another error sound right before it, and we have to rewind it
            if (first.targetKeyCode == 0) {
                Director firstError = first; // This better be rememberd as it may be half-played
                Director firstNotError = null;
                for (int i = directorStack.size() - 1; i >= 0; i--) {
                    Director CD = directorStack.get(i);
                    if (CD.targetKeyCode != 0) {
                        firstNotError = CD;
                        break;
                    }
                }
                if (firstNotError == null) {
                    throw new RuntimeException("FATAL: CAN NOT HAPPEN: ONLY ERRORS IN DIRECTORSTACK.");
                }
//                System.err.println("DEBUG: MYBUG: Expected "+firstNotError.targetKeyCode+" and got "+inputKeyCode);
                if (firstNotError.targetKeyCode == inputKeyCode) {
                    App.L.goodCount++;
                    while (!directorStack.peek().equals(firstNotError)) {
                        directorStack.peek().selfDestruct();
                    }
                    firstNotError.selfDestruct();
                    consumed = true;
                } else {
                    App.L.badCount++;
                    directorStack.pop(); // this should == firstError
                    addNew("systemsounds/hiba.wav", 0);
                    directorStack.push(firstError);
                }
            } else {
                if (first.targetKeyCode == inputKeyCode) {
                    // Proper key hit - add good point, stop sound, return true.
                    App.L.goodCount++;
                    first.selfDestruct();
                    consumed = true;
                } else {
                    // Not proper key hit - add bad point, regenerate (rewind) sound, pop error sound in front, start playing.
                    App.L.badCount++;
                    first.mySounderThread.selfDestruct();
                    first.mySounderThread = new SounderThread(first.wavBuffer);
                    first.stoppedPlaying();
                    addNew("systemsounds/hiba.wav", 0);
                }
            }
        }

        playFirst();

//        System.err.println("DEBUG: returning from consumeKeyDown(" + inputKeyCode + ") with " + consumed);
        return (consumed);
    }

}
