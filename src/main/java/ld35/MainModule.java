/// License [CC0](http://creativecommons.org/publicdomain/zero/1.0/)
package ld35;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import org.eclipse.xtext.xbase.lib.Functions.Function1;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.system.AppSettings;
import com.jme3x.jfx.FxPlatformExecutor;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import jme3_ext.AppSettingsLoader;
import jme3_ext.InputMapper;
import jme3_ext.InputMapperHelpers;
import jme3_ext.PageManager;
import rx.Observer;
import rx.subjects.PublishSubject;

class MainModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(1));
	}

	@Provides
	public AppSettingsLoader appSettingsLoader() {
		return new AppSettingsLoader() {
			final String prefKey = "" + this.getClass().getCanonicalName();

			@Override
			public AppSettings loadInto(AppSettings settings) throws Exception{
				settings.load(prefKey);
				return settings;
			}

			@Override
			public AppSettings save(AppSettings settings) throws Exception{
				settings.save(prefKey);
				return settings;
			}
		};
	}

	@Singleton
	@Provides
	public SimpleApplication simpleApplication(AppSettings appSettings) {
		//HACK
		final CountDownLatch initializedSignal = new CountDownLatch(1);
		SimpleApplication app = new SimpleApplication(){
			@Override
			public void simpleInitApp() {
				initializedSignal.countDown();
			}

			@Override
			public void destroy() {
				super.destroy();
				FxPlatformExecutor.runOnFxApplication(() -> {
					Platform.exit();
				});
			}
		};
		app.setSettings(appSettings);
		app.setShowSettings(false);
		app.setDisplayStatView(true);
		app.setDisplayFps(true);
		app.start();
		try {
			initializedSignal.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return app;
	}

	@Singleton
	@Provides
	public AppSettings appSettings(AppSettingsLoader appSettingsLoader, ResourceBundle resources) {
		AppSettings settings = new AppSettings(true);
		try {
			settings = appSettingsLoader.loadInto(settings);
		} catch (Exception e) {
			e.printStackTrace();
		}
		settings.setTitle(resources.getString("title"));
		settings.setUseJoysticks(true);
		settings.setGammaCorrection(true);
		settings.setRenderer(AppSettings.JOGL_OPENGL_BACKWARD_COMPATIBLE);
		settings.setAudioRenderer(AppSettings.JOAL);
		//settings.setRenderer(AppSettings.LWJGL_OPENGL3);
		settings.setFrameRate(60);
		settings.setFrequency(60);
		return settings;
	}

	@Singleton
	@Provides
	//@Named("pageRequests")
	public PublishSubject<Pages> pageRequests() {
		return PublishSubject.create();
	}

	@Singleton
	@Provides
	public PageManager<Pages> pageManager(SimpleApplication app, PublishSubject<Pages> pageRequests, PageWelcome pageWelcome,/* PageSettings pageSettings,*/ PageInGame pageInGame) {
		PageManager<Pages> pageManager = new PageManager<>(app.getStateManager());
		pageManager.pages.put(Pages.Welcome, pageWelcome);
		pageManager.pages.put(Pages.InGame, pageInGame);
		//pageManager.pages.put(Pages.Settings, pageSettings);
		pageRequests.subscribe((p) -> pageManager.goTo(p));
		return pageManager;
	}

	@Singleton
	@Provides
	public Locale locale() {
		return Locale.getDefault();
	}

	@Provides
	public ResourceBundle resources(Locale locale) {
		return ResourceBundle.getBundle("Interface.labels", locale);
	}

	@Provides
	public FXMLLoader fxmlLoader(ResourceBundle resources) {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setResources(resources);
		fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
		return fxmlLoader;
	}

	@Provides
	@Singleton
	public InputMapper inputMapper(Commands controls) {
		//TODO save / restore mapper, until then harcoded mapping
		InputMapper m = new InputMapper();
		mapKey(m, KeyInput.KEY_ESCAPE, controls.exit.value);
		// arrow
		mapKey(m, KeyInput.KEY_UP, controls.changeShape.value, 1);
		mapKey(m, KeyInput.KEY_DOWN, controls.changeShape.value, -1);
		mapKey(m, KeyInput.KEY_RIGHT, controls.moveRight.value, 1);
		mapKey(m, KeyInput.KEY_LEFT, controls.moveLeft.value, 1);
		// WASD / ZQSD
		//if (InputMapperHelpers.isKeyboardAzerty()) {
			mapKey(m, KeyInput.KEY_Z, controls.changeShape.value, 1);
			//InputMapperHelpers.mapKey(m, KeyInput.KEY_S, controls.changeShape.value, false);
			mapKey(m, KeyInput.KEY_Q, controls.moveLeft.value, 1);
			//InputMapperHelpers.mapKey(m, KeyInput.KEY_D, controls.moveRight.value, true);
		//} else {
			mapKey(m, KeyInput.KEY_W, controls.changeShape.value, 1);
			mapKey(m, KeyInput.KEY_S, controls.changeShape.value, -1);
			mapKey(m, KeyInput.KEY_A, controls.moveLeft.value, 1);
			mapKey(m, KeyInput.KEY_D, controls.moveRight.value, 1);
			/*
			m.map(InputMapperHelpers.tmplMouseMotionEvent(), (MouseMotionEvent evt) -> {
				controls.moveLeft.value.onNext(Math.max(0, -1 * Math.signum((float)evt.getDX())));
				controls.moveRight.value.onNext(Math.max(0, Math.signum((float)evt.getDX())));
				return Math.signum((float)evt.getDeltaWheel());
			}, controls.changeShape.value);
			m.map(InputMapperHelpers.tmplMouseButtonEvent(0), (MouseButtonEvent evt) -> evt.isPressed(), controls.action1.value);
			*/
		//}
		// actions
		mapKey(m, KeyInput.KEY_SPACE, controls.action1.value);
		return m;
	}

	public static void mapKey(final InputMapper m, final int keyCode, final Observer<Float> dest, float v) {
		m.map(InputMapperHelpers.tmplKeyInputEvent(keyCode), (KeyInputEvent evt) -> {
			return (evt.isPressed() || evt.isRepeating())? v: 0f; 
			}, dest);
	}
	public static void mapKey(final InputMapper m, final int keyCode, final Observer<Boolean> dest) {
		KeyInputEvent _tmplKeyInputEvent = InputMapperHelpers.tmplKeyInputEvent(keyCode);
		m.<KeyInputEvent, Boolean>map(_tmplKeyInputEvent, (KeyInputEvent evt) -> (evt.isPressed() || evt.isRepeating()), dest);
	}
}
