import javax.swing.JFrame;

public class Gamewindow {
    private JFrame jframe;

    public Gamewindow(GamePanel gamePanel) {
        jframe = new JFrame("Limit Break");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.add(gamePanel);
        jframe.setResizable(false);
        jframe.pack();
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);
    }
}