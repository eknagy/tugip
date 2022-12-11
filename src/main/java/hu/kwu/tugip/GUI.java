/**
 * @author Dr. Nagy Elemér Károly
 * @license GPLv3
 */
package hu.kwu.tugip;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;


public class GUI extends JFrame {
    Color [] colorTable = new Color [2*256];
    
    public void setIntensity(int Value, boolean isGreen) {
        if ((Value<0) || (Value>255)) {
            throw new RuntimeException("GUI.setIntensity() Value is "+Value);
        }
        this.getContentPane().setBackground(colorTable[Value+(isGreen?0:256)]);
    }
    
    public GUI() {
        for (int i=0; i<256; i++) {
            colorTable[i]=new Color(i<<8);
            colorTable[i+256]=new Color(i<<16);
        }
        setPreferredSize(new Dimension(200,200));
        pack();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    public void close() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));;
    }
}
