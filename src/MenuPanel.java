import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class MenuPanel extends JPanel {

    public MenuPanel(Runnable onPlayAction, Runnable onExitAction) {
        // จัดเลย์เอาต์ให้อยู่ตรงกลางหน้าจออย่างเป็นระเบียบ
        setLayout(new GridBagLayout());
        setBackground(new Color(30, 30, 45)); // สีพื้นหลังหน้าเมนู

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(15, 15, 15, 15); // ระยะห่างระหว่างองค์ประกอบ
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. หัวข้อ / โลโก้ชื่อเกม
        JLabel titleLabel = new JLabel("MY AWESOME GAME", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridy = 0;
        add(titleLabel, gbc);

        // 2. ปุ่ม PLAY
        JButton playBtn = new JButton("PLAY");
        styleButton(playBtn, new Color(46, 204, 113));
        playBtn.addActionListener(e -> onPlayAction.run());
        gbc.gridy = 1;
        add(playBtn, gbc);

        // 3. ปุ่ม EXIT
        JButton exitBtn = new JButton("EXIT");
        styleButton(exitBtn, new Color(231, 76, 60));
        exitBtn.addActionListener(e -> onExitAction.run());
        gbc.gridy = 2;
        add(exitBtn, gbc);
    }

    // ตกแต่งสไตล์ปุ่มกด
    private void styleButton(JButton btn, Color bgColor) {
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(200, 45));
    }
}