package ld35;

import com.jme3.asset.AssetManager;
import com.jme3.post.FilterPostProcessor;
//import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
//import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;
import javax.inject.Inject;
import jme3_ext.AppState0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * @author davidB
 */
public class AppStatePostProcessing extends AppState0 {
    Logger log = LoggerFactory.getLogger(AppStatePostProcessing.class);
    FilterPostProcessor fpp;

    @Inject
    public AppStatePostProcessing() {
    }
    @Override
    protected void doInitialize() {
        AssetManager assets = app.getAssetManager();
        AppSettings settings = app.getContext().getSettings();
        fpp = new FilterPostProcessor(assets);
        // Setup Bloom
        // --------------------------------------
        //BloomFilter bloom = new BloomFilter();
        //bloom.setEnabled(false);
        //bloom.setExposurePower(55);
        //bloom.setBloomIntensity(1.0f);
        //fpp.addFilter(bloom);
        // Setup FXAA only if regular AA is off
        // --------------------------------------
        // See if sampling is enabled
        boolean aa = settings.getSamples() != 0;
        log.info("antialias enabling : {}", aa);
        if (aa) {
            fpp.setNumSamples(settings.getSamples());
        }
        FXAAFilter fxaa = new FXAAFilter();
        fxaa.setEnabled(!aa);
        fpp.addFilter(fxaa); // And finally DoF
        // --------------------------------------
        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setEnabled(false);
        dof.setFocusDistance(5);
        dof.setFocusRange(192);
        fpp.addFilter(dof);
    }

    @Override
    protected void doDispose() {
        fpp.cleanup();
        fpp = null;
    }

    @Override
    protected void doEnable() {
        ViewPort viewport = app.getViewPort();
        viewport.addProcessor(fpp);
    }

    @Override
    protected void doDisable() {
        ViewPort viewport = app.getViewPort();
        viewport.removeProcessor(fpp);
    }

}
