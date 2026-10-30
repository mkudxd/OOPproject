public class Game implements Runnable {

    private Gamewindow gameWindow;
    private GamePanel gamePanel;
    private Thread gameThread;
    private final int FPS_SET = 120;
    private final int UPS_SET = 60;

    public static void main(String[] args) {
        new Game();
    }

    public Game() {
        gamePanel = new GamePanel();
        gameWindow = new Gamewindow(gamePanel);
        gamePanel.requestFocus();
        startGameloop();
    }

    private void startGameloop() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double nsPerFrame = 1000000000.0 / FPS_SET;
        double nsPerUpdate = 1000000000.0 / UPS_SET;
        long previous = System.nanoTime();
        double deltaU = 0, deltaF = 0;

        while (true) {
            long now = System.nanoTime();
            deltaU += (now - previous) / nsPerUpdate;
            deltaF += (now - previous) / nsPerFrame;
            previous = now;

            while (deltaU >= 1) {      // อัปเดตตรรกะเกม 60 ครั้ง/วินาที
                gamePanel.update();
                deltaU--;
            }
            if (deltaF >= 1) {         // วาดภาพ 120 ครั้ง/วินาที
                gamePanel.repaint();
                deltaF = 0;
            }
            try { Thread.sleep(1); } catch (InterruptedException e) { }
        }
    }
}