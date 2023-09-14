/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import static hu.kwu.tugip.App.L;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class GUI extends JFrame {

    private final static HashMap<String, Integer> EXPECTED_KEYCODES = new HashMap<>();
    private final static HashMap<String, Integer> NUMPAD_KEYCODES = new HashMap<>();
    private final static HashMap<Integer, Integer> NUMPAD_MAPPER = new HashMap<>();

    private final static HashMap<String, String> SPECIAL_FILENAMES = new HashMap<>();

    private final static HashSet<String> HANDLED_AS_KEYCODES = new HashSet<>();
    private final static HashSet<String> CAPITAL_LETTERS = new HashSet<>();

    public static boolean debugMode = false;

    private static String textToType = "";
    private static int textTypedPosition = 0;

    // private final static Font FONT144 = new Font("Monospaced", Font.BOLD, 144);
    private final static Font FONT72 = new Font("Monospaced", Font.BOLD, 72);
    private final static Font FONT36 = new Font("Monospaced", Font.BOLD, 36);

    private static boolean acceptInput = false;

    private final JPanel northPanel = new JPanel(new GridLayout(1, 5));
    private final JLabel goodPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel badPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel misTypeCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel passLabel = new JLabel("??% (??%)", SwingConstants.CENTER);
    private final JPanel visualiserPanel = new JPanel();

    private final static int TEXTPANELROWS = 5;
    private final static int TEXTPANELCOLS = 20;

    private final JPanel textPanel = new JPanel(new GridLayout(TEXTPANELROWS, TEXTPANELCOLS));
    private final JLabel[] textLabels = new JLabel[TEXTPANELCOLS * TEXTPANELROWS];
    private static final boolean[] misTypes = new boolean[TEXTPANELCOLS * TEXTPANELROWS];

    private final JPanel debugPanel = new JPanel(new GridLayout(2, 1));
    private final JPanel aboutPanel = new JPanel(new GridLayout(2, 1));
    private final Color[] colorTable = new Color[2 * 256];

    public final TextArea DebugDirectorTextArea = new TextArea("DEBUG: Loading...");
    public final TextArea DebugInputTextArea = new TextArea("DEBUG: Loading...");

    private int lastRegenerateIndex = -1;

    static {
        SPECIAL_FILENAMES.put(" ", "space.wav");
        SPECIAL_FILENAMES.put(",", "vessz.wav");
        SPECIAL_FILENAMES.put(".", "pont.wav");
        SPECIAL_FILENAMES.put("-", "kotojel.wav");
        SPECIAL_FILENAMES.put("–", "hosszu_kotojel.wav");
        SPECIAL_FILENAMES.put("á", "ha.wav");
        SPECIAL_FILENAMES.put("é", "he.wav");
        SPECIAL_FILENAMES.put("í", "hi.wav");
        SPECIAL_FILENAMES.put("ó", "ho.wav");
        SPECIAL_FILENAMES.put("ö", "hho.wav");
        SPECIAL_FILENAMES.put("ő", "hhho.wav");
        SPECIAL_FILENAMES.put("ú", "hu.wav");
        SPECIAL_FILENAMES.put("ü", "hhu.wav");
        SPECIAL_FILENAMES.put("ű", "hhhu.wav");

        EXPECTED_KEYCODES.put("a", KeyEvent.VK_A);
        EXPECTED_KEYCODES.put("b", KeyEvent.VK_B);
        EXPECTED_KEYCODES.put("á", 16777441);
        EXPECTED_KEYCODES.put("c", KeyEvent.VK_C);
        EXPECTED_KEYCODES.put("d", KeyEvent.VK_D);
        EXPECTED_KEYCODES.put("e", KeyEvent.VK_E);
        EXPECTED_KEYCODES.put("é", 16777449);
        EXPECTED_KEYCODES.put("f", KeyEvent.VK_F);
        EXPECTED_KEYCODES.put("g", KeyEvent.VK_G);
        EXPECTED_KEYCODES.put("h", KeyEvent.VK_H);
        EXPECTED_KEYCODES.put("i", KeyEvent.VK_I);
        EXPECTED_KEYCODES.put("í", 16777453);
        EXPECTED_KEYCODES.put("j", KeyEvent.VK_J);
        EXPECTED_KEYCODES.put("k", KeyEvent.VK_K);
        EXPECTED_KEYCODES.put("l", KeyEvent.VK_L);
        EXPECTED_KEYCODES.put("m", KeyEvent.VK_M);
        EXPECTED_KEYCODES.put("n", KeyEvent.VK_N);
        EXPECTED_KEYCODES.put("o", KeyEvent.VK_O);
        EXPECTED_KEYCODES.put("ó", 16777459);
        EXPECTED_KEYCODES.put("ö", 16777430);
        EXPECTED_KEYCODES.put("ő", 16777553);
        EXPECTED_KEYCODES.put("p", KeyEvent.VK_P);
        EXPECTED_KEYCODES.put("q", KeyEvent.VK_Q);
        EXPECTED_KEYCODES.put("r", KeyEvent.VK_R);
        EXPECTED_KEYCODES.put("s", KeyEvent.VK_S);
        EXPECTED_KEYCODES.put("t", KeyEvent.VK_T);
        EXPECTED_KEYCODES.put("u", KeyEvent.VK_U);
        EXPECTED_KEYCODES.put("ü", 16777468);
        EXPECTED_KEYCODES.put("ú", 16777466);
        EXPECTED_KEYCODES.put("ű", 16777585);
        EXPECTED_KEYCODES.put("v", KeyEvent.VK_V);
        EXPECTED_KEYCODES.put("w", KeyEvent.VK_W);
        EXPECTED_KEYCODES.put("x", KeyEvent.VK_X);
        EXPECTED_KEYCODES.put("y", KeyEvent.VK_Y);
        EXPECTED_KEYCODES.put("z", KeyEvent.VK_Z);

        for (String CK : EXPECTED_KEYCODES.keySet().toArray(new String[0])) {
            EXPECTED_KEYCODES.put(CK.toUpperCase(), EXPECTED_KEYCODES.get(CK));
            CAPITAL_LETTERS.add(CK.toUpperCase());
        }

        EXPECTED_KEYCODES.put("0", KeyEvent.VK_0);
        EXPECTED_KEYCODES.put("1", KeyEvent.VK_1);
        EXPECTED_KEYCODES.put("2", KeyEvent.VK_2);
        EXPECTED_KEYCODES.put("3", KeyEvent.VK_3);
        EXPECTED_KEYCODES.put("4", KeyEvent.VK_4);
        EXPECTED_KEYCODES.put("5", KeyEvent.VK_5);
        EXPECTED_KEYCODES.put("6", KeyEvent.VK_6);
        EXPECTED_KEYCODES.put("7", KeyEvent.VK_7);
        EXPECTED_KEYCODES.put("8", KeyEvent.VK_8);
        EXPECTED_KEYCODES.put("9", KeyEvent.VK_9);

        EXPECTED_KEYCODES.put("\u23CE", KeyEvent.VK_ENTER);
        EXPECTED_KEYCODES.put(" ", KeyEvent.VK_SPACE);
        EXPECTED_KEYCODES.put(",", KeyEvent.VK_COMMA);
        EXPECTED_KEYCODES.put("-", KeyEvent.VK_MINUS);
        EXPECTED_KEYCODES.put("–", 16785427); // En Dash - Windows Alt-150, Linux AltGr-z
        EXPECTED_KEYCODES.put(".", KeyEvent.VK_PERIOD);

        NUMPAD_KEYCODES.put("0", KeyEvent.VK_NUMPAD0);
        NUMPAD_KEYCODES.put("1", KeyEvent.VK_NUMPAD1);
        NUMPAD_KEYCODES.put("2", KeyEvent.VK_NUMPAD2);
        NUMPAD_KEYCODES.put("3", KeyEvent.VK_NUMPAD3);
        NUMPAD_KEYCODES.put("4", KeyEvent.VK_NUMPAD4);
        NUMPAD_KEYCODES.put("5", KeyEvent.VK_NUMPAD5);
        NUMPAD_KEYCODES.put("6", KeyEvent.VK_NUMPAD6);
        NUMPAD_KEYCODES.put("7", KeyEvent.VK_NUMPAD7);
        NUMPAD_KEYCODES.put("8", KeyEvent.VK_NUMPAD8);
        NUMPAD_KEYCODES.put("9", KeyEvent.VK_NUMPAD9);
        
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD0, KeyEvent.VK_0);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD1, KeyEvent.VK_1);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD2, KeyEvent.VK_2);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD3, KeyEvent.VK_3);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD4, KeyEvent.VK_4);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD5, KeyEvent.VK_5);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD6, KeyEvent.VK_6);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD7, KeyEvent.VK_7);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD8, KeyEvent.VK_8);
        NUMPAD_MAPPER.put(KeyEvent.VK_NUMPAD9, KeyEvent.VK_9);
        
        HANDLED_AS_KEYCODES.addAll(EXPECTED_KEYCODES.keySet());
    }

    public static void misType() {
        if (misTypes[textTypedPosition]==false) {
            misTypes[textTypedPosition]=true;
            L.misTypeCount++;
        }
    }
    
    public void regenerateText(boolean forceAll, boolean clearMisTypes) {
        int from = 0;
        int to = 0;
        if (clearMisTypes) {
            for (int i=0; i<misTypes.length; i++) {
                misTypes[i]=false;
            }
        }

        if ((forceAll) || (lastRegenerateIndex == -1)) {
            from = 0;
            to = textLabels.length;
            lastRegenerateIndex = textTypedPosition;
        } else {
            from = Math.max(0, textTypedPosition - 1);
            to = Math.min(textTypedPosition + 1, textLabels.length);
            lastRegenerateIndex = textTypedPosition;
        }
        String CL = L.getCurrentLine();
        System.err.println("DEBUG: regenerateText(" + forceAll + ") from " + from + " to " + to);
        for (int i = from; i < to; i++) {
            if (i < CL.length()) {
                if (i < textTypedPosition) {
                    if (misTypes[i]) {
                        textLabels[i].setBackground(Color.orange);
                    } else {
                        textLabels[i].setBackground(Color.green);
                    }
                } else if (i == textTypedPosition) {
                    if (misTypes[i]) {
                        textLabels[i].setBackground(Color.red);
                    } else {
                        textLabels[i].setBackground(Color.yellow);
                    }
                } else {
                    textLabels[i].setBackground(Color.white);
                }
                textLabels[i].setText(CL.substring(i, i + 1));
            } else {
                textLabels[i].setText("");
                textLabels[i].setBackground(Color.lightGray);
            }
        }
    }

    public void processKeyCode(int keyCode, char keyChar) {
        DebugInputTextArea.setText("" + keyCode + "\n" + DebugInputTextArea.getText());
        if (acceptInput) {
            if ((App.L.enableBackSpace) && (keyCode == KeyEvent.VK_BACK_SPACE)) {
                //System.err.println("Backspace consumed!");
                if (textTypedPosition > 0) {
                    textTypedPosition--;
                    regenerateText(true, false); // Regenerate GUI
                    Director.regenerateFirst(); // Reset first director (might be playing/finished) destroy errors after/beore it
                    insertDirector(textToType.substring(textTypedPosition, textTypedPosition + 1));
                    Director.playFirst();
                }
            } else {
                if (Director.consumeKeyDown(keyCode, keyChar)) {
                    textTypedPosition++;
                }
                regenerateText(false, false);
                goodPointCount.setText("" + L.goodCount);
                badPointCount.setText("" + L.badCount);
                misTypeCount.setText("" + L.misTypeCount);
                L.regeneratePassPanel(passLabel);
                Director.playFirst();
            }
        }
    }

    public void insertDirector(String nextChar) {
        if (!EXPECTED_KEYCODES.containsKey(nextChar)) {
            App.redAlert("Error: unhandled next char in GUI: " + nextChar + " in " + textToType);
        }
        int targetKeyCode = EXPECTED_KEYCODES.get(nextChar);

        if ("\u23CE".equals(nextChar)) {
            Director.addNew("enter.wav", targetKeyCode, '\n');
        } else if (SPECIAL_FILENAMES.containsKey(nextChar.toLowerCase())) {
            if (CAPITAL_LETTERS.contains(nextChar)) {
                Director.addNew(new String[]{"shift.wav", SPECIAL_FILENAMES.get(nextChar.toLowerCase())}, targetKeyCode, nextChar.charAt(0));
            } else {
                Director.addNew(SPECIAL_FILENAMES.get(nextChar), targetKeyCode, nextChar.charAt(0));
            }
        } else {
            if (CAPITAL_LETTERS.contains(nextChar)) {
                Director.addNew(new String[]{"shift.wav", nextChar.toLowerCase() + ".wav"}, targetKeyCode, nextChar.charAt(0));
            } else {
                Director.addNew(nextChar + ".wav", targetKeyCode, nextChar.charAt(0));
            }
        }
    }

    public void startLecture(boolean nextLineMode) {
        String[] helloFileNames = new String[0];

        textTypedPosition = 0;

        if (nextLineMode) {
            textToType = L.getNextLine();
        } else {
            textToType = L.getCurrentLine();
            try {
                helloFileNames = L.getHelloFilesNames();
            } catch (IOException E) {
                App.redAlert(E.toString());
            }
            L.resetCounts();
        }

        regenerateText(true, true);

        L.regeneratePassPanel(passLabel);

        Director.addNew(null, -3); // Director uses a stack (FILO) - "generate results" command goes in first, comes out last

//        Director.insertMissingDirectorsFrom(textToType, false);
        for (int i = textToType.length() - 1; i >= 0; i--) {
            String nextChar = textToType.substring(i, i + 1);
            insertDirector(nextChar);
        }

        if (!nextLineMode) {
            Director.addNew("uss_egy_szokozt_ha_kezdhetjuk.wav", -1);
            for (int h = helloFileNames.length - 1; h >= 0; h--) {
                Director.addNew(helloFileNames[h], -1);
            }
            Director.addNew("hello.wav", -1);
        }
        setVisible(true);
        textPanel.requestFocusInWindow();

        acceptInput = true;

        Director.playFirst();
    }

    public void setIntensity(int Value, boolean isGreen) {
        if (!debugMode) {
            if ((Value < 0) || (Value > 255)) {
                throw new RuntimeException("GUI.setIntensity() Value is " + Value);
            }
            visualiserPanel.setBackground(colorTable[Value + (isGreen ? 0 : 256)]);
            visualiserPanel.updateUI();
        }
    }

    public void registerKeyHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent KE) {
                if (KE.getID() == KeyEvent.KEY_TYPED) {
                    if (HANDLED_AS_KEYCODES.contains("" + KE.getKeyChar())) {
                        // Ignore, handled as extendedkeycode
                        return (true);
                    } else {
                        System.err.println("DEBUG: Unhandled KEY_TYPED: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                    }
                } else if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    System.err.println("DEBUG: KEY_PRESSED: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                    int EKC = KE.getExtendedKeyCode();
                    if ((EXPECTED_KEYCODES.containsValue(EKC)) || (EKC == KeyEvent.VK_ESCAPE)) {
                        processKeyCode(EKC, KE.getKeyChar());
                        return (true);
                    } else if (EKC == KeyEvent.VK_CAPS_LOCK || EKC == KeyEvent.VK_SHIFT) {
                        textPanel.setBackground(textPanel.getBackground() == Color.WHITE ? Color.CYAN : Color.WHITE);
                        // Caps lock press or shift down
                        return (false);
                    } else if (EKC == KeyEvent.VK_BACK_SPACE) {
                        processKeyCode(EKC, KE.getKeyChar());
                        return (true);
                    } else if (NUMPAD_KEYCODES.containsValue(EKC)) {
                        // TODO: FIXME: Add lecture option to disable numpad?
                        System.err.println("DEBUG: Numpad KEY_PRESSED: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                        processKeyCode(NUMPAD_MAPPER.get(KE.getExtendedKeyCode()), KE.getKeyChar());
                        return (true);
                    }
                    System.err.println("DEBUG: Unhanled KEY_PRESSED: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                } else if ((KE.getID() == KeyEvent.KEY_RELEASED) && (KE.getExtendedKeyCode() == KeyEvent.VK_SHIFT)) {
                    textPanel.setBackground(textPanel.getBackground() == Color.WHITE ? Color.CYAN : Color.WHITE);
                    // Shift up
                }
                return (false);
            }
        });

    }

    public GUI(Sounder S) {
        setTitle("TUGIP " + App.VERSION);

        for (int i = 0; i < textLabels.length; i++) {
            JLabel TL = new JLabel(" ");
            textLabels[i] = TL;
            TL.setOpaque(true);
            textPanel.add(TL);
            TL.setFont(FONT72);
            TL.setVerticalAlignment(SwingConstants.CENTER);
            TL.setHorizontalAlignment(SwingConstants.CENTER);
            // TL.setPreferredSize(new Dimension(1000, 600));
        }

        for (int i = 0; i < 256; i++) {
            colorTable[i] = new Color(i << 8);
            colorTable[i + 256] = new Color(i << 16);
        }
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);

        goodPointCount.setForeground(Color.GREEN);
        goodPointCount.setBackground(Color.WHITE);
        goodPointCount.setOpaque(true);
        goodPointCount.setFont(FONT36);
        northPanel.add(goodPointCount);
        badPointCount.setForeground(Color.RED);
        badPointCount.setBackground(Color.WHITE);
        badPointCount.setOpaque(true);
        badPointCount.setFont(FONT36);
        northPanel.add(badPointCount);
        misTypeCount.setForeground(Color.ORANGE);
        misTypeCount.setBackground(Color.WHITE);
        misTypeCount.setOpaque(true);
        misTypeCount.setFont(FONT36);
        northPanel.add(misTypeCount);
        passLabel.setForeground(Color.BLACK);
        passLabel.setBackground(Color.WHITE);
        passLabel.setOpaque(true);
        passLabel.setFont(FONT36);
        northPanel.add(passLabel);
        northPanel.add(visualiserPanel);

        GridBagLayout GBL = new GridBagLayout();
        this.setLayout(GBL);
        GridBagConstraints GBC = new GridBagConstraints();
        GBC.gridx = 1;
        GBC.fill = GridBagConstraints.BOTH;
        GBC.weightx = 1;
        GBC.weighty = 10;
        this.add(northPanel, GBC);
        GBC.weighty = 20;
        this.add(textPanel, GBC);
        GBC.weighty = 1;
        this.add(aboutPanel, GBC);
        aboutPanel.setBackground(Color.LIGHT_GRAY);

        JPanel aboutUpperPanel = new JPanel(new GridLayout(1, 4));
        JPanel aboutLowerPanel = new JPanel(new GridLayout(1, 2));
        aboutPanel.add(aboutUpperPanel);
        aboutPanel.add(aboutLowerPanel);
        aboutUpperPanel.add(new JLabel("Tugip v. " + App.VERSION, SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Gépírás tankönyv: Rácz Hajnalka", SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Projektmenedzser: Dr. Nógrádi Judit", SwingConstants.CENTER));
        aboutUpperPanel.add(new JLabel("Szoftverfejlesztő: Dr. Nagy Elemér Károly", SwingConstants.CENTER));
        aboutLowerPanel.add(new JLabel("A projektet a Gyengénlátók Általános Iskolája, EGYMI és Kollégiuma támogatta.", SwingConstants.CENTER));
        aboutLowerPanel.add(new JLabel("A projektet az FSF.hu Alapítvány a Szabad Szoftver Pályázat 2022 keretén belül támogatta.", SwingConstants.CENTER));
        textPanel.setBackground(Color.WHITE);
        /*
        textPanel.add(textLabel, BorderLayout.CENTER);
        textLabel.setFont(FONT144);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(1000, 600));
         */
        if (debugMode) {
            visualiserPanel.add(debugPanel, BorderLayout.NORTH);
        }
        debugPanel.setBackground(Color.yellow);
        DebugDirectorTextArea.setEditable(false);
        DebugDirectorTextArea.setPreferredSize(new Dimension(450, 150));
        debugPanel.add(DebugDirectorTextArea);
        DebugInputTextArea.setEditable(false);
        DebugInputTextArea.setPreferredSize(new Dimension(450, 150));
        debugPanel.add(DebugInputTextArea);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        registerKeyHandler();
    }

    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
