package minecraftstarter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
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
        def = true;

    }
    
    @Override
    public void find(String str) {
        System.out.println("Find: "+str);
        if (server!=null) {
            server.shutdownServer();
        }
        if (process!=null) {
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
        int sw = typeLauncher.getSelectedIndex();
        switch (sw) {
            case 0:
            default:
                pathText.setText("");
                patternText.setText("");
                matchStr.setText("");
                runClass.setText("");
                break;
            case 1:
                pathText.setText("/site/launcher.php");
                patternText.setText("login={login}&password={password}&action=auth");
                matchStr.setText("sessionId");
                runClass.setText("net.sashok724.launcher.run.I");
                break;
        }
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
            URL url = new URL("http://" + urlText.getText().trim() + pathText.getText().trim());
            System.out.println(url + "_" + urlParameters);
            URLConnection conn = url.openConnection();

            conn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());

            writer.write(urlParameters);
            writer.flush();

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            outArea.setText("");
            while ((line = reader.readLine()) != null) {
                outArea.append(line + "\n");
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
        String exec = java + " -Dhttp.proxyHost=" + proxyHost.getText() + " -Dhttp.proxyPort=" + proxyPort.getText() + " -jar " + jarExPatch.getText();

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
        params.add("net.sashok724.launcher.run.I");
                        
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
        jPanel2 = new javax.swing.JPanel();
        osArch = new javax.swing.JComboBox();
        osType = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        xmx = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        xms = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        javaVersion = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        urlText = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        outArea = new javax.swing.JTextArea();
        jLabel7 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        typeLauncher = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        pathText = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        patternText = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        consoleLog = new javax.swing.JTextArea();
        jarExPatch = new javax.swing.JButton();
        penGo = new javax.swing.JButton();
        proxyHost = new javax.swing.JTextField();
        proxyPort = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        runClass = new javax.swing.JTextField();
        matchStr = new javax.swing.JTextField();

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
                .addComponent(launchButton, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nameField)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(passwordField)
                    .addComponent(rBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(launchButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        urlText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        jLabel5.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel5.setText("URL:");

        jLabel6.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel6.setText("MD5:");

        outArea.setColumns(20);
        outArea.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        outArea.setRows(5);
        jScrollPane1.setViewportView(outArea);

        jLabel7.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel7.setText("SSID:");

        jTextField2.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        typeLauncher.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        typeLauncher.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "<!-- DONT USE -->", "sashok724 launcher", "other" }));
        typeLauncher.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                typeLauncherItemStateChanged(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel9.setText("Type:");

        pathText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        jLabel10.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel10.setText("Path:");

        jLabel8.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N
        jLabel8.setText("PT:");

        patternText.setFont(new java.awt.Font("Lucida Console", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(typeLauncher, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(urlText, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pathText))
                    .addComponent(patternText, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextField2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(typeLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(urlText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(pathText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(patternText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        consoleLog.setEditable(false);
        consoleLog.setColumns(20);
        consoleLog.setRows(5);
        consoleLog.addHierarchyListener(new java.awt.event.HierarchyListener() {
            public void hierarchyChanged(java.awt.event.HierarchyEvent evt) {
                consoleLogHierarchyChanged(evt);
            }
        });
        consoleLog.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                consoleLogInputMethodTextChanged(evt);
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
        });
        consoleLog.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                consoleLogPropertyChange(evt);
            }
        });
        consoleLog.addVetoableChangeListener(new java.beans.VetoableChangeListener() {
            public void vetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {
                consoleLogVetoableChange(evt);
            }
        });
        jScrollPane2.setViewportView(consoleLog);

        jarExPatch.setText("Select launcher.jar");
        jarExPatch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jarExPatchActionPerformed(evt);
            }
        });

        penGo.setText("GO!");
        penGo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                penGoActionPerformed(evt);
            }
        });

        proxyHost.setText("localhost");

        proxyPort.setText("8088");

        jButton1.setText("Proxy Start");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(runClass)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jarExPatch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(penGo))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(proxyHost, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(proxyPort, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(matchStr)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jarExPatch)
                    .addComponent(penGo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(runClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proxyHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proxyPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1)
                    .addComponent(matchStr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void typeLauncherItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_typeLauncherItemStateChanged
        update();
    }//GEN-LAST:event_typeLauncherItemStateChanged

    private void jarExPatchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jarExPatchActionPerformed
        setTextButton(jarExPatch, "Select jar", "jar", "Jar file (*.jar)");
    }//GEN-LAST:event_jarExPatchActionPerformed

    private void penGoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_penGoActionPerformed
        runExtJar();
    }//GEN-LAST:event_penGoActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        server = new Jhttpp2Server(true, Integer.parseInt(proxyPort.getText().trim()),this,matchStr.getText());

        if (Jhttpp2Server.error) {
            System.out.println("Error: " + Jhttpp2Server.error_msg);
        } else {
            new Thread(server).start();
            System.out.println("Running on port " + server.port);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void consoleLogPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_consoleLogPropertyChange

    }//GEN-LAST:event_consoleLogPropertyChange

    private void consoleLogVetoableChange(java.beans.PropertyChangeEvent evt)throws java.beans.PropertyVetoException {//GEN-FIRST:event_consoleLogVetoableChange

    }//GEN-LAST:event_consoleLogVetoableChange

    private void consoleLogHierarchyChanged(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_consoleLogHierarchyChanged

    }//GEN-LAST:event_consoleLogHierarchyChanged

    private void consoleLogInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_consoleLogInputMethodTextChanged

    }//GEN-LAST:event_consoleLogInputMethodTextChanged

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
    private javax.swing.JTextArea consoleLog;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JButton jarExPatch;
    private javax.swing.JComboBox javaVersion;
    private javax.swing.JButton launchButton;
    private javax.swing.JTextField matchStr;
    private javax.swing.JTextField nameField;
    private javax.swing.JComboBox osArch;
    private javax.swing.JComboBox osType;
    private javax.swing.JTextArea outArea;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JTextField pathText;
    private javax.swing.JTextField patternText;
    private javax.swing.JButton penGo;
    private javax.swing.JTextField proxyHost;
    private javax.swing.JTextField proxyPort;
    private javax.swing.JCheckBox rBox;
    private javax.swing.JTextField runClass;
    private javax.swing.JComboBox typeLauncher;
    private javax.swing.JTextField urlText;
    private javax.swing.JTextField xms;
    private javax.swing.JTextField xmx;
    // End of variables declaration//GEN-END:variables
}
