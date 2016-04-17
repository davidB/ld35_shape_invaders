/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld35;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.debug.Grid;
import com.jme3x.jfx.FxPlatformExecutor;

import javafx.animation.FadeTransition;
import javafx.util.Duration;
import jme3_ext.AppState0;
import jme3_ext.Hud;
import jme3_ext.HudTools;
import jme3_ext.InputMapper;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class PageInGame extends AppState0 {
	static final float widthHalf = 10;
	static final float height = 150;
	static final float heightEndZone = 3;
	
	private final HudInGame hudController;
	private final HudTools hudTools;
	private final Commands controls;
	private final InputMapper inputMapper;
	private final PublishSubject<Pages> pm;
	private final ScheduledExecutorService scheduler;
	
	enum ShapeKind {
		cube, pyramid
	}
	
	private Hud<HudInGame> hud;
	private FadeTransition ftWave;
	private FadeTransition ftEnd;
	
	public final List<Spatial> enemies = new LinkedList<>();
	public final List<Spatial> bullets = new LinkedList<>();

	Subscription inputSub;
	Subscription ginputSub;
	final Node scene = new Node("scene");
	final Node player0 = new Node("player");
	
	int idCnt = 0;
	int waveCnt = 0;
	int cycleCnt = 0;
	boolean isRunning = true;
	float ttlWave = 1f;

	@Inject
	public PageInGame(HudInGame hudController, HudTools hudTools, Commands controls, InputMapper inputMapper, PublishSubject<Pages> pm, ScheduledExecutorService scheduler) {
		super();
		this.hudController = hudController;
		this.hudTools = hudTools;
		this.controls = controls;
		this.inputMapper = inputMapper;
		this.pm = pm;
		this.scheduler = scheduler;
	}

	@Override
	protected void doInitialize() {
		hud = hudTools.newHud("Interface/HudInGame.fxml", hudController);
		ftWave = new FadeTransition(Duration.millis(3000), hudController.text);
		ftWave.setFromValue(1.0);
		ftWave.setToValue(0.1);
		//ft.setCycleCount(Timeline.INDEFINITE);
		ftWave.setAutoReverse(false);
		ftEnd = new FadeTransition(Duration.millis(3000), hudController.text);
		ftEnd.setFromValue(1.0);
		ftEnd.setToValue(0.3);
		//ft.setCycleCount(Timeline.INDEFINITE);
		ftEnd.setAutoReverse(false);
	}

	@Override
	protected void doEnable() {
		hudTools.show(hud);
		app.getStateManager().attach(new AppStatePostProcessing());

		inputSub = Subscriptions.from(
			controls.exit.value.subscribe((v) -> {
				if (!v) pm.onNext(Pages.Welcome);
			})
		);
		ginputSub = Subscriptions.from(
				controls.action1.value.subscribe((v) -> player0.getControl(Control4FireBullet.class).spawnBullet = v)
				, controls.moveLeft.value.subscribe((v) -> {player0.getControl(Control4Motion.class).speedLeft = v * 8f;})
				, controls.moveRight.value.subscribe((v) -> {player0.getControl(Control4Motion.class).speedRight = v * 8f;})
				, controls.changeShape.value.subscribe((v) -> {togglePlayerShape(player0, Math.round(v));})
			);
		app.getInputManager().addRawInputListener(inputMapper.rawInputListener);
		//app.getInputManager().setCursorVisible(false);
		app.gainFocus(); //HACK
		spawnScene();
	}

	@Override
	protected void doDisable() {
		unspawnScene();
		//app.getInputManager().setCursorVisible(true);
		app.getInputManager().removeRawInputListener(inputMapper.rawInputListener);
		hudTools.hide(hud);
		if (inputSub != null){
			inputSub.unsubscribe();
			inputSub = null;
		}
		if (ginputSub != null){
			ginputSub.unsubscribe();
			ginputSub = null;
		}
		app.getStateManager().detach(app.getStateManager().getState(AppStatePostProcessing.class));
	}

	@Override
	protected void doUpdate(final float tpf) {
		if (!isRunning) return;
		if (enemies.isEmpty()) {
			if (ttlWave <= 0) {
				startWave();
				ttlWave = 2f;
			} else {
				ttlWave -= tpf;
			}
		} else {
			enemies.removeIf((e) ->{
				ShapeKind k = kindOf(e);
				boolean sameKind = false;
				for(int i=bullets.size() - 1; i > -1; i--) {
					Spatial b = bullets.get(i);
					if (e.getWorldTranslation().distanceSquared(b.getWorldTranslation()) < 1) {
						b.removeFromParent();
						sameKind = sameKind || k == kindOf(b);
						bullets.remove(i);
					}
				}
				if (sameKind) e.removeFromParent();
				return sameKind;
			});
			boolean end = false;
			for(Spatial e : enemies) {
				end = end || (e.getWorldTranslation().z > -heightEndZone);
			}
			if (end) gameOver();
		}
	}

	void spawnScene() {
		idCnt = 0;
		waveCnt = 0;
		cycleCnt = 0;
		isRunning = true;
		enemies.clear();
		bullets.clear();
		ttlWave = 1f;

		app.enqueue(()-> {
			player0.getChildren().forEach((s) -> s.removeFromParent());
			player0.getChildren().clear();
			scene.getChildren().forEach((s) -> s.removeFromParent());
			scene.getChildren().clear();
			scene.attachChild(makePlayer());
			scene.attachChild(makeEnvironment());
			app.getRootNode().attachChild(scene);
			return true;
		});
	}

	void unspawnScene() {
		app.enqueue(()-> {
			scene.removeFromParent();
			return true;
		});
	}

	ShapeKind kindOf(Spatial s) {
		String kStr = s.getUserData("kind");
		if (kStr == null) {
			kStr = ShapeKind.cube.name();
		}
		return ShapeKind.valueOf(kStr);
	}
	
	ShapeKind kindOf(Spatial s, ShapeKind k) {
		s.setUserData("kind", k.name());
		return k;
	}

	void togglePlayerShape(Node player, int dir) {
		app.enqueue(()-> {
			ShapeKind k = kindOf(player0);
			int l = ShapeKind.values().length;
			k = ShapeKind.values()[(k.ordinal() + dir + l) % l];
			player.detachChildNamed("shape");
			Spatial shape = makeShape(k);
			shape.setName("shape");
			player.attachChild(shape);
			kindOf(player, k);
			return true;
		});
	}

	Spatial makePlayer() {
		Node root = player0;
		root.removeControl(Control4Motion.class);
		root.removeControl(Control4FireBullet.class);
		togglePlayerShape(root, 0);
		//Spatial g = app.getAssetManager().loadModel("Models/player.xbuf");
		//Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		//mat.setColor("Color", ColorRGBA.Red);
		//g.setMaterial(mat);
		//root.attachChild(g);
		root.setLocalTranslation(0, 0, 0);
		root.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
		root.addControl(new Control4Motion());
		root.addControl(new Control4FireBullet());
		return root;
	}

	Spatial makeShape(ShapeKind kind) {
		Spatial g = app.getAssetManager().loadModel("Models/" + kind + ".xbuf");
		g.setName(kind.name() + (idCnt++));
		kindOf(g, kind);
		return g;
	}

	Spatial makeBullet(ShapeKind kind, boolean top) {
		Spatial root = makeShape(kind);
		root.setLocalScale(0.2f);
		root.setName("bullet-" + root.getName());
		root.addControl(new Control4Bullet());
		return root;
	}

	void gameOver() {
		isRunning = false;
		if (ginputSub != null){
			ginputSub.unsubscribe();
			ginputSub = null;
		}
		displayGameOver();
		scheduler.schedule(()->{
			app.enqueue(()->{
				pm.onNext(Pages.Welcome);
			});
		}, 3, TimeUnit.SECONDS);
	}

	void displayGameOver() {
		FxPlatformExecutor.runOnFxApplication(() -> {
			String txt = "Game Over";
			System.err.println(txt);
			hudController.text.setText(txt);
			ftEnd.play();
		});
	}
	void startWave() {
		waveCnt++;
		cycleCnt = Math.floorDiv(waveCnt, Wave.waves.length);
		enemies.clear();
		bullets.clear();
		displayWave();
		generateWave();
	}
	
	void displayWave() {
		FxPlatformExecutor.runOnFxApplication(() -> {
			String txt = "Wave " + cycleCnt + "." + ((waveCnt % Wave.waves.length) + 1);
			System.err.println(txt);
			hudController.text.setText(txt);
			ftWave.play();
		});
	}

	void generateWave() {
		Wave wave = Wave.waves[waveCnt % Wave.waves.length];
		float speed = cycleCnt * wave.baseSpeed;
		float z0 = -height;
		float stepX = widthHalf * 2f / wave.width;
		float stepZ = 5;
		int zlg = wave.layout.length / wave.width;
		for(int zi = 0; zi <zlg; zi++) {
			for(int xi = 0; xi < wave.width; xi++) {
				int i = xi + zi * wave.width;
				int ki = wave.layout[i];
				if (ki > -1) {
					ShapeKind k = ShapeKind.values()[ki % ShapeKind.values().length];
					Spatial e = makeShape(k);
					e.setLocalTranslation(-widthHalf + (xi + 0.5f) * stepX, 0, z0 - (zi * stepZ));
					e.addControl(new Control4Enemy(speed));
					enemies.add(e);
					scene.attachChild(e);
				}
			}
		}
	}

	Spatial makeEnvironment() {
		Node root = new Node("environment");
		root.attachChild(makeGrid(new Vector3f(0, -1, -1 * (height /2) - heightEndZone), (int)height, (int)(widthHalf * 2 + 2), ColorRGBA.LightGray));
		root.attachChild(makeGrid(new Vector3f(0, -1, -1 * heightEndZone + 0.6f), (int)heightEndZone, (int)(widthHalf * 2 + 2), ColorRGBA.Red));

		CameraNode camn = new CameraNode("camera", app.getCamera());
		//camn.setLocalTranslation(new Vector3f(0,10,17));
		camn.setLocalTranslation(new Vector3f(0,16,15));
		camn.lookAt(new Vector3f(0,0,-14), new Vector3f(0, 1, 0)); //TODO try with Vector3f.UNIT_Y 
		root.attachChild(camn);

		return root;
	}

	Spatial makeGrid(Vector3f pos, int nbX, int nbY, ColorRGBA color){
		Geometry g = new Geometry("wireframe grid", new Grid(nbX, nbY, 1.0f) );
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", color);
		g.setMaterial(mat);
		g.center().move(pos);
		return g;
	}

	class Control4Motion extends AbstractControl {
		public float speedRight = 0f;
		public float speedLeft = 0f;
		Vector3f tmp = new Vector3f(0, 0, 0);
		@Override
		protected void controlUpdate(float tpf) {
			Vector3f pos = getSpatial().getLocalTranslation();
			pos.x += (speedRight - speedLeft) * cycleCnt *tpf;
			pos.x = Math.max(-widthHalf, Math.min(widthHalf, pos.x));
			getSpatial().setLocalTranslation(pos);
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}

	class Control4FireBullet extends AbstractControl {
		public boolean spawnBullet = false;
		public float lastShoot = 0;

		@Override
		protected void controlUpdate(float tpf) {
			if (spawnBullet) {
				if (lastShoot <= 0) {
					Spatial bullet = makeBullet(kindOf(player0), true);
					Vector3f pos = player0.getLocalTranslation();
					bullet.setLocalTranslation(pos.x, pos.y, pos.z);
					scene.attachChild(bullet);
					bullets.add(bullet);
					lastShoot = 0.1f;
				} else {
					lastShoot -= tpf;
				}
			} else {
				lastShoot = 0;
			}
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}

	class Control4Bullet extends AbstractControl {
		public float ttl = 8f;
		public float speedZ = -height / 2f;

		@Override
		protected void controlUpdate(float tpf) {
			ttl -= tpf;
			if (ttl <= 0 ) {
				getSpatial().removeFromParent();
				bullets.remove(getSpatial()); //TODO optimize
			}
			Vector3f pos = getSpatial().getLocalTranslation();
			pos.z += speedZ * tpf;
			getSpatial().setLocalTranslation(pos);
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}
	
	class Control4Enemy extends AbstractControl {
		public float speedZ = 2f;

		public Control4Enemy(float zs) {
			speedZ = zs;
		}

		@Override
		protected void controlUpdate(float tpf) {
			Vector3f pos = getSpatial().getLocalTranslation();
			pos.z += speedZ * tpf;
			getSpatial().setLocalTranslation(pos);
		}

		@Override
		protected void controlRender(RenderManager rm, ViewPort vp) {
		}

	}
}
