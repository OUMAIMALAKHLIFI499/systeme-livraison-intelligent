





package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;


public class CoordinatorAgent extends Agent {

    private static final Color NAVY = new Color(0x2F, 0x3C, 0x7E);
    private static final Color CORAL = new Color(0xF9, 0x61, 0x67);
    private static final Color GOLD = new Color(0xF9, 0xE7, 0x95);
    private static final Color TEAL = new Color(0x50, 0x80, 0x8E);
    private static final Color LIGHT_PINK = new Color(0xFB, 0xEA, 0xE8);
    private static final Color DARK_TEXT = new Color(0x1B, 0x1F, 0x3B);
    private static final Color GREEN = new Color(0x1D, 0x9E, 0x75);
    private static final Color ORANGE = new Color(0xE0, 0x8E, 0x1D);
    private static final Color RED = new Color(0xA3, 0x2D, 0x2D);

    private JComboBox<String> productCombo, quartierCombo;
    private JTextField phoneField;
    private JLabel productChip, deliveryChip, totalChip, errorLabel;
    private DefaultTableModel tableModel;
    private JTextArea notifications;
    private final AtomicInteger counter = new AtomicInteger(0);

    protected void setup() {
        buildGUI();
        MessageTemplate t = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = myAgent.receive(t);
                if (msg == null) { block(); return; }
                handleMessage(msg.getContent());
            }
        });
    }

    // Interface graphique

    private void buildGUI() {
        JFrame frame = new JFrame("Livraison - Agadir");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 620);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.WHITE);

        frame.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(LIGHT_PINK);
        tabs.setForeground(NAVY);
        tabs.addTab("Nouvelle commande", buildOrderPanel());
        tabs.addTab("Suivi des commandes", buildTrackingPanel());
        frame.add(tabs, BorderLayout.CENTER);

        frame.add(buildNotificationPanel(), BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("Livraison Intelligente \u2014 Agadir");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Systeme multi-agents (JADE)");
        subtitle.setFont(new Font("SansSerif", Font.ITALIC, 12));
        subtitle.setForeground(GOLD);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(3));
        box.add(subtitle);

        header.add(box, BorderLayout.WEST);
        return header;
    }

    private JPanel buildOrderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LIGHT_PINK);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        productCombo = new JComboBox<>(Catalog.PRODUCTS.keySet().toArray(new String[0]));
        quartierCombo = new JComboBox<>(Catalog.QUARTIERS.keySet().toArray(new String[0]));
        phoneField = new JTextField();
        styleCombo(productCombo);
        styleCombo(quartierCombo);
        styleField(phoneField);

        JButton validate = new JButton("Valider la commande");
        styleButton(validate);

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(RED);
        errorLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        productChip = makeChip(TEAL, Color.WHITE);
        deliveryChip = makeChip(GOLD, DARK_TEXT);
        totalChip = makeChip(CORAL, Color.WHITE);
        JPanel priceRow = new JPanel(new GridLayout(1, 3, 10, 0));
        priceRow.setOpaque(false);
        priceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        priceRow.add(productChip);
        priceRow.add(deliveryChip);
        priceRow.add(totalChip);

        panel.add(row("Produit :", productCombo));
        panel.add(Box.createVerticalStrut(14));
        panel.add(row("Quartier :", quartierCombo));
        panel.add(Box.createVerticalStrut(14));
        panel.add(row("Telephone :", phoneField));
        panel.add(Box.createVerticalStrut(22));
        panel.add(priceRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(errorLabel);
        panel.add(Box.createVerticalStrut(16));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(validate);
        panel.add(btnRow);

        productCombo.addActionListener(e -> updatePrice());
        quartierCombo.addActionListener(e -> updatePrice());
        updatePrice();
        validate.addActionListener(e -> onValidate());
        return panel;
    }

    private JPanel buildTrackingPanel() {
        tableModel = new DefaultTableModel(
            new String[]{"N", "Produit", "Quartier", "Telephone", "Total (DH)", "Statut"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setGridColor(new Color(0xE3, 0xE3, 0xE3));
        table.setSelectionBackground(GOLD);
        table.setSelectionForeground(DARK_TEXT);
        table.setShowGrid(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setBackground(NAVY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.setDefaultRenderer(Object.class, new StatusCellRenderer());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildNotificationPanel() {
        JPanel notifPanel = new JPanel(new BorderLayout());
        notifPanel.setBackground(Color.WHITE);
        notifPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, LIGHT_PINK),
            BorderFactory.createEmptyBorder(10, 18, 14, 18)));

        JLabel notifTitle = new JLabel("Notifications");
        notifTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        notifTitle.setForeground(NAVY);
        notifTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        notifications = new JTextArea(6, 0);
        notifications.setEditable(false);
        notifications.setFont(new Font("Consolas", Font.PLAIN, 12));
        notifications.setBackground(new Color(0xFA, 0xFA, 0xFA));
        notifications.setForeground(DARK_TEXT);
        notifications.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        notifPanel.add(notifTitle, BorderLayout.NORTH);
        notifPanel.add(new JScrollPane(notifications), BorderLayout.CENTER);
        return notifPanel;
    }

    // Petits aides visuelles

    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NAVY, 1),
            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
    }

    private void styleCombo(JComboBox<String> c) {
        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.setBackground(Color.WHITE);
    }

    private void styleButton(JButton b) {
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setBackground(CORAL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JLabel makeChip(Color bg, Color fg) {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        return l;
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(NAVY);
        l.setPreferredSize(new Dimension(100, 22));
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /** Colore la colonne Statut selon sa valeur, et alterne le fond des lignes. */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF6, 0xF6, 0xF6));
            }

            if (column == 5 && value != null) {
                String status = value.toString();
                setFont(getFont().deriveFont(Font.BOLD));
                if (status.contains("Livree")) c.setForeground(GREEN);
                else if (status.contains("route")) c.setForeground(ORANGE);
                else if (status.contains("Aucun")) c.setForeground(RED);
                else c.setForeground(NAVY);
            } else {
                setFont(getFont().deriveFont(Font.PLAIN));
                c.setForeground(DARK_TEXT);
            }
            return c;
        }
    }

    // Logique (inchangee)

    private void updatePrice() {
        double p = Catalog.productPrice((String) productCombo.getSelectedItem());
        double d = Catalog.deliveryPrice((String) quartierCombo.getSelectedItem());
        productChip.setText("Produit : " + (int) p + " DH");
        deliveryChip.setText("Livraison : " + (int) d + " DH");
        totalChip.setText("Total : " + (int) (p + d) + " DH");
    }

    private void onValidate() {
        String phone = phoneField.getText().trim();
        if (phone.length() < 8) { errorLabel.setText("Numero de telephone invalide."); return; }
        errorLabel.setText(" ");

        String product = (String) productCombo.getSelectedItem();
        String quartier = (String) quartierCombo.getSelectedItem();
        String orderId = String.valueOf(counter.incrementAndGet());
        log("Commande #" + orderId + " envoyee...");

        addBehaviour(new OneShotBehaviour(this) {
            public void action() {
                AID manager = findManager();
                if (manager == null) { log("Aucun gestionnaire trouve."); return; }
                ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                req.addReceiver(manager);
                req.setContent(orderId + "|" + product + "|" + quartier + "|" + phone);
                myAgent.send(req);
            }
        });
    }

    private AID findManager() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("order-manager");
        template.addServices(sd);
        try {
            DFAgentDescription[] r = DFService.search(this, template);
            if (r.length > 0) return r[0].getName();
        } catch (FIPAException e) { e.printStackTrace(); }
        return null;
    }

    private void handleMessage(String content) {
        String[] p = content.split("\\|");
        String orderId = p[0], type = p[1];

        if (type.equals("INFO")) {
            SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{orderId, p[2], p[3], p[4], p[5], "En attente"}));
            log("Commande #" + orderId + " confirmee - " + p[5] + " DH");
        } else {
            String status = p[2];
            String courier = p.length > 3 ? p[3] : "";
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 0).equals(orderId)) {
                        tableModel.setValueAt(status, i, 5);
                        break;
                    }
                }
            });
            log("Commande #" + orderId + " : " + status + (courier.isEmpty() ? "" : " (" + courier + ")"));
        }
    }

    private void log(String text) {
        SwingUtilities.invokeLater(() -> {
            notifications.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + text + "\n");
            notifications.setCaretPosition(notifications.getDocument().getLength());
        });
    }
}