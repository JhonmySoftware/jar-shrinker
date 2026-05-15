package com.jarshrinker.ui;

import com.jarshrinker.core.JarAnalyzer;
import com.jarshrinker.core.JarMinimizer;
import com.jarshrinker.core.JarMinimizer.MinimizeResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.jar.JarFile;

public class MainFrame extends JFrame {

    private static final Color ACCENT_RED = new Color(0xCC, 0x00, 0x33);
    private static final Color ACCENT_DARK = new Color(0x99, 0x00, 0x26);
    private static final Color BG_LIGHT = new Color(0xF5, 0xF5, 0xF5);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(0x33, 0x33, 0x33);
    private static final Color TEXT_GRAY = new Color(0x66, 0x66, 0x66);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 15);

    private JLabel dropLabel, fileLabel, sizeLabel, statusLabel;
    private JTextField entryField;
    private JButton selectBtn, compressBtn;
    private JProgressBar progressBar;
    private File selectedJar;

    public MainFrame() {
        super("Optimizador de JARs");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(createIcon());

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        initUI();
        setupDragDrop();

        setVisible(true);
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_LIGHT);

        main.add(createHeader(), BorderLayout.NORTH);
        main.add(createBody(), BorderLayout.CENTER);
        main.add(createFooter(), BorderLayout.SOUTH);

        add(main);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT_RED);
        header.setPreferredSize(new Dimension(600, 65));

        JLabel logo = new JLabel("JAR OPTIMIZER", JLabel.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(WHITE);
        logo.setBorder(new EmptyBorder(5, 0, 0, 0));

        JLabel sub = new JLabel("Optimizador Inteligente de JARs", JLabel.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(0xFF, 0xCC, 0xCC));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(ACCENT_RED);
        inner.add(logo, BorderLayout.CENTER);
        inner.add(sub, BorderLayout.SOUTH);
        header.add(inner, BorderLayout.CENTER);

        return header;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(WHITE);
        body.setBorder(new EmptyBorder(20, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        body.add(createDropZone(), gbc);

        gbc.gridy = 1;
        body.add(createInfoPanel(), gbc);

        gbc.gridy = 2;
        body.add(createEntryPanel(), gbc);

        gbc.gridy = 3; gbc.insets = new Insets(10, 0, 0, 0);
        body.add(createProgressPanel(), gbc);

        return body;
    }

    private JPanel createDropZone() {
        JPanel zone = new JPanel(new GridBagLayout());
        zone.setBackground(new Color(0xFF, 0xF0, 0xF0));
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_RED, 2, true),
                new EmptyBorder(25, 20, 25, 20)
        ));
        zone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        dropLabel = new JLabel("Arrastra tu JAR aqu\u00ED o haz clic para seleccionarlo", JLabel.CENTER);
        dropLabel.setFont(FONT_LABEL);
        dropLabel.setForeground(TEXT_GRAY);

        selectBtn = new JButton("Seleccionar JAR");
        selectBtn.setFont(FONT_BUTTON);
        selectBtn.setBackground(ACCENT_RED);
        selectBtn.setForeground(WHITE);
        selectBtn.setFocusPainted(false);
        selectBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        selectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectBtn.addActionListener(e -> selectJar());

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBackground(new Color(0xFF, 0xF0, 0xF0));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.insets = new Insets(0, 0, 10, 0);
        inner.add(dropLabel, c);
        c.gridy = 1;
        inner.add(selectBtn, c);

        zone.add(inner);
        zone.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectJar(); }
        });

        return zone;
    }

    private JPanel createInfoPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        fileLabel = new JLabel(" ");
        fileLabel.setFont(FONT_LABEL);
        fileLabel.setForeground(TEXT_DARK);

        sizeLabel = new JLabel(" ");
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sizeLabel.setForeground(TEXT_GRAY);

        c.gridx = 0; c.gridy = 0; c.weightx = 1;
        p.add(fileLabel, c);
        c.gridy = 1;
        p.add(sizeLabel, c);

        return p;
    }

    private JPanel createEntryPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        JLabel lbl = new JLabel("Paquetes ra\u00EDz a conservar (separados por coma):");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_DARK);

        entryField = new JTextField("org.example.tests");
        entryField.setFont(FONT_LABEL);
        entryField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel hint = new JLabel("Ej: org.example.tests, org.testng, com.misflujos");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(TEXT_GRAY);

        c.gridx = 0; c.gridy = 0; c.weightx = 1;
        p.add(lbl, c);
        c.gridy = 1;
        p.add(entryField, c);
        c.gridy = 2; c.insets = new Insets(1, 0, 0, 0);
        p.add(hint, c);

        return p;
    }

    private JPanel createProgressPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT_RED);
        progressBar.setBackground(new Color(0xFF, 0xE0, 0xE0));
        progressBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        progressBar.setPreferredSize(new Dimension(0, 25));
        progressBar.setVisible(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_GRAY);

        c.gridx = 0; c.gridy = 0; c.weightx = 1;
        p.add(progressBar, c);
        c.gridy = 1;
        p.add(statusLabel, c);

        return p;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(WHITE);
        footer.setBorder(new EmptyBorder(0, 25, 15, 25));

        compressBtn = new JButton("COMPRIMIR Y EXPORTAR");
        compressBtn.setFont(FONT_BUTTON);
        compressBtn.setBackground(ACCENT_RED);
        compressBtn.setForeground(WHITE);
        compressBtn.setFocusPainted(false);
        compressBtn.setBorder(new EmptyBorder(12, 30, 12, 30));
        compressBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compressBtn.setEnabled(false);
        compressBtn.addActionListener(e -> compress());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(WHITE);
        btnPanel.add(compressBtn);
        footer.add(btnPanel, BorderLayout.CENTER);

        return footer;
    }

    private void setupDragDrop() {
        new DropTarget(dropLabel.getParent().getParent(), new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        if (f.getName().endsWith(".jar")) {
                            selectJar(f);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void selectJar() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Archivos JAR (*.jar)", "jar"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectJar(fc.getSelectedFile());
        }
    }

    private void selectJar(File f) {
        selectedJar = f;
        String size = formatSize(f.length());
        fileLabel.setText("JAR: " + f.getName());
        sizeLabel.setText("Tama\u00F1o original: " + size);
        compressBtn.setEnabled(true);
        statusLabel.setText(" ");
        progressBar.setVisible(false);
    }

    private void compress() {
        if (selectedJar == null) return;

        JFileChooser fc = new JFileChooser(selectedJar.getParent());
        fc.setSelectedFile(new File(selectedJar.getName().replace(".jar", "-optimizado.jar")));
        fc.setDialogTitle("Guardar JAR optimizado");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File output = fc.getSelectedFile();
        String entries = entryField.getText().trim();

        compressBtn.setEnabled(false);
        selectBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(5);
        statusLabel.setText("Analizando JAR...");

        new SwingWorker<Void, Integer>() {
            private MinimizeResult result;
            private String error;

            protected Void doInBackground() {
                try {
                    publish(10);
                    JarAnalyzer analyzer = new JarAnalyzer();
                    try (JarFile jf = new JarFile(selectedJar)) {
                        analyzer.loadJar(jf);
                    }

                    publish(40);
                    statusLabel.setText("Construyendo grafo de dependencias...");

                    Set<String> entryPoints = new HashSet<>();
                    if (!entries.isEmpty()) {
                        String[] parts = entries.split(",");
                        for (String p : parts) {
                            p = p.trim();
                            for (String cls : analyzer.getAllClasses()) {
                                if (cls.startsWith(p)) entryPoints.add(cls);
                            }
                        }
                    } else {
                        entryPoints.addAll(analyzer.getAllClasses());
                    }

                    if (entryPoints.isEmpty()) {
                        error = "No se encontraron clases para los paquetes especificados.";
                        return null;
                    }

                    publish(60);
                    statusLabel.setText("Calculando clases alcanzables...");
                    Set<String> reachable = analyzer.findReachableClasses(entryPoints);

                    publish(80);
                    statusLabel.setText("Generando JAR optimizado...");
                    result = JarMinimizer.minimize(selectedJar, output, reachable,
                            analyzer.getAllClasses(), analyzer.getClassBytes());

                    publish(100);
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            protected void process(List<Integer> chunks) {
                int v = chunks.get(chunks.size() - 1);
                progressBar.setValue(v);
            }

            protected void done() {
                compressBtn.setEnabled(true);
                selectBtn.setEnabled(true);

                if (error != null) {
                    statusLabel.setText("Error: " + error);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Ocurri\u00F3 un error:\n" + error,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (result == null) {
                    statusLabel.setText("No se pudo completar el proceso.");
                    return;
                }

                progressBar.setValue(100);
                String saved = formatSize(result.savedBytes);
                String original = formatSize(selectedJar.length());
                String newSize = formatSize(selectedJar.length() - result.savedBytes);

                statusLabel.setForeground(new Color(0x00, 0x80, 0x00));
                statusLabel.setText("\u2713 Completado! Ahorraste " + saved
                        + " (" + result.savedPercent + "%)");

                String msg = String.format(
                        "JAR original: %s\nJAR optimizado: %s\n\n" +
                        "Clases totales: %,d\nClases eliminadas: %,d (%.0f%%)\n" +
                        "Clases conservadas: %,d\n\n" +
                        "Tama\u00F1o original: %s\n" +
                        "Tama\u00F1o final: %s\n" +
                        "\u2713 Ahorro total: %s (%d%%)",
                        selectedJar.getName(), output.getName(),
                        result.totalClasses, result.removedClasses,
                        result.totalClasses > 0 ? (result.removedClasses * 100.0 / result.totalClasses) : 0,
                        result.keptClasses,
                        original, newSize, saved, result.savedPercent
                );

                JOptionPane.showMessageDialog(MainFrame.this, msg,
                        "Optimizaci\u00F3n completada", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private Image createIcon() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(ACCENT_RED);
        g.fillRect(0, 0, 16, 16);
        g.setColor(WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("JS", 2, 12);
        g.dispose();
        return img;
    }

    private String formatSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        double size = bytes;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        DecimalFormat df = new DecimalFormat("#,##0.#");
        return df.format(size) + " " + units[unit];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
