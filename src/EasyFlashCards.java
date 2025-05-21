import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;

public class EasyFlashCards {

    static File CSVfile = null;
    static String fcLanguage = null;
    static String trLanguage = null;
    static Map<String, String> flashcards = new HashMap<String, String>();

    static int row = 1;

    private static void cleanAndRefresh(Screen screen) throws IOException {
        screen.clear();
        screen.refresh();
    }

    private static void titlePrint(Screen screen, TextGraphics tg, String subTitle) throws IOException {
        TerminalSize size = screen.getTerminalSize();

        String mainTitle = " ==== EASY FLASHCARDS ==== ";
        int titleCol = (size.getColumns() - mainTitle.length()) / 2;
        tg.putString(titleCol, row, mainTitle);
        row += 2;

        int subtitleCol = (size.getColumns() - subTitle.length()) / 2;

        tg.putString(subtitleCol, row, subTitle);
        screen.refresh();
        row += 2;
    }

    private static void footerPrint(Screen screen, TextGraphics tg) throws IOException {
        TerminalSize size = screen.getTerminalSize();

        String mark = "v1.0 – developed by ilaario";
        int markCol = (size.getColumns() - mark.length()) / 2;
        tg.putString(markCol, size.getRows() - 2, mark);

        screen.refresh();
    }

    private static void enterFooterPrint(Screen screen, TextGraphics tg) throws IOException {
        TerminalSize size = screen.getTerminalSize();

        String footer = "Press ENTER to continue";
        int footerCol = (size.getColumns() - footer.length()) / 2;
        tg.putString(footerCol, size.getRows() - 4, footer);

        screen.refresh();

        // aspetto Invio
        KeyStroke key;
        do {
            key = screen.readInput();
        } while (key.getKeyType() != KeyType.Enter);
    }

    static void loadFlashCards(Screen screen) throws IOException {
        row = 1;

        cleanAndRefresh(screen);

        TextGraphics tg = screen.newTextGraphics();
        TerminalSize size = screen.getTerminalSize();

        titlePrint(screen, tg, "LOAD CSV");

        footerPrint(screen, tg);

        boolean isFirstLine = true;
        if (CSVfile == null) {
            tg.putString(2, row, "Select the CSV file to load.");
            screen.refresh();
            row += 2;
            tg.putString(2, row,"Use a CSV to load the words and create flashcards.");
            row += 2;
            tg.putString(2, row,"Remember that the CSV file has to contain");
            row++;
            tg.putString(2, row,"words and translations, in this order:");
            row += 2;
            tg.putString(2, row,"---------------------------------------");
            row++;
            tg.putString(2, row,"|       Lang       |       Tran       |");
            row++;
            tg.putString(2, row,"|------------------|------------------|");
            row++;
            tg.putString(2, row,"|        W1        |        T1        |");
            row++;
            tg.putString(2, row,"|        W2        |        T2        |");
            row++;
            tg.putString(2, row,"|        W3        |        T2        |");
            row++;
            tg.putString(2, row,"|        W4        |        T2        |");
            row++;
            tg.putString(2, row,"---------------------------------------");
            row += 2;
            screen.refresh();

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            chooser.setDialogTitle("Select a CSV file");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            FileFilter csvFilter = new FileNameExtensionFilter("File CSV (*.csv)", "csv");
            chooser.setFileFilter(csvFilter);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                CSVfile = chooser.getSelectedFile();
                tg.putString(2, row,"You have chosen: " + CSVfile.getAbsolutePath());
                row += 2;
                screen.refresh();
            } else {
                tg.putString(2, row,"No file selected!");
                row += 2;
                screen.refresh();

                enterFooterPrint(screen, tg);
            }
        } else {
            tg.putString(2, row, "!! DEBUG !! - CSV file passed by args[]");
            screen.refresh();
            row += 2;
        }
        File csv = CSVfile;

