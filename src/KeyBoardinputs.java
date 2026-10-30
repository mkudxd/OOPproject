import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyBoardinputs implements KeyListener {
    private GamePanel gamePanel;

    public KeyBoardinputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  gamePanel.left = true; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: gamePanel.right = true; break;
            case KeyEvent.VK_W: case KeyEvent.VK_UP: gamePanel.up = true; break;
            case KeyEvent.VK_SPACE: gamePanel.jump = true; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN: gamePanel.down = true; break;
            case KeyEvent.VK_E:      gamePanel.queueInteract(); break;
            case KeyEvent.VK_ENTER:  gamePanel.enterPressed(); break;
            case KeyEvent.VK_ESCAPE: gamePanel.backToMenu(); break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  gamePanel.left = false; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: gamePanel.right = false; break;
            case KeyEvent.VK_W: case KeyEvent.VK_UP: gamePanel.up = false; break;
            case KeyEvent.VK_SPACE: gamePanel.jump = false; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN: gamePanel.down = false; break;
        }
    }
}