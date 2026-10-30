import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Sprites {

    // โหลดรูปจากโฟลเดอร์ res ไม่ว่าจะรันจาก VS Code, Code Runner หรือ javac
    // path เช่น "/res/player_R.png"  ถ้าหาไม่เจอจะคืน null (เกมวาดสี่เหลี่ยมแทน ไม่พัง)
    public static BufferedImage load(String path) {
        String name = path.startsWith("/") ? path.substring(1) : path;   // "res/player_R.png"

        // 1) หาจาก classpath (กรณี res อยู่ในโฟลเดอร์ที่คอมไพล์ หรือแพ็กใน jar)
        try (InputStream in = Sprites.class.getResourceAsStream("/" + name)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception e) { /* ลองวิธีถัดไป */ }

        // 2) หาจากไฟล์จริง โดยไล่ขึ้นไปจากโฟลเดอร์ที่ทำงานอยู่ (รองรับ res อยู่นอก src)
        String[] prefixes = {"", "../", "../../", "GameM/"};
        for (String pre : prefixes) {
            BufferedImage img = readFile(new File(pre + name));
            if (img != null) return img;
        }

        // 3) หาจากตำแหน่งของไฟล์ .class (เช่น bin/ หรือ src/) แล้วไล่ขึ้นไปหา res
        try {
            File base = new File(Sprites.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (File dir = base; dir != null; dir = dir.getParentFile()) {
                BufferedImage img = readFile(new File(dir, name));
                if (img != null) return img;
                if (dir.getParentFile() == null || base.toPath().getNameCount() - dir.toPath().getNameCount() >= 3) break;
            }
        } catch (Exception e) { /* ข้าม */ }

        System.err.println("[Sprites] ไม่พบรูป " + name + "  (โฟลเดอร์ที่รันอยู่: "
                + new File(".").getAbsolutePath() + ")");
        return null;
    }

    private static BufferedImage readFile(File f) {
        if (!f.isFile()) return null;
        try { return ImageIO.read(f); } catch (IOException e) { return null; }
    }
}