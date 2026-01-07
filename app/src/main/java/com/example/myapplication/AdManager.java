package com.example.myapplication;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

/**
 * AdManager class for handling Google AdMob ads
 * Manages Banner and Interstitial ads
 */
public class AdManager {
    private static final String TAG = "AdManager";
    
    // Official Google AdMob Test IDs (for testing only)
    // Banner Ad Unit ID (Test)
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    
    // Interstitial Ad Unit ID (Test)
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    
    private static AdManager instance;
    private InterstitialAd interstitialAd;
    private boolean isInitialized = false;
    
    private AdManager() {
    }
    
    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }
    
    /**
     * Initialize AdMob SDK
     * Should be called in Application class or MainActivity onCreate
     * @param callback Optional callback to notify when initialization is complete
     */
    public void initialize(Activity activity) {
        initialize(activity, null);
    }
    
    /**
     * Initialize AdMob SDK with callback
     */
    public void initialize(Activity activity, Runnable onInitialized) {
        if (isInitialized) {
            if (onInitialized != null) {
                onInitialized.run();
            }
            return;
        }
        
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.d(TAG, "AdMob initialization completed");
                isInitialized = true;
                // Load interstitial ad after initialization
                loadInterstitialAd(activity);
                // Notify callback
                if (onInitialized != null) {
                    onInitialized.run();
                }
            }
        });
    }
    
    /**
     * Load and display Banner Ad
     * @param activity The activity context
     * @param adContainer The LinearLayout container where the ad will be displayed
     */
    public void loadBannerAd(Activity activity, LinearLayout adContainer) {
        if (adContainer == null) {
            Log.e(TAG, "Ad container is null");
            return;
        }
        
        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized. Initializing now...");
            initialize(activity, () -> {
                // Retry loading ad after initialization
                loadBannerAd(activity, adContainer);
            });
            return;
        }
        
        // Make container visible immediately (will show loading state)
        adContainer.setVisibility(View.VISIBLE);
        
        // Remove any existing views
        adContainer.removeAllViews();
        
        AdView adView = new AdView(activity);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
        
        // Set up ad listener to handle ad loading events
        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
                adContainer.setVisibility(View.VISIBLE);
                // Scroll to ad if needed (optional)
                activity.runOnUiThread(() -> {
                    // Ad is now visible and loaded
                });
            }
            
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
                Log.e(TAG, "Error code: " + loadAdError.getCode());
                Log.e(TAG, "Error domain: " + loadAdError.getDomain());
                // Keep container visible but empty (or hide it)
                // For testing, you might want to keep it visible to see the container
                // adContainer.setVisibility(View.GONE);
            }
            
            @Override
            public void onAdOpened() {
                Log.d(TAG, "Banner ad opened");
            }
            
            @Override
            public void onAdClosed() {
                Log.d(TAG, "Banner ad closed");
            }
        });
        
        // Add adView to container
        adContainer.addView(adView);
        
        // Create ad request
        AdRequest adRequest = new AdRequest.Builder().build();
        
        // Load ad
        adView.loadAd(adRequest);
        
        Log.d(TAG, "Banner ad loading...");
    }
    
    /**
     * Load Interstitial Ad
     * @param activity The activity context
     */
    public void loadInterstitialAd(Activity activity) {
        if (!isInitialized) {
            Log.w(TAG, "AdMob not initialized. Call initialize() first.");
            return;
        }
        
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) {
                    interstitialAd = ad;
                    Log.d(TAG, "Interstitial ad loaded successfully");
                }
                
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                    interstitialAd = null;
                }
            });
    }
    
    /**
     * Show Interstitial Ad if loaded
     * @param activity The activity context
     * @return true if ad was shown, false if ad was not available
     */
    public boolean showInterstitialAd(Activity activity) {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            interstitialAd = null; // Clear after showing
            // Load next interstitial ad
            loadInterstitialAd(activity);
            return true;
        } else {
            Log.d(TAG, "Interstitial ad not ready. Loading new ad...");
            loadInterstitialAd(activity);
            return false;
        }
    }
    
    /**
     * Show Interstitial Ad with callback for when ad is closed
     * @param activity The activity context
     * @param onAdClosed Runnable to execute when ad is closed
     * @return true if ad was shown, false if ad was not available
     */
    public boolean showInterstitialAd(Activity activity, Runnable onAdClosed) {
        if (interstitialAd != null) {
            // Set up ad closed listener
            interstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed");
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                    interstitialAd = null;
                    // Load next interstitial ad
                    loadInterstitialAd(activity);
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                    if (onAdClosed != null) {
                        onAdClosed.run();
                    }
                    interstitialAd = null;
                    loadInterstitialAd(activity);
                }
            });
            
            interstitialAd.show(activity);
            return true;
        } else {
            Log.d(TAG, "Interstitial ad not ready. Executing callback immediately...");
            if (onAdClosed != null) {
                onAdClosed.run();
            }
            loadInterstitialAd(activity);
            return false;
        }
    }
    
    /**
     * Check if interstitial ad is loaded and ready
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }
    
    /**
     * Get Banner Ad Unit ID (for reference)
     */
    public String getBannerAdUnitId() {
        return BANNER_AD_UNIT_ID;
    }
    
    /**
     * Get Interstitial Ad Unit ID (for reference)
     */
    public String getInterstitialAdUnitId() {
        return INTERSTITIAL_AD_UNIT_ID;
    }
}

