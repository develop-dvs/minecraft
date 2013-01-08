package minecraftstarter;

import degif.Load;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import proxy.Jhttpp2Server;

/**
 *
 * @author Crimson
 */
public class MainFrame extends javax.swing.JFrame implements IFind {

    private boolean def;
    private String S = SystemUtils.FILE_SEPARATOR;
    private String Q = "\"";
    private String ext = ".exe";
    private String patch = "";
    private Jhttpp2Server server;
    private Process process;
    private Thread proxyThread;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        this.setLocationRelativeTo(null);
        if (SystemUtils.IS_OS_UNIX) {
            osType.setSelectedIndex(1);
            javaVersion.setEnabled(false);
            osArch.setEnabled(false);
        }
        if (SystemUtils.IS_JAVA_1_6) {
            javaVersion.setSelectedIndex(0);
        }
        if (SystemUtils.IS_JAVA_1_7) {
            javaVersion.setSelectedIndex(1);
        }
        if (SystemUtils.IS_JAVA_1_8) {
            javaVersion.setSelectedIndex(2);
        }
        if (SystemUtils.OS_ARCH.contains("64")) {
            osArch.setSelectedIndex(1);
        }
        patch = buildMinecraftPatch();
        dumpContents(patch);

        nameField.requestFocusInWindow();
        proxy();
        def = true;

    }

    @Override
    public void find(String str) {
        System.out.println("Find: " + str);
        /*if (server!=null) {
         server.shutdownServer();
         }*/
        if (process != null) {
            process.destroy();
        }
        consoleLog.setText(str);
    }

    public void launch() {
        String java = getJavaHome() + S + "java" + ext;
        if (!new File(java).exists()) {
            JOptionPane.showMessageDialog(rootPane, java, "JRE not found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java = Q + java + Q;
        String exec = java + " -Xms" + xms.getText().trim() + "m -Xmx" + xmx.getText().trim() + "m -cp " + Q
                + patch + S + "bin" + S + "minecraft.jar;"
                + patch + S + "bin" + S + "lwjgl.jar;"
                + patch + S + "bin" + S + "lwjgl_util.jar;"
                + patch + S + "bin" + S + "jinput.jar;"
                + Q
                + " -Djava.library.path=" + Q + patch + S + "bin" + S + "natives" + Q
                + " net.minecraft.client.Minecraft " + nameField.getText().trim() + ((passwordField.getPassword().length == 0) ? "" : " " + new String(passwordField.getPassword()));

        //System.out.println(exec);
        sendAuth();
        /*
         try {
         Runtime.getRuntime().exec(exec);
         writeUsername(patch);
         } catch (Exception ex) {
         System.err.println(ex.getLocalizedMessage());
         }

         System.exit(1);*/

    }

    public String getJavaHome() {
        if (def) {
            return SystemUtils.getJavaHome().getAbsolutePath() + S + "bin";
        } else {
            return buildPatch();
        }
    }

    public String buildPatch() {
        // TODO: *nix
        String p = System.getenv("ProgramFiles") + (((SystemUtils.OS_ARCH.contains("64")) && osArch.getSelectedIndex() == 0) ? " (x86)" : "");
        if (osArch.getSelectedIndex() == 1) {
            p = p.replace(" (x86)", "");
        }
        String j = "Java" + S + "jre" + javaVersion.getSelectedItem().toString() + S + "bin";
        return p + S + j;
    }

    private String buildMinecraftPatch() {
        String dataFolder = "";
        if (new File("bin").exists() && new File("resources").exists()) {
            try {
                dataFolder = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().toString();
            } catch (Exception ex) {
                dataFolder = "";
                System.err.println(ex.getLocalizedMessage());
            }
        }
        return (dataFolder.isEmpty()) ? System.getenv("APPDATA") + S + ".minecraft" : dataFolder;
    }

    private void dumpContents(String path) {
        try {
            File lastLogin = new File(path, "lastlogin");
            Cipher cipher = getCipher(2, "passwordfile");
            DataInputStream dIS;
            if (cipher != null) {
                dIS = new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin), cipher));
            } else {
                dIS = new DataInputStream(new FileInputStream(lastLogin));
            }
            nameField.setText(dIS.readUTF());
            passwordField.setText(dIS.readUTF());
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }

    private void writeUsername(String path) {
        try {
            File lastLogin = new File(path, "lastlogin");
            Cipher cipher = getCipher(1, "passwordfile");
            DataOutputStream dos;
            if (cipher != null) {
                dos = new DataOutputStream(new CipherOutputStream(new FileOutputStream(lastLogin), cipher));
            } else {
                dos = new DataOutputStream(new FileOutputStream(lastLogin));
            }
            dos.writeUTF(nameField.getText());
            dos.writeUTF(rBox.isSelected() ? new String(passwordField.getPassword()) : "");
            dos.close();
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }

    private static Cipher getCipher(int mode, String password) throws Exception {
        Random random = new Random(0x29482c2L);
        byte salt[] = new byte[8];
        random.nextBytes(salt);
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);
        javax.crypto.SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(mode, pbeKey, pbeParamSpec);
        return cipher;
    }

    private void update() {
    }

    private String buildPattern() {
        String ret = patternText.getText().trim();
        ret = ret.replace("{login}", nameField.getText().trim());
        ret = ret.replace("{password}", new String(passwordField.getPassword()));
        return ret;
    }

    private void sendAuth() {
        try {
            String urlParameters = buildPattern();
            URL url = new URL("http://" + urlText.getText().trim() + pathText.getSelectedItem().toString().trim());
            System.out.println(url + "_" + urlParameters);
            URLConnection conn = url.openConnection();

            conn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

            writer.write(urlParameters);
            writer.flush();

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            consoleLog.setText("");
            while ((line = reader.readLine()) != null) {
                consoleLog.append(line + "\n");
            }
            writer.close();
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String setTextButton(JButton jButton, String header, String ext, String text) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(header);
        fileChooser.setFileFilter(new FileNameExtensionFilter(text, ext));
        if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION) {
            //jButton.setText(prefix+fileChooser.getSelectedFile().getName().toString());
            jButton.setText(fileChooser.getSelectedFile().getAbsolutePath().toString());
        }
        return "";
    }

    public void runExtJar() {
        String java = getJavaHome() + S + "java" + ext;
        java = Q + java + Q;
        //String exec = java + " -Dhttp.proxyHost=" + proxyHost.getText() + " -Dhttp.proxyPort=" + proxyPort.getText() + " -jar " + jarExPatch.getText();

        ArrayList<String> params = new ArrayList<String>();
        params.add(java);
        params.add("-Dhttp.proxyHost=" + proxyHost.getText());
        params.add("-Dhttp.proxyPort=" + proxyPort.getText());

        params.add("-Dsun.java2d.noddraw=true");
        params.add("-Dsun.java2d.d3d=false");
        params.add("-Dsun.java2d.opengl=false");
        //params.add("-Dsun.java2d.pmoffscreen=false");

        params.add("-classpath");
        params.add(jarExPatch.getText());
        params.add(classText.getSelectedItem().toString());

        //params.add("-jar");
        //params.add(jarExPatch.getText());


        try {
            ProcessBuilder pb = new ProcessBuilder(params);
            process = pb.start();

            //System.out.println(exec);
            //Runtime.getRuntime().exec(exec);
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }

    private void proxy() {
        if (proxyButton.isSelected()) {
            proxyButton.setText("Proxy Stop");
            server = new Jhttpp2Server(true, Integer.parseInt(proxyPort.getText().trim()), this, patternText.getText());

            if (Jhttpp2Server.error) {
                consoleLog.append("Error: " + Jhttpp2Server.error_msg);
                proxyButton.setSelected(false);
            } else {
                proxyThread = new Thread(server);
                proxyThread.setName("Proxy");
                proxyThread.start();
                consoleLog.append("Proxy server running on port " + server.port + "\n");
                penetratePanel.setEnabled(true);
            }
        } else {
            proxyButton.setText("Proxy Start");
            if (server != null) {
                server.shutdownServer();
                if (proxyThread != null) {
                    proxyThread.stop();
                    consoleLog.append("Proxy server shutdown\n");
                }
            }
            penetratePanel.setEnabled(false);
        }

    }

    public void loadClass() {
        ArrayList<String> elements = new ArrayList<>();
        Load load = new Load(jarExPatch.getText().trim(), "I.I", "I", int.class);
        int max = Integer.parseInt(maxGif.getText());
        Pattern pattern = Pattern.compile(pathText.getSelectedItem().toString());
        
        for (int i = 0; i < max; i++) {
            //System.out.println("["+i+":] "+I.I(i));
            try {
                String add = load.invoke(i).toString();
                Matcher matcher = pattern.matcher(add);
                if (matcher.find()) {
                    elements.add(add);
                }
            } catch (Exception ex) {
            }
        }

        Set<String> s = new LinkedHashSet<>(elements);
        for (Iterator<String> it = s.iterator(); it.hasNext();) {
            String string = it.next();
            System.out.println(string);
        }
    }
    /*public static void setTextForm(JTextField textField, String header)
     {
     JFileChooser fileChooser = new JFileChooser();
     fileChooser.setCurrentDirectory(new java.io.File("."));
     fileChooser.setDialogTitle(header);
     fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
     fileChooser.setAcceptAllFileFilterUsed(false);
     if (fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION)
     {
     textField.setText(fileChooser.getSelectedFile().getAbsolutePath().toString());
     }
     }*/

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        nameField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();
        rBox = new javax.swing.JCheckBox();
        launchButton = new javax.swing.JButton();
        pathText1 = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        osArch = new javax.swing.JComboBox();
        osType = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        xmx = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        xms = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        javaVersion = new javax.swing.JComboBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        monitorPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        consoleLog = new javax.swing.JTextArea();
        proxyHost = new javax.swing.JTextField();
        proxyPort = new javax.swing.JTextField();
        proxyButton = new javax.swing.JToggleButton();
        penetratePanel = new javax.swing.JPanel();
        urlText = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        ssidText = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        patternText = new javax.swing.JTextField();
        jarExPatch = new javax.swing.JButton();
        penGo = new javax.swing.JButton();
        classText = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        pathText = new javax.swing.JComboBox();
        gifPanel = new javax.swing.JPanel();
        gifSelect = new javax.swing.JComboBox();
        gifStart = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        gifConsole = new javax.swing.JTextArea();
        gifStart1 = new javax.swing.JButton();
        maxGif = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Simple Minecraft Starter (Divasoft, inc.)");
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        nameField.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        nameField.setToolTipText("Login");
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nameFieldKeyReleased(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel1.setText("Name:");

        passwordField.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        passwordField.setToolTipText("Password ;)");
        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                passwordFieldKeyReleased(evt);
            }
        });

        rBox.setToolTipText("Remember");

        launchButton.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        launchButton.setText("Start");
        launchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launchButtonActionPerformed(evt);
            }
        });

        pathText1.setEditable(true);
        pathText1.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        pathText1.setToolTipText("Server NAME|IP:PORT");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(launchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pathText1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(pathText1))
                    .addComponent(nameField)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(passwordField)
                    .addComponent(rBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(launchButton))
                .addContainerGap())
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        osArch.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        osArch.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "x32", "x64" }));
        osArch.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                osArchItemStateChanged(evt);
            }
        });

        osType.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        osType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "WIN", "*nix" }));
        osType.setEnabled(false);
        osType.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                osTypeItemStateChanged(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel2.setText("JRE:");

        xmx.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        xmx.setText("1024");
        xmx.setMinimumSize(new java.awt.Dimension(30, 20));

        jLabel3.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel3.setText("-Xmx");

        xms.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        xms.setText("512");

        jLabel4.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel4.setText("-Xms");

        javaVersion.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        javaVersion.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "6", "7", "8" }));
        javaVersion.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                javaVersionItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(osType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(javaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(osArch, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xms, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xmx, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(osType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(javaVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(osArch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(xms, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(xmx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPane1.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        consoleLog.setEditable(false);
        consoleLog.setColumns(20);
        consoleLog.setFont(new java.awt.Font("Lucida Console", 0, 9)); // NOI18N
        consoleLog.setRows(5);
        jScrollPane2.setViewportView(consoleLog);

        proxyHost.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        proxyHost.setText("localhost");

        proxyPort.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        proxyPort.setText("8088");

        proxyButton.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        proxyButton.setText("Proxy Start");
        proxyButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                proxyButtonItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout monitorPanelLayout = new javax.swing.GroupLayout(monitorPanel);
        monitorPanel.setLayout(monitorPanelLayout);
        monitorPanelLayout.setHorizontalGroup(
            monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, monitorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, monitorPanelLayout.createSequentialGroup()
                        .addComponent(proxyHost, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(proxyPort, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(proxyButton)
                        .addGap(0, 200, Short.MAX_VALUE)))
                .addContainerGap())
        );
        monitorPanelLayout.setVerticalGroup(
            monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(monitorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(monitorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxyHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxyPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxyButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Monitor", monitorPanel);

        penetratePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        urlText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel5.setText("URL:");

        jLabel7.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel7.setText("SSID:");

        ssidText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel10.setText("<html><body><a href=\"http://myregexp.com/signedJar.html\" target=\"_BLANK\">ADR</a>:</body></html>");
        jLabel10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel10MouseClicked(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel8.setText("PT:");

        patternText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        patternText.setText("sessionId");

        jarExPatch.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jarExPatch.setText("Select launcher.jar");
        jarExPatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jarExPatchActionPerformed(evt);
            }
        });

        penGo.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        penGo.setText("GO!");
        penGo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                penGoActionPerformed(evt);
            }
        });

        classText.setEditable(true);
        classText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        classText.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "net.sashok724.launcher.run.I", "net.sashok724.launcher.run.MainClass", " " }));

        jLabel9.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel9.setText("CLASS:");

        pathText.setEditable(true);
        pathText.setFont(new java.awt.Font("Lucida Console", 0, 9)); // NOI18N
        pathText.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "(, ([a-z0-9\\.]{5,50}), ([0-9]{4,5}),([0-9a-zA-Z\\. ]{1,50}),)", "(([a-z0-9\\.]{5,50}), ([0-9]{4,5}), ([0-9.]{5}),([0-9a-zA-Z ]{5,50}))", " " }));
        pathText.setToolTipText("<html>Find: [649:] u, 11111, 1.2.5,iCraft mcMMO 1, <b>srv1.icraft.su, 22222, 1.2.5,iCraft mcMMO 2</b>, srv5.icraft.su, 22222, 1.2.5-iCraft Ha</html>");

        javax.swing.GroupLayout penetratePanelLayout = new javax.swing.GroupLayout(penetratePanel);
        penetratePanel.setLayout(penetratePanelLayout);
        penetratePanelLayout.setHorizontalGroup(
            penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(penetratePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(penetratePanelLayout.createSequentialGroup()
                        .addComponent(jarExPatch, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(penGo))
                    .addGroup(penetratePanelLayout.createSequentialGroup()
                        .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(patternText)
                            .addComponent(urlText, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(classText, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(ssidText, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(pathText, javax.swing.GroupLayout.Alignment.LEADING, 0, 387, Short.MAX_VALUE))))
                .addContainerGap())
        );
        penetratePanelLayout.setVerticalGroup(
            penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(penetratePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(urlText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(patternText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pathText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ssidText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(classText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(penetratePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jarExPatch)
                    .addComponent(penGo))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Settings", penetratePanel);

        gifSelect.setEditable(true);
        gifSelect.setFont(new java.awt.Font("Lucida Console", 0, 10)); // NOI18N
        gifSelect.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "I.gif", " " }));

        gifStart.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        gifStart.setText("Fing gif");

        gifConsole.setColumns(20);
        gifConsole.setFont(new java.awt.Font("Lucida Console", 0, 9)); // NOI18N
        gifConsole.setRows(5);
        jScrollPane1.setViewportView(gifConsole);

        gifStart1.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        gifStart1.setText("Decrypt");
        gifStart1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gifStart1ActionPerformed(evt);
            }
        });

        maxGif.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        maxGif.setText("5000");
        maxGif.setToolTipText("Max");

        javax.swing.GroupLayout gifPanelLayout = new javax.swing.GroupLayout(gifPanel);
        gifPanel.setLayout(gifPanelLayout);
        gifPanelLayout.setHorizontalGroup(
            gifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, gifPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(gifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addGroup(gifPanelLayout.createSequentialGroup()
                        .addComponent(gifStart, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gifSelect, 0, 205, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxGif, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gifStart1)))
                .addContainerGap())
        );
        gifPanelLayout.setVerticalGroup(
            gifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gifPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(gifPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gifSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gifStart)
                    .addComponent(gifStart1)
                    .addComponent(maxGif, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("GifDecrypt", gifPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void osTypeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_osTypeItemStateChanged
        def = false;
    }//GEN-LAST:event_osTypeItemStateChanged

    private void javaVersionItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_javaVersionItemStateChanged
        def = false;
    }//GEN-LAST:event_javaVersionItemStateChanged

    private void osArchItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_osArchItemStateChanged
        def = false;
    }//GEN-LAST:event_osArchItemStateChanged

    private void launchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchButtonActionPerformed
        launch();
    }//GEN-LAST:event_launchButtonActionPerformed

    private void nameFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nameFieldKeyReleased
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            launch();
        }
    }//GEN-LAST:event_nameFieldKeyReleased

    private void passwordFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passwordFieldKeyReleased
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            launch();
        }
    }//GEN-LAST:event_passwordFieldKeyReleased

    private void jarExPatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jarExPatchActionPerformed
        setTextButton(jarExPatch, "Select jar", "jar", "Jar file (*.jar)");
    }//GEN-LAST:event_jarExPatchActionPerformed

    private void penGoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_penGoActionPerformed
        runExtJar();
    }//GEN-LAST:event_penGoActionPerformed

    private void proxyButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_proxyButtonItemStateChanged
        proxy();
    }//GEN-LAST:event_proxyButtonItemStateChanged

    private void jLabel10MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel10MouseClicked
        try {
            Desktop.getDesktop().browse(new URI("http://myregexp.com/signedJar.html"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_jLabel10MouseClicked

    private void gifStart1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gifStart1ActionPerformed
        loadClass();
    }//GEN-LAST:event_gifStart1ActionPerformed

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
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox classText;
    private javax.swing.JTextArea consoleLog;
    private javax.swing.JTextArea gifConsole;
    private javax.swing.JPanel gifPanel;
    private javax.swing.JComboBox gifSelect;
    private javax.swing.JButton gifStart;
    private javax.swing.JButton gifStart1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton jarExPatch;
    private javax.swing.JComboBox javaVersion;
    private javax.swing.JButton launchButton;
    private javax.swing.JTextField maxGif;
    private javax.swing.JPanel monitorPanel;
    private javax.swing.JTextField nameField;
    private javax.swing.JComboBox osArch;
    private javax.swing.JComboBox osType;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JComboBox pathText;
    private javax.swing.JComboBox pathText1;
    private javax.swing.JTextField patternText;
    private javax.swing.JButton penGo;
    private javax.swing.JPanel penetratePanel;
    private javax.swing.JToggleButton proxyButton;
    private javax.swing.JTextField proxyHost;
    private javax.swing.JTextField proxyPort;
    private javax.swing.JCheckBox rBox;
    private javax.swing.JTextField ssidText;
    private javax.swing.JTextField urlText;
    private javax.swing.JTextField xms;
    private javax.swing.JTextField xmx;
    // End of variables declaration//GEN-END:variables
}
