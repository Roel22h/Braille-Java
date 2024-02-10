/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package braille.braille;

/**
 *
 * @author roel_
 */
public class Braille {

    public static void main(String[] args) {
        Form form = new Form();
        form.setVisible(true);
        form.loadPortsList();
        form.setRadioButtonGroup();
    }
}
