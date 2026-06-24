import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

class c2settings {

    // ----------------------------------------------------------------------------
    private static final Color BG           = new Color(0x1e, 0x1e, 0x2e);
    private static final Color BG_SECTION   = new Color(0x28, 0x28, 0x3c);
    private static final Color BG_ROW       = new Color(0x2d, 0x2d, 0x42);
    private static final Color BG_ROW_ALT   = new Color(0x32, 0x32, 0x48);
    private static final Color ACCENT       = new Color(0x5c, 0xa4, 0xff);
    private static final Color ACCENT_HOVER = new Color(0x82, 0xbe, 0xff);
    private static final Color ON_COLOR     = new Color(0x4c, 0xc9, 0x80);
    private static final Color OFF_COLOR    = new Color(0xe0, 0x5a, 0x5a);
    private static final Color FG           = new Color(0xe0, 0xe0, 0xf0);
    private static final Color FG_DIM       = new Color(0x90, 0x90, 0xa8);
    private static final Color BORDER_COLOR = new Color(0x40, 0x40, 0x60);

    // ----------------------------------------------------------------------------
    private static final String DIR                 = "settings/";
    private static final String F_ANIM_TOGGLE       = DIR + "fe-animtoggle.txt";
    private static final String F_BLUR_TOGGLE       = DIR + "FE-blurtoggle.txt";
    private static final String F_BLUR_AMOUNT       = DIR + "blurAmount.txt";
    private static final String F_FPS               = DIR + "FE-fpsvalue.txt";
    private static final String F_HZ                = DIR + "FE-hzvalue.txt";
    private static final String F_DISPLAY_FULLSCREEN = DIR + "display-fullscreen.txt";
    private static final String F_DISPLAY_WINDOW_SIZE = DIR + "display-window-size.txt";
    private static final String F_DISABLE_WINDOW_RESIZE = DIR + "disable-window-resize.txt";
    private static final String F_ENEMY_SOUND       = DIR + "od-enable-enemy-sounds.txt";
    private static final String F_SOUND_LIST        = DIR + "UE-oggfiles.txt";
    private static final String F_MUSIC_ENABLED     = DIR + "music-enabled.txt";
    private static final String F_BACKGROUND_COLOR  = DIR + "background-color.txt";
    private static final String F_ANIM_SPEEDS       = DIR + "gameplayAnimationSpeeds.txt";
    private static final String F_COMBO_HELPER      = DIR + "comboGoalHelper.txt";
    private static final String F_CONSOLE_PRINTING  = DIR + "consolePrinting.txt";
    private static final String F_EXPLICIT_TIMER    = DIR + "explicitComboTimer.txt";
    private static final String F_REPLAY            = DIR + "replay_settings.txt";

    private static final String[] SOUND_FILES = {
        "exp-04-menu-bing.ogg", "exp-01-menu-woosh.ogg", "exp-07-book.ogg",
        "exp-08-zap-error.ogg", "exp-09-round-countdown.ogg", "exp-10-round-start.ogg",
        "exp-16-harddrop-pure.ogg", "exp-15-harddrop-pure-effect.ogg", "exp-05-drop-effect.ogg",
        "exp-02-attack-effect.ogg", "exp-03-lines-in-end.ogg", "exp-06-glass-shatter.ogg",
        "exp-11-restricted-bpm-eat.ogg", "exp-12-impressive.ogg", "exp-13-perfect.ogg",
        "exp-14-godlike.ogg"
    };
    private static final String[] SOUND_LABELS = {
        "Menu Bing", "Menu Woosh", "Book",
        "Zap / Error", "Round Countdown", "Round Start",
        "Hard Drop", "Hard Drop Effect", "Soft Drop Effect",
        "Attack Effect", "Lines Received", "Glass Shatter",
        "BPM Restricted Eat", "Impressive", "Perfect",
        "Godlike"
    };

