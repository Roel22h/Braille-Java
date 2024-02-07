package braille.braille;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author roel_
 */
public class Form extends javax.swing.JFrame {

    private Arduino arduino;

    private String vowelsExercise = "aeiou";
    private String numbresExercise = "1234567890";
    private String letersExercise = "bcdfghjklmnpqrstvwxyz";
    private String alphabetExercise = "abcdefghijklmnpoqrstuvwxyz";
    private String customExercise = "";

    private ButtonGroup rbGroup;

    public Form() {
        initComponents();
        arduino = new Arduino();
        loadPortsList();
        setRadioButtonGroup();
    }

    private void loadPortsList() {
        this.cbPorts.removeAllItems();
        SerialPort[] ports = Arduino.getSerialPorts();

        for (int i = 0; i < ports.length; i++) {
            this.cbPorts.addItem(ports[i].getSystemPortName());
        }
    }

    private void setRadioButtonGroup() {
        ButtonGroup group = new ButtonGroup();
        rbGroup = group;

        rbGroup.add(rbNumbers);
        rbGroup.add(rbVowels);
        rbGroup.add(rbLetters);
        rbGroup.add(rbCustomText);
        rbGroup.add(rbAlphabet);
    }

    public void intListener() {
        if ((arduino.isConnected())) {
            SerialPort serialPort = arduino.getSerialPort();

            Thread serialThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    // Esperar a que haya datos disponibles
                    while (serialPort.bytesAvailable() == 0) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            // Manejar interrupciones del hilo
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    // Leer datos disponibles
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int bytesRead = serialPort.readBytes(buffer, buffer.length);

                    // Procesar los datos recibidos
                    if (bytesRead > 0) {

                        String receivedData = new String(buffer, 0, bytesRead);

                        switch (receivedData) {
                            case "1":
                            case "0":
                                arduino.setAnswer(receivedData);
                                break;

                            case "N":
                                if (arduino.testIsFinished()) {
                                    arduino.setScore();
                                    setResultTest();

                                    arduino.optionRepearTest();
                                } else {
                                    arduino.sendText();
                                }
                                break;
                            case "A":
                                arduino.clearLists();
                                arduino.playAnswStatusAudio("again");
                                startTest();
                                break;
                            case "F":
                                enableForm(true);
                                arduino.clearLists();
                                arduino.playAnswStatusAudio("end");
                                break;
                            default:
                                JOptionPane.showMessageDialog(this, "Respuesta de Arduino no identificada.");
                                throw new AssertionError();
                        }
                    }
                }
            });

            serialThread.start();
        } else {
            JOptionPane.showMessageDialog(this, "No existe conexión al módulo Arduino.");
        }
    }

    private void enableForm(boolean status) {
        rbAlphabet.setEnabled(status);
        rbVowels.setEnabled(status);
        rbNumbers.setEnabled(status);
        rbLetters.setEnabled(status);
        rbCustomText.setEnabled(status);

        cbRandomLevel.setEnabled(status);

        tfStudent.setEnabled(status);
        btnStart.setEnabled(status);
    }

    private JRadioButton getSelectedRadioButton(ButtonGroup group) {
        Enumeration<AbstractButton> buttons = group.getElements();
        while (buttons.hasMoreElements()) {
            JRadioButton button = (JRadioButton) buttons.nextElement();
            if (button.isSelected()) {
                return button;
            }
        }
        return null;
    }

    private void startTest() {
        if (!(arduino.isConnected())) {
            return;
        }

        if (tfStudent.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el nombre del estudiante.");
            return;
        }
        enableForm(false);
        DefaultTableModel jTable = (DefaultTableModel) jtResponse.getModel();
        jTable.setRowCount(0);

        String exerciseText = "";
        int level = Integer.parseInt((String) cbRandomLevel.getSelectedItem());

        JRadioButton selectedRadioButton = getSelectedRadioButton(rbGroup);
        String rbText = selectedRadioButton.getText();

        switch (rbText) {
            case "Vocales":
                exerciseText = this.vowelsExercise;
                break;

            case "Números":
                exerciseText = this.numbresExercise;
                break;

            case "Letras":
                exerciseText = this.letersExercise;
                break;

            case "Alfabeto":
                exerciseText = this.alphabetExercise;
                break;

            case "Personalizado":
                exerciseText = (tfCustomText.getText()).toLowerCase();
                customExercise = exerciseText;
                break;

            default:
                JOptionPane.showMessageDialog(this, "Selecione un ejercicio.");
        }

        arduino.initProperties(rbText, exerciseText, level);
        arduino.sendText();
    }

    public void setResultTest() {
        int arraysLength = arduino.getArraysLength();
        int score = arduino.getScore();

        ArrayList<String> listText = arduino.getListText();
        ArrayList<String> listShuffleText = arduino.getListShuffleText();
        ArrayList<Integer> listSolution = arduino.getListSolution();
        ArrayList<Integer> listAnswers = arduino.getListAnswers();

        DefaultTableModel jTable = (DefaultTableModel) jtResponse.getModel();
        jTable.setRowCount(0);
        for (int i = 0; i < arraysLength; i++) {
            Object[] rowData = {listText.get(i), listShuffleText.get(i), listSolution.get(i), listAnswers.get(i)};
            jTable.addRow(rowData);
        }

        lbScore.setText(score + "/" + arraysLength);
        arduino.playScoreAudio(score, arraysLength);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        lbTitle = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbPorts = new javax.swing.JLabel();
        cbPorts = new javax.swing.JComboBox<>();
        lbConnectedStatusTitle = new javax.swing.JLabel();
        lbConnectionStatus = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        btnConnect = new javax.swing.JButton();
        lbExercises = new javax.swing.JLabel();
        lbParameters = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        rbVowels = new javax.swing.JRadioButton();
        rbNumbers = new javax.swing.JRadioButton();
        rbLetters = new javax.swing.JRadioButton();
        rbAlphabet = new javax.swing.JRadioButton();
        rbCustomText = new javax.swing.JRadioButton();
        tfCustomText = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        lbRandomLevel = new javax.swing.JLabel();
        tfStudent = new javax.swing.JTextField();
        cbRandomLevel = new javax.swing.JComboBox<>();
        lbStudent = new javax.swing.JLabel();
        btnStart = new javax.swing.JToggleButton();
        lbResultTitle = new javax.swing.JLabel();
        lbScore = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtResponse = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lbTitle.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        lbTitle.setText("Alfabeto Braille");

        lbPorts.setText("Puertos");

        lbConnectedStatusTitle.setText("Estado:");

        lbConnectionStatus.setForeground(new java.awt.Color(204, 0, 0));
        lbConnectionStatus.setText("DESCONECTADO");

        btnConnect.setText("Conectar");
        btnConnect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnConnectMouseClicked(evt);
            }
        });

        lbExercises.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbExercises.setText("Ejercicios");

        lbParameters.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbParameters.setText("Parametros");

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        rbVowels.setSelected(true);
        rbVowels.setText("Vocales");
        rbVowels.setEnabled(false);

        rbNumbers.setText("Números");
        rbNumbers.setEnabled(false);

        rbLetters.setText("Letras");
        rbLetters.setEnabled(false);

        rbAlphabet.setText("Alfabeto");
        rbAlphabet.setEnabled(false);

        rbCustomText.setText("Personalizado");
        rbCustomText.setEnabled(false);
        rbCustomText.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbCustomTextStateChanged(evt);
            }
        });

        tfCustomText.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfCustomText)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(rbVowels)
                            .addComponent(rbNumbers)
                            .addComponent(rbLetters)
                            .addComponent(rbAlphabet)
                            .addComponent(rbCustomText))
                        .addGap(0, 101, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(rbVowels)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbNumbers)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbLetters)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbAlphabet)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbCustomText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfCustomText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lbRandomLevel.setText("Nivel de aleatoriedad");

        tfStudent.setEnabled(false);

        cbRandomLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        cbRandomLevel.setEnabled(false);

        lbStudent.setText("Estudiante");

        btnStart.setText("Iniciar");
        btnStart.setEnabled(false);
        btnStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnStartMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lbRandomLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tfStudent)
                        .addComponent(cbRandomLevel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(lbStudent))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnStart)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(lbRandomLevel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbRandomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(lbStudent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfStudent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnStart)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbResultTitle.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbResultTitle.setText("Resultado");

        lbScore.setText("0/0");

        jtResponse.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Texto", "Txt. Random", "Solución", "Respuestas"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jtResponse);

        jScrollPane1.setViewportView(jScrollPane2);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSeparator1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lbPorts)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(cbPorts, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnConnect)
                                        .addGap(46, 46, 46)
                                        .addComponent(lbConnectedStatusTitle)
                                        .addGap(18, 18, 18)
                                        .addComponent(lbConnectionStatus)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lbExercises)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lbParameters)
                                        .addGap(152, 152, 152))))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(lbResultTitle)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lbScore))
                                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(157, 157, 157)
                        .addComponent(lbTitle)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbPorts)
                    .addComponent(cbPorts, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lbConnectedStatusTitle)
                    .addComponent(lbConnectionStatus)
                    .addComponent(btnConnect))
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbExercises)
                    .addComponent(lbParameters))
                .addGap(29, 29, 29)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbResultTitle)
                    .addComponent(lbScore))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConnectMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnConnectMouseClicked

        int i = this.cbPorts.getSelectedIndex();

        if (arduino.connect(i)) {
            lbConnectionStatus.setText("CONECTADO");
            lbConnectionStatus.setForeground(Color.BLUE);

            JOptionPane.showMessageDialog(this, "Conectado");

            intListener();
            enableForm(true);
        } else {
            lbConnectionStatus.setText("DESCONECTADO");
            lbConnectionStatus.setForeground(Color.red);

            JOptionPane.showMessageDialog(this, "Error al intentar conectarse con el módulo Arduino.");

            enableForm(false);
        }
    }//GEN-LAST:event_btnConnectMouseClicked

    private void rbCustomTextStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbCustomTextStateChanged

        if (rbCustomText.isSelected()) {
            tfCustomText.setEnabled(true);
        } else {
            tfCustomText.setEnabled(false);
            tfCustomText.setText("");
        }
    }//GEN-LAST:event_rbCustomTextStateChanged

    private void btnStartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnStartMouseClicked

        startTest();
    }//GEN-LAST:event_btnStartMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Form.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Form().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConnect;
    private javax.swing.JToggleButton btnStart;
    private javax.swing.JComboBox<String> cbPorts;
    private javax.swing.JComboBox<String> cbRandomLevel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTable jtResponse;
    private javax.swing.JLabel lbConnectedStatusTitle;
    private javax.swing.JLabel lbConnectionStatus;
    private javax.swing.JLabel lbExercises;
    private javax.swing.JLabel lbParameters;
    private javax.swing.JLabel lbPorts;
    private javax.swing.JLabel lbRandomLevel;
    private javax.swing.JLabel lbResultTitle;
    private javax.swing.JLabel lbScore;
    private javax.swing.JLabel lbStudent;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JRadioButton rbAlphabet;
    private javax.swing.JRadioButton rbCustomText;
    private javax.swing.JRadioButton rbLetters;
    private javax.swing.JRadioButton rbNumbers;
    private javax.swing.JRadioButton rbVowels;
    private javax.swing.JTextField tfCustomText;
    private javax.swing.JTextField tfStudent;
    // End of variables declaration//GEN-END:variables
}
