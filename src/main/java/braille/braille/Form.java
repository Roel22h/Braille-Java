package braille.braille;

import com.fazecast.jSerialComm.SerialPort;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private final Arduino arduino;

    private final String vowelsExercise = "aeiou";
    private final String numbresExercise = "1234567890";
    private final String letersExercise = "bcdfghjklmnpqrstvwxyz";
    private final String alphabetExercise = "abcdefghijklmnpoqrstuvwxyz";
    private int randomLevel = 0;
    private ButtonGroup rbGroup;

    public Form() {
        initComponents();
        this.arduino = new Arduino();
    }

    public void loadPortsList() {
        SerialPort[] ports = Arduino.getSerialPorts();
        boolean conectedStatus = false;

        for (int i = 0; i < ports.length; i++) {
            if (this.arduino.connect(i)) {
                conectedStatus = true;
                break;
            }
        }

        if (conectedStatus) {
            lbConnectionStatus.setText("CONECTADO");
            lbConnectionStatus.setForeground(Color.BLUE);
            intListener();
            enableForm(true);
        } else {
            lbConnectionStatus.setText("DESCONECTADO");
            lbConnectionStatus.setForeground(Color.red);

            JOptionPane.showMessageDialog(this, "Error al intentar conectarse con el módulo Arduino.");

            enableForm(false);
        }
    }

    public void setRadioButtonGroup() {
        ButtonGroup group = new ButtonGroup();
        rbGroup = group;

        rbGroup.add(rbNumbers);
        rbGroup.add(rbVowels);
        rbGroup.add(rbLetters);
        rbGroup.add(rbCustomText);
        rbGroup.add(rbAlphabet);
    }

    public void intListener() {
        if ((this.arduino.isConnected())) {
            SerialPort serialPort = this.arduino.getSerialPort();

            Thread serialThread;
            serialThread = new Thread(new Runnable() {
                @Override
                public void run() {
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
                        }   // Leer datos disponibles
                        byte[] buffer = new byte[serialPort.bytesAvailable()];
                        int bytesRead = serialPort.readBytes(buffer, buffer.length);
                        // Procesar los datos recibidos
                        if (bytesRead > 0) {
                            String receivedData;
                            receivedData = new String(buffer, 0, bytesRead);                            
                            System.out.println("RECIBIDO " + receivedData);
                            
                            switch (receivedData) {
                                case "1", "0" ->
                                    Form.this.arduino.setAnswer(receivedData);
                                case "N" -> {
                                    if (Form.this.arduino.testIsFinished()) {
                                        Form.this.arduino.setScore();
                                        setResultTest();
                                        Form.this.arduino.optionRepearTest();
                                    } else {
                                        Form.this.arduino.sendText();
                                    }
                                }
                                case "A" -> {
                                    Form.this.arduino.playFinalOptionAudio("again");
                                    startTest();
                                }
                                case "F" -> {
                                    enableForm(true);
                                    Form.this.arduino.playFinalOptionAudio("end");
                                }
                                case "S" -> {
                                    String charRepeat = Form.this.arduino.getCharacterText();
                                    Form.this.arduino.playCharacterAudio(charRepeat);
                                }
                                default -> {
                                    JOptionPane.showMessageDialog(Form.this, "Respuesta de Arduino no identificada.");
                                    throw new AssertionError();
                                }
                            }
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
        cbExerciseType.setEnabled(status);

        tfStudent.setEnabled(status);
        btnStart.setEnabled(status);

        setRandomLevelStatus();
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
        if (!(this.arduino.isConnected())) {
            return;
        }

        if (tfStudent.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el nombre del estudiante.");
            return;
        }

        DefaultTableModel jTable = (DefaultTableModel) jtResponse.getModel();
        jTable.setRowCount(0);

        String exerciseText = "";
        JRadioButton selectedRadioButton = getSelectedRadioButton(rbGroup);
        String rbText = selectedRadioButton.getText();

        switch (rbText) {
            case "Vocales" ->
                exerciseText = this.vowelsExercise;

            case "Números" ->
                exerciseText = this.numbresExercise;

            case "Letras" ->
                exerciseText = this.letersExercise;

            case "Alfabeto" ->
                exerciseText = this.alphabetExercise;

            case "Personalizado" -> {
                exerciseText = getCustomTxtSerialized((tfCustomText.getText()).toLowerCase());
                if ("".equals(exerciseText)) {
                    return;
                }
            }

            default ->
                JOptionPane.showMessageDialog(this, "Selecione un ejercicio.");
        }

        enableForm(false);
        this.arduino.initProperties(rbText, exerciseText, this.randomLevel);
        this.arduino.sendText();
    }

    public void setTxtOutput(String exerciseText) {
        String txtExcersiceOutput = String.join(",", (exerciseText).chars().mapToObj(c -> String.valueOf((char) c)).toArray(String[]::new));
        this.tfCustomText.setText(txtExcersiceOutput);
    }

    public String getCustomTxtSerialized(String customText) {
        int customTextLength = customText.length();
        
        if(customTextLength > 60){
            JOptionPane.showMessageDialog(this, "El texto ingresado supera el límite de 30 caracteres como máximo.");
            return "";
        }
        
        if (isValidFormat(customText)) {
            return customText.replaceAll(",", "");
        } else {
            JOptionPane.showMessageDialog(this, "El texto ingresado no cumple con el formato válido.");
            return "";
        }
    }

    public static boolean isValidFormat(String input) {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9](,[a-zA-Z0-9])*(,[a-zA-Z0-9])?$");
        Matcher matcher = pattern.matcher(input);
        boolean isValid = matcher.matches();
        return isValid;
    }

    public void setResultTest() {
        int arraysLength = this.arduino.getArraysLength();
        int score = this.arduino.getScore();

        ArrayList<String> listText = this.arduino.getListText();
        ArrayList<String> listShuffleText = this.arduino.getListShuffleText();
        ArrayList<Integer> listSolution = this.arduino.getListSolution();
        ArrayList<Integer> listAnswers = this.arduino.getListAnswers();

        DefaultTableModel jTable = (DefaultTableModel) jtResponse.getModel();
        jTable.setRowCount(0);
        for (int i = 0; i < arraysLength; i++) {
            Object[] rowData = {listText.get(i), listShuffleText.get(i), listSolution.get(i), listAnswers.get(i)};
            jTable.addRow(rowData);
        }

        lbScore.setText(score + "/" + arraysLength);
        this.arduino.playScoreAudio(score, arraysLength);
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
        lbConnectionStatus = new javax.swing.JLabel();
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
        tfStudent = new javax.swing.JTextField();
        lbStudent = new javax.swing.JLabel();
        btnStart = new javax.swing.JToggleButton();
        lbResultTitle = new javax.swing.JLabel();
        lbScore = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtResponse = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        lbExcerciseType = new javax.swing.JLabel();
        cbExerciseType = new javax.swing.JComboBox<>();
        lbRandomLevel = new javax.swing.JLabel();
        cbRandomLevel = new javax.swing.JComboBox<>();
        lbOptions = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lbTitle.setFont(new java.awt.Font("Dialog", 0, 24)); // NOI18N
        lbTitle.setText("JUGANDO CON EL ALFABETO BRAILLE");

        lbConnectionStatus.setForeground(new java.awt.Color(204, 0, 0));
        lbConnectionStatus.setText("DESCONECTADO");

        lbExercises.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbExercises.setText("Ejercicios");

        lbParameters.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbParameters.setText("Parametros");

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        rbVowels.setSelected(true);
        rbVowels.setText("Vocales");
        rbVowels.setEnabled(false);
        rbVowels.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbVowelsMouseClicked(evt);
            }
        });

        rbNumbers.setText("Números");
        rbNumbers.setEnabled(false);
        rbNumbers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbNumbersMouseClicked(evt);
            }
        });

        rbLetters.setText("Letras");
        rbLetters.setEnabled(false);
        rbLetters.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbLettersMouseClicked(evt);
            }
        });

        rbAlphabet.setText("Alfabeto");
        rbAlphabet.setEnabled(false);
        rbAlphabet.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbAlphabetMouseClicked(evt);
            }
        });

        rbCustomText.setText("Personalizado");
        rbCustomText.setEnabled(false);
        rbCustomText.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbCustomTextMouseClicked(evt);
            }
        });

        tfCustomText.setText("a,e,i,o,u");
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
                        .addGap(0, 144, Short.MAX_VALUE)))
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

        tfStudent.setEnabled(false);

        lbStudent.setText("Estudiante");

        btnStart.setText("Empezar juego");
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
                    .addComponent(tfStudent)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(lbStudent)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 73, Short.MAX_VALUE)
                        .addComponent(btnStart)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbStudent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfStudent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnStart)
                .addContainerGap())
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

        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lbExcerciseType.setText("Tipo de ejercico");

        cbExerciseType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Secuencial", "Aleatorio" }));
        cbExerciseType.setEnabled(false);
        cbExerciseType.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbExerciseTypeItemStateChanged(evt);
            }
        });

        lbRandomLevel.setText("Nivel de aleatoriedad");

        cbRandomLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Muy baja", "Baja", "Media", "Alta", "Muy alta" }));
        cbRandomLevel.setEnabled(false);
        cbRandomLevel.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbRandomLevelItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lbExcerciseType, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(112, 112, 112))
                    .addComponent(lbRandomLevel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cbRandomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cbExerciseType, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbExcerciseType)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbExerciseType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbRandomLevel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbRandomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lbOptions.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        lbOptions.setText("Opciones");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(lbExercises)
                                .addGap(175, 175, 175)
                                .addComponent(lbOptions)
                                .addGap(158, 158, 158)
                                .addComponent(lbParameters)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 139, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(lbConnectionStatus)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lbResultTitle)
                                .addGap(38, 38, 38)
                                .addComponent(lbScore)
                                .addGap(18, 18, 18))))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 176, Short.MAX_VALUE)
                .addComponent(lbTitle)
                .addGap(73, 73, 73))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbExercises)
                            .addComponent(lbParameters))
                        .addGap(26, 26, 26))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(lbOptions)
                        .addGap(18, 18, 18)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbResultTitle)
                    .addComponent(lbScore)
                    .addComponent(lbConnectionStatus))
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

    private void btnStartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnStartMouseClicked

        startTest();
    }//GEN-LAST:event_btnStartMouseClicked

    private void rbVowelsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbVowelsMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.vowelsExercise);
    }//GEN-LAST:event_rbVowelsMouseClicked

    private void rbNumbersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbNumbersMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.numbresExercise);
    }//GEN-LAST:event_rbNumbersMouseClicked

    private void rbLettersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbLettersMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.letersExercise);
    }//GEN-LAST:event_rbLettersMouseClicked

    private void rbAlphabetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbAlphabetMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.alphabetExercise);
    }//GEN-LAST:event_rbAlphabetMouseClicked

    private void rbCustomTextMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbCustomTextMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(true);
        tfCustomText.setText("");
    }//GEN-LAST:event_rbCustomTextMouseClicked

    private void setRandomLevelStatus() {
        if ((String) cbExerciseType.getSelectedItem() == "Secuencial") {
            cbRandomLevel.setSelectedItem("Muy baja");
            this.randomLevel = 0;
            cbRandomLevel.setEnabled(false);
        } else {
            cbRandomLevel.setEnabled(true);
        }
    }

    private void cbExerciseTypeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbExerciseTypeItemStateChanged
        // TODO add your handling code here:
        setRandomLevelStatus();
    }//GEN-LAST:event_cbExerciseTypeItemStateChanged

    private void cbRandomLevelItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbRandomLevelItemStateChanged
        // TODO add your handling code here:
        String randomLevel = (String) cbRandomLevel.getSelectedItem();

        switch (randomLevel) {
            case "Muy baja":
                this.randomLevel = 1;
                break;
            case "Baja":
                this.randomLevel = 3;
                break;
            case "Media":
                this.randomLevel = 5;
                break;
            case "Alta":
                this.randomLevel = 7;
                break;
            case "Muy alta":
                this.randomLevel = 10;
                break;
            default:
                throw new AssertionError();
        }
    }//GEN-LAST:event_cbRandomLevelItemStateChanged

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
        java.awt.EventQueue.invokeLater(() -> {
            new Form().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnStart;
    private javax.swing.JComboBox<String> cbExerciseType;
    private javax.swing.JComboBox<String> cbRandomLevel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jtResponse;
    private javax.swing.JLabel lbConnectionStatus;
    private javax.swing.JLabel lbExcerciseType;
    private javax.swing.JLabel lbExercises;
    private javax.swing.JLabel lbOptions;
    private javax.swing.JLabel lbParameters;
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
