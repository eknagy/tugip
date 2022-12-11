/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


public class GUI extends JFrame {
    JPanel VisualiserPanel=new JPanel();
    JPanel TextPanel=new JPanel(new BorderLayout());
    JLabel TextLabel=new JLabel("<html><div style='text-align: center;'>fffff<br />jjjjj<br />fjfjfjfjfj<br />fj fj fj fj fj</div><html>");
    JPanel ProgressPanel=new JPanel();
    Color [] colorTable = new Color [2*256];
    
    public void setIntensity(int Value, boolean isGreen) {
        if ((Value<0) || (Value>255)) {
            throw new RuntimeException("GUI.setIntensity() Value is "+Value);
        }
        VisualiserPanel.setBackground(colorTable[Value+(isGreen?0:256)]);
    }
    
    public GUI() {
        for (int i=0; i<256; i++) {
            colorTable[i]=new Color(i<<8);
            colorTable[i+256]=new Color(i<<16);
        }
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        GridBagLayout GBL=new GridBagLayout();
        this.setLayout(GBL);
        GridBagConstraints GBC = new GridBagConstraints();
        GBC.gridx=1;
        GBC.fill=GridBagConstraints.BOTH;
        GBC.weightx=1;
        GBC.weighty=3;
        this.add(VisualiserPanel, GBC);
        GBC.weighty=9;
        this.add(TextPanel, GBC);
        GBC.weighty=1;
        this.add(ProgressPanel, GBC);
        pack();
        setVisible(true);
        TextPanel.setBackground(Color.white);
        TextPanel.add(TextLabel, BorderLayout.CENTER);
        TextLabel.setBackground(Color.yellow);
        TextLabel.setOpaque(true);
        TextLabel.setFont(new Font("Courier", Font.BOLD, 144));
        TextLabel.setVerticalAlignment(SwingConstants.CENTER);
        TextLabel.setHorizontalAlignment(SwingConstants.CENTER);
        ProgressPanel.setBackground(Color.MAGENTA);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
