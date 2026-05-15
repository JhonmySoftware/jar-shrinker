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
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MainFrame extends JFrame {

    private static final Color ACCENT = new Color(0xCC, 0x00, 0x33);
    private static final Color ACCENT_DARK = new Color(0x99, 0x00, 0x26);
    private static final Color BG_LIGHT = new Color(0xF5, 0xF5, 0xF5);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(0x33, 0x33, 0x33);
    private static final Color TEXT_GRAY = new Color(0x66, 0x66, 0x66);
    private static final Color BORDER = new Color(0xCC, 0xCC, 0xCC);
    private static final Color ROW_HOVER = new Color(0xFF, 0xEB, 0xEB);
    private static final Color GREEN = new Color(0x00, 0x80, 0x00);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LIST = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_SUB = new Font("Segoe UI", Font.PLAIN, 11);

    private JLabel dropLabel, fileLabel, sizeLabel, statusLabel, countLabel, etaLabel;
    private JList<String> packageList;
    private DefaultListModel<String> listModel;
    private JTextField searchField;
    private JButton selectBtn, compressBtn, selectAllBtn, clearBtn, resetBtn;
    private JProgressBar progressBar;
    private File selectedJar;
    private JarAnalyzer cachedAnalyzer;
    private final Set<String> selectedPackages = new HashSet<>();
    private final Set<String> autoDetectedPackages = new HashSet<>();
    private final Set<String> autoEntryPoints = new HashSet<>();
    private List<String> projectGroups = new ArrayList<>();
    private Map<String, Set<String>> projectToPackages = new HashMap<>();

    public MainFrame() {
        super("JAR Optimizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(680, 720);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(createIcon());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
        header.setBackground(WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        header.setPreferredSize(new Dimension(680, 58));

        JLabel logo = new JLabel("JAR OPTIMIZER", JLabel.LEFT);
        logo.setFont(FONT_TITLE);
        logo.setForeground(ACCENT);
        logo.setBorder(new EmptyBorder(12, 22, 0, 0));

        JLabel sub = new JLabel("Elimina clases no utilizadas de tus JARs", JLabel.LEFT);
        sub.setFont(FONT_SUB);
        sub.setForeground(TEXT_GRAY);
        sub.setBorder(new EmptyBorder(0, 22, 10, 0));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(WHITE);
        inner.add(logo, BorderLayout.NORTH);
        inner.add(sub, BorderLayout.SOUTH);

        header.add(inner, BorderLayout.CENTER);
        return header;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                new EmptyBorder(7, 16, 7, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG_LIGHT);
        body.setBorder(new EmptyBorder(12, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

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
        zone.setBackground(WHITE);
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        zone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        dropLabel = new JLabel("Arrastra tu JAR aqu\u00ED o haz clic para seleccionarlo", JLabel.CENTER);
        dropLabel.setFont(FONT_LABEL);
        dropLabel.setForeground(TEXT_GRAY);

        selectBtn = makeButton("Seleccionar JAR", ACCENT);
        selectBtn.addActionListener(e -> selectJar());

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBackground(WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.insets = new Insets(0, 0, 6, 0);
        inner.add(dropLabel, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0);
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
        p.setBorder(new EmptyBorder(2, 8, 2, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 0, 1, 0);

        fileLabel = new JLabel(" ");
        fileLabel.setFont(FONT_LIST);
        fileLabel.setForeground(TEXT_DARK);

        sizeLabel = new JLabel(" ");
        sizeLabel.setFont(FONT_LABEL);
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
                BorderFactory.createLineBorder(BORDER),
                "Proyectos detectados",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                FONT_LABEL, TEXT_DARK));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topBar.setBackground(WHITE);

        selectAllBtn = makeButton("Seleccionar todos", new Color(0x55, 0x55, 0x55));
        selectAllBtn.setEnabled(false);
        selectAllBtn.addActionListener(e -> selectAll());

        resetBtn = makeButton("Solo necesarios", ACCENT);
        resetBtn.setEnabled(false);
        resetBtn.addActionListener(e -> resetToAutoDetected());

        clearBtn = makeButton("Deseleccionar todos", new Color(0x88, 0x88, 0x88));
        clearBtn.setEnabled(false);
        clearBtn.addActionListener(e -> clearSelection());

        countLabel = new JLabel("Proyectos: 0/0");
        countLabel.setFont(FONT_LABEL);
        countLabel.setForeground(TEXT_GRAY);
        countLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        topBar.add(selectAllBtn);
        topBar.add(resetBtn);
        topBar.add(clearBtn);
        topBar.add(countLabel);

        JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        searchRow.setBackground(WHITE);
        searchRow.setBorder(new EmptyBorder(4, 0, 4, 0));

        searchField = new JTextField();
        searchField.setFont(FONT_LIST);
        searchField.setForeground(TEXT_GRAY);
        searchField.setText("Buscar proyecto...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(4, 8, 4, 8)));
        searchField.setEnabled(false);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Buscar proyecto...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_DARK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(TEXT_GRAY);
                    searchField.setText("Buscar proyecto...");
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterPackages(); }
        });

        searchRow.add(searchField, BorderLayout.CENTER);

        listModel = new DefaultListModel<>();
        packageList = new JList<>(listModel);
        packageList.setFont(FONT_LIST);
        packageList.setCellRenderer(new CheckboxRenderer());
        packageList.setBackground(WHITE);
        packageList.setFixedCellHeight(30);
        packageList.setBorder(new EmptyBorder(0, 0, 0, 0));
        packageList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int idx = packageList.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    String group = listModel.get(idx);
                    Set<String> pkgs = projectToPackages.get(group);
                    if (pkgs == null || pkgs.isEmpty()) return;
                    boolean allSelected = selectedPackages.containsAll(pkgs);
                    if (allSelected) selectedPackages.removeAll(pkgs);
                    else selectedPackages.addAll(pkgs);
                    packageList.repaint();
                    updateCount();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(packageList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel center = new JPanel(new BorderLayout(0, 3));
        center.setBackground(WHITE);
        center.add(searchRow, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        p.add(topBar, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel createProgressPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(new Color(0xFF, 0xE0, 0xE0));
        progressBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        progressBar.setPreferredSize(new Dimension(0, 22));
        progressBar.setVisible(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FONT_LABEL);
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
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        footer.setPreferredSize(new Dimension(680, 54));

        compressBtn = makeButton("COMPRIMIR Y EXPORTAR", ACCENT_DARK);
        compressBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        compressBtn.setBorder(new EmptyBorder(10, 28, 10, 28));
        compressBtn.setEnabled(false);
        compressBtn.addActionListener(e -> compress());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(WHITE);
        btnPanel.add(compressBtn);
        footer.add(btnPanel, BorderLayout.CENTER);
        return footer;
    }

    private void resetToAutoDetected() {
        selectedPackages.clear();
        selectedPackages.addAll(autoDetectedPackages);
        packageList.repaint();
        updateCount();
        long selected = projectToPackages.entrySet().stream()
                .filter(e -> selectedPackages.containsAll(e.getValue()))
                .count();
        statusLabel.setText("Restaurada seleccion automatica: " + selected + "/" + projectGroups.size() + " proyectos.");
    }

    private void updateCount() {
        long selectedGroups = projectToPackages.entrySet().stream()
                .filter(e -> selectedPackages.containsAll(e.getValue()))
                .count();
        countLabel.setText("Proyectos: " + selectedGroups + "/" + projectGroups.size());
    }

    private void selectAll() {
        selectedPackages.clear();
        for (Set<String> pkgs : projectToPackages.values()) {
            selectedPackages.addAll(pkgs);
        }
        packageList.repaint();
        updateCount();
        statusLabel.setText("Seleccionados todos los proyectos.");
    }

    private void clearSelection() {
        selectedPackages.clear();
        packageList.repaint();
        updateCount();
        statusLabel.setText("Ningun proyecto seleccionado.");
    }

    private void filterPackages() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty() || searchField.getText().equals("Buscar proyecto...")) {
            listModel.clear();
            for (String group : projectGroups) listModel.addElement(group);
        } else {
            listModel.clear();
            for (String group : projectGroups) {
                if (group.toLowerCase().contains(q)) listModel.addElement(group);
            }
        }
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
        autoDetectedPackages.clear();
        autoEntryPoints.clear();
        selectedPackages.clear();
        listModel.clear();
        projectGroups.clear();
        projectToPackages.clear();
        searchField.setEnabled(false);
        searchField.setText("Buscar proyecto...");
        String size = formatSize(f.length());
        fileLabel.setText("JAR: " + f.getName());
        sizeLabel.setText("Tamano original: " + size);
        compressBtn.setEnabled(false);
        selectAllBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        updateCount();
        statusLabel.setForeground(TEXT_GRAY);
        statusLabel.setText("Analizando JAR...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        analyzeJar();
    }

    private void analyzeJar() {
        if (selectedJar == null) return;

        new SwingWorker<Void, Void>() {
            private String[] packages;
            private Set<String> reachablePkgs;
            private String error;

            protected Void doInBackground() {
                try {
                    cachedAnalyzer = new JarAnalyzer();
                    try (JarFile jf = new JarFile(selectedJar)) {
                        cachedAnalyzer.loadJar(jf);
                    }
                    packages = cachedAnalyzer.getPackages();

                    reachablePkgs = new HashSet<>();
                    autoEntryPoints.clear();
                    Set<String> entryPoints = cachedAnalyzer.detectEntryPoints();
                    try (JarFile jf = new JarFile(selectedJar)) {
                        java.util.jar.Manifest mf = jf.getManifest();
                        if (mf != null) {
                            entryPoints.addAll(cachedAnalyzer.detectEntryPointsFromManifest(mf));
                        }
                    }
                    if (!entryPoints.isEmpty()) {
                        autoEntryPoints.addAll(entryPoints);
                        Set<String> reachableClasses = cachedAnalyzer.findReachableClasses(entryPoints);

                        try (JarFile jf = new JarFile(selectedJar)) {
                            Enumeration<JarEntry> ents = jf.entries();
                            while (ents.hasMoreElements()) {
                                JarEntry e = ents.nextElement();
                                String en = e.getName();
                                if (en.startsWith("META-INF/services/") && !e.isDirectory()) {
                                    try (InputStream in = jf.getInputStream(e)) {
                                        java.util.Scanner sc = new java.util.Scanner(in).useDelimiter("\\A");
                                        String content = sc.hasNext() ? sc.next() : "";
                                        for (String line : content.split("\\r?\\n")) {
                                            line = line.trim();
                                            if (!line.isEmpty() && !line.startsWith("#")) {
                                                reachableClasses.add(line);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        reachablePkgs = new HashSet<>();
                        for (String cls : reachableClasses) {
                            int dot = cls.lastIndexOf('.');
                            if (dot > 0) reachablePkgs.add(cls.substring(0, dot));
                            else reachablePkgs.add("(default)");
                        }
                    }
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);

                if (error != null) {
                    statusLabel.setText("Error: " + error);
                    return;
                }

                if (packages == null || packages.length == 0) {
                    statusLabel.setText("No se encontraron paquetes en el JAR.");
                    return;
                }

                Map<String, Set<String>> groups = new TreeMap<>();
                for (String pkg : packages) {
                    groups.computeIfAbsent(getProjectGroup(pkg), k -> new LinkedHashSet<>()).add(pkg);
                }
                projectGroups = new ArrayList<>(groups.keySet());
                projectToPackages = groups;
                selectedPackages.clear();
                autoDetectedPackages.clear();
                Set<String> selectedProjects = new LinkedHashSet<>();

                for (Map.Entry<String, Set<String>> e : groups.entrySet()) {
                    boolean hasReachable = false;
                    for (String pkg : e.getValue()) {
                        if (reachablePkgs != null && reachablePkgs.contains(pkg)) {
                            hasReachable = true;
                            selectedPackages.add(pkg);
                        }
                    }
                    if (hasReachable) selectedProjects.add(e.getKey());
                }
                autoDetectedPackages.addAll(selectedPackages);

                if (selectedPackages.isEmpty()) {
                    for (Set<String> pkgs : groups.values()) selectedPackages.addAll(pkgs);
                    autoDetectedPackages.addAll(selectedPackages);
                }

                for (String group : projectGroups) {
                    listModel.addElement(group);
                }
                searchField.setEnabled(true);
                compressBtn.setEnabled(true);
                selectAllBtn.setEnabled(true);
                resetBtn.setEnabled(true);
                clearBtn.setEnabled(true);
                packageList.repaint();
                updateCount();

                int totalPkgs = (int) groups.values().stream().mapToLong(Set::size).sum();
                if (!selectedProjects.isEmpty()) {
                    statusLabel.setForeground(GREEN);
                    statusLabel.setText("Analisis completo: " + selectedProjects.size() + "/" + projectGroups.size()
                            + " proyectos necesarios (" + autoDetectedPackages.size() + "/" + totalPkgs + " paquetes).");
                } else {
                    statusLabel.setForeground(TEXT_GRAY);
                    statusLabel.setText("No se detectaron entry points. Seleccionados todos los proyectos. Desmarca los que no necesites.");
                }
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
        selectAllBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        statusLabel.setForeground(TEXT_GRAY);
        statusLabel.setText("Preparando...");

        new SwingWorker<Void, Integer>() {
            private MinimizeResult result;
            private String error;

            protected Void doInBackground() {
                try {
                    publish(0);
                    JarAnalyzer analyzer;
                    if (cachedAnalyzer != null) analyzer = cachedAnalyzer;
                    else {
                        analyzer = new JarAnalyzer();
                        try (JarFile jf = new JarFile(selectedJar)) { analyzer.loadJar(jf); }
                    }
                    Set<String> allClasses = analyzer.getAllClasses();

                    publish(5);

                    Set<String> entryPoints = new HashSet<>();
                    for (String ep : autoEntryPoints) {
                        int dot = ep.lastIndexOf('.');
                        String epPkg = dot > 0 ? ep.substring(0, dot) : "";
                        if (selectedPackages.contains(epPkg)) {
                            entryPoints.add(ep);
                        }
                    }
                    for (String pkg : selectedPackages) {
                        boolean hasAuto = false;
                        for (String ep : autoEntryPoints) {
                            if (ep.startsWith(pkg + ".") || ep.equals(pkg)) {
                                hasAuto = true;
                                break;
                            }
                        }
                        if (!hasAuto) {
                            for (String cls : allClasses) {
                                if (cls.startsWith(pkg)) entryPoints.add(cls);
                            }
                        }
                    }

                    if (entryPoints.isEmpty()) {
                        error = "Selecciona al menos un proyecto.";
                        return null;
                    }

                    publish(20);
                    statusLabel.setText("Trazando dependencias desde " + entryPoints.size() + " entry points...");
                    Set<String> reachable = analyzer.findReachableClasses(entryPoints);

                    publish(40);
                    statusLabel.setText("Expandiendo a paquetes completos...");
                    Set<String> toKeep = new HashSet<>();
                    for (String cls : reachable) {
                        String pkg = cls.contains(".") ? cls.substring(0, cls.lastIndexOf('.')) : "";
                        for (String c : allClasses) {
                            if (c.startsWith(pkg + ".") || c.equals(pkg)) toKeep.add(c);
                        }
                    }

                    publish(50);
                    statusLabel.setText("Incluyendo servicios SPI (META-INF/services)...");
                    try (JarFile jf = new JarFile(selectedJar)) {
                        Enumeration<JarEntry> ents = jf.entries();
                        while (ents.hasMoreElements()) {
                            JarEntry e = ents.nextElement();
                            String en = e.getName();
                            if (en.startsWith("META-INF/services/") && !e.isDirectory()) {
                                try (InputStream in = jf.getInputStream(e)) {
                                    java.util.Scanner sc = new java.util.Scanner(in).useDelimiter("\\A");
                                    String content = sc.hasNext() ? sc.next() : "";
                                    for (String line : content.split("\\r?\\n")) {
                                        line = line.trim();
                                        if (!line.isEmpty() && !line.startsWith("#")) {
                                            String pkg = line.contains(".") ? line.substring(0, line.lastIndexOf('.')) : "";
                                            for (String c : allClasses) {
                                                if (c.startsWith(pkg + ".") || c.equals(pkg)) toKeep.add(c);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    publish(70);
                    statusLabel.setText("Generando JAR optimizado (" + toKeep.size() + "/" + allClasses.size() + " clases)...");
                    result = JarMinimizer.minimize(selectedJar, output, toKeep, allClasses, analyzer.getClassBytes());

                    publish(100);
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            protected void process(List<Integer> chunks) {
                int v = chunks.get(chunks.size() - 1);
                progressBar.setValue(v);
                if (v == 0) progressBar.setString("");
                else progressBar.setString(v + "%");
            }

            protected void done() {
                compressBtn.setEnabled(true);
                selectBtn.setEnabled(true);
                selectAllBtn.setEnabled(true);
                resetBtn.setEnabled(true);
                clearBtn.setEnabled(true);

                if (error != null) {
                    statusLabel.setForeground(ACCENT_DARK);
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
                progressBar.setString("100%");
                String saved = formatSize(result.savedBytes);
                String original = formatSize(selectedJar.length());
                String newSize = formatSize(selectedJar.length() - result.savedBytes);

                statusLabel.setForeground(GREEN);
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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT);
        g.fillRoundRect(0, 0, 16, 16, 3, 3);
        g.setColor(WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        String t = "JO";
        int x = (16 - fm.stringWidth(t)) / 2;
        int y = (16 + fm.getAscent()) / 2 - 1;
        g.drawString(t, x, y);
        g.dispose();
        return img;
    }

    private static String getProjectGroup(String pkg) {
        int first = pkg.indexOf('.');
        if (first < 0) return pkg;
        int second = pkg.indexOf('.', first + 1);
        if (second < 0) return pkg;
        return pkg.substring(0, second);
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
            setFont(FONT_LIST);
        }
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            Set<String> pkgs = projectToPackages.get(value);
            boolean checked = pkgs != null && !pkgs.isEmpty() && selectedPackages.containsAll(pkgs);
            setSelected(checked);
            setBackground(isSelected ? ROW_HOVER : WHITE);
            setForeground(TEXT_DARK);
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
