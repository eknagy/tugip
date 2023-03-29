/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.App.G;
import static hu.kwu.tugip.App.L;
import static hu.kwu.tugip.App.S;
import static hu.kwu.tugip.Sounder.loadWavsToBuffer;
import static hu.kwu.tugip.Sounder.selectedLine;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    private byte EnDashTwoMinusState = 0;

    private int targetKeyCode = -1;
    private char targetKeyChar = '\0';
    private byte[] wavBuffer = null;
    private SounderThread mySounderThread = null;
    private String[] wavFileNames = null;
    private String wavFileNamesString = null;
    private boolean playing = false;
    private static boolean ignoreClose = false;

    @Override
    public String toString() {
        return super.toString() + " " + targetKeyCode + " " + java.util.Arrays.toString(wavFileNames) + (isPlaying() ? " (playing)" : "");
    }

    public boolean isPlaying() {
        return (playing);
    }

    public static void refreshDebug() {
        if (G.debugMode) {
            G.D.setText(Director.toStringAll());
            G.D.repaint();
        } else {
            //        System.err.println(Director.toStringAll());
        }
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
        System.err.println("DEBUG: Playing: " + directorStack.peek());
        if (targetKeyCode == -4) {
            consumeKeyDown(KeyEvent.VK_SPACE, ' ');
        } else if (targetKeyCode == -3) {
            consumeKeyDown(KeyEvent.VK_SPACE, ' ');
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
                // If we close a line (sound ended) we can start playing the next sound, if:
                // it is an error sound (0 KeyCode)
                // it is a skippable sound (-2 KeyCode)
                // it and the next are both intro sounds (-1 KeyCode)
                if ((le.getType() == LineEvent.Type.CLOSE) && (!ignoreClose)) {
                    synchronized (directorStack) {

                        if (!directorStack.empty()) {
                            Director first = directorStack.peek();
                            first.stoppedPlaying();
                            System.err.println("DEBUG: " + System.currentTimeMillis() % 100000 + " Line Close while first is " + first);
                            boolean canStartNext = (first.targetKeyCode == 0) || (first.targetKeyCode == -2);

                            if (first.targetKeyCode == -1) {
                                Director second = directorStack.get(directorStack.size() - 2);
                                canStartNext = ((null != second) && (second.targetKeyCode == -1));
//                                System.err.println("DEBUG: Line Close while first is " + first + " and second is " + second + " and " + canStartNext);
                            }

                            if (canStartNext) {
                                first.selfDestruct();

                                if (!directorStack.empty()) {
                                    directorStack.peek().play();
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private Director(String[] wavFileNames, int targetKeyCode, char targetKeyChar) {
        this.wavFileNames = wavFileNames;
        this.targetKeyChar = targetKeyChar;
        this.targetKeyCode = targetKeyCode;
        if (wavFileNames == null) { // No sound file, this is a special command Director (like "evaluate typist")
            mySounderThread = null;
        } else {
            wavFileNamesString = java.util.Arrays.toString(wavFileNames);
//            System.err.println("DEBUG: new Director(): "+toString());
            if (wavBufferLookUpTable.containsKey(wavFileNamesString)) {
                wavBuffer = wavBufferLookUpTable.get(wavFileNamesString);
                System.err.println("DEBUG Director: lookup " + this.toString());
            } else {
                System.err.println("DEBUG Director: load   " + this.toString());
                try {
                    BufferedInputStream[] Candidates = new BufferedInputStream[wavFileNames.length];
                    for (int i = 0; i < Candidates.length; i++) {
                        Candidates[i] = S.getAutoPathFor(wavFileNames[i]);
                        if (Candidates[i] == null) {
                            Candidates[i] = S.getAutoPathFor("hiba.wav");
                        }
                    }
                    wavBuffer = loadWavsToBuffer(Candidates);
                    wavBufferLookUpTable.put(wavFileNamesString, wavBuffer);
                } catch (UnsupportedAudioFileException | IOException E) {
                    G.setIntensity(255, false);
                    App.redAlert("Exception in Director(" + wavFileNamesString + "," + targetKeyCode + "): " + E.toString());
//                    System.err.println("Exception in Director(" + wavFileName + "," + targetKeyCode + "): " + E.toString());
                }
            }
            mySounderThread = new SounderThread(wavBuffer);
        }
    }

    public static void addNew(String wavFileName, int targetKeyCode) {
        addNew(wavFileName, targetKeyCode, '\0');
    }

    public static void addNew(String wavFileName, int targetKeyCode, char targetKeyChar) {
        synchronized (directorStack) {
            directorStack.push(new Director(new String[]{wavFileName}, targetKeyCode, targetKeyChar));
            refreshDebug();
        }
    }

    public static void addNew(String[] wavFileNames, int targetKeyCode, char targetKeyChar) {
        synchronized (directorStack) {
            directorStack.push(new Director(wavFileNames, targetKeyCode, targetKeyChar));
            refreshDebug();
        }
    }

    public static void destroyAll() {
        ignoreClose = true;
        synchronized (directorStack) {
            while (!directorStack.isEmpty()) {
                Director current = directorStack.pop();
                current.selfDestruct();
            }
        }
        ignoreClose = false;
    }

    public void selfDestruct() {
        System.err.println("DEBUG: " + System.currentTimeMillis() % 100000 + " selfDestruct:" + this);
        ignoreClose = true;
        synchronized (directorStack) {
            if (!directorStack.isEmpty()) {
                directorStack.remove(this);
            }
            //       System.err.println("DEBUG: selfDestruct.postpeek:" +directorStack.peek());
            if (mySounderThread != null) {
                mySounderThread.selfDestruct();
            }
            stoppedPlaying();
        }
        ignoreClose = false;
        refreshDebug();
    }

    public static String[] generateNumberFileNames(int input) {
//        System.err.println("DEBUG: in: " + input);
        String[] TFNP = new String[]{"hiba"};
        if (input < 0) {
            throw new RuntimeException("Can not generateNumber(" + input + ")");
        } else if (((input < 11) && (input >= 0)) || (input == 20) || (input == 100)) {
            TFNP = new String[]{"" + input};
        } else if ((input >= 11) && (input < 20)) {
            TFNP = new String[]{"" + input % 10, "10en"};
        } else if ((input > 20) && (input < 30)) {
            TFNP = new String[]{"" + input % 10, "20on"};
        } else if ((input > 20) && (input < 100) && (input % 10 == 0)) {
            TFNP = new String[]{"" + input};
        } else if ((input > 20) && (input < 100)) {
            TFNP = new String[]{"" + (input % 10), "" + (input / 10) * 10};
        } else if (input == 100) {
            TFNP = new String[]{"100"};
        } else if ((input > 100) && (input < 200)) {
            String[] secondPart = generateNumberFileNames(input % 100);
            TFNP = new String[secondPart.length + 1];
            System.arraycopy(secondPart, 0, TFNP, 0, secondPart.length);
            TFNP[TFNP.length - 1] = "100";
        } else if ((input > 199) && (input < 1000)) {
            if (input % 100 == 0) {
                return (new String[]{"100", "" + input / 100});
            } else {
                String[] secondPart = generateNumberFileNames(input % 100);
                TFNP = new String[secondPart.length + 2];
                System.arraycopy(secondPart, 0, TFNP, 0, secondPart.length);
                TFNP[TFNP.length - 2] = "100";
                TFNP[TFNP.length - 1] = "" + input / 100;
            }
        } else if (input == 1000) {
            TFNP = new String[]{"1000"};
        } else if ((input > 1000) && (input < 2000)) {
            String[] secondPart = generateNumberFileNames(input % 1000);
            TFNP = new String[secondPart.length + 1];
            System.arraycopy(secondPart, 0, TFNP, 0, secondPart.length);
            TFNP[TFNP.length - 1] = "1000";
        } else if ((input > 1999) && (input < 1000000)) {
            if (input % 1000 == 0) {
                String[] firstPart = generateNumberFileNames(input / 1000);
                TFNP = new String[1 + firstPart.length];
                TFNP[0] = "1000";
                System.arraycopy(firstPart, 0, TFNP, 1, firstPart.length);
            } else {
                String[] firstPart = generateNumberFileNames(input / 1000);
                String[] secondPart = generateNumberFileNames(input % 1000);
//                System.err.println("DEBUG: fP: " + java.util.Arrays.toString(firstPart));
//                System.err.println("DEBUG: sP: " + java.util.Arrays.toString(secondPart));
                TFNP = new String[secondPart.length + 1 + firstPart.length];
                System.arraycopy(secondPart, 0, TFNP, 0, secondPart.length);
                TFNP[secondPart.length] = "1000";
                System.arraycopy(firstPart, 0, TFNP, secondPart.length + 1, firstPart.length);
//                System.err.println("DEBUG: rv: " + java.util.Arrays.toString(TFNP));
            }
        } else {
            App.redAlert("Can not generateNumber(" + input + ")");
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

    public boolean targetMatches(int targetKeyCode, char targetKeyChar) {
        if (('í'==targetKeyChar) && (targetKeyCode==74)) {
            targetKeyCode=16777453; // Windows 10 + Oracle JRE 8 treats AltGr+j different than the í key
        }
        if (('Í'==targetKeyChar) && (targetKeyCode==73)) {
            targetKeyCode=16777453; // Windows 10 + Oracle JRE 8 treats AltGr+i different than the shift+í key
        }
        if (L.ignoreCase == true) {
            return (this.targetKeyCode == targetKeyCode);
        } else {
            return ((this.targetKeyCode == targetKeyCode) && (this.targetKeyChar == targetKeyChar));
        }
    }

    public static void generateAnalysis(int goodPoints, int badPoints, int currentPercent, int targetPercent) {
        // "0 jo es 0 rossz leutesed volt az eredmenyed 0 szazalek ez a lecke 0 szazalektol sikeres"
        Director.addNew(null, -4);
        Director.addNew((currentPercent >= targetPercent ? "yuhuu" : "ooo") + ".wav", -2);
        for (String CS : generateTextFileNames("szazalektol_sikeres")) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(targetPercent)) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("szazalek_ez_a_lecke")) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(currentPercent)) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("rossz_leutesed_volt_az_eredmenyed")) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(badPoints)) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateTextFileNames("jo_es")) {
            Director.addNew(CS + ".wav", -2);
        }
        for (String CS : generateNumberFileNames(goodPoints)) {
            Director.addNew(CS + ".wav", -2);
        }
        Director.addNew((currentPercent >= targetPercent ? "yuhuu" : "ooo") + ".wav", -2);
    }

    public byte handleEnDash(int inputKeyCode, char inputKeyChar) {
        if (this.targetKeyCode == 16785427) {
            System.err.println("DEBUG: fNE is 16785427");
            if (inputKeyCode == KeyEvent.VK_MINUS) {
                // We need to check for double-minus as it is used for en dash
                // in MS Word and LibreOffice and in many other places
                if (EnDashTwoMinusState==0) {
                    // The first minus should set EnDashTwoMinusState to 1
                    // and return false - character is not to be consumed
                    EnDashTwoMinusState=1;
                    System.err.println("DEBUG: fNE eat first -");
                } else {
                    // The second minus should be interpreted as an em dash
                    System.err.println("DEBUG: fNE second -");
                    EnDashTwoMinusState=2;
                }
            } else {
                // Wrong key pressed - we should reset the state
                System.err.println("DEBUG: fNE reset");
                EnDashTwoMinusState = 0;
            }
        }
        return (EnDashTwoMinusState);
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
     * If the expected keyCode is -3 / End of Line, load the next line (if it
     * exits) and reset GUI and directorStack (end-of-line). If no more lines
     * exist, evaluate the typist and construct the necessary Directors
     * (end-of-lecture mode).<br>
     * If the expected keyCode is -4 / End of Lecture, remove all remaining
     * Directors and according the settings and available lectures, quit or
     * restart or go to the next lecture.<br>
     * If the expected keyCode is en dash ("gondolatjel, nagykötőjel"), either
     * two minus keys are expected or the en dash character itself (it can be
     * typed with Alt-150 in Windows and AltGr-z in Linux). If the expected
     * keyCode matches the input (see inputKeyChar as well), destruct the
     * Director on the top of the stack, start the next and return true (the
     * typist hit a matching or good key)<br>
     * If the expected keyCode differs from the input, destroy this half-played
     * or fully played instance, create a fresh one, push it on the stack and
     * then push a freshly created error sound on the top of the stack, and
     * return false (the typist hit a bad / not matching key).<br>
     *
     * @param inputKeyChar - if the lecture is set to ignore case, ignore this,
     * otherwise check if the case matches as well.
     *
     * @return true, if the KeyCode is consumed and typist should progress a
     * character forward, false otherwise<br>
     */
    public static boolean consumeKeyDown(int inputKeyCode, char inputKeyChar) {
        boolean consumed = false;
        System.err.println("DEBUG: cKD: " + inputKeyCode);
        if (inputKeyCode == KeyEvent.VK_ESCAPE) { // We should quit
            G.close();
            return (false);
        }

        if (directorStack.empty()) {
            return (false);
        }

        Director first = directorStack.peek();

        if (first.targetKeyCode == -2) { // We should skip all -2 sounds (typist analisys playback).
            first.selfDestruct();
            while ((!directorStack.isEmpty()) && (directorStack.peek().targetKeyCode == -2)) {
                directorStack.pop();
            }
            playFirst();
            return (false);
        }

        if (first.targetKeyCode == -3) { // End of line signal
            first.selfDestruct();

            if (L.hasNextLine()) {
                G.startLecture(true);
                return (false);
            } else {
                generateAnalysis(L.goodCount, L.badCount, L.getCurrentPercent(), L.passPercent);
                playFirst();
                return (false);
            }
        }

        if (first.targetKeyCode == -4) { // End of Lecture
            destroyAll();
            if (L.getCurrentPercent() >= L.passPercent) {
                String nextLectureName = L.getNextLectureName();
                if (null == nextLectureName) {
                    G.close();
                } else {
                    try {
                        L = new Lecturer(nextLectureName);
                    } catch (IOException IOE) {
                        App.redAlert(IOE.toString() + " in new Lecturer(" + nextLectureName + ")");
                        G.close();
                    }

                    try {
                        Lecturer.progressProperties.put("nextLecture", nextLectureName);
                        new File("lectures").mkdirs();
                        File LPP = new File("lectures/progress.properties");
                        LPP.createNewFile();
                        FileOutputStream FOS = new FileOutputStream(LPP);
                        Lecturer.progressProperties.store(FOS, "");
//                        System.err.println("DEBUG: pP: "+Lecturer.progressProperties);
                    } catch (IOException IOE) {
                        ; // Read-only filesystem or other problem - silently ignoring it
                    }
                    G.startLecture(false);
                }
            } else {
                L.resetLineCounter();
                G.startLecture(false);
            }
            return (false);
        }

        if (first.targetKeyCode == -1) {
            if (inputKeyCode == KeyEvent.VK_SPACE) {
//                System.err.println("DEBUG: got space during intro - eat all intro sounds (KeyCode==-1) and stop sound and return false.");
                ignoreClose = true;
                synchronized (directorStack) {
                    first = directorStack.pop(); // We will destroy it later, but first we destroy all following intro sounds to prevent autoplay
//                    System.err.println("DEBUG: prep: "+directorStack.peek());
                    while ((!directorStack.isEmpty()) && (directorStack.peek().targetKeyCode == -1)) {
                        System.err.println("DEBUG: -1 SD peek: " + directorStack.peek());

                        directorStack.peek().selfDestruct();
                    }
                    System.err.println("DEBUG: -1 SD next: " + directorStack.peek());
                    first.selfDestruct();
                    playFirst();
                }
                ignoreClose = false;
            } else {
                // We are waiting for a space to skip the intro - ignore other keystroke
            }
            return (false);
        } else {
            // We have to find the first non-zero inputKeyCode - error sounds consume no keypress, but the first normal ones after errors do
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
                switch (firstNotError.handleEnDash(inputKeyCode, inputKeyChar)) {
                        case 0: break;
                        case 1: return(false);
                        case 2: inputKeyCode = 16785427;
                                inputKeyChar = '–';
                                break;
                        default: throw new RuntimeException("Unimplemented!");
                }
                
                if (firstNotError.targetMatches(inputKeyCode, inputKeyChar)) {
                    L.goodCount++;
                    while (!directorStack.peek().equals(firstNotError)) {
                        directorStack.peek().selfDestruct();
                    }
                    firstNotError.selfDestruct();
                    consumed = true;
                } else {
                    L.badCount++;
                    directorStack.pop(); // this should == firstError
                    addNew("hiba.wav", 0);
                    directorStack.push(firstError);
                }
            } else {
                switch (first.handleEnDash(inputKeyCode, inputKeyChar)) {
                        case 0: break;
                        case 1: return(false);
                        case 2: inputKeyCode = 16785427;
                                inputKeyChar = '–';
                                break;
                        default: throw new RuntimeException("Unimplemented!");
                }
                if (first.targetMatches(inputKeyCode, inputKeyChar)) {
                    // Proper key hit - add good point, stop sound, return true.
                    L.goodCount++;
                    first.selfDestruct();
                    consumed = true;
                } else {
                    // Not proper key hit - add bad point, regenerate (rewind) sound, pop error sound in front, start playing.
                    L.badCount++;
                    first.mySounderThread.selfDestruct();
                    first.mySounderThread = new SounderThread(first.wavBuffer);
                    first.stoppedPlaying();
                    addNew("hiba.wav", 0);
                }
            }
        }

//        System.err.println("DEBUG: returning from consumeKeyDown(" + inputKeyCode + ") with " + consumed);
        return (consumed);
    }

}
