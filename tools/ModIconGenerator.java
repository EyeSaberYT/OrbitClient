import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the mod's icon (shown in Mod Menu and the launcher): an eclipse with a glowing corona and a
 * "diamond ring" flare, circled by a tilted orbit, on a dark rounded tile.
 * Usage: java -Djava.awt.headless=true tools/ModIconGenerator.java <output.png> [size, default 512]
 */
public class ModIconGenerator {
	static final int S = 512;

	public static void main(String[] args) throws Exception {
		int size = args.length > 1 ? Integer.parseInt(args[1]) : S;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.scale(size / (double) S, size / (double) S); // drawn in 512 units at any size
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		// Tile: deep night-blue gradient with a soft vignette.
		Shape tile = new RoundRectangle2D.Double(8, 8, S - 16, S - 16, 120, 120);
		g.setPaint(new GradientPaint(0, 0, new Color(0x1B2240), S, S, new Color(0x07080F)));
		g.fill(tile);
		g.setClip(tile);
		g.setPaint(new RadialGradientPaint(S / 2f, S / 2f, S * 0.48f, new float[]{0f, 1f},
			new Color[]{new Color(0x3A5BD9, false).darker(), new Color(0, 0, 0, 0)}));
		g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
		g.fillRect(0, 0, S, S);
		g.setComposite(AlphaComposite.SrcOver);

		// A few stars.
		int[][] stars = {{90, 110, 4}, {410, 90, 3}, {440, 380, 4}, {70, 400, 3}, {150, 60, 2}, {380, 450, 2}, {460, 230, 2}, {50, 250, 2}};
		for (int[] s : stars) {
			g.setColor(new Color(255, 255, 255, 200));
			g.fill(new Ellipse2D.Double(s[0] - s[2] / 2.0, s[1] - s[2] / 2.0, s[2], s[2]));
		}

		double cx = S / 2.0, cy = S / 2.0, r = 118;

		// Orbit ring, back half (behind the eclipse).
		AffineTransform tilt = AffineTransform.getRotateInstance(Math.toRadians(-22), cx, cy);
		Shape orbit = tilt.createTransformedShape(new Ellipse2D.Double(cx - 205, cy - 62, 410, 124));
		g.setStroke(new BasicStroke(7f));
		g.setColor(new Color(0x6E8CFF));
		Shape oldClip = g.getClip();
		g.clip(tilt.createTransformedShape(new Rectangle2D.Double(0, 0, S, cy)));
		g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
		g.draw(orbit);
		g.setComposite(AlphaComposite.SrcOver);
		g.setClip(oldClip);

		// Corona: layered glow fading outward.
		g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (r * 2.0), new float[]{0.45f, 0.52f, 0.7f, 1f},
			new Color[]{new Color(255, 255, 255, 255), new Color(190, 215, 255, 220), new Color(90, 130, 255, 90), new Color(40, 60, 200, 0)}));
		g.fill(new Ellipse2D.Double(cx - r * 2, cy - r * 2, r * 4, r * 4));

		// Corona streamers.
		g.setComposite(AlphaComposite.SrcOver.derive(0.35f));
		for (int i = 0; i < 12; i++) {
			double a = Math.toRadians(i * 30 + 8), len = r * (i % 2 == 0 ? 1.75 : 1.45);
			Path2D ray = new Path2D.Double();
			ray.moveTo(cx + Math.cos(a - 0.12) * r, cy + Math.sin(a - 0.12) * r);
			ray.lineTo(cx + Math.cos(a) * len, cy + Math.sin(a) * len);
			ray.lineTo(cx + Math.cos(a + 0.12) * r, cy + Math.sin(a + 0.12) * r);
			ray.closePath();
			g.setPaint(new GradientPaint((float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), new Color(220, 232, 255),
				(float) (cx + Math.cos(a) * len), (float) (cy + Math.sin(a) * len), new Color(120, 150, 255, 0)));
			g.fill(ray);
		}
		g.setComposite(AlphaComposite.SrcOver);

		// The moon: near-black disc, slightly offset so a thin bright rim shows on one side.
		g.setPaint(new RadialGradientPaint((float) (cx - 30), (float) (cy - 34), (float) r * 1.2f, new float[]{0f, 1f},
			new Color[]{new Color(0x1A1D2B), new Color(0x05060A)}));
		g.fill(new Ellipse2D.Double(cx - r + 6, cy - r + 5, r * 2 - 4, r * 2 - 4));

		// Diamond-ring flare on the rim.
		double fa = Math.toRadians(-48), fx = cx + Math.cos(fa) * (r + 2), fy = cy + Math.sin(fa) * (r + 2);
		g.setPaint(new RadialGradientPaint((float) fx, (float) fy, 46f, new float[]{0f, 0.25f, 1f},
			new Color[]{new Color(255, 255, 255, 255), new Color(200, 225, 255, 200), new Color(120, 160, 255, 0)}));
		g.fill(new Ellipse2D.Double(fx - 46, fy - 46, 92, 92));
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(fx - 40, fy, fx + 40, fy));
		g.draw(new Line2D.Double(fx, fy - 40, fx, fy + 40));

		// Orbit ring, front half, and the orbiting dot.
		g.setStroke(new BasicStroke(7f));
		g.clip(tilt.createTransformedShape(new Rectangle2D.Double(0, cy, S, S)));
		g.setPaint(new GradientPaint(0, (float) cy, new Color(0x9DB4FF), S, (float) cy, new Color(0x4F6BFF)));
		g.draw(orbit);
		g.setClip(oldClip);
		Point2D dot = tilt.transform(new Point2D.Double(cx - 205 * Math.cos(Math.toRadians(35)), cy + 62 * Math.sin(Math.toRadians(35))), null);
		g.setPaint(new RadialGradientPaint((float) dot.getX(), (float) dot.getY(), 28f, new float[]{0f, 1f},
			new Color[]{new Color(160, 190, 255, 200), new Color(80, 110, 255, 0)}));
		g.fill(new Ellipse2D.Double(dot.getX() - 28, dot.getY() - 28, 56, 56));
		g.setColor(Color.WHITE);
		g.fill(new Ellipse2D.Double(dot.getX() - 12, dot.getY() - 12, 24, 24));

		// Subtle inner border on the tile.
		g.setClip(null); // clips are in device space, so this also resets the scale's clip
		g.setStroke(new BasicStroke(4f));
		g.setColor(new Color(255, 255, 255, 28));
		g.draw(new RoundRectangle2D.Double(10, 10, S - 20, S - 20, 116, 116));
		g.dispose();

		File out = new File(args[0]);
		out.getParentFile().mkdirs();
		ImageIO.write(img, "png", out);
		System.out.println("Wrote " + out);
	}
}
