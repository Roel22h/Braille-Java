package braille.braille;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.URL;
import javax.sound.sampled.*;

/**
 *
 * @author roel_
 */
public class Arduino {

    private SerialPort serialPort;
    private String exercise;
    private String characterText;
    private String randomCharacterText;

    private final ArrayList<String> listText = new ArrayList<>();
    private final ArrayList<String> listShuffleText = new ArrayList<>();
    private final ArrayList<Integer> listSolution = new ArrayList<>();
    private final ArrayList<Integer> listAnswers = new ArrayList<>();

    private int score = 0;
    private int arraysLength = 0;
    private int index = 0;

//  CONSTANS ANSWERS
    static final String FILE_NAME_ANNW_CORRECT_IS = "correct_is";
    static final String FILE_NAME_ANNW_CORRECT_NOT_IS = "correct_not_is";
    static final String FILE_NAME_ANNW_INCORRECT_IS = "incorrect_is";
    static final String FILE_NAME_ANNW_INCORRECT_NOT_IS = "incorrect_not_is";

    static final String FILE_NAME_END_OR_AGAIN = "end_or_again";

    static final String INPUT_CORRECT_VALUE = "1";
    static final String INPUT_INCORRECT_VALUE = "0";

    static final String INPUT_AGAIN_VALUE = "A";
    static final String INPUT_NEXT_VALUE = "N";

//  GETTERS
    public String getCharacterText() {
        return this.characterText;
    }

    public SerialPort getSerialPort() {
        return this.serialPort;
    }

    public int getScore() {
        return this.score;
    }

    public int getArraysLength() {
        return this.arraysLength;
    }

    public int getIndex() {
        return this.index;
    }

    public ArrayList getListText() {
        return this.listText;
    }

    public ArrayList getListShuffleText() {
        return this.listShuffleText;
    }

    public ArrayList getListSolution() {
        return this.listSolution;
    }

    public ArrayList getListAnswers() {
        return this.listAnswers;
    }

    public String getInputCorrectValue() {
        return this.INPUT_CORRECT_VALUE;
    }

    public String getInputIncorrectValue() {
        return this.INPUT_INCORRECT_VALUE;
    }

    public String getInputAgainValue() {
        return this.INPUT_AGAIN_VALUE;
    }

    public String getInputNextValue() {
        return this.INPUT_NEXT_VALUE;
    }

//  CONEXION  
    public static SerialPort[] getSerialPorts() {
        SerialPort ports[] = SerialPort.getCommPorts();
        return ports;
    }

    public boolean connect(int indice) {
        this.serialPort = SerialPort.getCommPorts()[indice];
        this.serialPort.openPort();
        this.serialPort.setComPortParameters(9600, 8, 1, 0);
        this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        return isConnected();
    }

    public boolean isConnected() {
        return this.serialPort.isOpen();
    }

