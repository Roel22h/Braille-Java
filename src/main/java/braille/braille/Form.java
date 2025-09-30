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

//        rbGroup.add(rbNumbers);
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
//        rbNumbers.setEnabled(status);
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
        String texto = String.join(", ", this.arduino.getListShuffleText());
        tfRandom.setText(texto);
        this.arduino.sendText();
    }

    public void setTxtOutput(String exerciseText) {
        String txtExcersiceOutput = String.join(",", (exerciseText).chars().mapToObj(c -> String.valueOf((char) c)).toArray(String[]::new));
        this.tfCustomText.setText(txtExcersiceOutput);
    }

    public String getCustomTxtSerialized(String customText) {
        int customTextLength = customText.length();

        if (customTextLength > 60) {
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
            String solution = listSolution.get(i).equals(1) ? "Sí es" : "No es";
            String answer = listAnswers.get(i).equals(1) ? "Sí es" : "No es";

            Object[] rowData = {listText.get(i), listShuffleText.get(i), solution, answer};
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

        pGeneral = new javax.swing.JPanel();
        spTable = new javax.swing.JScrollPane();
        jtResponse = new javax.swing.JTable();
        lbConnectionStatus = new javax.swing.JLabel();
        lbResultTitle = new javax.swing.JLabel();
        lbScore = new javax.swing.JLabel();
        lbTitle = new javax.swing.JLabel();
        pStudent = new javax.swing.JPanel();
        lbStudent = new javax.swing.JLabel();
        tfStudent = new javax.swing.JTextField();
        pParameters = new javax.swing.JPanel();
        lbExcerciseType = new javax.swing.JLabel();
        cbExerciseType = new javax.swing.JComboBox<>();
        lbRandomLevel = new javax.swing.JLabel();
        cbRandomLevel = new javax.swing.JComboBox<>();
        pGame = new javax.swing.JPanel();
        rbVowels = new javax.swing.JRadioButton();
        rbLetters = new javax.swing.JRadioButton();
        rbAlphabet = new javax.swing.JRadioButton();
        rbCustomText = new javax.swing.JRadioButton();
        tfCustomText = new javax.swing.JTextField();
        pImage = new javax.swing.JPanel();
        lbImage = new javax.swing.JLabel();
        pResult = new javax.swing.JPanel();
        tfRandom = new javax.swing.JTextField();
        btnStart = new javax.swing.JToggleButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        pGeneral.setBackground(new java.awt.Color(250, 250, 250));
        pGeneral.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jtResponse.setBackground(new java.awt.Color(255, 255, 255));
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
        spTable.setViewportView(jtResponse);

        pGeneral.add(spTable, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 430, 680, 230));

        lbConnectionStatus.setFont(new java.awt.Font("Roboto", 1, 14)); // NOI18N
        lbConnectionStatus.setForeground(new java.awt.Color(204, 0, 0));
        lbConnectionStatus.setText("DESCONECTADO");
        pGeneral.add(lbConnectionStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 670, -1, -1));

        lbResultTitle.setFont(new java.awt.Font("Roboto", 1, 14)); // NOI18N
        lbResultTitle.setForeground(new java.awt.Color(51, 51, 51));
        lbResultTitle.setText("Resultado");
        pGeneral.add(lbResultTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 670, -1, -1));

        lbScore.setFont(new java.awt.Font("Roboto", 1, 14)); // NOI18N
        lbScore.setForeground(new java.awt.Color(51, 51, 51));
        lbScore.setText("0/0");
        pGeneral.add(lbScore, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 670, -1, -1));

        lbTitle.setFont(new java.awt.Font("Roboto", 1, 24)); // NOI18N
        lbTitle.setForeground(new java.awt.Color(51, 51, 51));
        lbTitle.setText("APU BRAILLE GAME");
        pGeneral.add(lbTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 10, -1, -1));

        pStudent.setBackground(new java.awt.Color(255, 255, 255));
        pStudent.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(41, 43, 45)), "STUDENT", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Roboto", 1, 18), new java.awt.Color(51, 51, 51))); // NOI18N

        lbStudent.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        lbStudent.setForeground(new java.awt.Color(51, 51, 51));
        lbStudent.setText("Name");

        tfStudent.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        tfStudent.setEnabled(false);

        javax.swing.GroupLayout pStudentLayout = new javax.swing.GroupLayout(pStudent);
        pStudent.setLayout(pStudentLayout);
        pStudentLayout.setHorizontalGroup(
            pStudentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pStudentLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pStudentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pStudentLayout.createSequentialGroup()
                        .addComponent(lbStudent)
                        .addGap(0, 251, Short.MAX_VALUE))
                    .addComponent(tfStudent))
                .addContainerGap())
        );
        pStudentLayout.setVerticalGroup(
            pStudentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pStudentLayout.createSequentialGroup()
                .addComponent(lbStudent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 8, Short.MAX_VALUE))
        );

        pGeneral.add(pStudent, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, 310, 100));

        pParameters.setBackground(new java.awt.Color(255, 255, 255));
        pParameters.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(41, 43, 45)), "PARAMETERS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Roboto", 1, 18), new java.awt.Color(51, 51, 51))); // NOI18N

        lbExcerciseType.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        lbExcerciseType.setForeground(new java.awt.Color(51, 51, 51));
        lbExcerciseType.setText("Tipo de ejercico");

        cbExerciseType.setBackground(new java.awt.Color(255, 255, 255));
        cbExerciseType.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        cbExerciseType.setForeground(new java.awt.Color(51, 51, 51));
        cbExerciseType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Secuencial", "Aleatorio" }));
        cbExerciseType.setEnabled(false);
        cbExerciseType.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbExerciseTypeItemStateChanged(evt);
            }
        });

        lbRandomLevel.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        lbRandomLevel.setForeground(new java.awt.Color(51, 51, 51));
        lbRandomLevel.setText("Nivel de aleatoriedad");

        cbRandomLevel.setBackground(new java.awt.Color(255, 255, 255));
        cbRandomLevel.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        cbRandomLevel.setForeground(new java.awt.Color(51, 51, 51));
        cbRandomLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Muy baja", "Baja", "Media", "Alta", "Muy alta" }));
        cbRandomLevel.setEnabled(false);
        cbRandomLevel.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbRandomLevelItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout pParametersLayout = new javax.swing.GroupLayout(pParameters);
        pParameters.setLayout(pParametersLayout);
        pParametersLayout.setHorizontalGroup(
            pParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbExerciseType, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cbRandomLevel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pParametersLayout.createSequentialGroup()
                        .addGroup(pParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbExcerciseType, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbRandomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 158, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pParametersLayout.setVerticalGroup(
            pParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pParametersLayout.createSequentialGroup()
                .addComponent(lbExcerciseType)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbExerciseType, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addComponent(lbRandomLevel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbRandomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pGeneral.add(pParameters, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 210, 310, 200));

        pGame.setBackground(new java.awt.Color(255, 255, 255));
        pGame.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(41, 43, 45)), "GAME", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Roboto", 1, 18), new java.awt.Color(0, 0, 0))); // NOI18N

        rbVowels.setBackground(new java.awt.Color(255, 255, 255));
        rbVowels.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        rbVowels.setForeground(new java.awt.Color(51, 51, 51));
        rbVowels.setSelected(true);
        rbVowels.setText("Vocales");
        rbVowels.setEnabled(false);
        rbVowels.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbVowelsMouseClicked(evt);
            }
        });

        rbLetters.setBackground(new java.awt.Color(255, 255, 255));
        rbLetters.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        rbLetters.setForeground(new java.awt.Color(51, 51, 51));
        rbLetters.setText("Letras");
        rbLetters.setEnabled(false);
        rbLetters.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbLettersMouseClicked(evt);
            }
        });

        rbAlphabet.setBackground(new java.awt.Color(255, 255, 255));
        rbAlphabet.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        rbAlphabet.setForeground(new java.awt.Color(51, 51, 51));
        rbAlphabet.setText("Alfabeto");
        rbAlphabet.setEnabled(false);
        rbAlphabet.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbAlphabetMouseClicked(evt);
            }
        });

        rbCustomText.setBackground(new java.awt.Color(255, 255, 255));
        rbCustomText.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        rbCustomText.setForeground(new java.awt.Color(51, 51, 51));
        rbCustomText.setText("Personalizado");
        rbCustomText.setEnabled(false);
        rbCustomText.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rbCustomTextMouseClicked(evt);
            }
        });
        rbCustomText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbCustomTextActionPerformed(evt);
            }
        });

        tfCustomText.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        tfCustomText.setText("a,e,i,o,u");
        tfCustomText.setEnabled(false);

        javax.swing.GroupLayout pGameLayout = new javax.swing.GroupLayout(pGame);
        pGame.setLayout(pGameLayout);
        pGameLayout.setHorizontalGroup(
            pGameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pGameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pGameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(rbCustomText, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addComponent(tfCustomText, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbAlphabet, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rbLetters, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rbVowels, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pGameLayout.setVerticalGroup(
            pGameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pGameLayout.createSequentialGroup()
                .addComponent(rbVowels, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbLetters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbAlphabet, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rbCustomText, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfCustomText, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(93, 93, 93))
        );

        pGeneral.add(pGame, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 90, 310, 210));

        lbImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/form-image.jpg"))); // NOI18N

        javax.swing.GroupLayout pImageLayout = new javax.swing.GroupLayout(pImage);
        pImage.setLayout(pImageLayout);
        pImageLayout.setHorizontalGroup(
            pImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pImageLayout.setVerticalGroup(
            pImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pImageLayout.createSequentialGroup()
                .addComponent(lbImage, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pGeneral.add(pImage, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 0, 500, 700));

        pResult.setBackground(new java.awt.Color(255, 255, 255));
        pResult.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        tfRandom.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        tfRandom.setEnabled(false);

        btnStart.setFont(new java.awt.Font("Roboto", 0, 14)); // NOI18N
        btnStart.setText("Empezar juego");
        btnStart.setEnabled(false);
        btnStart.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btnStartMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pResultLayout = new javax.swing.GroupLayout(pResult);
        pResult.setLayout(pResultLayout);
        pResultLayout.setHorizontalGroup(
            pResultLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pResultLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pResultLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfRandom)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pResultLayout.createSequentialGroup()
                        .addGap(0, 192, Short.MAX_VALUE)
                        .addComponent(btnStart)))
                .addContainerGap())
        );
        pResultLayout.setVerticalGroup(
            pResultLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pResultLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(tfRandom, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnStart)
                .addContainerGap(9, Short.MAX_VALUE))
        );

        pGeneral.add(pResult, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 310, 310, 100));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pGeneral, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pGeneral, javax.swing.GroupLayout.PREFERRED_SIZE, 699, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btnStartMouseClicked

        startTest();
    }//GEN-LAST:event_btnStartMouseClicked

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

    private void cbExerciseTypeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbExerciseTypeItemStateChanged
        // TODO add your handling code here:
        setRandomLevelStatus();
    }//GEN-LAST:event_cbExerciseTypeItemStateChanged

    private void rbCustomTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbCustomTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rbCustomTextActionPerformed

    private void rbCustomTextMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbCustomTextMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(true);
        tfCustomText.setText("");
    }//GEN-LAST:event_rbCustomTextMouseClicked

    private void rbAlphabetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbAlphabetMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.alphabetExercise);
    }//GEN-LAST:event_rbAlphabetMouseClicked

    private void rbLettersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbLettersMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.letersExercise);
    }//GEN-LAST:event_rbLettersMouseClicked

    private void rbVowelsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rbVowelsMouseClicked
        // TODO add your handling code here:
        tfCustomText.setEnabled(false);
        setTxtOutput(this.vowelsExercise);
    }//GEN-LAST:event_rbVowelsMouseClicked

    private void setRandomLevelStatus() {
        if ((String) cbExerciseType.getSelectedItem() == "Secuencial") {
            cbRandomLevel.setSelectedItem("Muy baja");
            this.randomLevel = 0;
            cbRandomLevel.setEnabled(false);
        } else {
            cbRandomLevel.setEnabled(true);
        }
    }

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
    private javax.swing.JTable jtResponse;
    private javax.swing.JLabel lbConnectionStatus;
    private javax.swing.JLabel lbExcerciseType;
    private javax.swing.JLabel lbImage;
    private javax.swing.JLabel lbRandomLevel;
    private javax.swing.JLabel lbResultTitle;
    private javax.swing.JLabel lbScore;
    private javax.swing.JLabel lbStudent;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel pGame;
    private javax.swing.JPanel pGeneral;
    private javax.swing.JPanel pImage;
    private javax.swing.JPanel pParameters;
    private javax.swing.JPanel pResult;
    private javax.swing.JPanel pStudent;
    private javax.swing.JRadioButton rbAlphabet;
    private javax.swing.JRadioButton rbCustomText;
    private javax.swing.JRadioButton rbLetters;
    private javax.swing.JRadioButton rbVowels;
    private javax.swing.JScrollPane spTable;
    private javax.swing.JTextField tfCustomText;
    private javax.swing.JTextField tfRandom;
    private javax.swing.JTextField tfStudent;
    // End of variables declaration//GEN-END:variables
}