        if (CSVfile == null) {
            enterFooterPrint(screen, tg);
        } else {
            long totalLines;
            try {
                totalLines = Files.lines(Path.of(csv.getPath())).count() - 1;
            } catch (IOException e) {
                throw new RuntimeException("Impossibile contare le righe del CSV", e);
            }

            long processed = 0;
            final int BAR_WIDTH = 50;

            try (Scanner scanner = new Scanner(csv)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();

                    if (isFirstLine) {
                        // header
                        String[] columns = line.split(",");
                        fcLanguage = columns[0];
                        trLanguage = columns[1];
                        isFirstLine = false;
                        tg.putString(2, row, "Language: " + fcLanguage);
                        row++;
                        tg.putString(2, row, "Translation: " + trLanguage);
                        row += 2;
                        screen.refresh();
                        continue;
                    }

                    String[] columns = line.split(",");
                    flashcards.put(columns[0], columns[1]);
                    processed++;

                    int percent = (int)(processed * 100 / totalLines);
                    int filled = (int)(processed * BAR_WIDTH / totalLines);

                    StringBuilder bar = new StringBuilder("[");
                    for (int i = 0; i < BAR_WIDTH; i++) {
                        bar.append(i < filled ? "=" : " ");
                    }
                    bar.append("] ").append(percent).append("%");

                    tg.putString(2, row, bar.toString());
                    screen.refresh();

                    TimeUnit.MILLISECONDS.sleep(25);
                }
                row += 2;
                tg.putString(2, row,"Loading completed!");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            enterFooterPrint(screen, tg);
        }
    }

    static int runMenu(Screen screen, List<String> options) throws IOException {
        int selected = 0;

        TerminalSize size = screen.getTerminalSize();

        while (true) {
            row = 1;
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();

            titlePrint(screen, tg, "MAIN MENU");

            for (int i = 0; i < options.size(); i++) {
                if (i == selected) {
                    tg.setBackgroundColor(TextColor.ANSI.WHITE);
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                } else {
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                }
                // disegna l’opzione alla riga i, colonna 2
                tg.putString(2, row + i, options.get(i));
            }

            // 4) Un footer (ad esempio versione o copyright)
            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);

            if(CSVfile == null) {
                String mark = "NO CSV FILE FOUND";
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            } else {
                String mark = "CSV File = " + CSVfile.getPath();
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            }

            String mark = "v1.0 – developed by ilaario";
            int footerCol = (size.getColumns() - mark.length()) / 2;
            tg.putString(footerCol, size.getRows() - 2, mark);
            screen.refresh();

            // 3.2) Leggi il tasto
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp) {
                selected = (selected - 1 + options.size()) % options.size();
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                selected = (selected + 1) % options.size();
            } else if (key.getKeyType() == KeyType.Enter) {
                break;
            }
        }

        if (selected > options.size()) {
            throw new RuntimeException("Error with menu handling - ask ilaario for help :(");
        }
        return selected;
    }

    static void showFlashCards(Screen screen, String title) throws IOException {
        int row = 1;

        screen.clear();
        screen.refresh();

        TextGraphics tg = screen.newTextGraphics();
        TerminalSize size = screen.getTerminalSize();

        String mainTitle = " ==== EASY FLASHCARDS ==== ";
        int titleCol = (size.getColumns() - mainTitle.length()) / 2;
        tg.putString(titleCol, row, mainTitle);
        row += 2;

        int subtitleCol = (size.getColumns() - title.length()) / 2;

        tg.putString(subtitleCol, row, title);
        screen.refresh();
        row += 2;

        String mark = "v1.0 – developed by ilaario";
        int markCol = (size.getColumns() - mark.length()) / 2;
        tg.putString(markCol, size.getRows() - 2, mark);
        screen.refresh();

        if(flashcards.isEmpty()) {
            String NoFCWarn = "!!! NO FLASHCARDS FOUND !!!";
            int NoFCWarnCol = (size.getColumns() - NoFCWarn.length()) / 2;
            int NoFCWarnRow = size.getRows() / 2;
            tg.putString(NoFCWarnCol, NoFCWarnRow, NoFCWarn);
            screen.refresh();
        } else {
            int totalFlashcards = flashcards.size();
            int usableLines = size.getRows() - row;
            int usableCol = size.getColumns() - 4;

            int spaceBetween = usableCol / 3;

            int row2 = row;
            int iter = 0;
            for (Map.Entry<String, String> entry : flashcards.entrySet()) {
                tg.putString((2 + (spaceBetween * iter)), row2,entry.getKey() + " -> " + entry.getValue());
                totalFlashcards--;
                if(row2 == usableLines - 1 && iter == 2) {
                    tg.putString((2 + (spaceBetween * iter)), row2,"And " + totalFlashcards + " more...                           ");
                    break;
                }
                row2++;
                if (row2 == usableLines) {
                    row2 = row;
                    iter++;
                }
            }
        }
        // CODE

        enterFooterPrint(screen, tg);
    }

    static int flashCardsExercises(Screen screen, String title, List<String> options) throws IOException {
        int selected = 0;

        TerminalSize size = screen.getTerminalSize();

        while (true) {
            row = 1;
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();

            titlePrint(screen, tg, title);

            for (int i = 0; i < options.size(); i++) {
                if (i == selected) {
                    tg.setBackgroundColor(TextColor.ANSI.WHITE);
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                } else {
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                }
                tg.putString(2, row + i, options.get(i));
            }

            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);

            if(CSVfile == null) {
                String mark = "NO CSV FILE FOUND";
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            } else {
                String mark = "CSV File = " + CSVfile.getPath();
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            }

            footerPrint(screen, tg);

            // 3.2) Leggi il tasto
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp) {
                selected = (selected - 1 + options.size()) % options.size();
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                selected = (selected + 1) % options.size();
            } else if (key.getKeyType() == KeyType.Enter) {
                break;
            }
        }

        if (selected > options.size()) {
            throw new RuntimeException("Error with menu handling - ask ilaario for help :(");
        }
        return selected;
    }

    static void ex4tr(Screen screen, String title, int exercises) throws IOException {
        for(int i = 0; i < exercises; i++){
            row = 1;
            cleanAndRefresh(screen);

            TerminalSize size = screen.getTerminalSize();
            TextGraphics tg = screen.newTextGraphics();
            titlePrint(screen, tg, title);
            footerPrint(screen, tg);

            String counter = "Exercise n°" + (i + 1);
            int countCol = (size.getColumns() - counter.length()) / 2;
            tg.putString(countCol, row, counter);

            Random random = new Random();
            List<String> translations = new ArrayList<>(flashcards.values());

            // Get random entry from flashcards
            int randomIndex = random.nextInt(flashcards.size());
            String[] keys = flashcards.keySet().toArray(new String[0]);
            String word = keys[randomIndex];
            String correctTranslation = flashcards.get(word);

            // Get 3 random wrong translations
            List<String> wrongTranslations = new ArrayList<>();
            while (wrongTranslations.size() < 3) {
                String randomTranslation = translations.get(random.nextInt(translations.size()));
                if (!randomTranslation.equals(correctTranslation) && !wrongTranslations.contains(randomTranslation)) {
                    wrongTranslations.add(randomTranslation);
                }
            }

            System.out.println(word + " " + correctTranslation + " " + wrongTranslations);

            int spaceRows = size.getRows() / 3;
            int spaceCols = size.getColumns() / 3;
            int wordCol = (size.getColumns() - word.length()) / 2;
            tg.putString(wordCol, spaceRows, word);
            
            boolean correctTr = false;
            int correctIndex = -1;
            
            if(random.nextInt(100) < 25) {
                correctTr = true;
                int correctCol = spaceCols - ((correctTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) - 2;
                tg.putString(correctCol, correctRow, "1. " + correctTranslation);
                correctIndex = 1;
            } else {
                String randomTranslation = wrongTranslations.get(random.nextInt(wrongTranslations.size()));
                wrongTranslations.remove(randomTranslation);

                int correctCol = spaceCols - ((randomTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) - 2;
                tg.putString(correctCol, correctRow, "1. " + randomTranslation);
            }

            if(random.nextInt(100) < 50 && !correctTr) {
                correctTr = true;
                int correctCol = (spaceCols + spaceCols) - ((correctTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) - 2;
                tg.putString(correctCol, correctRow, "2. " + correctTranslation);
                correctIndex = 2;
            } else {
                String randomTranslation = wrongTranslations.get(random.nextInt(wrongTranslations.size()));
                wrongTranslations.remove(randomTranslation);

                int correctCol = (spaceCols + spaceCols) - ((randomTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) - 2;
                tg.putString(correctCol, correctRow, "2. " + randomTranslation);
            }

            if(random.nextInt(100) < 75 && !correctTr) {
                correctTr = true;
                int correctCol = spaceCols - ((correctTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) + 2;
                tg.putString(correctCol, correctRow, "3. " + correctTranslation);
                correctIndex = 3;
            } else {
                String randomTranslation = wrongTranslations.get(random.nextInt(wrongTranslations.size()));
                wrongTranslations.remove(randomTranslation);

                int correctCol = spaceCols - ((randomTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) + 2;
                tg.putString(correctCol, correctRow, "3. " + randomTranslation);
            }

            if(!correctTr) {
                int correctCol = (spaceCols + spaceCols) - ((correctTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) + 2;
                tg.putString(correctCol, correctRow, "4. " + correctTranslation);
                correctIndex = 4;
            } else {
                String randomTranslation = wrongTranslations.getFirst();
                wrongTranslations.remove(randomTranslation);

                int correctCol = (spaceCols + spaceCols) - ((randomTranslation.length() / 2) + 3);
                int correctRow = (spaceRows * 2) + 2;
                tg.putString(correctCol, correctRow, "4. " + randomTranslation);
            }

            screen.refresh();

            int guess = -1;
            boolean waitForInput = false;
            while (!waitForInput) {
                KeyStroke key = screen.readInput();
                KeyType type = key.getKeyType();

                if (type == KeyType.Character) {
                    char c = key.getCharacter();
                    switch (c) {
                        case '1':
                            guess = 1;
                            waitForInput = true;
                            break;
                        case '2':
                            guess = 2;
                            waitForInput = true;
                            break;
                        case '3':
                            guess = 3;
                            waitForInput = true;
                            break;
                        case '4':
                            guess = 4;
                            waitForInput = true;
                            break;
                        default:
                            // altri caratteri, ignora
                    }
                }
                else if (type == KeyType.Escape) {
                    return;
                }
            }

            if (guess == correctIndex) {
                String correctOptString = "Correct translation!";
                int markCol = (size.getColumns() - correctOptString.length()) / 2;
                tg.putString(markCol, size.getRows() - 6, correctOptString);
            } else {
                String notCorrectOptString = "Wrong translation! The correct one is n°" + correctIndex;
                int markCol = (size.getColumns() - notCorrectOptString.length()) / 2;
                tg.putString(markCol, size.getRows() - 6, notCorrectOptString);
            }

            enterFooterPrint(screen, tg);
        }
    }

    static void extof(Screen screen, String title) throws IOException {
        row = 1;
        cleanAndRefresh(screen);

        TextGraphics tg = screen.newTextGraphics();
        titlePrint(screen, tg, title);

        footerPrint(screen, tg);
    }

    static void exwrtr(Screen screen, String title) throws IOException {
        row = 1;
        cleanAndRefresh(screen);

        TextGraphics tg = screen.newTextGraphics();
        titlePrint(screen, tg, title);

        footerPrint(screen, tg);
    }

    static void ex4trMenu(Screen screen, String title, List<String> options) throws IOException {
        int selected = 0;
        int nExercises = 1;

        TerminalSize size = screen.getTerminalSize();

        while (true) {
            row = 1;
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();

            titlePrint(screen, tg, title);

            for (int i = 0; i < options.size(); i++) {
                if (i == selected) {
                    tg.setBackgroundColor(TextColor.ANSI.WHITE);
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                } else {
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.setForegroundColor(TextColor.ANSI.DEFAULT);
                }
                tg.putString(2, row + i, options.get(i));
            }

            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);

            if(CSVfile == null) {
                String mark = "NO CSV FILE FOUND";
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            } else {
                String mark = "CSV File = " + CSVfile.getPath();
                int footerCol = (size.getColumns() - mark.length()) / 2;
                tg.putString(footerCol, size.getRows() - 4, mark);
                screen.refresh();
            }

            String stringEx = "N° of exercises = " + nExercises;
            int markCol = (size.getColumns() - stringEx.length()) / 2;
            tg.putString(markCol, size.getRows() - 6, stringEx);
            footerPrint(screen, tg);

            // 3.2) Leggi il tasto
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp) {
                selected = (selected - 1 + options.size()) % options.size();
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                selected = (selected + 1) % options.size();
            } else if (key.getKeyType() == KeyType.Enter) {
                switch (selected) {
                    case 0:
                        ex4tr(screen, title, nExercises);
                        return;
                    case 1:
                        nExercises += 1;
                        break;
                    case 2:
                        nExercises += 5;
                        break;
                    case 3:
                        nExercises += 10;
                        break;
                    case 4:
                        nExercises -= 10;
                        break;
                    case 5:
                        nExercises -= 5;
                        break;
                    case 6:
                        nExercises -= 1;
                        break;
                    case 7:
                        return;
                    default:
                        throw new RuntimeException("Error with menu handling - ask ilaario for help :(");
                }
            }
        }
    }



    public static void main(String[] args) throws IOException {
        SwingTerminalFontConfiguration fontConfig =
                SwingTerminalFontConfiguration.getDefaultOfSize(18);

        TerminalSize size = new TerminalSize(120, 40);

        DefaultTerminalFactory factory = new DefaultTerminalFactory()
                .setTerminalEmulatorFontConfiguration(fontConfig)
                .setInitialTerminalSize(size)
                .setTerminalEmulatorTitle("Easy FlashCards");;

        Terminal terminal = factory.createTerminalEmulator();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.doResizeIfNecessary();
        screen.setCursorPosition(null);


        if(args.length != 0){
            try {
                CSVfile = new File(args[0]);
                loadFlashCards(screen);
            } catch (NullPointerException e) {
                throw new RuntimeException(e);
            }
        }

        // Ciclo principale sui menu
        boolean running = true;
        while (running) {
            int mainChoice = runMenu(screen, Arrays.asList(
                    // "Load CSV",
                    CSVfile != null ? "Load CSV - DONE" : "Load CSV",
                    "Show Flashcards",
                    "Exercises",
                    "Quit"));

            switch (mainChoice) {
                case 0:
                    loadFlashCards(screen);      // chiamo il metodo che gestisce il sotto-menu Carica CSV
                    break;

                case 1:
                    // Sotto‐menu “Mostra Flashcard”
                    showFlashCards(screen, "SHOW FLASHCARDS");
                    break;

                case 2:
                    // Sotto‐menu “Mostra Flashcard”
                    int exChoice = flashCardsExercises(screen, "EXERCISES", Arrays.asList("4 Translations", "True or False", "Write the translation", "Back"));

                    switch (exChoice) {
                        case 0:
                            ex4trMenu(screen, "4 TRANSLATIONS", Arrays.asList(
                                    "Start",
                                    "+1 Exercise",
                                    "+5 Exercises",
                                    "+10 Exercises",
                                    "-10 Exercises",
                                    "-5 Exercises",
                                    "-1 Exercise",
                                    "Back"));
                            break;
                        case 1:
                            extof(screen, "TRUE OR FALSE");
                            break;
                        case 2:
                            exwrtr(screen, "WRITE THE TRANSLATION");
                            break;
                        case 3:
                            break;
                    }

                    break;

                case 3:
                    running = false;  // Esci dall’app
                    break;
            }
        }

        screen.stopScreen();
        System.out.println("Applicazione terminata.");
    }
}
