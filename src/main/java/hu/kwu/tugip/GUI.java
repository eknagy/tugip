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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class GUI extends JFrame {

    private final static HashMap<String, Integer> EXPECTED_KEYCODES = new HashMap<>();

    public static boolean debugMode = false;

    private static String textToType = "";
    private static int textTypedPosition = 0;

    private final static Font FONT144 = new Font("Monospaced", Font.BOLD, 144);
    private final static Font FONT72 = new Font("Monospaced", Font.BOLD, 72);
    private final static Font FONT36 = new Font("Monospaced", Font.BOLD, 36);

    private static boolean acceptInput = false;

    private final JPanel northPanel = new JPanel(new GridLayout(1, 4));
    private final JLabel goodPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel badPointCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel passLabel = new JLabel("??% (??%)", SwingConstants.CENTER);
    private final JPanel visualiserPanel = new JPanel();

    private final static int TEXTPANELROWS = 5;
    private final static int TEXTPANELCOLS = 20;

    private final JPanel textPanel = new JPanel(new GridLayout(TEXTPANELROWS, TEXTPANELCOLS));
    private final JLabel[] textLabels = new JLabel[TEXTPANELCOLS * TEXTPANELROWS];

    private final JPanel debugPanel = new JPanel(new BorderLayout());
    private final JPanel aboutPanel = new JPanel(new GridLayout(2, 1));
    private final Color[] colorTable = new Color[2 * 256];

    public final TextArea D = new TextArea("DEBUG: Loading...");
    public final TextArea DI = new TextArea("DEBUG: Loading...");

    private int lastRegenerateIndex = -1;

    static {
        EXPECTED_KEYCODES.put("\u23CE", KeyEvent.VK_ENTER);
        EXPECTED_KEYCODES.put(" ", KeyEvent.VK_SPACE);
        EXPECTED_KEYCODES.put(",", KeyEvent.VK_COMMA);
        EXPECTED_KEYCODES.put("a", KeyEvent.VK_A);
        EXPECTED_KEYCODES.put("á", 16777441);
        EXPECTED_KEYCODES.put("d", KeyEvent.VK_D);
        EXPECTED_KEYCODES.put("e", KeyEvent.VK_E);
        EXPECTED_KEYCODES.put("é", 16777449);
        EXPECTED_KEYCODES.put("f", KeyEvent.VK_F);
        EXPECTED_KEYCODES.put("g", KeyEvent.VK_G);
        EXPECTED_KEYCODES.put("h", KeyEvent.VK_H);
        EXPECTED_KEYCODES.put("i", KeyEvent.VK_I);
        EXPECTED_KEYCODES.put("j", KeyEvent.VK_J);
        EXPECTED_KEYCODES.put("k", KeyEvent.VK_K);
        EXPECTED_KEYCODES.put("l", KeyEvent.VK_L);
        EXPECTED_KEYCODES.put("m", KeyEvent.VK_M);
        EXPECTED_KEYCODES.put("o", KeyEvent.VK_O);
        EXPECTED_KEYCODES.put("ő", 16777553);
        EXPECTED_KEYCODES.put("r", KeyEvent.VK_R);
        EXPECTED_KEYCODES.put("s", KeyEvent.VK_S);
        EXPECTED_KEYCODES.put("t", KeyEvent.VK_T);
        EXPECTED_KEYCODES.put("u", KeyEvent.VK_U);
        EXPECTED_KEYCODES.put("v", KeyEvent.VK_V);
        EXPECTED_KEYCODES.put("z", KeyEvent.VK_Z);
    }

    public void regenerateText(boolean forceAll) {
        int from = 0;
        int to = 0;
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
                    textLabels[i].setBackground(Color.green);
                } else if (i == textTypedPosition) {
                    textLabels[i].setBackground(Color.yellow);
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

    public void processKeyCode(int keyCode) {
        DI.setText(DI.getText() + " " + keyCode);
        if (acceptInput) {
            if (Director.consumeKeyDown(keyCode)) {
                textTypedPosition++;
                regenerateText(false);
            }
            goodPointCount.setText("" + L.goodCount);
            badPointCount.setText("" + L.badCount);
            L.regeneratePassPanel(passLabel);
            Director.playFirst();
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

        regenerateText(true);

        L.regeneratePassPanel(passLabel);

        Director.addNew(null, -3); // Director uses a stack (FILO) - "generate results" command first

        /*        if (true) {
            Stack<String> tmpStack = new Stack<>();

            Director.addNew(App.SYSTEMSOUNDDIR+"hello.wav", -2);
            for (int i = 100; i >=0; i--) {
                for (String CS : generateNumberFileNames(i)) {
                    Director.addNew(App.NUMBERSSOUNDDIR + CS + ".wav", -2);
                }
            }
            generateEnding(1,2,4,3);
            acceptInput = true;
            Director.play();
            return;
        }
         */
        for (int i = textToType.length() - 1; i >= 0; i--) {
            String nextChar = textToType.substring(i, i + 1);
            if (!EXPECTED_KEYCODES.containsKey(nextChar)) {
                App.redAlert("Error: unhandled next char in GUI: " + nextChar);
            }
            int targetKeyCode = EXPECTED_KEYCODES.get(nextChar);

            if ("\u23CE".equals(nextChar)) {
                nextChar = "enter";
            } else if (" ".equals(nextChar)) {
                nextChar = "space";
            } else if (",".equals(nextChar)) {
                nextChar = "vessz";
            }
            Director.addNew(nextChar + ".wav", targetKeyCode);
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
        if ((Value < 0) || (Value > 255)) {
            throw new RuntimeException("GUI.setIntensity() Value is " + Value);
        }
        visualiserPanel.setBackground(colorTable[Value + (isGreen ? 0 : 256)]);
        visualiserPanel.updateUI();
    }

    public void registerKeyHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent KE) {
                if (KE.getID() == KeyEvent.KEY_PRESSED) {
                    int EKC = KE.getExtendedKeyCode();
                    if ((EXPECTED_KEYCODES.containsValue(EKC)) || (EKC==KeyEvent.VK_ESCAPE)) {
                        processKeyCode(EKC);
                    } else if (EKC == KeyEvent.VK_CAPS_LOCK || EKC == KeyEvent.VK_SHIFT) {
                        textPanel.setBackground(textPanel.getBackground() == Color.WHITE ? Color.CYAN : Color.WHITE);
                        // Caps lock press or shift down
                    } else {
                        System.err.println("DEBUG: Unknown KeyEvent: " + KE + " or " + (0 + KE.getExtendedKeyCode()));
                        // Ignore
                    }
                    return (true);
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
            textPanel.add(debugPanel, BorderLayout.NORTH);

        }
        debugPanel.setBackground(Color.yellow);
        D.setEditable(false);
        D.setPreferredSize(new Dimension(1000, 300));
        DI.setEditable(false);
        DI.setPreferredSize(new Dimension(1000, 50));
        debugPanel.add(D, BorderLayout.CENTER);
        debugPanel.add(DI, BorderLayout.SOUTH);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        registerKeyHandler();
    }

    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
