import javax.swing.JPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {

    public static final int W = 960, H = 540, GROUND = 440;
    private static final int PW = 28, PH = 48;
    private static final float SPEED = 4f, GRAV = 0.55f, JUMP = 12f;
    private static final float CLIMB = 2.5f;   // ความเร็วปีนบันได
    private static final int LAMP_X = W - 60, LAMP_Y = 30;  // โคมไฟส่อง กลับมาอยู่มุมขวาบนที่เดิม
    private static final int TDX = -380;                    // เลื่อนหอคอยมาทางซ้าย (ใกล้จุดเริ่มต้น)
    private static final int STEP = 16;   // ความสูงขั้นบันไดที่เดินขึ้นได้เองโดยไม่ต้องกระโดด
    private static final String KEY = "picklock", REMOTE = "remote";   // KEY = ไอเทมด่าน 1 (picklock)
    private static final int ITEM = 44;                              // ขนาดไอคอนไอเทม

    // อินพุต
    public boolean left, right, jump, up, down;
    private boolean interactQueued;

    // ผู้เล่น
    private float px, py, vy;
    private boolean onGround, facingRight = true;

    // ด่าน
    private int level = 1, tick, charges, lightTimer;
    private final List<Rectangle> solids = new ArrayList<>();
    private final List<String> inventory = new ArrayList<>();
    private Rectangle itemRect, door, exit, bed;
    private static final int BED_X = 190, BED_H = 52, BED_SURFACE = 35;   // ตำแหน่ง/ความสูงเตียง (ภาพ 97x52)
    private String itemName = "";
    private boolean doorOpen;
    private Polygon beam = new Polygon();
    private final List<Rectangle> ladders = new ArrayList<>();   // บันได (ด่าน 2)
    private boolean climbing;
    private Rectangle curLadder;
    private int climbLock;                 // กันกระโดดทันทีหลังขึ้น/ลงบันไดเสร็จ
    private float jumpPower = JUMP;
    private final List<Rectangle> movers = new ArrayList<>();  // แพลตฟอร์มเลื่อน (ด่าน 3)
    private float[] mBase, mAmp, mSpd, mPh;

    // เมนู
    private final Rectangle playBtn = new Rectangle(W / 2 - 110, 270, 220, 56);
    private final Rectangle exitBtn = new Rectangle(W / 2 - 110, 350, 220, 56);
    private int hover = 0;

    // รูปตัวละคร (โหลดครั้งเดียวตอนเริ่มเกม)
    private final BufferedImage spriteR = Sprites.load("/res/player_R.png");
    private final BufferedImage spriteL = Sprites.load("/res/player_L.png");
    private final BufferedImage bedImg = Sprites.load("/res/bed.png");
    private final BufferedImage toiletImg = Sprites.load("/res/toilet.png");
    private final BufferedImage iconPick = Sprites.load("/res/item_picklock.png");
    private final BufferedImage iconRemote = Sprites.load("/res/item_remote.png");

    private final Font thai = new Font("Tahoma", Font.PLAIN, 18);
    private final Font thaiBold = new Font("Tahoma", Font.BOLD, 22);

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        Mouseinputs m = new Mouseinputs(this);
        addKeyListener(new KeyBoardinputs(this));
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private BufferedImage icon(String name) {
        return name.equals(KEY) ? iconPick : name.equals(REMOTE) ? iconRemote : null;
    }

    // ---------- การควบคุมสถานะ ----------
    public void queueInteract() { interactQueued = true; }

    public void enterPressed() {
        if (Gamestate.state == Gamestate.CAUGHT) {
            loadLevel(level);
            Gamestate.state = Gamestate.PLAYING;
        } else if (Gamestate.state == Gamestate.WIN) {
            Gamestate.state = Gamestate.MENU;
        } else if (Gamestate.state == Gamestate.MENU) {
            startGame();
        }
    }

    public void backToMenu() {
        if (Gamestate.state == Gamestate.PLAYING) Gamestate.state = Gamestate.MENU;
    }

    private void startGame() {
        loadLevel(1);
        Gamestate.state = Gamestate.PLAYING;
    }

    public void menuHover(int x, int y) {
        hover = playBtn.contains(x, y) ? 1 : exitBtn.contains(x, y) ? 2 : 0;
    }

    public void menuClick(int x, int y) {
        if (Gamestate.state != Gamestate.MENU) return;
        if (playBtn.contains(x, y)) startGame();
        else if (exitBtn.contains(x, y)) System.exit(0);
    }

    // ---------- สร้างด่าน ----------
    private void loadLevel(int n) {
        level = n;
        solids.clear();
        inventory.clear();
        itemRect = null; door = null; doorOpen = false;
        bed = null; movers.clear(); ladders.clear(); climbing = false; climbLock = 0; jumpPower = JUMP;
        charges = 0; lightTimer = 0; tick = 0; vy = 0;
        px = 40; py = GROUND - PH;
        left = right = jump = up = down = false;

        switch (n) {
            case 1: // ห้องขัง: เก็บกุญแจ -> ไขกรงขัง
                solids.add(new Rectangle(0, GROUND, W, H - GROUND));
                bed = bedImg != null
                        ? new Rectangle(BED_X + 7, GROUND - BED_SURFACE, 88, BED_SURFACE)   // ผิวผ้าห่มเป็นพื้นให้ยืนได้
                        : new Rectangle(180, GROUND - 24, 140, 24);
                solids.add(bed);                                      // เตียง
                door = new Rectangle(690, GROUND - 160, 36, 160); // ประตูกรงขัง
                solids.add(door);
                itemRect = new Rectangle(122, GROUND - ITEM, ITEM, ITEM);
                px = 70;                                   // เริ่มข้างโถส้วม ไม่ซ้อนกัน
                itemName = KEY;
                exit = new Rectangle(W - 30, GROUND - 150, 30, 150);
                break;
            case 2: // หอคอยยาม: ปีนบันไดสลับซ้ายขวาขึ้นไปเก็บรีโมทข้างบน
                jumpPower = 9.5f;   // กระโดดเตี้ยลง จึงข้ามชั้นไม่ได้ ต้องปีนบันไดเท่านั้น
                solids.add(new Rectangle(0, GROUND, W, H - GROUND));
                // พื้นแต่ละชั้นเว้นช่องกว้าง 20 ไว้ให้บันไดทะลุผ่าน
                solids.add(new Rectangle(540 + TDX, 330, 164, 8));  solids.add(new Rectangle(724 + TDX, 330, 16, 8));   // ชั้น A
                solids.add(new Rectangle(560 + TDX, 220, 10, 8));   solids.add(new Rectangle(590 + TDX, 220, 130, 8));  // ชั้น B
                solids.add(new Rectangle(550 + TDX, 110, 126, 8));  solids.add(new Rectangle(696 + TDX, 110, 34, 8));   // ชั้นบนสุด
                ladders.add(new Rectangle(700 + TDX, 330, 28, 110)); // พื้น -> A (ขวา)
                ladders.add(new Rectangle(566 + TDX, 220, 28, 110)); // A -> B (ซ้าย)
                ladders.add(new Rectangle(672 + TDX, 110, 28, 110)); // B -> บนสุด (ขวา)
                itemRect = new Rectangle(560 + TDX, 110 - ITEM, ITEM, ITEM); // รีโมทอยู่ด้านบนสุด
                itemName = REMOTE;
                exit = new Rectangle(W - 30, GROUND - 150, 30, 150);
                break;
            case 3: // ข้ามหลุม: กระโดดบนแพลตฟอร์ม
                py = 400 - PH;
                solids.add(new Rectangle(0, 400, 200, H - 400));
                float[] xs = {260, 420, 590, 760};
                mBase = new float[]{360, 310, 350, 300};
                mAmp  = new float[]{40, 40, 40, 40};
                mSpd  = new float[]{0.025f, 0.030f, 0.022f, 0.028f};
                mPh   = new float[]{0f, 1.5f, 3f, 4.5f};
                for (int i = 0; i < 4; i++) {
                    int y0 = Math.round(mBase[i] + mAmp[i] * (float) Math.sin(mPh[i]));
                    Rectangle m = new Rectangle((int) xs[i], y0, 90, 16);
                    movers.add(m);
                    solids.add(m);
                }
                solids.add(new Rectangle(900, 340, 60, H - 340));
                exit = new Rectangle(915, 280, 40, 60);
                break;
        }
    }

    // ---------- อัปเดตตรรกะ ----------
    private void tryGrabLadder() {
        if (climbing || (!up && !down)) return;
        Rectangle p = pr();
        int cx = p.x + PW / 2;
        for (Rectangle l : ladders) {
            if (cx < l.x || cx > l.x + l.width) continue;
            if (up && p.intersects(l)) { startClimb(l); return; }          // ปีนขึ้นจากข้างล่าง
            if (down && onGround && Math.abs(p.y + PH - l.y) <= 2) {        // ปีนลงจากข้างบน
                startClimb(l);
                py += 4;
                return;
            }
        }
    }

    private void startClimb(Rectangle l) {
        climbing = true;
        curLadder = l;
        px = l.x + (l.width - PW) / 2f;
        vy = 0;
    }

    private void endClimb() {
        climbing = false;
        onGround = true;
        vy = 0;
        climbLock = 12;
    }

    private void climb() {
        vy = 0;
        if (up) py -= CLIMB; else if (down) py += CLIMB;
        if (jump && (left || right)) {                 // กระโดดออกจากบันได
            climbing = false;
            vy = -6f;
            climbLock = 12;
            return;
        }
        if (py + PH <= curLadder.y) {                  // ถึงยอดบันได
            py = curLadder.y - PH;
            endClimb();
        } else if (py + PH >= curLadder.y + curLadder.height) { // ถึงล่างสุด
            py = curLadder.y + curLadder.height - PH;
            endClimb();
        }
    }

    private void physics() {
        float vx = 0;
        if (left)  { vx -= SPEED; facingRight = false; }
        if (right) { vx += SPEED; facingRight = true; }
        boolean wantJump = jump || (up && climbLock == 0);
        if (wantJump && onGround) { vy = -jumpPower; onGround = false; }
        vy = Math.min(vy + GRAV, 15f);

        // แกน X
        px += vx;
        Rectangle r = pr();
        for (Rectangle s : solids) {
            if (isMover(s) || !r.intersects(s)) continue;   // แพลตฟอร์มเลื่อนไม่ชนด้านข้าง
            int rise = r.y + PH - s.y;
            Rectangle upR = new Rectangle(r.x, s.y - PH, PW, PH);
            if (onGround && vx != 0 && rise > 0 && rise <= STEP && free(upR, s)) {
                py = s.y - PH;                              // เดินขึ้นขั้นเตี้ยๆ ได้เอง
            } else if (vx > 0) {
                px = s.x - PW;
            } else if (vx < 0) {
                px = s.x + s.width;
            }
            r = pr();
        }
        px = Math.max(0, Math.min(W - PW, px));

        // แกน Y
        float prevBottom = py + PH;
        py += vy;
        onGround = false;
        r = pr();
        for (Rectangle s : solids) {
            if (!r.intersects(s)) continue;
            if (isMover(s)) {
                // แพลตฟอร์มเลื่อน: กระโดดทะลุจากข้างล่างได้ ลงจอดได้จากข้างบนเท่านั้น
                if (vy >= 0 && prevBottom <= s.y + 8) {
                    py = s.y - PH; onGround = true; vy = 0; r = pr();
                }
                continue;
            }
            if (vy > 0) { py = s.y - PH; onGround = true; }
            else if (vy < 0) { py = s.y + s.height; }
            vy = 0;
            r = pr();
        }

        // ตกหลุม
        if (py > H + 60) Gamestate.state = Gamestate.CAUGHT;
    }

    private boolean isMover(Rectangle s) {
        for (Rectangle m : movers) if (m == s) return true;
        return false;
    }

    private boolean free(Rectangle t, Rectangle ignore) {
        for (Rectangle o : solids)
            if (o != ignore && !isMover(o) && t.intersects(o)) return false;
        return true;
    }

    private Rectangle pr() { return new Rectangle(Math.round(px), (int) Math.ceil(py), PW, PH); }

    public synchronized void update() {
        if (Gamestate.state != Gamestate.PLAYING) return;
        tick++;
        if (climbLock > 0) climbLock--;

        // แพลตฟอร์มเลื่อนขึ้นลง (ถ้าผู้เล่นยืนอยู่ ให้ติดไปด้วย)
        for (int i = 0; i < movers.size(); i++) {
            Rectangle m = movers.get(i);
            Rectangle p0 = pr();
            boolean rider = onGround && Math.abs(p0.y + PH - m.y) <= 1
                    && p0.x + PW > m.x && p0.x < m.x + m.width;
            m.y = Math.round(mBase[i] + mAmp[i] * (float) Math.sin(tick * mSpd[i] + mPh[i]));
            if (rider) { py = m.y - PH; vy = 0; }
        }

        tryGrabLadder();
        if (climbing) climb(); else physics();
        if (Gamestate.state != Gamestate.PLAYING) return;

        // โต้ตอบ (กด E)
        if (interactQueued) {
            interactQueued = false;
            Rectangle p = pr();
            Rectangle near = new Rectangle(p.x - 30, p.y - 10, PW + 60, PH + 20);
            if (itemRect != null && near.intersects(itemRect)) {
                inventory.add(itemName);
                if (itemName.equals(REMOTE)) charges = 2;
                itemRect = null;
            } else if (level == 1 && door != null && !doorOpen
                    && near.intersects(door) && inventory.remove(KEY)) {
                doorOpen = true;
                solids.remove(door);
            } else if (level == 2 && charges > 0 && lightTimer <= 0) {
                charges--;
                lightTimer = 360; // ปิดไฟ 6 วินาที
                if (charges == 0) inventory.remove(REMOTE);
            }
        }

        // ไฟส่อง (ด่าน 2)
        if (level == 2) {
            double tx = 500 + 350 * Math.sin(tick * 0.02);
            beam = new Polygon(
                new int[]{LAMP_X, (int) tx - 70, (int) tx + 70},
                new int[]{LAMP_Y, GROUND, GROUND}, 3);
            if (lightTimer > 0) lightTimer--;
            else {
                Rectangle p = pr();
                if (beam.intersects(p.x + 4, p.y, PW - 8, PH)) {
                    Gamestate.state = Gamestate.CAUGHT;
                    return;
                }
            }
        }

        // ถึงทางออก
        if (pr().intersects(exit)) {
            if (level < 3) loadLevel(level + 1);
            else Gamestate.state = Gamestate.WIN;
        }
    }

    // ---------- วาดภาพ ----------
    @Override
    public synchronized void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (Gamestate.state) {
            case MENU: drawMenu(g); break;
            case PLAYING: drawLevel(g); break;
            case CAUGHT:
                drawLevel(g);
                overlay(g, new Color(150, 0, 0, 170), "ถูกจับได้!", "กด ENTER เพื่อเริ่มด่านใหม่");
                break;
            case WIN:
                drawLevel(g);
                overlay(g, new Color(0, 110, 40, 190), "หนีออกจากคุกสำเร็จ!", "กด ENTER เพื่อกลับเมนู");
                break;
        }
    }

    private void drawCentered(Graphics2D g, String s, int cx, int y) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private void overlay(Graphics2D g, Color c, String title, String sub) {
        g.setColor(c);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 52));
        drawCentered(g, title, W / 2, 240);
        g.setFont(thaiBold);
        drawCentered(g, sub, W / 2, 290);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(30, 30, 45));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(110, 110, 125));       // ซี่กรงตามสตอรีบอร์ด
        for (int x = 20; x < W; x += 80) g.fillRect(x, 0, 28, H);

        g.setColor(new Color(30, 30, 45, 220));
        g.fillRoundRect(W / 2 - 200, 110, 400, 320, 20, 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 56));
        drawCentered(g, "Limit Break", W / 2, 200);

        drawButton(g, playBtn, "PLAY", new Color(46, 204, 113), hover == 1);
        drawButton(g, exitBtn, "EXIT", new Color(231, 76, 60), hover == 2);
    }

    private void drawButton(Graphics2D g, Rectangle b, String text, Color c, boolean hot) {
        g.setColor(hot ? c.brighter() : c);
        g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        drawCentered(g, text, b.x + b.width / 2, b.y + 36);
    }

    private void drawLevel(Graphics2D g) {
        // พื้นหลัง
        Color bg = level == 1 ? new Color(60, 60, 72) : level == 2 ? new Color(28, 34, 52) : new Color(18, 18, 28);
        g.setColor(bg);
        g.fillRect(0, 0, W, H);

        if (level == 1) {
            g.setColor(new Color(40, 40, 50));
            for (int x = 10; x < W; x += 60) g.fillRect(x, 0, 8, GROUND);
            if (toiletImg != null) {
                g.drawImage(toiletImg, 8, GROUND - toiletImg.getHeight(), null);   // โถส้วม
            } else {
                g.setColor(new Color(200, 200, 210));
                g.fillRect(30, GROUND - 34, 26, 34);
            }
            if (bedImg != null) {
                g.drawImage(bedImg, BED_X, GROUND - BED_H, null);                    // เตียง
            } else {
                g.setColor(new Color(140, 90, 90));
                g.fillRect(185, GROUND - 40, 130, 16);                   // ผ้าห่ม
            }
        } else if (level == 2) {
            g.setColor(new Color(60, 70, 95));
            g.fillRect(16, 16, 150, 44);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("PRISON", 32, 48);
            g.setColor(new Color(255, 240, 150));
            g.fillOval(W - 76, 14, 32, 32);                               // โคมไฟมุมขวาบน
            drawTower(g);
            boolean on = lightTimer <= 0;
            g.setColor(on ? new Color(255, 245, 150, 95) : new Color(255, 245, 150, 18));
            g.fillPolygon(beam);
        } else {
            g.setColor(new Color(120, 20, 20));
            g.fillRect(0, H - 40, W, 40);                                 // ใต้หลุม
        }

        // พื้น/แพลตฟอร์ม
        for (Rectangle s : solids) {
            if (s == door) continue;                                      // กรงวาดแยกด้านล่าง
            if (s == bed && bedImg != null) continue;                     // เตียงวาดเป็นรูปแล้ว
            boolean mv = false;
            for (Rectangle m : movers) if (m == s) mv = true;
            boolean deck = level == 2 && s.y < GROUND;                    // พื้นแต่ละชั้นของหอคอย
            g.setColor(mv ? new Color(70, 120, 190) : deck ? new Color(160, 165, 182) : new Color(95, 95, 108));
            g.fillRect(s.x, s.y, s.width, s.height);
            if (mv) {                                                     // ลูกศรบอกว่าเลื่อนได้
                g.setColor(new Color(200, 230, 255));
                g.fillRect(s.x, s.y, s.width, 4);
            }
        }

        // กรงขัง (ด่าน 1)
        if (door != null) {
            if (!doorOpen) {
                g.setColor(new Color(25, 25, 32, 200));
                g.fillRect(door.x, door.y, door.width, door.height);
                g.setColor(new Color(170, 175, 190));
                for (int bx = door.x; bx <= door.x + door.width - 4; bx += 8)
                    g.fillRect(bx, door.y, 4, door.height);               // ซี่กรง
                g.fillRect(door.x - 4, door.y, door.width + 8, 8);        // คานบน
                g.fillRect(door.x - 4, door.y + door.height / 2, door.width + 8, 5);
                g.setColor(new Color(240, 200, 60));
                g.fillRect(door.x + door.width / 2 - 8, door.y + door.height / 2 - 12, 16, 18); // กุญแจล็อก
            } else {
                g.setColor(new Color(170, 175, 190));
                g.fillRect(door.x - 4, door.y, 4, door.height);           // บานกรงเปิดอยู่
            }
        }

        // ไอเทม
        if (itemRect != null) {
            BufferedImage ic = icon(itemName);
            int bob = -(int) Math.round(Math.abs(Math.sin(tick * 0.06)) * 6);   // ลอยขึ้นลงเบาๆ ให้สังเกตเห็น
            if (ic != null) {
                g.drawImage(ic, itemRect.x, itemRect.y + bob, null);
            } else {
                g.setColor(itemName.equals(KEY) ? new Color(250, 210, 40) : new Color(80, 200, 255));
                g.fillRect(itemRect.x + 12, itemRect.y + 12, 20, 20);
            }
        }

        // ทางออก
        g.setColor(new Color(46, 204, 113, 160));
        g.fillRect(exit.x, exit.y, exit.width, exit.height);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("EXIT", W - 52, exit.y - 6);

        drawPlayer(g);
        drawHud(g);
    }

    // โครงหอคอยยามแบบในรูปอ้างอิง: โครงเหล็กไขว้ X, บันไดสลับซ้าย-ขวา, ห้องยามพร้อมหลังคา, ไฟส่อง
    private double legL(int y) { return 520 + (GROUND - y) * 60.0 / 330; }
    private double legR(int y) { return 760 - (GROUND - y) * 60.0 / 330; }

    private void drawTower(Graphics2D g) {
        Stroke old = g.getStroke();
        g.translate(TDX, 0);
        Color steel = new Color(110, 120, 142);
        g.setColor(steel);
        g.setStroke(new BasicStroke(6f));
        g.drawLine((int) legL(GROUND), GROUND, (int) legL(118), 118);           // ขาซ้าย
        g.drawLine((int) legR(GROUND), GROUND, (int) legR(118), 118);           // ขาขวา
        g.fillRect(508, GROUND - 4, 28, 6);                                      // ฐานเท้า
        g.fillRect(744, GROUND - 4, 28, 6);
        g.setStroke(new BasicStroke(3f));
        int[] ys = {GROUND, 330, 220, 110};
        for (int i = 0; i < 3; i++) {                                            // โครงไขว้ X
            int lo = ys[i], hi = ys[i + 1];
            g.drawLine((int) legL(lo), lo, (int) legR(hi), hi);
            g.drawLine((int) legR(lo), lo, (int) legL(hi), hi);
        }

        // ห้องยามด้านบน
        g.setColor(new Color(24, 27, 38));
        g.fillRect(590, 58, 100, 52);
        g.setColor(new Color(225, 235, 190));
        g.fillRect(604, 68, 72, 22);                                             // หน้าต่าง
        g.setColor(new Color(70, 75, 98));
        g.fillPolygon(new int[]{574, 640, 706}, new int[]{58, 22, 58}, 3);       // หลังคา
        g.setColor(steel);
        g.drawLine(640, 22, 640, 2);                                             // เสาอากาศ

        // ราวระเบียง + โคมไฟสองข้าง
        g.setStroke(new BasicStroke(2f));
        g.drawLine(550, 88, 590, 88); g.drawLine(550, 88, 550, 110); g.drawLine(590, 88, 590, 110);
        g.drawLine(690, 88, 730, 88); g.drawLine(690, 88, 690, 110); g.drawLine(730, 88, 730, 110);
        g.setColor(new Color(24, 27, 38));
        g.fillOval(538, 78, 12, 18);                                             // โคมประดับ (ไม่ส่องแสง)
        g.fillOval(730, 78, 12, 18);
        g.translate(-TDX, 0);

        // บันได (พิกัดจริงของเกม จึงวาดนอก translate)
        g.setColor(new Color(175, 180, 196));
        g.setStroke(new BasicStroke(2f));
        for (Rectangle l : ladders) {
            g.drawLine(l.x + 4, l.y, l.x + 4, l.y + l.height);
            g.drawLine(l.x + l.width - 4, l.y, l.x + l.width - 4, l.y + l.height);
            for (int ry = l.y + 8; ry < l.y + l.height; ry += 10)
                g.drawLine(l.x + 4, ry, l.x + l.width - 4, ry);
        }
        g.setStroke(old);
    }

    private void drawPlayer(Graphics2D g) {
        int x = Math.round(px), y = (int) Math.ceil(py);
        BufferedImage img = facingRight ? spriteR : spriteL;
        if (img != null) {
            // เท้าชิดขอบล่าง hitbox และอยู่กึ่งกลางตัว; เดินบนพื้นให้ตัวเด้งเบาๆ
            boolean walking = !climbing && onGround && (left || right);
            int bob = walking ? (tick / 6 % 2) : 0;
            int dx = x + PW / 2 - img.getWidth() / 2;
            int dy = y + PH - img.getHeight() - bob;
            g.drawImage(img, dx, dy, null);
            return;
        }
        g.setColor(new Color(255, 140, 0));                               // สำรอง: สี่เหลี่ยมเดิม
        g.fillRect(x, y + 14, PW, PH - 14);
        g.setColor(new Color(245, 210, 170));
        g.fillOval(x + 2, y, 24, 24);
        g.setColor(Color.BLACK);
        g.fillOval(x + (facingRight ? 16 : 7), y + 8, 4, 4);
    }

    private void drawHud(Graphics2D g) {
        String[] names = {"", "ห้องขัง", "หอคอยไฟส่อง", "ข้ามหลุม"};
        g.setColor(Color.WHITE);
        g.setFont(thaiBold);
        g.drawString("ด่าน " + level + "/3 : " + names[level], 520, 36);

        g.setFont(thai);
        String hint;
        if (level == 1) hint = inventory.contains(KEY) ? "เดินไปที่กรงขังแล้วกด E เพื่อใช้ picklock แงะล็อก" : "เก็บ picklock (กด E ใกล้ๆ) ไว้ใช้เปิดกรงขัง";
        else if (level == 2) hint = inventory.contains(REMOTE) ? "กด E ปิดไฟส่อง (เหลือ " + charges + " ครั้ง)" : "ปีนบันไดหอคอย (W ขึ้น / S ลง) ไปเก็บ remote ข้างบน (E) แล้วหลบแสงไฟ";
        else hint = "แพลตฟอร์มเลื่อนขึ้นลง กระโดดขึ้นจากด้านล่างทะลุได้ จับจังหวะให้ดี";
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(hint, 20, 458);

        // ช่องเก็บไอเทม 4 ช่อง (แสดงไอคอน + ชื่อ)
        g.setFont(new Font("Tahoma", Font.PLAIN, 14));
        g.drawString("Item", 20, 482);
        for (int i = 0; i < 4; i++) {
            int x = 20 + i * 120, y = 486;
            g.setColor(new Color(20, 20, 28));
            g.fillRoundRect(x, y, 110, 50, 8, 8);
            g.setColor(Color.WHITE);
            g.drawRoundRect(x, y, 110, 50, 8, 8);
            if (i < inventory.size()) {
                String n = inventory.get(i);
                BufferedImage ic = icon(n);
                if (ic != null) g.drawImage(ic, x + 3, y + 3, null);
                if (n.equals(REMOTE)) n += " x" + charges;
                g.setFont(new Font("Tahoma", Font.PLAIN, 13));
                g.drawString(n, x + 52, y + 30);
                g.setFont(new Font("Tahoma", Font.PLAIN, 14));
            }
        }
        g.drawString("A/D เดิน  W/Space กระโดด  W/S ปีนบันได  E โต้ตอบ  Esc เมนู", 520, 520);
    }
}