    public boolean disconnect() {
        return this.serialPort.closePort();
    }

//  SET DE PROPIEDADES
    public void initProperties(String exercise, String character, int level) {
        System.out.println("Difultad: " + level);
        setExercise(exercise);
        setListText(character); //ASIGNANDO TEXTO
        setListyShuffleText(level); //BARAJEANDO TEXTO
        setListSolution(); //ESTABLECIENDO SOLUCION
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public void setListText(String text) {
        this.arraysLength = text.length();

        char[] charArray = text.toCharArray();
        for (int i = 0; i < this.arraysLength; i++) {
            this.listText.add(String.valueOf(charArray[i]));
            this.listShuffleText.add(String.valueOf(charArray[i]));
        }
    }

    public void setListyShuffleText(int randomProbability) {
        Random random = new Random();
        int intercambios = (int) (this.arraysLength * (randomProbability / 10.0));

        for (int i = 0; i < intercambios; i++) {
            int indice1 = random.nextInt(this.arraysLength);
            int indice2 = random.nextInt(this.arraysLength);

            String temp = this.listShuffleText.get(indice1);
            this.listShuffleText.set(indice1, this.listShuffleText.get(indice2));
            this.listShuffleText.set(indice2, temp);
        }
    }

    public void setListSolution() {
        for (int i = 0; i < this.arraysLength; i++) {
            int answ = this.listText.get(i).equals(this.listShuffleText.get(i)) ? 1 : 0;
            this.listSolution.add(answ);
        }
    }

    // EJECUCION
    public boolean sendText() {
        try {

            this.characterText = this.listText.get(this.index);
            this.randomCharacterText = this.listShuffleText.get(this.index);

            playCharacterAudio(this.characterText);

            this.serialPort.getOutputStream().write(this.randomCharacterText.getBytes());
            this.serialPort.getOutputStream().flush();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public void setAnswer(String data) {
        Integer intAnswer = Integer.parseInt(data);
        String fileName = "";

        if (intAnswer.equals(this.listSolution.get(this.index))) {
            if (this.listText.get(this.index).equals(this.listShuffleText.get(this.index))) {
                fileName = FILE_NAME_ANNW_CORRECT_IS;
            } else {
                fileName = FILE_NAME_ANNW_CORRECT_NOT_IS;
            }
        } else {
            if (this.listText.get(this.index).equals(this.listShuffleText.get(this.index))) {
                fileName = FILE_NAME_ANNW_INCORRECT_IS;
            } else {
                fileName = FILE_NAME_ANNW_INCORRECT_NOT_IS;
            }
        }

        playAnswStatusAudio(fileName);

        this.listAnswers.add(intAnswer);
        this.index++;
    }

    public void setScore() {
        for (int i = 0; i < this.arraysLength; i++) {
            if (this.listAnswers.get(i) == this.listSolution.get(i)) {
                this.score++;
            }
        }
    }

    public void clearLists() {
        this.exercise = "";
        this.characterText = "";
        this.randomCharacterText = "";

        this.listText.clear();
        this.listShuffleText.clear();
        this.listSolution.clear();
        this.listAnswers.clear();

        this.arraysLength = 0;
        this.score = 0;
        this.index = 0;
    }

    public boolean testIsFinished() {
        return (this.index + 1 > this.arraysLength);
    }

    public boolean optionRepearTest() {
        try {
            this.serialPort.getOutputStream().write("?".getBytes());
            this.serialPort.getOutputStream().flush();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    // REPRODUCIOR SONIDO CARACTER
    public void playCharacterAudio(String fileName) {
        try {
            URL soundURL = searchCharacterSound(fileName);

            if (soundURL != null) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundURL);
                if (audioInputStream != null) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInputStream);
                    clip.start();
                } else {
                    System.err.println("No se pudo obtener el AudioInputStream de la letra/número.");
                }
            } else {
                System.err.println("No se pudo encontrar el archivo de sonido de la letra/número.");
            }

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public URL searchCharacterSound(String fileName) {
        String exercisePath = "";

        switch (this.exercise) {
            case "Vocales":
                exercisePath = "letters";
                break;

//            case "Números":
//                exercisePath = "numbers";
//                break;

            case "Letras":
                exercisePath = "letters";
                break;

            case "Alfabeto":
                exercisePath = "letters";
                break;

            case "Personalizado":
                URL soundURL = null;
                soundURL = getClass().getResource("/audio/letters/" + fileName + ".wav");
                if (soundURL == null) {
                    soundURL = getClass().getResource("/audio/numbers/" + fileName + ".wav");
                }

                return soundURL;

            default:
                System.err.println("Carpeta del sonido no identificada.");
                return null;
        }

        String audioURL = "/audio/" + exercisePath + "/" + fileName + ".wav";
        return getClass().getResource(audioURL);
    }

    // REPRODUCIOR SONIDO ESTADO RESPUESTA
    public void playAnswStatusAudio(String fileName) {
        try {
            URL soundAswURL = getClass().getResource("/audio/answers/" + fileName + ".wav");
            URL sountCharURL = searchCharacterSound(this.listText.get(this.index));

            if (soundAswURL != null && sountCharURL != null) {
                AudioInputStream audioInputAswStream = AudioSystem.getAudioInputStream(soundAswURL);
                // AudioInputStream audioInputCharStream = AudioSystem.getAudioInputStream(sountCharURL);
                if (audioInputAswStream != null) {
                    try {
                        Clip clipScore = AudioSystem.getClip();
                        clipScore.open(audioInputAswStream);
                        clipScore.start();

//                        clipScore.addLineListener(eventAsw -> {
//                            if (eventAsw.getType() == LineEvent.Type.STOP) {
//                                try {
//                                    clipScore.close();
//
//                                    Clip clipTotal = AudioSystem.getClip();
//                                    clipTotal.open(audioInputCharStream);
//                                    clipTotal.start();
//                                } catch (IOException | LineUnavailableException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        });

                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("No se pudo obtener el AudioInputStream de la respuesta.");
                }
            } else {
                System.err.println("No se pudo encontrar el archivo de la respuesta.");
            }

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    // REPRODUCIOR SONIDO RESULTADO
    public void playScoreAudio(int score, int total) {
        try {
            String scoreFileName = String.valueOf(score);
            String totalFileName = String.valueOf(total);

            URL soundScoreURL = getClass().getResource("/audio/results/score/" + scoreFileName + ".wav");
            URL soundTotalURL = getClass().getResource("/audio/results/total/" + totalFileName + ".wav");
            URL soundEndOrAgainURL = getClass().getResource("/audio/answers/" + FILE_NAME_END_OR_AGAIN + ".wav");

            if (soundScoreURL != null && soundTotalURL != null && soundEndOrAgainURL != null) {
                AudioInputStream audioInputScoreStream = AudioSystem.getAudioInputStream(soundScoreURL);
                AudioInputStream audioInputTotalStream = AudioSystem.getAudioInputStream(soundTotalURL);
                AudioInputStream audioInputEndOrAgainStream = AudioSystem.getAudioInputStream(soundEndOrAgainURL);

                if (audioInputScoreStream != null && audioInputTotalStream != null && audioInputEndOrAgainStream != null) {

                    try {
                        Clip clipScore = AudioSystem.getClip();
                        clipScore.open(audioInputScoreStream);
                        clipScore.start();

                        clipScore.addLineListener(event -> {
                            if (event.getType() == LineEvent.Type.STOP) {
                                try {
                                    clipScore.close();

                                    Clip clipTotal = AudioSystem.getClip();
                                    clipTotal.open(audioInputTotalStream);
                                    clipTotal.start();

                                    clipTotal.addLineListener(event2 -> {
                                        if (event2.getType() == LineEvent.Type.STOP) {
                                            try {
                                                clipTotal.close();

                                                Clip clipEndOrAgainTotal = AudioSystem.getClip();
                                                clipEndOrAgainTotal.open(audioInputEndOrAgainStream);
                                                clipEndOrAgainTotal.start();

                                            } catch (IOException | LineUnavailableException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                } catch (IOException | LineUnavailableException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                    }

                } else {
                    System.err.println("No se pudo obtener el AudioInputStream del resultado.");
                }
            } else {
                System.err.println("No se pudo encontrar el archivo de sonido del resultado.");
            }

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    public void playFinalOptionAudio(String fileName) {
        try {
            URL soundURL = getClass().getResource("/audio/final_options/" + fileName + ".wav");

            if (soundURL != null) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundURL);
                if (audioInputStream != null) {
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInputStream);
                    clip.start();

                    try {
                        clearLists();
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("No se pudo obtener el AudioInputStream de la letra/número.");
                }
            } else {
                System.err.println("No se pudo encontrar el archivo de sonido de la letra/número.");
            }

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
