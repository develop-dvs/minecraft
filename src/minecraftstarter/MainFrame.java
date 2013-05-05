package minecraftstarter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author Crimson
 */
public class MainFrame extends javax.swing.JFrame {

    private boolean def;
    private String S = SystemUtils.FILE_SEPARATOR;
    private String Q = "\"";
    private String ext = ".exe";
    private String patch = "";
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
        patchButton.setText(patch);
        nameField.requestFocusInWindow();
        def = true;
    }
    
    public void setPatch() {
        JFileChooser jfc = new JFileChooser(patch);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            patch=jfc.getSelectedFile().getAbsolutePath();
            patchButton.setText(patch);
        }
    }
    
    public void launch() {
        String java = getJavaHome() + S + "java" + ext;
        if (!new File(java).exists()) {
            JOptionPane.showMessageDialog(rootPane, java, "JRE not found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java = Q + java + Q;

        ArrayList<String> params = new ArrayList<String>();
        params.add(java);
        params.add("-Xms" + xms.getText().trim() + "m");
        params.add("-Xmx" + xmx.getText().trim() + "m");
        params.add("-cp");
        params.add(Q
                + patch + S + "bin" + S + "minecraft.jar;"
                + patch + S + "bin" + S + "lwjgl.jar;"
                + patch + S + "bin" + S + "lwjgl_util.jar;"
                + patch + S + "bin" + S + "jinput.jar;"
                + Q);
        params.add("-Djava.library.path=" + Q + patch + S + "bin" + S + "natives" + Q);
        params.add("net.minecraft.client.Minecraft");
        params.add(nameField.getText().trim());
        if (passwordField.getPassword().length != 0) {
            params.add(new String(passwordField.getPassword()));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(params);
            String dbg="";
            for (String string : params) {
                dbg+=string+" ";
            }
            System.out.println(dbg);
            process = pb.start();
            
            writeUsername(patch);
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
    }
    
    @Deprecated
    public void launch_exec() {
        String java = getJavaHome() + S + "java" + ext;
        if (!new File(java).exists()) {
            JOptionPane.showMessageDialog(rootPane, java,"JRE not found",JOptionPane.ERROR_MESSAGE);
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
                + " net.minecraft.client.Minecraft " + nameField.getText().trim() + ((passwordField.getPassword().length==0)?"":" "+new String(passwordField.getPassword()));

        System.out.println(exec);
        try {
            Runtime.getRuntime().exec(exec);
            writeUsername(patch);
        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }
        System.exit(1);
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
        patchButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Simple Minecraft Starter (Divasoft, inc.)");
        setAlwaysOnTop(true);
        setResizable(false);
        setType(java.awt.Window.Type.UTILITY);

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
                .addComponent(launchButton, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE)
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

        patchButton.setText("patch");
        patchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                patchButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(patchButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(patchButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void patchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_patchButtonActionPerformed
        setPatch();
    }//GEN-LAST:event_patchButtonActionPerformed

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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JComboBox javaVersion;
    private javax.swing.JButton launchButton;
    private javax.swing.JTextField nameField;
    private javax.swing.JComboBox osArch;
    private javax.swing.JComboBox osType;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JButton patchButton;
    private javax.swing.JCheckBox rBox;
    private javax.swing.JTextField xms;
    private javax.swing.JTextField xmx;
    // End of variables declaration//GEN-END:variables
}
