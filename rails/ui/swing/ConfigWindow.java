package rails.ui.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Map;
import java.util.List;

import rails.common.ConfigItem;
import rails.common.LocalText;
import rails.common.ConfigManager;
import rails.common.parser.ConfigurationException;
import rails.ui.swing.elements.RailsIcon;
import rails.util.Util;

class ConfigWindow extends JFrame {
    private static final long serialVersionUID = 1L;

    //restrict field width as there may be extremely long texts
    //(e.g. specifying file names >2000px)
    private static final int MAX_FIELD_WIDTH = 200;
    
    private JPanel profilePanel;
    private JTabbedPane configPane;
    private JPanel buttonPanel;
    
    private boolean fromStatusWindow;
    
    private ConfigManager cm;
    
    ConfigWindow(boolean fromStatusWindow) {
        cm = ConfigManager.getInstance();
        
        // store for handling of close
        this.fromStatusWindow = fromStatusWindow;

        // JFrame properties
        setTitle(LocalText.getText("CONFIG_WINDOW_TITLE"));
        
        // add profile panel
        profilePanel = new JPanel();
        add(profilePanel, "North");
        
        // configSetup pane
        configPane = new JTabbedPane();
        add(configPane, "Center");
 
        // buttons
        buttonPanel = new JPanel();
        add(buttonPanel, "South");

        
        // hide on close and inform  
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConfig(false);
            }
        });
    }
    
    public void init(final boolean startUp) {
        setupProfilePanel();
        setupConfigPane();
        setupButtonPanel();
        
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                ConfigWindow.this.repaint();
                if (startUp) {
                    ConfigWindow.this.setSize(600,400);
                }
            }
        });
    }

    private void setupProfilePanel() {
        profilePanel.removeAll();

        String activeProfile = cm.getActiveProfile();
        String profileText;
        if (cm.IsActiveUserProfile()) {
            profileText =  LocalText.getText("CONFIG_USER_PROFILE", activeProfile);
        } else {
            profileText =  LocalText.getText("CONFIG_PREDEFINED_PROFILE", activeProfile);
        }
        
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, profileText);
        profilePanel.setBorder(titled);
        
        JLabel userLabel = new JLabel(LocalText.getText("CONFIG_SELECT_PROFILE"));
        profilePanel.add(userLabel);

        final JComboBox comboBoxProfile = new JComboBox(cm.getProfiles().toArray());
        comboBoxProfile.setSelectedItem(activeProfile);
        comboBoxProfile.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                changeProfile((String)comboBoxProfile.getSelectedItem());
            }
        }
        );
        profilePanel.add(comboBoxProfile);
    }
    
    
    
    private void setupConfigPane() {
        configPane.removeAll();
        
        Border etched = BorderFactory.createEtchedBorder();
        Border titled = BorderFactory.createTitledBorder(etched, LocalText.getText("CONFIG_SETTINGS"));
        configPane.setBorder(titled);
        
        Map<String, List<ConfigItem>> configSections = cm.getConfigSections();
        int maxElements = cm.getMaxElementsInPanels();
       
        for (String sectionName:configSections.keySet()) {
            JPanel newPanel = new JPanel();
            newPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.weightx = 0.8;
            gbc.weighty = 0.8;
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;
            for (ConfigItem item:configSections.get(sectionName)) {
                gbc.gridx = 0;
                gbc.gridy++;
                defineElement(newPanel, item, gbc);
            }
            // fill up to maxElements
            gbc.gridx = 0;
            while (++gbc.gridy < maxElements) {
                JLabel emptyLabel = new JLabel("");
                newPanel.add(emptyLabel, gbc);
            }
            JScrollPane slider = new JScrollPane(newPanel);
            configPane.addTab(LocalText.getText("Config.section." + sectionName), slider);
        }
    }
    
    
    private void addToGridBag(JComponent container, JComponent element, GridBagConstraints gbc) {
        container.add(element, gbc);
        gbc.gridx ++;
    }
    
    private void addEmptyLabel(JComponent container, GridBagConstraints gbc) {
        JLabel label = new JLabel("");
        addToGridBag(container, label, gbc );
    }
    
    private void defineElement(JPanel panel, final ConfigItem item, GridBagConstraints gbc) {
        
        // current value (based on current changes and properties)
        String configValue = item.getCurrentValue();

        // item label, toolTip and infoText
        final String itemLabel = LocalText.getText("Config.label." + item.name);
        final String toolTip = LocalText.getTextWithDefault("Config.toolTip." + item.name, null);
        final String infoText = LocalText.getTextWithDefault("Config.infoText." + item.name, null);
  
        // define label        
        JLabel label = new JLabel(itemLabel);
        label.setToolTipText(toolTip);
        gbc.fill = GridBagConstraints.NONE;
        addToGridBag(panel, label, gbc);
        
        switch (item.type) {
        case BOOLEAN:
            final JCheckBox checkBox = new JCheckBox();
            boolean selected = Util.parseBoolean(configValue);
            checkBox.setSelected(selected);
            checkBox.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                    if (checkBox.isSelected()) {
                        item.setNewValue("yes");
                    } else {
                        item.setNewValue("no");
                    }
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, checkBox, gbc);
            break;
        case PERCENT: // percent uses a spinner with 5 changes
        case INTEGER:
            int spinnerStepSize;
            final int spinnerMultiple;
            if (item.type == ConfigItem.ConfigType.PERCENT) {
                spinnerStepSize = 5;
                spinnerMultiple = 100;
            } else {
                spinnerStepSize = 1;
                spinnerMultiple = 1;
            }
            int spinnerValue;
            try {
                spinnerValue = (int)Math.round(Double.parseDouble(configValue) * spinnerMultiple);
            } catch (NumberFormatException e) {
                spinnerValue = 0;
            }
            final JSpinner spinner = new JSpinner(new SpinnerNumberModel
                   (spinnerValue, Integer.MIN_VALUE, Integer.MAX_VALUE, spinnerStepSize));
            ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().
                addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                    Integer value = (Integer)spinner.getValue();
                    if (item.type == ConfigItem.ConfigType.PERCENT) {
                        Double adjValue = (double)value / spinnerMultiple;
                        item.setNewValue(adjValue.toString());
                    } else {
                        item.setNewValue(value.toString());
                    }
                    
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, spinner, gbc);
            addEmptyLabel(panel, gbc);
        break;
        case FONT: // fonts are a special list
            if (!Util.hasValue(configValue)) {
                configValue = ((Font)UIManager.getDefaults().get("Label.font")).getFamily();
            }
        case LIST:
            String[] allowedValues; 
            if (item.type == ConfigItem.ConfigType.FONT) {
                allowedValues = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getAvailableFontFamilyNames(); 
            } else {
                allowedValues = (String[])item.allowedValues.toArray();
            }
            final JComboBox comboBox = new JComboBox(allowedValues);
            comboBox.setSelectedItem(configValue);
            comboBox.setToolTipText(toolTip);
            comboBox.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                  item.setNewValue((String)comboBox.getSelectedItem());
                }
            }
            );
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, comboBox, gbc);
            addEmptyLabel(panel, gbc);
            break;
        case DIRECTORY:
        case FILE:
            final JLabel dirLabel = new JLabel(configValue + " "); //add whitespace for non-zero preferred size
            dirLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dirLabel.setToolTipText(toolTip);
            dirLabel.setPreferredSize(new Dimension(
                    MAX_FIELD_WIDTH,
                    dirLabel.getPreferredSize().height));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, dirLabel, gbc);
            JButton dirButton = new JButton("Choose...");
            dirButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            JFileChooser fc = new JFileChooser();
                            if (item.type == ConfigItem.ConfigType.DIRECTORY) { 
                                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                            } else {
                                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            }
                            fc.setSelectedFile(new File(dirLabel.getText()));
                            int state = fc.showOpenDialog(ConfigWindow.this);
                            if ( state == JFileChooser.APPROVE_OPTION ){
                                File file = fc.getSelectedFile();
                                dirLabel.setText(file.getPath());
                                item.setNewValue(file.getPath());
                            }
                        }
                    }
            );
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, dirButton, gbc);
            break;
        case COLOR:
            final JLabel colorLabel = new JLabel(configValue);
            Color selectedColor;
            try {
                selectedColor = Util.parseColour(configValue);
            } catch (ConfigurationException e) {
               selectedColor = Color.WHITE;
            }
            colorLabel.setOpaque(true);
            colorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            colorLabel.setBackground(selectedColor);
            if (Util.isDark(selectedColor)) {
                colorLabel.setForeground(Color.WHITE);
            } else {
                colorLabel.setForeground(Color.BLACK);
            }
            colorLabel.setToolTipText(toolTip);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, colorLabel, gbc);
            JButton colorButton = new JButton("Choose...");
            colorButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Color selectedColor=JColorChooser.showDialog(ConfigWindow.this, "", colorLabel.getBackground());
                            if (selectedColor == null) return;
                            String newValue = Integer.toHexString(selectedColor.getRGB()).substring(2); 
                            colorLabel.setText(newValue);
                            item.setNewValue(newValue);
                            colorLabel.setBackground(selectedColor);
                            if (Util.isDark(selectedColor)) {
                                colorLabel.setForeground(Color.WHITE);
                            } else {
                                colorLabel.setForeground(Color.BLACK);
                            }
                        }
                    }
            );
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, colorButton, gbc);
            break;
        case STRING:
        default: // default like String
            final JFormattedTextField textField = new JFormattedTextField();
            textField.setValue(configValue);
            textField.addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent arg0) {
                    // do nothing
                }
                public void focusLost(FocusEvent arg0) {
                  item.setNewValue(textField.getText());
                }
            }
            );
            textField.setPreferredSize(new Dimension(
                    Math.min(MAX_FIELD_WIDTH,textField.getPreferredSize().width),
                    textField.getPreferredSize().height));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addToGridBag(panel, textField, gbc);
            addEmptyLabel(panel, gbc);
            break;
        }
        // add info icon for infoText 
        if (infoText != null) {
            JLabel infoIcon = new JLabel(RailsIcon.INFO.smallIcon);
            infoIcon.addMouseListener(new MouseListener() {
                public void mousePressed(MouseEvent e) {
                    final JDialog dialog = new JDialog(ConfigWindow.this, false);
                    final JOptionPane optionPane = new JOptionPane();
                    optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                    optionPane.setMessage(infoText);
                    optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY,
                            new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent e) {
                                       dialog.dispose();
                                   }
                            }
                    );
                    dialog.setTitle(LocalText.getText("CONFIG_INFO_TITLE", itemLabel));
                    dialog.getContentPane().add(optionPane);
                    dialog.pack();
                    dialog.setVisible(true);
                }

                public void mouseClicked(MouseEvent e) {
                }
                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            gbc.fill = GridBagConstraints.NONE;
            addToGridBag(panel, infoIcon, gbc);
            addEmptyLabel(panel, gbc);
        }
    }

    private void setupButtonPanel() {
        buttonPanel.removeAll();
        
        // apply button
        if (cm.IsActiveUserProfile()) {
            JButton applyButton = new JButton(LocalText.getText("APPLY"));
            applyButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent arg0) {
                            ConfigWindow.this.saveConfig();
                        }
                    }
                    );
            buttonPanel.add(applyButton);
        }

        // save (as) button
        JButton saveButton = new JButton(LocalText.getText("SAVE_AND_APPLY"));
        saveButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.saveAsConfig();
                    }
                }
        );
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton(LocalText.getText("CANCEL"));
        cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ConfigWindow.this.closeConfig(true);
                    }
                }
        );
        buttonPanel.add(cancelButton);
        
    }
    
    private void changeProfile(String profileName) {
        cm.changeProfile(profileName);
        repaintLater();
    }
    
    private void repaintLater() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                init(false);
                ConfigWindow.this.repaint();
            }
        });
    }
    
    private boolean saveConfig() {
        if (cm.saveProfile(fromStatusWindow)) {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_CONFIRM_MESSAGE", cm.getActiveProfile()),
                LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            return true;
        } else {
            JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_ERROR_MESSAGE", cm.getActiveProfile()),
                    LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean saveAsConfig() {
        // get Names
        List<String> allProfileNames = cm.getProfiles();
        String newProfile = null;
        do {
            newProfile = JOptionPane.showInputDialog(ConfigWindow.this, LocalText.getText("CONFIG_NEW_MESSAGE"),
                LocalText.getText("CONFIG_NEW_TITLE"), JOptionPane.QUESTION_MESSAGE);
        } while (allProfileNames.contains(newProfile));
        
        boolean result = false;
        if (newProfile != null) {
            if (cm.saveNewProfile(newProfile, fromStatusWindow)) {
                JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_CONFIRM_MESSAGE", cm.getActiveProfile()),
                        LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(ConfigWindow.this, LocalText.getText("CONFIG_SAVE_ERROR_MESSAGE", cm.getActiveProfile()),
                        LocalText.getText("CONFIG_SAVE_TITLE"), JOptionPane.ERROR_MESSAGE);
                result = false;
            }
            changeProfile(newProfile);
        } 
        return result;
    }
    
    private void closeConfig(boolean cancel) {
        if (cancel) cm.changeProfile(cm.getActiveProfile());
        this.setVisible(false);
        if (fromStatusWindow) {
            StatusWindow.uncheckMenuItemBox(StatusWindow.CONFIG_CMD);
        }
    }

}
