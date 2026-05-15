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
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

public class MainFrame extends JFrame {

    private static final Color ACCENT_RED = new Color(0xCC, 0x00, 0x33);
    private static final Color BG_LIGHT = new Color(0xF5, 0xF5, 0xF5);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(0x33, 0x33, 0x33);
    private static final Color TEXT_GRAY = new Color(0x66, 0x66, 0x66);
    private static final Color HIGHLIGHT = new Color(0xFF, 0xEB, 0xEB);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 15);

    private JLabel dropLabel, fileLabel, sizeLabel, statusLabel, countLabel;
    private JList<String> packageList;
    private DefaultListModel<String> listModel;
    private JTextField searchField;
    private JButton selectBtn, compressBtn, detectBtn, toggleBtn;
    private JProgressBar progressBar;
    private File selectedJar;
    private JarAnalyzer cachedAnalyzer;
    private final Set<String> selectedPackages = new HashSet<>();
    private final Set<String> detectedPkgs = new HashSet<>();
    private List<String> allPackages = new ArrayList<>();

    public MainFrame() {
        super("JAR Optimizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 700);
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
        header.setPreferredSize(new Dimension(640, 65));

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
        body.setBorder(new EmptyBorder(15, 20, 5, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 0, 3, 0);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1;
        body.add(createDropZone(), gbc);

        gbc.gridy = 1;
        body.add(createInfoPanel(), gbc);

        gbc.gridy = 2; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        body.add(createListPanel(), gbc);

        gbc.gridy = 3; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        body.add(createProgressPanel(), gbc);

        return body;
    }

    private JPanel createDropZone() {
        JPanel zone = new JPanel(new GridBagLayout());
        zone.setBackground(new Color(0xFF, 0xF0, 0xF0));
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_RED, 2, true),
                new EmptyBorder(20, 20, 20, 20)));
        zone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        dropLabel = new JLabel("Arrastra tu JAR aqu\u00ED o haz clic para seleccionarlo", JLabel.CENTER);
        dropLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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
        c.gridx = 0; c.gridy = 0; c.insets = new Insets(0, 0, 8, 0);
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
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
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

    private JPanel createListPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xDD, 0xDD, 0xDD)),
                "Paquetes encontrados en el JAR (marca los que quieres conservar)",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.PLAIN, 12), TEXT_DARK));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topBar.setBackground(WHITE);

        detectBtn = new JButton("Detectar");
        detectBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detectBtn.setBackground(ACCENT_RED);
        detectBtn.setForeground(WHITE);
        detectBtn.setFocusPainted(false);
        detectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detectBtn.setEnabled(false);
        detectBtn.addActionListener(e -> detectPackages());

        toggleBtn = new JButton("Seleccionar detectados");
        toggleBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toggleBtn.setBackground(new Color(0x55, 0x55, 0x55));
        toggleBtn.setForeground(WHITE);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setEnabled(false);
        toggleBtn.addActionListener(e -> toggleSelection());

        countLabel = new JLabel("Seleccionados: 0");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(TEXT_GRAY);

        topBar.add(detectBtn);
        topBar.add(toggleBtn);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(countLabel);

        JPanel searchRow = new JPanel(new BorderLayout(5, 0));
        searchRow.setBackground(WHITE);
        searchRow.setBorder(new EmptyBorder(3, 0, 3, 0));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(TEXT_GRAY);
        searchField.setText("Buscar paquete...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                new EmptyBorder(5, 8, 5, 8)));
        searchField.setEnabled(false);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Buscar paquete...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_DARK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(TEXT_GRAY);
                    searchField.setText("Buscar paquete...");
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterPackages();
            }
        });

        searchRow.add(searchIcon, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);

        listModel = new DefaultListModel<>();
        packageList = new JList<>(listModel);
        packageList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        packageList.setCellRenderer(new CheckboxRenderer());
        packageList.setBackground(WHITE);
        packageList.setFixedCellHeight(28);
        packageList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = packageList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    String pkg = listModel.get(idx);
                    boolean selected = selectedPackages.contains(pkg);
                    if (selected) selectedPackages.remove(pkg);
                    else selectedPackages.add(pkg);
                    packageList.repaint();
                    updateCount();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(packageList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xDD, 0xDD, 0xDD)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout(0, 3));
        center.setBackground(WHITE);
        center.add(searchRow, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        p.add(topBar, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private void filterPackages() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty() || searchField.getText().equals("Buscar paquete...")) {
            listModel.clear();
            for (String pkg : allPackages) listModel.addElement(pkg);
        } else {
            listModel.clear();
            for (String pkg : allPackages) {
                if (pkg.toLowerCase().contains(q)) listModel.addElement(pkg);
            }
        }
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
        footer.setBorder(new EmptyBorder(0, 20, 15, 20));

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

    private void updateCount() {
        countLabel.setText("Seleccionados: " + selectedPackages.size());
    }

    private void toggleSelection() {
        boolean allSelected = selectedPackages.size() == listModel.size();
        if (allSelected || selectedPackages.isEmpty()) {
            selectedPackages.clear();
            selectedPackages.addAll(detectedPkgs);
        } else {
            selectedPackages.clear();
            selectedPackages.addAll(detectedPkgs);
        }
        packageList.repaint();
        updateCount();
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
        cachedAnalyzer = null;
        selectedPackages.clear();
        detectedPkgs.clear();
        listModel.clear();
        allPackages.clear();
        searchField.setEnabled(false);
        searchField.setForeground(TEXT_GRAY);
        searchField.setText("Buscar paquete...");
        String size = formatSize(f.length());
        fileLabel.setText("JAR: " + f.getName());
        sizeLabel.setText("Tama\u00F1o original: " + size);
        compressBtn.setEnabled(false);
        detectBtn.setEnabled(true);
        toggleBtn.setEnabled(false);
        updateCount();
        statusLabel.setText("Haz clic en \"Detectar\" para analizar el JAR");
        progressBar.setVisible(false);
    }

    private void detectPackages() {
        if (selectedJar == null) return;

        detectBtn.setEnabled(false);
        toggleBtn.setEnabled(false);
        listModel.clear();
        allPackages.clear();
        selectedPackages.clear();
        detectedPkgs.clear();
        searchField.setEnabled(false);
        searchField.setForeground(TEXT_GRAY);
        searchField.setText("Buscar paquete...");
        statusLabel.setText("Analizando JAR...");

        new SwingWorker<Void, Void>() {
            private String[] packages;
            private Set<String> detected;

            protected Void doInBackground() {
                try {
                    cachedAnalyzer = new JarAnalyzer();
                    try (JarFile jf = new JarFile(selectedJar)) {
                        cachedAnalyzer.loadJar(jf);
                    }
                    packages = cachedAnalyzer.getTopLevelPackages();
                    detected = cachedAnalyzer.detectEntryPoints();

                    Set<String> pkgSet = new TreeSet<>();
                    for (String cls : detected) {
                        int dot = cls.lastIndexOf('.');
                        if (dot > 0) pkgSet.add(cls.substring(0, cls.indexOf('.', dot + 1) > 0 ? cls.indexOf('.', dot + 1) : cls.length()));
                        else if (dot > 0) pkgSet.add(cls.substring(0, dot));
                    }
                    detectedPkgs.addAll(pkgSet);
                } catch (Exception e) {
                    packages = new String[]{"Error: " + e.getMessage()};
                    detected = Collections.emptySet();
                }
                return null;
            }

            protected void done() {
                detectBtn.setEnabled(true);
                compressBtn.setEnabled(true);
                if (packages == null || packages.length == 0) {
                    statusLabel.setText("No se encontraron paquetes en el JAR.");
                    return;
                }
                allPackages = new ArrayList<>(Arrays.asList(packages));
                for (String pkg : allPackages) {
                    listModel.addElement(pkg);
                    if (detectedPkgs.contains(pkg)) {
                        selectedPackages.add(pkg);
                    }
                }
                searchField.setEnabled(true);
                toggleBtn.setEnabled(true);
                packageList.repaint();
                updateCount();
                statusLabel.setText("Detectados " + detectedPkgs.size() + " paquetes con entry points. Revisa y ajusta las selecciones.");
            }
        }.execute();
    }

    private void compress() {
        if (selectedJar == null) return;

        JFileChooser fc = new JFileChooser(selectedJar.getParent());
        fc.setSelectedFile(new File(selectedJar.getName().replace(".jar", "-optimizado.jar")));
        fc.setDialogTitle("Guardar JAR optimizado");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File output = fc.getSelectedFile();

        compressBtn.setEnabled(false);
        selectBtn.setEnabled(false);
        detectBtn.setEnabled(false);
        toggleBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(5);
        statusLabel.setText("Preparando...");

        new SwingWorker<Void, Integer>() {
            private MinimizeResult result;
            private String error;

            protected Void doInBackground() {
                try {
                    publish(10);
                    JarAnalyzer analyzer;
                    if (cachedAnalyzer != null) analyzer = cachedAnalyzer;
                    else {
                        analyzer = new JarAnalyzer();
                        try (JarFile jf = new JarFile(selectedJar)) { analyzer.loadJar(jf); }
                    }

                    publish(30);
                    statusLabel.setText("Calculando entry points...");

                    Set<String> entryPoints = new HashSet<>();
                    if (!selectedPackages.isEmpty()) {
                        for (String pkg : selectedPackages) {
                            for (String cls : analyzer.getAllClasses()) {
                                if (cls.startsWith(pkg)) entryPoints.add(cls);
                            }
                        }
                    }
                    if (entryPoints.isEmpty()) {
                        statusLabel.setText("Usando deteccion automatica...");
                        entryPoints = analyzer.detectEntryPoints();
                    }
                    if (entryPoints.isEmpty()) {
                        error = "Selecciona al menos un paquete o usa Detectar.";
                        return null;
                    }

                    publish(50);
                    statusLabel.setText("Construyendo grafo de dependencias (" + entryPoints.size() + " entry points)...");
                    Set<String> reachable = analyzer.findReachableClasses(entryPoints);

                    publish(75);
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
                progressBar.setValue(chunks.get(chunks.size() - 1));
            }

            protected void done() {
                compressBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                detectBtn.setEnabled(true);
                toggleBtn.setEnabled(true);

                if (error != null) {
                    statusLabel.setText("Error: " + error);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Ocurrio un error:\n" + error, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (result == null) {
                    statusLabel.setText("No se pudo completar.");
                    return;
                }

                progressBar.setValue(100);
                String saved = formatSize(result.savedBytes);
                String original = formatSize(selectedJar.length());
                String newSize = formatSize(selectedJar.length() - result.savedBytes);

                statusLabel.setForeground(new Color(0x00, 0x80, 0x00));
                statusLabel.setText("\u2713 Completado! Ahorraste " + saved + " (" + result.savedPercent + "%)");

                JOptionPane.showMessageDialog(MainFrame.this,
                        "JAR original: " + selectedJar.getName() + " (" + original + ")\n" +
                        "JAR optimizado: " + output.getName() + " (" + newSize + ")\n\n" +
                        "Clases totales: " + String.format("%,d", result.totalClasses) + "\n" +
                        "Clases eliminadas: " + String.format("%,d", result.removedClasses) +
                        " (" + (result.totalClasses > 0 ? (result.removedClasses * 100 / result.totalClasses) : 0) + "%)\n" +
                        "Clases conservadas: " + String.format("%,d", result.keptClasses) + "\n\n" +
                        "\u2713 Ahorro: " + saved + " (" + result.savedPercent + "%)",
                        "Optimizacion completada", JOptionPane.INFORMATION_MESSAGE);
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
        return new DecimalFormat("#,##0.#").format(size) + " " + units[unit];
    }

    class CheckboxRenderer extends JCheckBox implements ListCellRenderer<String> {
        CheckboxRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            setSelected(selectedPackages.contains(value));
            setBackground(isSelected ? HIGHLIGHT : WHITE);
            setForeground(TEXT_DARK);
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