    // ----------------------------------------------------------------------------
    private JToggleButton tbAnim, tbBlur, tbFullscreen, tbDisableResize, tbMusic, tbEnemySound, tbComboHelper, tbExplicitTimer;
    private JToggleButton tbConsoleGeneral, tbConsoleChat;
    private JToggleButton tbReplayOn;
    private JToggleButton[] tbSounds;
    private JSpinner spBlur, spFps, spHz, spWindowSize, spHardDrop, spLineClear;
    private JSpinner spBgRed, spBgGreen, spBgBlue;
    private JComboBox<String> cbVerbosity;
    private JPanel backgroundPreview;
    private JLabel statusLabel;

    // ----------------------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new c2settings().show());
    }

    private void show() {
        applyDarkDefaults();

        JFrame frame = new JFrame("C2 Settings");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(FG);
        tabs.setFont(tabFont());

        tabs.addTab("Display",  buildDisplayTab());
        tabs.addTab("Audio",    buildAudioTab());
        tabs.addTab("Gameplay", buildGameplayTab());
        tabs.addTab("System",   buildSystemTab());

        // ----------------------------------------------------------------------------
        statusLabel = new JLabel(" ");
        statusLabel.setFont(smallFont());
        statusLabel.setForeground(FG_DIM);
        statusLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        JButton saveBtn = accentButton("Save All");
        saveBtn.addActionListener(e -> saveAll());

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(BG);
        bottom.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
            new EmptyBorder(8, 12, 8, 12)));
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(saveBtn, BorderLayout.EAST);

        frame.add(tabs, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        loadAll();

        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.min(620, Math.max(560, usable.width - 80));
        int height = Math.min(600, Math.max(480, usable.height - 100));
        frame.setMinimumSize(new Dimension(540, 480));
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ----------------------------------------------------------------------------
    // Tabs
    // ----------------------------------------------------------------------------

    private JScrollPane buildDisplayTab() {
        JPanel p = tabPanel();

        p.add(sectionHeader("Window"));
        tbFullscreen = addToggleRow(p, "Fullscreen", "Start Cultris II in fullscreen");
        tbDisableResize = addToggleRow(p, "Disable window resizing",
            "Safer default. Turn OFF only if you want experimental resize / fullscreen. Changes apply after restarting Cultris II.");
        spWindowSize = addSpinnerRow(p, "Window size", 0, 0, 2400, 20,
            "Startup window size", "0 keeps the game default. Changes apply after restarting Cultris II.");

        p.add(sectionHeader("Rendering"));
        tbAnim = addToggleRow(p, "Animations", "Show piece / line animations");
        tbBlur = addToggleRow(p, "Blur", "Apply blur effect to the background");

        spBlur = addSpinnerRow(p, "Blur amount", 3, 0, 20, 1,
            "Blur intensity (0 = none)");

        p.add(sectionHeader("Background Color"));
        JPanel bgRow = row(BG_ROW);
        JLabel bgLbl = new JLabel("Playfield background");
        bgLbl.setFont(baseFont());
        bgLbl.setForeground(FG);
        bgLbl.setToolTipText("RGB color used by the game background");

        spBgRed = colorSpinner();
        spBgGreen = colorSpinner();
        spBgBlue = colorSpinner();
        backgroundPreview = new JPanel();
        backgroundPreview.setPreferredSize(new Dimension(42, 30));
        backgroundPreview.setMaximumSize(new Dimension(42, 30));
        backgroundPreview.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        JButton pickBackground = accentButton("Pick");
        pickBackground.setPreferredSize(new Dimension(72, 30));
        pickBackground.addActionListener(e -> chooseBackgroundColor());

        bgRow.add(bgLbl);
        bgRow.add(Box.createHorizontalGlue());
        bgRow.add(rgbLabel("R"));
        bgRow.add(spBgRed);
        bgRow.add(Box.createHorizontalStrut(6));
        bgRow.add(rgbLabel("G"));
        bgRow.add(spBgGreen);
        bgRow.add(Box.createHorizontalStrut(6));
        bgRow.add(rgbLabel("B"));
        bgRow.add(spBgBlue);
        bgRow.add(Box.createHorizontalStrut(10));
        bgRow.add(backgroundPreview);
        bgRow.add(Box.createHorizontalStrut(10));
        bgRow.add(pickBackground);
        p.add(bgRow);

        p.add(sectionHeader("Frame Rates"));
        spFps = addSpinnerRow(p, "FPS cap", 240, 1, 2000, 1,
            "Maximum frames per second rendered");
        spHz  = addSpinnerRow(p, "Hz cap", 60, 1, 1000, 1,
            "Polling / input rate cap in Hz");

        return scrollWrap(p);
    }

    private JScrollPane buildAudioTab() {
        JPanel p = tabPanel();

        p.add(sectionHeader("General"));
        tbMusic = addToggleRow(p, "Music", "Play original Cultris II MO3 background music");
        tbEnemySound = addToggleRow(p, "Enemy sounds", "Play sounds triggered by opponents");

        p.add(sectionHeader("Sound Effects"));
        tbSounds = new JToggleButton[SOUND_FILES.length];
        for (int i = 0; i < SOUND_FILES.length; i++)
            tbSounds[i] = addToggleRow(p, SOUND_LABELS[i], SOUND_FILES[i]);

        return scrollWrap(p);
    }

    private JScrollPane buildGameplayTab() {
        JPanel p = tabPanel();

        p.add(sectionHeader("Animation Speeds"));
        spHardDrop  = addSpinnerRow(p, "Hard drop trail speed", 12, -100, 500, 1,
            "How fast the hard-drop trail disappears",
            "Default 12  |  fast: 24  |  drunkard: 2  |  higher = faster, negative = reverse");
        spLineClear = addSpinnerRow(p, "Line clear speed", 16, -100, 500, 1,
            "How fast cleared lines disappear",
            "Default 16  |  fast: 32  |  drunkard: low value  |  higher = more instant");

        p.add(sectionHeader("Helpers"));
        tbComboHelper   = addToggleRow(p, "Combo goal helper", "Shows a combo target overlay during play");
        tbExplicitTimer = addToggleRow(p, "Explicit combo timer", "Display the combo timer explicitly on screen");

        return scrollWrap(p);
    }

    private JScrollPane buildSystemTab() {
        JPanel p = tabPanel();

        p.add(sectionHeader("Console Output"));
        tbConsoleGeneral = addToggleRow(p, "General info",
            "Logins, round results, saved replays");
        tbConsoleChat    = addToggleRow(p, "Player chat",
            "Print in-game chat messages to the console");

        p.add(sectionHeader("Replay Recording"));
        tbReplayOn = addToggleRow(p, "Record replays", "Gather replay files during play");

        JPanel verbRow = row(BG_ROW);
        JLabel verbLbl = new JLabel("Chat verbosity");
        verbLbl.setFont(baseFont());
        verbLbl.setForeground(FG);
        verbLbl.setPreferredSize(new Dimension(210, 28));

        cbVerbosity = new JComboBox<>(new String[]{
            "Silent — no chat messages",
            "When playing / tagging replays",
            "Also when a replay is saved"
        });
        styleCombo(cbVerbosity);

        verbRow.add(verbLbl);
        verbRow.add(cbVerbosity);
        p.add(verbRow);

        return scrollWrap(p);
    }

    // ----------------------------------------------------------------------------
    // Load / Save
    // ----------------------------------------------------------------------------

    private void loadAll() {
        // Display
        setToggle(tbFullscreen, readBool(F_DISPLAY_FULLSCREEN, false));
        setToggle(tbDisableResize, readBool(F_DISABLE_WINDOW_RESIZE, true));
        spWindowSize.setValue(readInt(F_DISPLAY_WINDOW_SIZE, 0));
        setToggle(tbAnim, readBool(F_ANIM_TOGGLE, false));
        setToggle(tbBlur, readBool(F_BLUR_TOGGLE, false));
        spBlur.setValue(readInt(F_BLUR_AMOUNT, 3));
        spFps.setValue(readInt(F_FPS, 240));
        spHz.setValue(readInt(F_HZ, 60));
        int[] bg = readRgb(F_BACKGROUND_COLOR, 47, 47, 47);
        spBgRed.setValue(bg[0]);
        spBgGreen.setValue(bg[1]);
        spBgBlue.setValue(bg[2]);
        updateBackgroundPreview();

        // Audio
        setToggle(tbMusic, readBool(F_MUSIC_ENABLED, true));
        setToggle(tbEnemySound, readBool(F_ENEMY_SOUND, false));
        try {
            File f = new File(F_SOUND_LIST);
            if (f.exists()) {
                List<String> lines = Files.readAllLines(f.toPath());
                for (int i = 0; i < tbSounds.length && i < lines.size(); i++)
                    setToggle(tbSounds[i], !lines.get(i).startsWith("disabled_"));
            }
        } catch (IOException e) { e.printStackTrace(); }

        // Gameplay
        try {
            List<String> lines = Files.readAllLines(new File(F_ANIM_SPEEDS).toPath());
            if (lines.size() >= 2) spHardDrop.setValue(parseInt(lines.get(1), 12));
            if (lines.size() >= 5) spLineClear.setValue(parseInt(lines.get(4), 16));
        } catch (IOException ignored) {}

        try {
            List<String> lines = Files.readAllLines(new File(F_COMBO_HELPER).toPath());
            if (lines.size() >= 2) setToggle(tbComboHelper, "1".equals(lines.get(1).trim()));
        } catch (IOException ignored) {}

        setToggle(tbExplicitTimer, readBool(F_EXPLICIT_TIMER, false));

        // System
        try {
            List<String> lines = Files.readAllLines(new File(F_CONSOLE_PRINTING).toPath());
            if (lines.size() >= 2) setToggle(tbConsoleGeneral, "1".equals(lines.get(1).trim()));
            if (lines.size() >= 5) setToggle(tbConsoleChat,    "1".equals(lines.get(4).trim()));
        } catch (IOException ignored) {}

        try {
            List<String> lines = Files.readAllLines(new File(F_REPLAY).toPath());
            for (String line : lines) {
                String t = line.trim();
                if (t.startsWith("replay_activated"))
                    setToggle(tbReplayOn, t.endsWith("1"));
                if (t.startsWith("replay_verbosity"))
                    cbVerbosity.setSelectedIndex(Math.min(parseInt(t.replaceAll("[^0-9]", ""), 1), 2));
            }
        } catch (IOException ignored) {}
    }

    private void saveAll() {
        try {
            writeFile(F_ANIM_TOGGLE,  tbAnim.isSelected()        ? "1" : "0");
            writeFile(F_BLUR_TOGGLE,  tbBlur.isSelected()        ? "1" : "0");
            writeFile(F_BLUR_AMOUNT,  spBlur.getValue().toString());
            writeFile(F_FPS,          spFps.getValue().toString());
            writeFile(F_HZ,           spHz.getValue().toString());
            writeFile(F_DISPLAY_FULLSCREEN, tbFullscreen.isSelected() ? "1" : "0");
            writeFile(F_DISABLE_WINDOW_RESIZE, tbDisableResize.isSelected() ? "1" : "0");
            writeFile(F_DISPLAY_WINDOW_SIZE, spWindowSize.getValue().toString());
            writeFile(F_BACKGROUND_COLOR,
                spBgRed.getValue() + ", " + spBgGreen.getValue() + ", " + spBgBlue.getValue());
            writeFile(F_MUSIC_ENABLED, tbMusic.isSelected() ? "1" : "0");
            writeFile(F_ENEMY_SOUND,  tbEnemySound.isSelected()  ? "1" : "0");
            writeFile(F_EXPLICIT_TIMER, tbExplicitTimer.isSelected() ? "1" : "0");

            List<String> soundLines = new ArrayList<>();
            for (int i = 0; i < tbSounds.length; i++)
                soundLines.add(tbSounds[i].isSelected() ? SOUND_FILES[i] : "disabled_" + SOUND_FILES[i]);
            Files.write(new File(F_SOUND_LIST).toPath(), soundLines);

            writeFile(F_ANIM_SPEEDS,
                "Line placement animation speed, i.e. how fast the trail of the harddrop disappears. 12 is default. 24 is azazea's speed. 2 is drunkard. Can be as high as needed but at some point no difference can be appreciated. Negative values possible, don't recommend...\n"
                + spHardDrop.getValue() + "\n"
                + "\n"
                + "Line clear animation speed, i.e. how fast the white lines disappear. 16 is default. 32 is azazeas' speed. Higher values make it almost instant, lower values is drunkard.\n"
                + spLineClear.getValue());

            try {
                List<String> lines = Files.readAllLines(new File(F_COMBO_HELPER).toPath());
                if (lines.size() >= 2) lines.set(1, tbComboHelper.isSelected() ? "1" : "0");
                Files.write(new File(F_COMBO_HELPER).toPath(), lines);
            } catch (IOException ignored) {}

            writeFile(F_CONSOLE_PRINTING,
                "Print general info in the console: (Logins, who won last round, saved replays, etc)\n"
                + (tbConsoleGeneral.isSelected() ? "1" : "0") + "\n\n"
                + "Print players chatting in the console:\n"
                + (tbConsoleChat.isSelected() ? "1" : "0"));

            writeFile(F_REPLAY,
                "# 1 if the client should gather replays, 0 if the client shouldn't\n"
                + "replay_activated = " + (tbReplayOn.isSelected() ? "1" : "0") + "\n\n"
                + "# 0 to not send text to the chat ingame at all.\n"
                + "# 1 to only send text when you are playing or tagging replays.\n"
                + "# 2 to additionally send text when a replay is being saved.\n"
                + "replay_verbosity = " + cbVerbosity.getSelectedIndex());

            statusLabel.setForeground(ON_COLOR);
            statusLabel.setText("Saved successfully.");
        } catch (IOException ex) {
            statusLabel.setForeground(OFF_COLOR);
            statusLabel.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------
    // Widget builders
    // ----------------------------------------------------------------------------

    private JToggleButton addToggleRow(JPanel parent, String label, String tooltip) {
        JPanel row = row(parent.getComponentCount() % 2 == 0 ? BG_ROW : BG_ROW_ALT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(baseFont());
        lbl.setForeground(FG);
        lbl.setPreferredSize(new Dimension(210, 28));
        if (!tooltip.isEmpty()) lbl.setToolTipText(tooltip);

        JLabel hint = new JLabel(tooltip);
        hint.setFont(smallFont());
        hint.setForeground(FG_DIM);

        JToggleButton tb = new JToggleButton("OFF");
        tb.setPreferredSize(new Dimension(64, 28));
        tb.setFont(baseFont());
        styleToggle(tb);

        tb.addItemListener(e -> updateToggleVisual(tb));

        row.add(lbl);
        row.add(hint);
        row.add(Box.createHorizontalGlue());
        row.add(tb);
        parent.add(row);
        return tb;
    }

    private JSpinner addSpinnerRow(JPanel parent, String label, int def, int min, int max, int step, String tooltip) {
        return addSpinnerRow(parent, label, def, min, max, step, tooltip, null);
    }

    private JSpinner addSpinnerRow(JPanel parent, String label, int def, int min, int max, int step, String tooltip, String hint) {
        Color rowBg = parent.getComponentCount() % 2 == 0 ? BG_ROW : BG_ROW_ALT;

        JPanel row = row(rowBg);

        JLabel lbl = new JLabel(label);
        lbl.setFont(baseFont());
        lbl.setForeground(FG);
        if (!tooltip.isEmpty()) lbl.setToolTipText(tooltip);

        JSpinner sp = new JSpinner(new SpinnerNumberModel(def, min, max, step));
        sp.setPreferredSize(new Dimension(90, 30));
        sp.setMaximumSize(new Dimension(90, 30));
        styleSpinner(sp);

        row.add(lbl);
        row.add(Box.createHorizontalGlue());
        row.add(sp);
        parent.add(row);

        if (hint != null && !hint.isEmpty()) {
            JPanel hintRow = new JPanel(new BorderLayout());
            hintRow.setBackground(rowBg);
            hintRow.setBorder(new EmptyBorder(0, 12, 6, 12));
            hintRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            JLabel hintLbl = new JLabel(hint);
            hintLbl.setFont(smallFont());
            hintLbl.setForeground(FG_DIM);
            hintRow.add(hintLbl, BorderLayout.WEST);
            parent.add(hintRow);
        }

        return sp;
    }

    private JSpinner colorSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(47, 0, 255, 1));
        sp.setPreferredSize(new Dimension(58, 30));
        sp.setMaximumSize(new Dimension(58, 30));
        styleSpinner(sp);
        sp.addChangeListener(e -> updateBackgroundPreview());
        return sp;
    }

    private JLabel rgbLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont());
        label.setForeground(FG_DIM);
        label.setBorder(new EmptyBorder(0, 0, 0, 3));
        return label;
    }

    private void chooseBackgroundColor() {
        Color current = currentBackgroundColor();
        Color chosen = JColorChooser.showDialog(null, "Choose Cultris II background", current);
        if (chosen != null) {
            spBgRed.setValue(chosen.getRed());
            spBgGreen.setValue(chosen.getGreen());
            spBgBlue.setValue(chosen.getBlue());
            updateBackgroundPreview();
        }
    }

    private Color currentBackgroundColor() {
        return new Color(
            spinnerInt(spBgRed, 47),
            spinnerInt(spBgGreen, 47),
            spinnerInt(spBgBlue, 47));
    }

    private void updateBackgroundPreview() {
        if (backgroundPreview != null && spBgRed != null && spBgGreen != null && spBgBlue != null) {
            backgroundPreview.setBackground(currentBackgroundColor());
            backgroundPreview.repaint();
        }
    }

    private JPanel sectionHeader(String title) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_SECTION);
        h.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(5, 12, 5, 12)));
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(new Font("Arial", Font.BOLD, 11));
        lbl.setForeground(ACCENT);
        h.add(lbl, BorderLayout.WEST);
        return h;
    }

    private JPanel row(Color bg) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(bg);
        row.setBorder(new EmptyBorder(6, 12, 6, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return row;
    }

    private JPanel tabPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        return p;
    }

    private JScrollPane scrollWrap(JPanel p) {
        // Push content to top
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(p, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        return scroll;
    }

    // ----------------------------------------------------------------------------
    // Styling helpers
    // ----------------------------------------------------------------------------

    private void setToggle(JToggleButton tb, boolean on) {
        tb.setSelected(on);
        updateToggleVisual(tb);
    }

    private void updateToggleVisual(JToggleButton tb) {
        boolean on = tb.isSelected();
        tb.setText(on ? "ON" : "OFF");
        // putClientProperty lets paintComponent read the current state
        tb.putClientProperty("_on", on);
        tb.repaint();
    }

    private void styleToggle(JToggleButton tb) {
        tb.setContentAreaFilled(false);
        tb.setOpaque(false);
        tb.setBorderPainted(false);
        tb.setFocusPainted(false);
        tb.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                boolean on = Boolean.TRUE.equals(c.getClientProperty("_on"));
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // background
                g2.setColor(on ? new Color(0x1a, 0x4a, 0x2a) : new Color(0x4a, 0x1a, 0x1a));
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);
                // border
                g2.setColor(on ? ON_COLOR : OFF_COLOR);
                g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);
                // text
                g2.setColor(on ? ON_COLOR : OFF_COLOR);
                g2.setFont(c.getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = ((JToggleButton) c).getText();
                int x = (c.getWidth()  - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
        updateToggleVisual(tb);
    }

    private void styleSpinner(JSpinner sp) {
        JFormattedTextField tf = ((JSpinner.DefaultEditor) sp.getEditor()).getTextField();
        sp.setBackground(BG_ROW);
        sp.setForeground(FG);
        tf.setBackground(BG_ROW);
        tf.setForeground(FG);
        tf.setCaretColor(FG);
        tf.setEditable(true);
        tf.setBorder(new EmptyBorder(0, 4, 0, 4));
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_ROW);
        cb.setForeground(FG);
        cb.setFont(baseFont());
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        cb.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object val,
                    int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, val, idx, sel, focus);
                setBackground(sel ? ACCENT : BG_ROW);
                setForeground(sel ? BG : FG);
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
    }

    private JButton accentButton(String text) {
        JButton b = new JButton(text) {
            boolean hovering = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovering = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
            }); }
            protected void paintComponent(Graphics g) {
                g.setColor(hovering ? ACCENT_HOVER : ACCENT);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(BG);
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(getText(), x, y);
            }
        };
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(130, 36));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void applyDarkDefaults() {
        UIManager.put("TabbedPane.background",          BG);
        UIManager.put("TabbedPane.foreground",          FG);
        UIManager.put("TabbedPane.selected",            BG_SECTION);
        UIManager.put("TabbedPane.contentAreaColor",    BG);
        UIManager.put("TabbedPane.tabAreaBackground",   BG);
        UIManager.put("TabbedPane.unselectedBackground", BG);
        UIManager.put("TabbedPane.borderHightlightColor", BORDER_COLOR);
        UIManager.put("TabbedPane.darkShadow",          BORDER_COLOR);
        UIManager.put("TabbedPane.shadow",              BORDER_COLOR);
        UIManager.put("TabbedPane.light",               BG_SECTION);
        UIManager.put("TabbedPane.focus",               ACCENT);
        UIManager.put("ScrollBar.background",           BG);
        UIManager.put("ScrollBar.thumb",                BORDER_COLOR);
        UIManager.put("ScrollBar.thumbHighlight",       ACCENT);
        UIManager.put("ScrollPane.background",          BG);
        UIManager.put("ToolTip.background",             BG_SECTION);
        UIManager.put("ToolTip.foreground",             FG);
        UIManager.put("ToolTip.border",                 BorderFactory.createLineBorder(BORDER_COLOR));
    }

    // ----------------------------------------------------------------------------
    private static Font baseFont()  { return new Font("Arial", Font.PLAIN, 13); }
    private static Font smallFont() { return new Font("Arial", Font.PLAIN, 11); }
    private static Font tabFont()   { return new Font("Arial", Font.BOLD,  13); }

    // ----------------------------------------------------------------------------
    private boolean readBool(String path, boolean def) {
        try {
            File f = new File(path);
            if (!f.exists()) return def;
            return "1".equals(Files.readAllLines(f.toPath()).get(0).trim());
        } catch (Exception e) { return def; }
    }

    private int readInt(String path, int def) {
        try {
            File f = new File(path);
            if (!f.exists()) return def;
            return Integer.parseInt(Files.readAllLines(f.toPath()).get(0).trim());
        } catch (Exception e) { return def; }
    }

    private int[] readRgb(String path, int red, int green, int blue) {
        try {
            File f = new File(path);
            if (!f.exists()) return new int[] { red, green, blue };
            List<String> lines = Files.readAllLines(f.toPath());
            if (lines.isEmpty()) return new int[] { red, green, blue };
            String[] parts = lines.get(0).split(",");
            if (parts.length != 3) return new int[] { red, green, blue };
            return new int[] {
                clampColor(parseRgbPart(parts[0], red)),
                clampColor(parseRgbPart(parts[1], green)),
                clampColor(parseRgbPart(parts[2], blue))
            };
        } catch (Exception e) {
            return new int[] { red, green, blue };
        }
    }

    private int parseRgbPart(String value, int def) {
        try {
            float parsed = Float.parseFloat(value.trim());
            if (parsed >= 0.0f && parsed <= 1.0f) {
                return Math.round(parsed * 255.0f);
            }
            return Math.round(parsed);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int spinnerInt(JSpinner spinner, int def) {
        Object value = spinner.getValue();
        if (value instanceof Number) {
            return clampColor(((Number) value).intValue());
        }
        return def;
    }

    private void writeFile(String path, String content) throws IOException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes());
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
