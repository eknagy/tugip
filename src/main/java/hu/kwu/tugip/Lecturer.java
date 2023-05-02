/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.JLabel;

public class Lecturer {

    private final Properties lectureProperties = new Properties();
    public final static Properties progressProperties;
    private final String lectureName;
    private final String[] wavDirs;
    private int currentLineID = 1;
    public int passPercent = 80;
    public int badCount = 0;
    public int goodCount = 0;
    private static ArrayList<String> availableLectures = null;

    public boolean enableBackSpace=false;
    public boolean ignoreCase=true;
    
    static {
        progressProperties = new Properties();
        try {
            BufferedInputStream BIS = new BufferedInputStream(new FileInputStream("lectures/progress.properties"));
            System.err.println("DEBUG: BIS: " + BIS);
            progressProperties.load(BIS);
            System.err.println("DEBUG: progressProperties: " + progressProperties);

        } catch (IOException E) {
            System.err.println("DEBUG: IOE: " + E.toString()); // Ignore it, we just start from the first lecture (kiosk mode)
        }
    }

    public void resetCounts() {
        badCount = 0;
        goodCount = 0;
    }

    public int getCurrentPercent() {
        return (((badCount + goodCount) == 0) ? -1 : (100 * goodCount) / (badCount + goodCount));
    }

    public void regeneratePassPanel(JLabel passLabel) {
        int currentPercent = getCurrentPercent();
        if (currentPercent < 0) {
            passLabel.setText("??% (" + passPercent + "%)");
            passLabel.setBackground(Color.WHITE);
        } else {
            passLabel.setText("" + currentPercent + "% (" + passPercent + "%)");
            passLabel.setBackground((currentPercent >= passPercent) ? Color.GREEN : Color.RED);
        }
    }

    public Lecturer(String lectureName) throws IOException {
        this.lectureName = lectureName;
        try {
            BufferedInputStream FIS = new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream("lectures/" + this.lectureName + "/config.properties"));
            lectureProperties.load(FIS);
        } catch (IOException E) {
            System.err.println("Lecturer(" + this.lectureName + ") Exception: " + E.toString());
            throw new IOException(E);
        }
        String[] tmp = lectureProperties.getProperty("WAVDIR", "").split(" ");
        wavDirs = new String[tmp.length + 2];
        wavDirs[0] = "lectures/" + lectureName + "/";
        wavDirs[1] = "lecturesounds/";
        System.arraycopy(tmp, 0, wavDirs, 2, tmp.length);
        for (int i = 2; i < wavDirs.length; i++) {
            wavDirs[i] = "lecturesounds/" + wavDirs[i] + "/";
        }

        try {
            passPercent = Integer.parseInt(lectureProperties.getProperty("PASS_PERCENT", "80").trim());
        } catch (NumberFormatException NFE) {
            System.err.println("passPercent defaulting to 80 because of NFE: " + NFE.toString());
        }
        
        ignoreCase = Boolean.parseBoolean(lectureProperties.getProperty("IGNORE_CASE", "true"));
        enableBackSpace = Boolean.parseBoolean(lectureProperties.getProperty("ENABLE_BACKSPACE", "false"));
        System.err.println("DEBUG: L.ignoreCase="+ignoreCase);
    }

    public static ArrayList<String> listAvailableLecturesSorted() throws IOException {
        if (availableLectures != null) {
            return availableLectures;
        }
        try {
            BufferedReader BR = new BufferedReader(new InputStreamReader(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("lectures/lectures.list")));

            availableLectures = new ArrayList<>();
            String CLine = null;
            while (null != (CLine = BR.readLine())) {
                availableLectures.add(CLine);
            }

        } catch (IOException E) {
            System.err.println("Lecturer.listAvailableLecturesSorted() Exception: " + E.toString());
            throw new IOException(E);
        }

        availableLectures.sort(null);
        return (availableLectures);
    }

    public String getNextLectureName() {
        int nextIndex = availableLectures.indexOf(lectureName) + 1;
        return (nextIndex >= availableLectures.size() ? null : availableLectures.get(nextIndex));
    }

    public String[] getWavDirs() {
        return wavDirs;
    }
    
    public void resetLineCounter() {
        currentLineID = 1;
    }

    public boolean hasNextLine() {
        return (null != lectureProperties.getProperty("TEXT_" + (currentLineID + 1)));
    }

    public String getCurrentLine() {
        System.err.println("DEBUG: currentLineID: "+currentLineID);
        return (lectureProperties.getProperty("TEXT_" + currentLineID)+"\u23CE"); // .replaceAll(" ", "\u2423")
    }

    public String getNextLine() {
        currentLineID++;
        return (getCurrentLine());
    }

    public String[] getHelloFilesNames() throws IOException {
        String helloFilesLine = lectureProperties.getProperty("BEFORE_LECTURE", "hello");
        String[] helloFilesStrings = helloFilesLine.split("\\s");
        String[] helloFilesNames = new String[helloFilesStrings.length];

        for (int i = 0; i < helloFilesStrings.length; i++) {
            helloFilesNames[i] = helloFilesStrings[i] + ".wav";
        }

        System.err.println("DEBUG: gHFNs(): " + java.util.Arrays.toString(helloFilesNames));

        return (helloFilesNames);
    }
}
